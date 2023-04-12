package io.jenkins.plugins.azurecosmosdb;

import static io.jenkins.plugins.azurecosmosdb.CredentialsHelper.findCredentials;
import static java.util.Objects.requireNonNull;

import com.azure.cosmos.CosmosClient;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.azure.util.AzureImdsCredentials;
import hudson.model.Item;
import java.time.Duration;
import java.util.Objects;
import jenkins.util.SystemProperties;

/** Cache to prevent authenticating every time the step is invoked. */
public class AzureCosmosDBCache {

    private static final long MAX_SIZE =
            SystemProperties.getLong(AzureCosmosDBCache.class.getName() + ".MAX_CACHE_SIZE", 50L);

    // If you use folder based auth you may wish to shorten this so your old caches are cleaned up
    // quicker
    private static final int CACHE_DURATION_HOURS =
            SystemProperties.getInteger(AzureCosmosDBCache.class.getName() + ".CACHE_DURATION_HOURS", 24);

    private static final Duration EXPIRE_AFTER = Duration.ofHours(CACHE_DURATION_HOURS);

    private static final LoadingCache<CacheKey, CosmosClient> CACHE = Caffeine.newBuilder()
            .maximumSize(MAX_SIZE)
            .evictionListener((k, v, c) -> {
                CosmosClient client = (CosmosClient) v;
                requireNonNull(client).close();
            })
            .expireAfterWrite(EXPIRE_AFTER)
            .build(AzureCosmosDBCache::createClient);

    private AzureCosmosDBCache() {}

    static long cacheSize() {
        return CACHE.estimatedSize();
    }

    public static CosmosClient get(String credentialsId, Item item) {
        AzureCosmosDBCredentials credentials = lookupCredentials(credentialsId, item, AzureCosmosDBCredentials.class);

        StandardCredentials authCreds =
                lookupCredentials(credentials.getCredentialsId(), item, StandardCredentials.class);

        return CACHE.get(new CacheKey(authCreds, credentials.getUrl(), credentials.getPreferredRegion()));
    }

    private static <T extends StandardCredentials> T lookupCredentials(String credentialsId, Item item, Class<T> type) {
        StandardCredentials credentials = findCredentials(credentialsId, item);
        if (credentials != null && type.isAssignableFrom(credentials.getClass())) {
            return type.cast(credentials);
        }

        if (credentials == null) {
            throw new RuntimeException("Could not find credentials: " + credentialsId);
        }

        throw new RuntimeException("Unexpected credentials type: "
                + credentials.getClass().getSimpleName().replace("Impl", "")
                + " for ID: "
                + credentialsId);
    }

    /** Used to notify when credentials change, e.g. service principal secret updated. */
    public static void invalidateCache() {
        CACHE.invalidateAll();
    }

    static class CacheKey {

        private final StandardCredentials cosmosAuthCredentials;
        private final String url;
        private final String preferredRegion;

        CacheKey(StandardCredentials cosmosAuthCredentials, String url, String preferredRegion) {
            this.cosmosAuthCredentials = cosmosAuthCredentials;
            this.url = url;
            this.preferredRegion = preferredRegion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            boolean simpleTypesEqual =
                    Objects.equals(cosmosAuthCredentials.getId(), cacheKey.cosmosAuthCredentials.getId())
                            && Objects.equals(url, cacheKey.url)
                            && Objects.equals(preferredRegion, cacheKey.preferredRegion);
            if (!simpleTypesEqual) {
                return false;
            }

            // equals gets more complicated to validate here as IdCredentials (part of the class
            // hierarchy) declares a final equals method
            // that only checks the ID, but that's not enough as IDs are not guaranteed to be unique
            // across credentials providers.
            if (cosmosAuthCredentials.getClass() != cacheKey.cosmosAuthCredentials.getClass()) {
                return false;
            }

            if (cosmosAuthCredentials instanceof AzureImdsCredentials) {
                AzureImdsCredentials imdsCredentials = (AzureImdsCredentials) cosmosAuthCredentials;
                AzureImdsCredentials cacheKeyImdsCreds = (AzureImdsCredentials) cacheKey.cosmosAuthCredentials;

                return Objects.equals(imdsCredentials.getSubscriptionId(), cacheKeyImdsCreds.getSubscriptionId());
            }

            if (cosmosAuthCredentials instanceof AzureCredentials) {
                AzureCredentials azureCredentials = (AzureCredentials) cosmosAuthCredentials;
                AzureCredentials cacheKeyAzureCredentials = (AzureCredentials) cacheKey.cosmosAuthCredentials;

                return Objects.equals(
                                azureCredentials.getSubscriptionId(), cacheKeyAzureCredentials.getSubscriptionId())
                        && Objects.equals(azureCredentials.getTenant(), cacheKeyAzureCredentials.getTenant())
                        && Objects.equals(azureCredentials.getClientId(), cacheKeyAzureCredentials.getClientId())
                        && Objects.equals(
                                azureCredentials.getPlainClientSecret(),
                                cacheKeyAzureCredentials.getPlainClientSecret());
            }

            if (cosmosAuthCredentials instanceof AzureCosmosDBKeyCredentials) {
                AzureCosmosDBKeyCredentials keyCredentials = (AzureCosmosDBKeyCredentials) cosmosAuthCredentials;
                AzureCosmosDBKeyCredentials cacheKeyCredentials =
                        (AzureCosmosDBKeyCredentials) cacheKey.cosmosAuthCredentials;

                return Objects.equals(keyCredentials.getKey(), cacheKeyCredentials.getKey());
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cosmosAuthCredentials, url, preferredRegion);
        }

        @Override
        public String toString() {
            return String.format(
                    "CacheKey{cosmosAuthCredentials=%s, url='%s', preferredRegion='%s'}",
                    cosmosAuthCredentials.getId(), url, preferredRegion);
        }
    }

    private static CosmosClient createClient(CacheKey cacheKey) {
        return CredentialsHelper.createClient(cacheKey.cosmosAuthCredentials, cacheKey.preferredRegion, cacheKey.url);
    }
}
