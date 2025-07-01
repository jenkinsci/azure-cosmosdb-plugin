package io.jenkins.plugins.azurecosmosdb;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.SnippetizerTester;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AzureCosmosDBCreateDocumentStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void doFillCredentialsIdItemsNoItemNoAdmin() {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(realm);
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Job.CONFIGURE, Item.DISCOVER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCreateDocumentStep.DescriptorImpl descriptor =
                new AzureCosmosDBCreateDocumentStep.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(null, "cosmos-connection");

            assertCannotSeeCredentials(listBoxModel);
        }
    }

    @Test
    void doFillCredentialsIdItemsNoItemHasAdminFindsCredentials() {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(realm);
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCreateDocumentStep.DescriptorImpl descriptor =
                new AzureCosmosDBCreateDocumentStep.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(null, "cosmos-connection");

            assertFindsCredentials(listBoxModel);
        }
    }

    private void assertFindsCredentials(ListBoxModel listBoxModel) {
        assertThat(listBoxModel, hasSize(3));
        ListBoxModel.Option current = listBoxModel.get(0);
        assertThat(current.name, equalTo("- none -"));
        assertThat(current.value, equalTo(""));

        ListBoxModel.Option key = listBoxModel.get(1);
        assertThat(key.name, equalTo("cosmos-connection"));
        assertThat(key.value, equalTo("cosmos-connection"));

        ListBoxModel.Option key2 = listBoxModel.get(2);
        assertThat(key2.name, equalTo("cosmos-connection2"));
        assertThat(key2.value, equalTo("cosmos-connection2"));
    }

    @Test
    void doFillCredentialsIdItemsWithItemNoCredentialsAccess() throws Exception {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(realm);
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Item.DISCOVER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCreateDocumentStep.DescriptorImpl descriptor =
                new AzureCosmosDBCreateDocumentStep.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline");
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(job, "cosmos-connection");

            assertCannotSeeCredentials(listBoxModel);
        }
    }

    @Test
    void doFillCredentialsIdItemsWithItemHasUseItemHasCredentialsAccess() throws Exception {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(realm);
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(CredentialsProvider.USE_ITEM, Item.DISCOVER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCreateDocumentStep.DescriptorImpl descriptor =
                new AzureCosmosDBCreateDocumentStep.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline");
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(job, "cosmos-connection");

            assertFindsCredentials(listBoxModel);
        }
    }

    private void assertCannotSeeCredentials(ListBoxModel listBoxModel) {
        assertThat(listBoxModel, hasSize(1));
        ListBoxModel.Option option = listBoxModel.get(0);
        assertThat(option.name, equalTo("- current -"));
        assertThat(option.value, equalTo("cosmos-connection"));
    }

    @Test
    void doFillCredentialsIdItemsWithItemFindsCredential() throws Exception {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(realm);
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Job.CONFIGURE, Item.DISCOVER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCreateDocumentStep.DescriptorImpl descriptor =
                new AzureCosmosDBCreateDocumentStep.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline");
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(job, "cosmos-connection");

            assertFindsCredentials(listBoxModel);
        }
    }

    @Test
    void configRoundTrip() throws Exception {
        AzureCosmosDBCreateDocumentStep step =
                new AzureCosmosDBCreateDocumentStep("cosmos-connection", "jenkins", "jenkins", "{ \"id\": \"1234\" }");

        SnippetizerTester st = new SnippetizerTester(j);
        st.assertRoundTrip(
                step,
                "azureCosmosDBCreateDocument container: 'jenkins', credentialsId: 'cosmos-connection', database: 'jenkins', document: '{ \"id\": \"1234\" }'");
    }

    /**
     * Most people will use a groovy object but snippetizer can't generate that, we allow not
     * providing a document during snippet generation.
     */
    @Test
    void configRoundTripAllowEmptyDocument() throws Exception {
        AzureCosmosDBCreateDocumentStep step =
                new AzureCosmosDBCreateDocumentStep("cosmos-connection", "jenkins", "jenkins", "");

        SnippetizerTester st = new SnippetizerTester(j);
        st.assertRoundTrip(
                step,
                "azureCosmosDBCreateDocument container: 'jenkins', credentialsId: 'cosmos-connection', database: 'jenkins'");
    }

    private void loadCredentials() {
        List<Credentials> credentials = SystemCredentialsProvider.getInstance().getCredentials();
        credentials.add(new AzureCosmosDBKeyCredentialsImpl("key", null, Secret.fromString("abcd")));
        credentials.add(new AzureCosmosDBKeyCredentialsImpl("key2", null, Secret.fromString("abcdefgh")));

        String cosmosCredentialsId = "cosmos-connection";
        credentials.add(new AzureCosmosDBCredentialsImpl(
                null, cosmosCredentialsId, null, "key", "UK South", "https://fake-url"));
        credentials.add(new AzureCosmosDBCredentialsImpl(
                null, cosmosCredentialsId + "2", null, "key2", "UK South", "https://fake-url"));
    }
}
