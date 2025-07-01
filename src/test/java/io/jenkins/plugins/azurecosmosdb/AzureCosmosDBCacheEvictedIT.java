package io.jenkins.plugins.azurecosmosdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.jvnet.hudson.test.LogRecorder.recorded;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.implementation.RxDocumentClientImpl;
import java.util.logging.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LogRecorder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@SetSystemProperty(key = "io.jenkins.plugins.azurecosmosdb.AzureCosmosDBCache.MAX_CACHE_SIZE", value = "1")
class AzureCosmosDBCacheEvictedIT extends BaseIntegrationTest {

    private final LogRecorder recorder =
            new LogRecorder().record(RxDocumentClientImpl.class, Level.INFO).capture(100);

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
        AzureCosmosDBCache.invalidateCache();
    }

    @Test
    void keyClientsAreCached() throws Exception {
        String credentialsId = loadValidCredentials();
        AzureCosmosDBCache.get(credentialsId, null);
        CosmosClient cosmosClient = AzureCosmosDBCache.get(credentialsId, null);

        String spCredentialsId = loadServicePrincipalCredentials();
        AzureCosmosDBCache.get(spCredentialsId, null);

        // Can't tell directly if it has been closed, but it does log that it has been closed so force
        // another one.
        cosmosClient.close();

        waitForEvictionListenerToRun();

        assertThat(AzureCosmosDBCache.cacheSize(), equalTo(1L));
        assertThat(recorder, recorded(Level.WARNING, containsString("Already shutdown!")));
    }

    @Test
    void keyClientsAreCachedAndAlreadyClosedClientsAreIgnored() throws Exception {
        String credentialsId = loadValidCredentials();
        AzureCosmosDBCache.get(credentialsId, null);
        CosmosClient cosmosClient = AzureCosmosDBCache.get(credentialsId, null);

        String spCredentialsId = loadServicePrincipalCredentials();
        AzureCosmosDBCache.get(spCredentialsId, null);

        // Can't tell directly if it has been closed, but it does log that it has been closed so force
        // another one.
        cosmosClient.close();

        waitForEvictionListenerToRun();

        assertThat(AzureCosmosDBCache.cacheSize(), equalTo(1L));
        assertThat(recorder, recorded(Level.WARNING, containsString("Already shutdown!")));
    }

    private void waitForEvictionListenerToRun() throws Exception {
        long attempts = 0;
        while (AzureCosmosDBCache.cacheSize() != 1L) {
            Thread.sleep(50L);
            attempts++;
            if (attempts > 20) {
                break;
            }
        }
    }
}
