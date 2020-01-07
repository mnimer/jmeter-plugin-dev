package com.google.swarm.listeners.bigquery;

import com.google.cloud.bigquery.*;
import com.google.swarm.Utils;
import org.apache.jmeter.reporters.ResultSaver;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * JMeter Listener that will write all test results into a BigQuery table, for future reporting
 *
 * @Author John-Paul Zebrowski
 */
public class BQListener extends ResultSaver {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final long serialVersionUID = 155L;
    private static final BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
    private static final String PROJECT = "BQListener.project";
    private static final String DATASET = "BQListener.dataset";
    private static final String LABEL = "BQListener.label";

    String jobGuid = null;

    public void setLabel(String value) {
        setProperty(LABEL, value);
    }

    public String getLabelAsString() {
        return getPropertyAsString(LABEL);
    }
    public void setProject(String dataset) {
        setProperty(PROJECT, dataset);
    }

    public String getProjectAsString() {
        return getPropertyAsString(PROJECT);
    }

    public void setDataset(String dataset) {
        setProperty(DATASET, dataset);
    }

    public String getDatasetAsString() {
        return getPropertyAsString(DATASET);
    }

    public String getJobsTableAsString() {
        return "jmeter_tests";
    }

    public String getResultsTableAsString() {
        return "jmeter_results";
    }

    @Override
    public void testStarted() {
        super.testStarted();
    }

    @Override
    public void testStarted(String host) {
        super.testStarted(host);
        handleTestStart();
    }

    @Override
    public void sampleOccurred(SampleEvent e){
        processSampleForBQ(e.getResult());
    }

    @Override
    public void testEnded() {
        super.testEnded();
    }

    @Override
    public void testEnded(String host) {
        super.testEnded(host);
        handleTestEnd();
    }


    /**
     * - Make sure the required BQ tables exist at the start of the test, so all results can be saved.
     * - Create a new UUID for this job, to link all results together
     */
    private void handleTestStart() {
        //assign a job id at the start of each job run
        jobGuid = UUID.randomUUID().toString();

        String project = getProjectAsString();
        String dataset = getDatasetAsString();
        String jobsTable = getJobsTableAsString();
        String resultsTable = getResultsTableAsString();

        if (project == null || project.length() == 0 || project.startsWith("$")) {
            log.error("Empty or Null Project");
            return;
        }
        if (dataset == null || dataset.length() == 0 || dataset.startsWith("$")) {
            log.error("Empty or Null Dataset");
            return;
        }


        JMeterContext ctx = JMeterContextService.getContext();
        Map<String, Object> variables = Utils.getContextVariableMap(ctx);


        TableId jobsTableId = TableId.of(project, dataset, jobsTable);
        if (bigquery.getTable(jobsTableId) == null) {
            createJobsTable(jobsTableId);
        }

        TableId resultsTableId = TableId.of(project, dataset, resultsTable);
        if (bigquery.getTable(resultsTableId) == null) {
            createResultsTable(resultsTableId, variables);
        }

    }


    /**
     * Create a Job entry, with start/stop times to link all results as a set
     */
    private void handleTestEnd() {
        //todo,  update row with end timestamp
        JMeterContext ctx = JMeterContextService.getContext();
        Map<String, Object> variables = Utils.getContextVariableMap(ctx);

        String project = getProjectAsString();
        String dataset = getDatasetAsString();
        String jobsTable = getJobsTableAsString();

        TableId jobsTableId = TableId.of(project, dataset, jobsTable);


        Map<String, Object> row = createJobsRow(variables);
        InsertAllResponse response = bigquery
            .insertAll(InsertAllRequest.newBuilder(jobsTableId).addRow(UUID.randomUUID().toString(), row).build());

        if (response.hasErrors()) {
            // TODO: Log something useful about the errors
            log.error(response.getInsertErrors().toString());
        }
    }


    private void processSampleForBQ(SampleResult s) {
        writeSampleToBQ(s);
        SampleResult[] sampleResults = s.getSubResults();
        for (SampleResult sampleResult : sampleResults) {
            processSampleForBQ(sampleResult);
        }
    }


