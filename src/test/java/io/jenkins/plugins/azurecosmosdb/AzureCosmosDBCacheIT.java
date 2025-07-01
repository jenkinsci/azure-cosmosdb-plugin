package io.jenkins.plugins.azurecosmosdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.azure.cosmos.CosmosClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AzureCosmosDBCacheIT extends BaseIntegrationTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
        AzureCosmosDBCache.invalidateCache();
    }

    @Test
    void keyClientsAreCached() {
        String credentialsId = loadValidCredentials();
        AzureCosmosDBCache.get(credentialsId, null);
        CosmosClient cosmosClient = AzureCosmosDBCache.get(credentialsId, null);
        cosmosClient.close();

        assertThat(AzureCosmosDBCache.cacheSize(), equalTo(1L));
    }

    @Test
    void keyAndSpClientsCacheMiss() {
        String credentialsId = loadValidCredentials();
        String spCredentialsId = loadServicePrincipalCredentials();
        AzureCosmosDBCache.get(credentialsId, null);
        AzureCosmosDBCache.get(spCredentialsId, null);

        assertThat(AzureCosmosDBCache.cacheSize(), equalTo(2L));
    }

    @Test
    void spClientsAreCached() {
        String spCredentialsId = loadServicePrincipalCredentials();
        AzureCosmosDBCache.get(spCredentialsId, null);
        AzureCosmosDBCache.get(spCredentialsId, null);

        assertThat(AzureCosmosDBCache.cacheSize(), equalTo(1L));
    }
}
