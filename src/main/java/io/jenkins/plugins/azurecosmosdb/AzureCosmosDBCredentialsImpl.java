package io.jenkins.plugins.azurecosmosdb;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.either;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;
import static io.jenkins.plugins.azurecosmosdb.CredentialsHelper.findCredentials;

import com.azure.cosmos.CosmosClient;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.microsoft.azure.util.AzureBaseCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AzureCosmosDBCredentialsImpl extends BaseStandardCredentials
    implements AzureCosmosDBCredentials {

  private final String credentialsId;
  private final String preferredRegion;
  private final String url;

  @DataBoundConstructor
  public AzureCosmosDBCredentialsImpl(
      @CheckForNull CredentialsScope scope,
      String id,
      String description,
      String credentialsId,
      String preferredRegion,
      String url) {
    super(scope, id, description);
    this.credentialsId = credentialsId;
    this.preferredRegion = preferredRegion;
    this.url = url;
  }

  @Override
  public String getCredentialsId() {
    return credentialsId;
  }

  @Override
  public String getPreferredRegion() {
    return preferredRegion;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Extension
  public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
    @Override
    public String getDisplayName() {
      return "Azure Cosmos DB";
    }

    @POST
    public ListBoxModel doFillCredentialsIdItems(
        @AncestorInPath Item item, @QueryParameter String credentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();
      if (item == null) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
          return result.includeCurrentValue(credentialsId);
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return result.includeCurrentValue(credentialsId);
        }
      }
      return result
          .includeEmptyValue()
          .includeMatchingAs(
              ACL.SYSTEM,
              item,
              StandardCredentials.class,
              Collections.emptyList(),
              either(
                  instanceOf(AzureCosmosDBKeyCredentials.class),
                  instanceOf(AzureBaseCredentials.class)))
          .includeCurrentValue(credentialsId);
    }

    @POST
    public FormValidation doTestConnection(
        @QueryParameter String credentialsId,
        @QueryParameter String preferredRegion,
        @QueryParameter String url,
        @AncestorInPath Item item) {

      if (item == null) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
          return FormValidation.ok();
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return FormValidation.ok();
        }
      }
      if (StringUtils.isBlank(credentialsId)) {
        return FormValidation.ok();
      }

      StandardCredentials credentials = findCredentials(credentialsId, item);

      if (credentials == null) {
        return FormValidation.error("Cannot find currently selected credentials");
      }

      try (CosmosClient client =
          CredentialsHelper.createClient(credentials, preferredRegion, url)) {
        return FormValidation.ok(
            "Found " + client.readAllDatabases().stream().count() + " database(s).");
      } catch (RuntimeException e) {
        return FormValidation.error(e, "Failed to validate credentials: " + e.getMessage());
      }
    }
  }
}
