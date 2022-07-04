package io.jenkins.plugins.azurecosmosdb;

import com.cloudbees.plugins.credentials.common.StandardCredentials;

public interface AzureCosmosDBCredentials extends StandardCredentials {

  String getCredentialsId();

  String getPreferredRegion();

  String getUrl();
}
