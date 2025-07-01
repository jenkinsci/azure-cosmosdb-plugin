package io.jenkins.plugins.azurecosmosdb;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.jvnet.hudson.test.JenkinsMatchers.hasKind;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AzureCosmosDBCredentialsImplIT extends BaseIntegrationTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void doTestConnectionAdminValidatesOk() {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(realm);
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor = new AzureCosmosDBCredentialsImpl.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            FormValidation validation = descriptor.doTestConnection("key", "UK South", COSMOS_URL, null);

            assertThat(validation, hasKind(FormValidation.Kind.OK));
            assertThat(validation.getMessage(), containsString("Found "));
        }
    }

    @Test
    void doTestConnectionWithServicePrincipalAndAdminValidatesOk() {
        loadServicePrincipalCredentials();
        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(realm);
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor = new AzureCosmosDBCredentialsImpl.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            FormValidation validation = descriptor.doTestConnection("sp", "UK South", COSMOS_URL, null);

            assertThat(validation, hasKind(FormValidation.Kind.OK));
            assertThat(validation.getMessage(), containsString("Found "));
        }
    }

    /**
     * This could almost be a regular IT without real credentials but if the URL doesn't exist this
     * test takes over 1 minute because it retries a lot with backoff, and I couldn't find any
     * configuration for it.
     */
    @Test
    void doTestConnectionAdminInvalidKeyAndAccountErrors() {
        List<Credentials> credentials = SystemCredentialsProvider.getInstance().getCredentials();
        credentials.add(
                new AzureCosmosDBKeyCredentialsImpl("key", null, Secret.fromString("dGhpc2lzbm90YXBhc3N3b3JkCg==")));

        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(realm);
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor = new AzureCosmosDBCredentialsImpl.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            FormValidation validation = descriptor.doTestConnection("key", "UK South", COSMOS_URL, null);

            assertThat(validation, hasKind(FormValidation.Kind.ERROR));
            assertThat(validation.getMessage(), containsString("Failed to validate credentials"));
        }
    }

    private void loadCredentials() {
        List<Credentials> credentials = SystemCredentialsProvider.getInstance().getCredentials();
        credentials.add(new AzureCosmosDBKeyCredentialsImpl("key", null, Secret.fromString(COSMOS_KEY)));
    }
}
