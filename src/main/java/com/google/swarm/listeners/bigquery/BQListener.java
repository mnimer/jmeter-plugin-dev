package com.google.swarm.listeners.bigquery;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.jmeter.reporters.ResultSaver;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

import com.google.cloud.bigquery.*;
import com.google.swarm.Utils;


/**
 * JMeter Listener that will write all test results into a BigQuery table, for future reporting
 *
 * @Author John-Paul Zebrowski
 */
public class BQListener extends ResultSaver {

    private static final long serialVersionUID = 155L;
    private static final BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
    private static final String DATASET = "BQListener.dataset";
    private static final String TABLE = "BQListener.table";
    private final String dataset = getDatasetAsString();


    @Override
    public void sampleOccurred(SampleEvent e){
    	processSampleForBQ(e.getResult());
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
        Map<String, Object> row = createRow(sampleResult, variables);
    	String table = StringUtils.isNotEmpty(getTableAsString()) ? getTableAsString() : "results";

		try {
			TableId tableId = TableId.of(dataset, table);
			if (bigquery.getTable(tableId) == null) {
				createTable(tableId, variables);
			}

			InsertAllResponse response = bigquery
					.insertAll(InsertAllRequest.newBuilder(tableId).addRow(UUID.randomUUID().toString(), row).build());

			if (response.hasErrors()) {
				// TODO: Log something useful about the errors
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
     }

	private void createTable(TableId tableId, Map<String, Object> variables) {
	    Schema schema = createSchema(variables);
	    TableDefinition tableDefinition = StandardTableDefinition.of(schema);
	    TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
	    bigquery.create(tableInfo);
	}

    private Map<String, Object> createRow(SampleResult sampleResult, Map<String, Object> variables) {
		Map<String, Object> row = new HashMap<>();
		row.put("label", sampleResult.getSampleLabel());
		row.put("threadGroup", sampleResult.getThreadName());
		row.put("elapsed", sampleResult.getTime());
		if (variables.containsKey("nodes")) {
			row.put("nodes", Integer.parseInt((String) variables.get("nodes")));
		}
		if (variables.containsKey("numThreads")) {
			row.put("users", Integer.parseInt((String) variables.get("numThreads")));
		}
		row.put("timestamp", Math.round(sampleResult.getStartTime()/1000));
		return row;
    }

	private Schema createSchema(Map<String, Object> variables) {
		List<Field> fields = new ArrayList<Field>();
		fields.add(Field.of("label", LegacySQLTypeName.STRING));
		fields.add(Field.of("threadGroup", LegacySQLTypeName.STRING));
		fields.add(Field.of("elapsed", LegacySQLTypeName.INTEGER));
		if (variables.containsKey("nodes")) {
			fields.add(Field.of("nodes", LegacySQLTypeName.INTEGER));
		}
		if (variables.containsKey("users")) {
			fields.add(Field.of("users", LegacySQLTypeName.INTEGER));
		}
		fields.add(Field.of("timestamp", LegacySQLTypeName.TIMESTAMP));
		return Schema.of(fields);
	}

    public void setDataset(String dataset) {
        setProperty(DATASET, dataset);
    }

    public String getDatasetAsString() {
        return getPropertyAsString(DATASET);
    }

    public void setTable(String table) {
        setProperty(TABLE, table);
    }

    public String getTableAsString() {
        return getPropertyAsString(TABLE);
    }

}
