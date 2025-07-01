package io.jenkins.plugins.azurecosmosdb;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import hudson.model.Result;
import java.util.Optional;
import java.util.UUID;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Run with failsafe:integration-test, this isn't bound to a phase by default relies on pre-existing
 * cloud resources, see the required variables at the top of the test.
 */
@WithJenkins
class AzureCosmosDBCreateDocumentStepIT extends BaseIntegrationTest {

    private static final String DEFAULT_PIPELINE_NAME = "test-scripted-pipeline";

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testScriptedPipeline() throws Exception {
        String cosmosCredentialsId = loadValidCredentials();
        String id = UUID.randomUUID().toString();

        WorkflowJob job = j.createProject(WorkflowJob.class, DEFAULT_PIPELINE_NAME);
        String pipelineScript = "azureCosmosDBCreateDocument credentialsId: '"
                + cosmosCredentialsId
                + "', database: '"
                + DATABASE_NAME
                + "', container: '"
                + CONTAINER_NAME
                + "', document: [id : '"
                + id
                + "']";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "Created document in database";
        j.assertLogContains(expectedString, completedBuild);

        CosmosClient cosmosClient = AzureCosmosDBCache.get(cosmosCredentialsId, job);
        CosmosDatabase database = cosmosClient.getDatabase(DATABASE_NAME);
        CosmosContainer container = database.getContainer(CONTAINER_NAME);

        Optional<Id> item = container
                .queryItems("SELECT c.id from c where c.id = '" + id + "'", new CosmosQueryRequestOptions(), Id.class)
                .stream()
                .findFirst();

        assertThat(item, isPresent());
        Id idPojo = item.orElseThrow(RuntimeException::new);
        assertThat(idPojo.getId(), equalTo(id));
    }

    @Test
    void testMissingDatabase() throws Exception {
        String cosmosCredentialsId = loadValidCredentials();
        String id = UUID.randomUUID().toString();

        WorkflowJob job = j.createProject(WorkflowJob.class, DEFAULT_PIPELINE_NAME);
        String pipelineScript = "azureCosmosDBCreateDocument credentialsId: '"
                + cosmosCredentialsId
                + "', container: '"
                + CONTAINER_NAME
                + "', document: [id : '"
                + id
                + "']";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
        String expectedString = "Database must be set";
        j.assertLogContains(expectedString, completedBuild);
    }

    @Test
    void documentAsString() throws Exception {
        String cosmosCredentialsId = loadValidCredentials();
        String id = UUID.randomUUID().toString();

        WorkflowJob job = j.createProject(WorkflowJob.class, DEFAULT_PIPELINE_NAME);
        String pipelineScript = "azureCosmosDBCreateDocument credentialsId: '"
                + cosmosCredentialsId
                + "', database: '"
                + DATABASE_NAME
                + "', container: '"
                + CONTAINER_NAME
                + "', document: '{ \"id\": \""
                + id
                + "\" }'";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = "Created document in database";
        j.assertLogContains(expectedString, completedBuild);
    }

    public static class Id {
        private final String id;

        @JsonCreator
        public Id(@JsonProperty("id") String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return String.format("Id{id='%s'}", id);
        }
    }
}
