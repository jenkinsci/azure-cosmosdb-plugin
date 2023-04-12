package io.jenkins.plugins.azurecosmosdb;

import com.azure.core.credential.TokenCredential;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.azure.util.AzureCredentials;
import hudson.model.Item;
import hudson.security.ACL;
import java.util.Collections;

public class CredentialsHelper {

    private CredentialsHelper() {}

    public static StandardCredentials findCredentials(String credentialsId, Item context) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardCredentials.class, context, ACL.SYSTEM, Collections.emptyList()),
                CredentialsMatchers.withId(credentialsId));
    }

    public static CosmosClient createClient(
            StandardCredentials standardCredentials, String preferredRegion, String url) {
        CosmosClientBuilder builder = new CosmosClientBuilder()
                .endpoint(url)
                .preferredRegions(Collections.singletonList(preferredRegion))
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .gatewayMode();

        if (standardCredentials instanceof AzureCosmosDBKeyCredentials) {
            builder = builder.key(
                    ((AzureCosmosDBKeyCredentials) standardCredentials).getKey().getPlainText());
        } else if (standardCredentials instanceof AzureBaseCredentials) {
            TokenCredential tokenCredential =
                    AzureCredentials.getTokenCredential((AzureBaseCredentials) standardCredentials);

            builder = builder.credential(tokenCredential);
        } else {
            throw new RuntimeException("Unexpected credentials type: "
                    + standardCredentials.getClass().getSimpleName().replace("Impl", ""));
        }

        return builder.buildClient();
    }
}
