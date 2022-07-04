package io.jenkins.plugins.azurecosmosdb;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;

public interface AzureCosmosDBKeyCredentials extends StandardCredentials {

  Secret getKey();
}
