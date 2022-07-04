package io.jenkins.plugins.azurecosmosdb;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.jvnet.hudson.test.JenkinsMatchers.hasKind;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class AzureCosmosDBCredentialsImplTest {

  @Rule public JenkinsRule r = new JenkinsRule();

  @Test
  public void doFillCredentialsIdItemsNoItemNoAdmin() throws IOException {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy()
            .grant(Job.CONFIGURE, Item.DISCOVER)
            .everywhere()
            .to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    User cassandra = requireNonNull(User.getById("cassandra", true));
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(null, "key");

      assertCannotSeeCredentials(listBoxModel);
    }
  }

  @Test
  public void doFillCredentialsIdItemsNoItemHasAdminFindsCredentials() {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy().grant(Jenkins.ADMINISTER).everywhere().to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    User cassandra = requireNonNull(User.getById("cassandra", true));
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(null, "key");

      assertFindsCredentials(listBoxModel);
    }
  }

  private void assertFindsCredentials(ListBoxModel listBoxModel) {
    assertThat(listBoxModel, hasSize(3));
    ListBoxModel.Option current = listBoxModel.get(0);
    assertThat(current.name, equalTo("- none -"));
    assertThat(current.value, equalTo(""));

    ListBoxModel.Option key = listBoxModel.get(1);
    assertThat(key.name, equalTo("key"));
    assertThat(key.value, equalTo("key"));

    ListBoxModel.Option key2 = listBoxModel.get(2);
    assertThat(key2.name, equalTo("key2"));
    assertThat(key2.value, equalTo("key2"));
  }

  @Test
  public void doFillCredentialsIdItemsWithItemNoCredentialsAccess() throws IOException {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy()
            .grant(Item.READ, Item.DISCOVER)
            .everywhere()
            .to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    User cassandra = requireNonNull(User.getById("cassandra", true));
    WorkflowJob job = r.createProject(WorkflowJob.class, "pipeline");
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(job, "key");

      assertCannotSeeCredentials(listBoxModel);
    }
  }

  @Test
  public void doFillCredentialsIdItemsWithItemHasUseItemHasCredentialsAccess() throws IOException {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy()
            .grant(CredentialsProvider.USE_ITEM, Item.DISCOVER)
            .everywhere()
            .to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    User cassandra = requireNonNull(User.getById("cassandra", true));
    WorkflowJob job = r.createProject(WorkflowJob.class, "pipeline");
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(job, "key");

      assertFindsCredentials(listBoxModel);
    }
  }

  private void assertCannotSeeCredentials(ListBoxModel listBoxModel) {
    assertThat(listBoxModel, hasSize(1));
    ListBoxModel.Option option = listBoxModel.get(0);
    assertThat(option.name, equalTo("- current -"));
    assertThat(option.value, equalTo("key"));
  }

  @Test
  public void doFillCredentialsIdItemsWithItemFindsCredential() throws IOException {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy()
            .grant(Job.CONFIGURE, Item.DISCOVER)
            .everywhere()
            .to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    User cassandra = requireNonNull(User.getById("cassandra", true));
    WorkflowJob job = r.createProject(WorkflowJob.class, "pipeline");
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      ListBoxModel listBoxModel = descriptor.doFillCredentialsIdItems(job, "key");

      assertFindsCredentials(listBoxModel);
    }
  }

  @Test
  public void doTestConnectionNoItemNoAdminGetsOk() {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy()
            .grant(Job.CONFIGURE, Item.DISCOVER)
            .everywhere()
            .to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    User cassandra = requireNonNull(User.getById("cassandra", true));
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      FormValidation validation =
          descriptor.doTestConnection("key", "UK South", "https://some-url", null);

      assertThat(validation, hasKind(FormValidation.Kind.OK));
    }
  }

  @Test
  public void doTestConnectionItemNoUseItemGetsOk() throws IOException {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy().grant(Job.READ).everywhere().to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    WorkflowJob job = r.jenkins.createProject(WorkflowJob.class, "pipeline");
    User cassandra = requireNonNull(User.getById("cassandra", true));
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      FormValidation validation =
          descriptor.doTestConnection("key", "UK South", "https://some-url", job);

      assertThat(validation, hasKind(FormValidation.Kind.OK));
    }
  }

  @Test
  public void doTestConnectionAdminNoCredentialsIdGetsOk() {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy().grant(Jenkins.ADMINISTER).everywhere().to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    User cassandra = requireNonNull(User.getById("cassandra", true));
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      FormValidation validation =
          descriptor.doTestConnection("", "UK South", "https://some-url", null);

      assertThat(validation, hasKind(FormValidation.Kind.OK));
    }
  }

  @Test
  public void doTestConnectionAdminCredentialsIdDoesNotExistGetsError() {
    loadCredentials();
    JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
    r.jenkins.setSecurityRealm(realm);
    r.jenkins.setAuthorizationStrategy(
        new MockAuthorizationStrategy().grant(Jenkins.ADMINISTER).everywhere().to("cassandra"));

    AzureCosmosDBCredentialsImpl.DescriptorImpl descriptor =
        new AzureCosmosDBCredentialsImpl.DescriptorImpl();

    User cassandra = requireNonNull(User.getById("cassandra", true));
    try (ACLContext ignored = ACL.as2(cassandra.impersonate2())) {
      FormValidation validation =
          descriptor.doTestConnection("does-not-exist", "UK South", "https://some-url", null);

      assertThat(validation, hasKind(FormValidation.Kind.ERROR));
      assertThat(validation.getMessage(), equalTo("Cannot find currently selected credentials"));
    }
  }

  private void loadCredentials() {
    List<Credentials> credentials = SystemCredentialsProvider.getInstance().getCredentials();
    credentials.add(new AzureCosmosDBKeyCredentialsImpl("key", null, Secret.fromString("abcd")));
    credentials.add(
        new AzureCosmosDBKeyCredentialsImpl("key2", null, Secret.fromString("YWJjZGVmZwo=")));

    String cosmosCredentialsId = "cosmos-connection";
    credentials.add(
        new AzureCosmosDBCredentialsImpl(
            null, cosmosCredentialsId, null, "key", "UK South", "https://fake-url"));
  }
}
