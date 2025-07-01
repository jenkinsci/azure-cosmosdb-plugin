package io.jenkins.plugins.azurecosmosdb;

import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.jvnet.hudson.test.JenkinsMatchers.hasPlainText;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.casc.CredentialsRootConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void importConfig(JenkinsConfiguredWithCodeRule r) {
        SystemCredentialsProvider instance = SystemCredentialsProvider.getInstance();
        List<Credentials> credentials = instance.getCredentials();
        assertThat(credentials, hasSize(2));

        Credentials keyCredentials = credentials.get(0);
        assertThat(keyCredentials, instanceOf(AzureCosmosDBKeyCredentials.class));
        AzureCosmosDBKeyCredentials azureCosmosDBKeyCredentials = (AzureCosmosDBKeyCredentials) keyCredentials;

        assertThat(azureCosmosDBKeyCredentials.getKey(), hasPlainText("abcd"));

        Credentials cosmosDbCredentials = credentials.get(1);
        assertThat(cosmosDbCredentials, instanceOf(AzureCosmosDBCredentials.class));

        AzureCosmosDBCredentials azureCosmosDBCredentials = (AzureCosmosDBCredentials) cosmosDbCredentials;
        assertThat(azureCosmosDBCredentials.getCredentialsId(), equalTo("key-credential-id"));
        assertThat(azureCosmosDBCredentials.getPreferredRegion(), equalTo("UK South"));
        assertThat(azureCosmosDBCredentials.getUrl(), equalTo("https://your-account-name.documents.azure.com:443/"));
    }

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void exportConfig(JenkinsConfiguredWithCodeRule r) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getCredentialsRoot(context).get("system");

        String exported = toYamlString(yourAttribute)
                // secret export is not deterministic, replace it with the expected string
                .replaceAll("key: \".+\"", "key: \"abcd\"");

        String expected = toStringFromYamlFile(this, "expected-output.yaml");

        assertThat(exported, is(expected));
    }

    private static Mapping getCredentialsRoot(ConfigurationContext context) throws Exception {
        CredentialsRootConfigurator unclassifiedConfigurator = new CredentialsRootConfigurator();
        return Objects.requireNonNull(unclassifiedConfigurator.describe(
                        unclassifiedConfigurator.getTargetComponent(context), context))
                .asMapping();
    }
}
