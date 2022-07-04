package io.jenkins.plugins.azurecosmosdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.jvnet.hudson.test.LoggerRule.recorded;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.implementation.RxDocumentClientImpl;
import java.util.logging.Level;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.FlagRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;

public class AzureCosmosDBCacheEvictedIT extends BaseIntegrationTest {

  public JenkinsRule j = new JenkinsRule();

  public TestRule flagRule =
      FlagRule.systemProperty(
          "io.jenkins.plugins.azurecosmosdb.AzureCosmosDBCache.MAX_CACHE_SIZE", "1");

  public LoggerRule loggerRule =
      new LoggerRule().record(RxDocumentClientImpl.class, Level.INFO).capture(100);

  @Rule public RuleChain chain = RuleChain.outerRule(flagRule).around(j).around(loggerRule);

  @Before
  public void before() {
    AzureCosmosDBCache.invalidateCache();
  }

  @Test
  public void keyClientsAreCached() throws InterruptedException {
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
    assertThat(loggerRule, recorded(Level.WARNING, containsString("Already shutdown!")));
  }

  @Test
  public void keyClientsAreCachedAndAlreadyClosedClientsAreIgnored() throws InterruptedException {
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
    assertThat(loggerRule, recorded(Level.WARNING, containsString("Already shutdown!")));
  }

  private void waitForEvictionListenerToRun() throws InterruptedException {
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
