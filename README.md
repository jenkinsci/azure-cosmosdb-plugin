# azure-cosmosdb

## Introduction

Adds support to create documents in [Azure Cosmos DB](https://docs.microsoft.com/en-us/azure/cosmos-db/introduction) from Jenkins pipeline.

## Getting started

This plugin provides two credential types:
- Azure Cosmos DB Key
- Azure Cosmos DB

It also integrates with:
- Azure Managed Identity
- Azure Service Principal

When creating a credential of type `Azure Cosmos DB` you have to select another credential type depending on how you want to authenticate to Cosmos DB.
Key, service principal and managed identity are all supported.

If you are a [Configuration as Code](https://plugins.jenkins.io/configuration-as-code) user you can use the relevant part of the below example:

```yaml
credentials:
  system:
    domainCredentials:
    - credentials:
      - azureCosmosDBKey:
          id: "cosmos-key"
          key: "$COSMOS_KEY"
      - azureCosmosDB:
          credentialsId: "cosmos-key"
          id: "cosmos-connection-using-key"
          preferredRegion: "UK South"
          scope: GLOBAL
          url: "https://$COSMOS_ACCOUNT_NAME.documents.azure.com:443/"
      - azure:
          azureEnvironmentName: "Azure"
          clientId: "$CLIENT_ID"
          clientSecret: $CLIENT_SECRET
          id: "service-principal"
          scope: GLOBAL
          subscriptionId: "$SUBSCRIPTION_ID"
          tenant: "$TENANT_ID"
      - azureCosmosDB:
          credentialsId: "service-principal"
          id: "cosmos-connection-using-sp"
          preferredRegion: "UK South"
          scope: GLOBAL
          url: "https://$COSMOS_ACCOUNT_NAME.documents.azure.com:443/"
      - azureImds:
          azureEnvName: "Azure"
          id: "managed-identity"
          scope: GLOBAL
      - azureCosmosDB:
          credentialsId: "managed-identity"
          id: "cosmos-connection-using-mi"
          preferredRegion: "UK South"
          scope: GLOBAL
          url: "https://$COSMOS_ACCOUNT_NAME.documents.azure.com:443/"
```

### `azureCosmosDBCreateDocument`

The pipeline step `azureCosmosDBCreateDocument` can be used to create documents in Cosmos DB.

See the reference documentation on the [Jenkins website](https://www.jenkins.io/doc/pipeline/steps/azure-cosmosdb/).

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

