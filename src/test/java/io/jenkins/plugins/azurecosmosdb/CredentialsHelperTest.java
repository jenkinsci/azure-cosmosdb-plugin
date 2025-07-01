package io.jenkins.plugins.azurecosmosdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CredentialsHelperTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void createClient() {
        StringCredentialsImpl credentials =
                new StringCredentialsImpl(null, "invalid-type", null, Secret.fromString("some-string"));

        RuntimeException runtimeException = assertThrows(
                RuntimeException.class,
                () -> CredentialsHelper.createClient(credentials, "UK South", "https://does-not-matter"));
        assertThat(runtimeException.getMessage(), is("Unexpected credentials type: StringCredentials"));
    }
}
