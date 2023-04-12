package io.jenkins.plugins.azurecosmosdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.azure.util.AzureImdsCredentials;
import hudson.util.Secret;
import io.jenkins.plugins.azurecosmosdb.AzureCosmosDBCache.CacheKey;
import java.util.List;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

public class AzureCosmosDBCacheTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void missingCredentialsId() {
        RuntimeException runtimeException =
                assertThrows(RuntimeException.class, () -> AzureCosmosDBCache.get("does-not-exist", null));
        assertThat(runtimeException.getMessage(), is("Could not find credentials: does-not-exist"));
    }

    @Test
    public void credentialsWithInvalidType() {
        List<Credentials> credentials = SystemCredentialsProvider.getInstance().getCredentials();
        String id = "invalid-type";
        credentials.add(new StringCredentialsImpl(null, id, null, Secret.fromString("some-string")));

        RuntimeException runtimeException =
                assertThrows(RuntimeException.class, () -> AzureCosmosDBCache.get(id, null));
        assertThat(
                runtimeException.getMessage(),
                is("Unexpected credentials type: StringCredentials for ID: invalid-type"));
    }

    @Test
    @WithoutJenkins
    public void toStringAsExpected() {
        AzureCosmosDBKeyCredentialsImpl credentials =
                new AzureCosmosDBKeyCredentialsImpl("key", null, Secret.fromString("abcd"));
        CacheKey cacheKey = new CacheKey(credentials, "https://your-account-name.documents.azure.com:443/", "UK South");

        assertThat(
                cacheKey.toString(),
                equalTo(
                        "CacheKey{cosmosAuthCredentials=key, url='https://your-account-name.documents.azure.com:443/', preferredRegion='UK South'}"));
    }

    @Test
    @WithoutJenkins
    public void equalsIsFalseForNonMatchingType() {
        String url = "https://your-account-name.documents.azure.com:443/";
        StringCredentialsImpl stringCredentials =
                new StringCredentialsImpl(CredentialsScope.GLOBAL, "id", null, Secret.fromString("a"));
        AzureCosmosDBKeyCredentialsImpl credentials =
                new AzureCosmosDBKeyCredentialsImpl("id", null, Secret.fromString("abcd"));
        CacheKey invalidTypeCacheKey = new CacheKey(stringCredentials, url, "UK South");
        CacheKey validTypeCacheKey = new CacheKey(credentials, url, "UK South");

        assertThat(invalidTypeCacheKey.equals(validTypeCacheKey), is(false));
    }

    @Test
    @WithoutJenkins
    public void equalsIsFalseForInvalidType() {
        String url = "https://your-account-name.documents.azure.com:443/";
        StringCredentialsImpl stringCredentials =
                new StringCredentialsImpl(CredentialsScope.GLOBAL, "id", null, Secret.fromString("a"));
        CacheKey invalidTypeCacheKey = new CacheKey(stringCredentials, url, "UK South");
        CacheKey invalidTypeCacheKey2 = new CacheKey(stringCredentials, url, "UK South");

        assertThat(invalidTypeCacheKey.equals(invalidTypeCacheKey2), is(false));
    }

    @Test
    @WithoutJenkins
    public void equalsMatchesForImdsCredentials() {
        String url = "https://your-account-name.documents.azure.com:443/";
        AzureImdsCredentials imdsCredentials = new AzureImdsCredentials(CredentialsScope.GLOBAL, "id", null);
        CacheKey imds1 = new CacheKey(imdsCredentials, url, "UK South");
        CacheKey imds2 = new CacheKey(imdsCredentials, url, "UK South");

        assertThat(imds1.equals(imds2), is(true));
    }

    @Test
    @WithoutJenkins
    public void equalsDoesNotMatchesForDifferentTenantSpCredentials() {
        String url = "https://your-account-name.documents.azure.com:443/";
        AzureCredentials sp =
                new AzureCredentials(CredentialsScope.GLOBAL, "sp", null, "1234", "12345", Secret.fromString("1234"));
        sp.setTenant("123456");

        AzureCredentials sp2 =
                new AzureCredentials(CredentialsScope.GLOBAL, "sp", null, "1234", "12345", Secret.fromString("1234"));
        sp2.setTenant("1234567");
        CacheKey sp1Cache = new CacheKey(sp, url, "UK South");
        CacheKey sp2Cache = new CacheKey(sp2, url, "UK South");

        assertThat(sp1Cache.equals(sp2Cache), is(false));
    }

    @Test
    @WithoutJenkins
    public void equalsDoesNotMatchWhenDifferentIds() {
        String url = "https://your-account-name.documents.azure.com:443/";
        AzureImdsCredentials imdsCredentials = new AzureImdsCredentials(CredentialsScope.GLOBAL, "id", null);
        AzureImdsCredentials imdsCredentials2 = new AzureImdsCredentials(CredentialsScope.GLOBAL, "id2", null);
        CacheKey imds1 = new CacheKey(imdsCredentials, url, "UK South");
        CacheKey imds2 = new CacheKey(imdsCredentials2, url, "UK South");

        assertThat(imds1.equals(imds2), is(false));
    }

    @Test
    @WithoutJenkins
    @SuppressWarnings("ConstantConditions")
    public void nullEqualsIsNotEqual() {
        String url = "https://your-account-name.documents.azure.com:443/";
        AzureImdsCredentials imdsCredentials = new AzureImdsCredentials(CredentialsScope.GLOBAL, "id", null);
        CacheKey imds1 = new CacheKey(imdsCredentials, url, "UK South");

        assertThat(imds1.equals(null), is(false));
    }

    @Test
    @WithoutJenkins
    @SuppressWarnings("EqualsWithItself")
    public void sameReferenceIsEqual() {
        String url = "https://your-account-name.documents.azure.com:443/";
        AzureImdsCredentials imdsCredentials = new AzureImdsCredentials(CredentialsScope.GLOBAL, "id", null);
        CacheKey imds1 = new CacheKey(imdsCredentials, url, "UK South");

        assertThat(imds1.equals(imds1), is(true));
    }
}
