package io.jenkins.plugins.azurecosmosdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CredentialsHelperTest {

  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void createClient() {
    StringCredentialsImpl credentials =
        new StringCredentialsImpl(null, "invalid-type", null, Secret.fromString("some-string"));

    RuntimeException runtimeException =
        assertThrows(
            RuntimeException.class,
            () ->
                CredentialsHelper.createClient(credentials, "UK South", "https://does-not-matter"));
    assertThat(runtimeException.getMessage(), is("Unexpected credentials type: StringCredentials"));
  }
}
