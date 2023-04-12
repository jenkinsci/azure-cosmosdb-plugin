package io.jenkins.plugins.azurecosmosdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.azure.cosmos.CosmosClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AzureCosmosDBCacheIT extends BaseIntegrationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void before() {
        AzureCosmosDBCache.invalidateCache();
    }

    @Test
    public void keyClientsAreCached() {
        String credentialsId = loadValidCredentials();
        AzureCosmosDBCache.get(credentialsId, null);
        CosmosClient cosmosClient = AzureCosmosDBCache.get(credentialsId, null);
        cosmosClient.close();

        assertThat(AzureCosmosDBCache.cacheSize(), equalTo(1L));
    }

    @Test
    public void keyAndSpClientsCacheMiss() {
        String credentialsId = loadValidCredentials();
        String spCredentialsId = loadServicePrincipalCredentials();
        AzureCosmosDBCache.get(credentialsId, null);
        AzureCosmosDBCache.get(spCredentialsId, null);

        assertThat(AzureCosmosDBCache.cacheSize(), equalTo(2L));
    }

    @Test
    public void spClientsAreCached() {
        String spCredentialsId = loadServicePrincipalCredentials();
        AzureCosmosDBCache.get(spCredentialsId, null);
        AzureCosmosDBCache.get(spCredentialsId, null);

        assertThat(AzureCosmosDBCache.cacheSize(), equalTo(1L));
    }
}
