package io.jenkins.plugins.azurecosmosdb;

import static java.util.Objects.requireNonNull;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.microsoft.azure.util.AzureCredentials;
import hudson.util.Secret;
import java.util.List;

class BaseIntegrationTest {

    protected static final String KEY_CREDENTIALS_ID = "cosmos-key";
    protected static final String COSMOS_KEY = getEnvVar("IT_COSMOS_KEY");
    protected static final String COSMOS_URL = getEnvVar("IT_COSMOS_URL");
    protected static final String CONTAINER_NAME = getEnvVar("IT_COSMOS_CONTAINER_NAME");
    protected static final String DATABASE_NAME = getEnvVar("IT_COSMOS_DATABASE_NAME");

    protected static final String SP_SUBSCRIPTION_ID = getEnvVar("IT_SP_SUBSCRIPTION_ID");
    protected static final String SP_CLIENT_ID = getEnvVar("IT_SP_CLIENT_ID");
    protected static final String SP_CLIENT_SECRET = getEnvVar("IT_SP_CLIENT_SECRET");
    protected static final String SP_TENANT_ID = getEnvVar("IT_SP_TENANT_ID");

    private static String getEnvVar(String envVar) {
        return requireNonNull(System.getenv(envVar), "Missing environment variable: " + envVar);
    }

    protected String loadValidCredentials() {
        List<Credentials> credentials = SystemCredentialsProvider.getInstance().getCredentials();
        credentials.add(new AzureCosmosDBKeyCredentialsImpl(KEY_CREDENTIALS_ID, null, Secret.fromString(COSMOS_KEY)));

        String cosmosCredentialsId = "cosmos-connection";
        credentials.add(new AzureCosmosDBCredentialsImpl(
                null, cosmosCredentialsId, null, KEY_CREDENTIALS_ID, "UK South", COSMOS_URL));
        return cosmosCredentialsId;
    }

    protected String loadServicePrincipalCredentials() {
        List<Credentials> credentials = SystemCredentialsProvider.getInstance().getCredentials();
        AzureCredentials sp = new AzureCredentials(
                CredentialsScope.GLOBAL,
                "sp",
                null,
                SP_SUBSCRIPTION_ID,
                SP_CLIENT_ID,
                Secret.fromString(SP_CLIENT_SECRET));
        sp.setTenant(SP_TENANT_ID);
        credentials.add(sp);

        String cosmosCredentialsId = "cosmos-connection-sp";
        credentials.add(
                new AzureCosmosDBCredentialsImpl(null, cosmosCredentialsId, null, "sp", "UK South", COSMOS_URL));
        return cosmosCredentialsId;
    }
}