    private void writeSampleToBQ(SampleResult sampleResult) {
        JMeterContext ctx = JMeterContextService.getContext();
        if (ctx.getCurrentSampler() == null) {
            return;
        }

        Map<String, Object> variables = Utils.getContextVariableMap(ctx);
        Map<String, Object> row = createResultsRow(sampleResult, variables);

        try {
            String project = getProjectAsString();
            String dataset = getDatasetAsString();
            //String jobsTable = getJobsTableAsString();
            String resultsTable = getResultsTableAsString();
            TableId tableId = TableId.of(project, dataset, resultsTable);

            InsertAllResponse response = bigquery
                .insertAll(InsertAllRequest.newBuilder(tableId).addRow(UUID.randomUUID().toString(), row).build());

            if (response.hasErrors()) {
                // TODO: Log something useful about the errors
                log.error(response.getInsertErrors().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createJobsTable(TableId tableId) {
        Schema schema = createJobsSchema();
        TableDefinition tableDefinition = StandardTableDefinition.of(schema);
        TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
        bigquery.create(tableInfo);
    }

    private void createResultsTable(TableId tableId, Map<String, Object> variables) {
        Schema schema = createResultsSchema(variables);
        TableDefinition tableDefinition = StandardTableDefinition.of(schema);
        TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
        bigquery.create(tableInfo);
    }

    private Map<String, Object> createJobsRow(Map<String, Object> variables) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");
        Map<String, Object> row = new HashMap<>();
        row.put("jobId", jobGuid);
        row.put("label", getLabelAsString());
        row.put("startTime", df.format(new Date(Long.valueOf((String)variables.get("TESTSTART.MS")))));
        row.put("endTime", df.format(new Date())); //set to now, since this is run at end of test
        return row;
    }

    private Map<String, Object> createResultsRow(SampleResult sampleResult, Map<String, Object> variables) {
        SimpleDateFormat df =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Map<String, Object> row = new HashMap<>();
        row.put("jobId", jobGuid);
        row.put("label", sampleResult.getSampleLabel());
        row.put("threadGroup", sampleResult.getThreadName());
        row.put("elapsed", sampleResult.getTime());
        if (variables.containsKey("nodes")) {
            row.put("nodes", Integer.parseInt((String) variables.get("nodes")));
        }
        if (variables.containsKey("numThreads")) {
            row.put("users", Integer.parseInt((String) variables.get("numThreads")));
        }

        //new
        row.put("startTime", df.format(new Date(sampleResult.getStartTime())));
        row.put("endTime", df.format(new Date(sampleResult.getEndTime())));
        row.put("responseCode", sampleResult.getResponseCode());
        row.put("responseDataType", sampleResult.getDataType());
        row.put("responseContentType", sampleResult.getContentType());
        row.put("response", sampleResult.getResponseMessage());
        return row;
    }

    private Schema createJobsSchema() {
        List<Field> fields = new ArrayList<Field>();
        fields.add(Field.of("jobId", LegacySQLTypeName.STRING));
        fields.add(Field.of("label", LegacySQLTypeName.STRING));
        fields.add(Field.of("startTime", LegacySQLTypeName.DATETIME));
        fields.add(Field.of("endTime", LegacySQLTypeName.DATETIME));
        return Schema.of(fields);
    }

    private Schema createResultsSchema(Map<String, Object> variables) {
        List<Field> fields = new ArrayList<Field>();
        fields.add(Field.of("jobId", LegacySQLTypeName.STRING));
        fields.add(Field.of("label", LegacySQLTypeName.STRING));
        fields.add(Field.of("threadGroup", LegacySQLTypeName.STRING));
        fields.add(Field.of("elapsed", LegacySQLTypeName.INTEGER));
        if (variables.containsKey("nodes")) {
            fields.add(Field.of("nodes", LegacySQLTypeName.INTEGER));
        }
        if (variables.containsKey("users")) {
            fields.add(Field.of("users", LegacySQLTypeName.INTEGER));
        }
        fields.add(Field.of("startTime", LegacySQLTypeName.DATETIME));
        fields.add(Field.of("endTime", LegacySQLTypeName.DATETIME));
        fields.add(Field.of("responseCode", LegacySQLTypeName.STRING));
        fields.add(Field.of("responseDataType", LegacySQLTypeName.STRING));
        fields.add(Field.of("responseContentType", LegacySQLTypeName.STRING));
        fields.add(Field.of("response", LegacySQLTypeName.STRING));

        return Schema.of(fields);
    }


}
