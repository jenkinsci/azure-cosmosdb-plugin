package io.jenkins.plugins.azurecosmosdb;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;
import static java.util.Objects.requireNonNull;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AzureCosmosDBCreateDocumentStep extends Step {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String credentialsId;
    private final String database;
    private final String container;

    private final Object document;

    @DataBoundConstructor
    public AzureCosmosDBCreateDocumentStep(String credentialsId, String database, String container, Object document) {
        this.credentialsId = Util.fixEmpty(credentialsId);
        this.database = Util.fixEmpty(database);
        this.container = Util.fixEmpty(container);

        Object tmpDocument = document;
        if (document instanceof String) {
            String doc = (String) tmpDocument;
            if ("".equals(doc)) {
                tmpDocument = null;
            }
        }

        this.document = tmpDocument;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {

        Run<?, ?> run = context.get(Run.class);
        requireNonNull(run, "Run must not be null");
        Job<?, ?> item = run.getParent();

        CosmosClient cosmosClient = AzureCosmosDBCache.get(credentialsId, item);
        return new Execution(context, cosmosClient, database, container, document);
    }

    public Object getDocument() {
        return document;
    }

    public String getDatabase() {
        return database;
    }

    public String getContainer() {
        return container;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            context.add(TaskListener.class);
            return Collections.unmodifiableSet(context);
        }

        @Override
        public String getFunctionName() {
            return "azureCosmosDBCreateDocument";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Create document in Azure Cosmos DB";
        }

        @POST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return result.includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            item,
                            AzureCosmosDBCredentials.class,
                            Collections.emptyList(),
                            instanceOf(AzureCosmosDBCredentials.class))
                    .includeCurrentValue(credentialsId);
        }
    }

    @SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID", justification = "Not used in XStream")
    private static class Execution extends SynchronousNonBlockingStepExecution<Void> {

        private final transient CosmosClient client;
        private final String database;
        private final String container;
        private final Object document;

        protected Execution(
                @NonNull StepContext context, CosmosClient client, String database, String container, Object document) {
            super(context);
            this.client = client;
            this.database = database;
            this.container = container;
            this.document = document;
        }

        @Override
        protected Void run() throws Exception {
            requireNonNull(database, "Database must be set");
            requireNonNull(container, "Container must be set");
            requireNonNull(document, "Document must be set");

            CosmosDatabase cosmosDatabase = client.getDatabase(database);
            CosmosContainer cosmosContainer = cosmosDatabase.getContainer(container);
            Object tmpDocument = document;
            if (document instanceof String) {
                ObjectNode node = (ObjectNode) OBJECT_MAPPER.readTree((String) document);
                tmpDocument = OBJECT_MAPPER.treeToValue(node, Object.class);
            }
            cosmosContainer.createItem(tmpDocument);

            TaskListener taskListener = getContext().get(TaskListener.class);
            if (taskListener != null) {
                taskListener
                        .getLogger()
                        .printf("Created document in database: %s, container: %s%n", database, container);
            }

            return null;
        }
    }
}
