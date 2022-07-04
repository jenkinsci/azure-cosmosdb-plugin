package io.jenkins.plugins.azurecosmosdb;

import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

public class AzureCosmosDBKeyCredentialsImpl extends BaseStandardCredentials
    implements AzureCosmosDBKeyCredentials {

  private final Secret key;

  @DataBoundConstructor
  public AzureCosmosDBKeyCredentialsImpl(String id, String description, Secret key) {
    super(id, description);
    this.key = key;

    AzureCosmosDBCache.invalidateCache();
  }

  @Override
  public Secret getKey() {
    return key;
  }

  @Extension
  public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

    @Override
    public String getDisplayName() {
      return "Azure Cosmos DB Key";
    }
  }
}
