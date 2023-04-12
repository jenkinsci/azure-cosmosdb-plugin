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
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.SnippetizerTester;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class AzureCosmosDBCreateDocumentStepTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void doFillCredentialsIdItemsNoItemNoAdmin() throws IOException {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
        r.jenkins.setSecurityRealm(realm);
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
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
    public void doFillCredentialsIdItemsNoItemHasAdminFindsCredentials() {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
        r.jenkins.setSecurityRealm(realm);
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
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
    public void doFillCredentialsIdItemsWithItemNoCredentialsAccess() throws IOException {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
        r.jenkins.setSecurityRealm(realm);
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Item.DISCOVER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCreateDocumentStep.DescriptorImpl descriptor =
                new AzureCosmosDBCreateDocumentStep.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        WorkflowJob job = r.createProject(WorkflowJob.class, "pipeline");
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(job, "cosmos-connection");

            assertCannotSeeCredentials(listBoxModel);
        }
    }

    @Test
    public void doFillCredentialsIdItemsWithItemHasUseItemHasCredentialsAccess() throws IOException {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
        r.jenkins.setSecurityRealm(realm);
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(CredentialsProvider.USE_ITEM, Item.DISCOVER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCreateDocumentStep.DescriptorImpl descriptor =
                new AzureCosmosDBCreateDocumentStep.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        WorkflowJob job = r.createProject(WorkflowJob.class, "pipeline");
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
    public void doFillCredentialsIdItemsWithItemFindsCredential() throws IOException {
        loadCredentials();
        JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
        r.jenkins.setSecurityRealm(realm);
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Job.CONFIGURE, Item.DISCOVER)
                .everywhere()
                .to("cassandra"));

        AzureCosmosDBCreateDocumentStep.DescriptorImpl descriptor =
                new AzureCosmosDBCreateDocumentStep.DescriptorImpl();

        User cassandra = requireNonNull(User.getById("cassandra", true));
        WorkflowJob job = r.createProject(WorkflowJob.class, "pipeline");
        try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
            ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(job, "cosmos-connection");

            assertFindsCredentials(listBoxModel);
        }
    }

    @Test
    public void configRoundTrip() throws Exception {
        AzureCosmosDBCreateDocumentStep step =
                new AzureCosmosDBCreateDocumentStep("cosmos-connection", "jenkins", "jenkins", "{ \"id\": \"1234\" }");

        SnippetizerTester st = new SnippetizerTester(r);
        st.assertRoundTrip(
                step,
                "azureCosmosDBCreateDocument container: 'jenkins', credentialsId: 'cosmos-connection', database: 'jenkins', document: '{ \"id\": \"1234\" }'");
    }

    /**
     * Most people will use a groovy object but snippetizer can't generate that, we allow not
     * providing a document during snippet generation.
     */
    @Test
    public void configRoundTripAllowEmptyDocument() throws Exception {
        AzureCosmosDBCreateDocumentStep step =
                new AzureCosmosDBCreateDocumentStep("cosmos-connection", "jenkins", "jenkins", "");

        SnippetizerTester st = new SnippetizerTester(r);
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
