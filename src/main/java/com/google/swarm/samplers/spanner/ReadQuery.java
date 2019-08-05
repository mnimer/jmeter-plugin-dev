/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.swarm.samplers.spanner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.TransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.spanner.*;
import io.opencensus.common.Scope;
import io.opencensus.contrib.grpc.metrics.RpcViews;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.*;
import io.opencensus.trace.samplers.Samplers;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * The specific logic to run SQL Queries
 *
 * @Author Mike Nimer
 * @Author John-Paul Zebrowski
 */
public class ReadQuery extends AbstractJavaSamplerClient implements Serializable {

    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final Tracer tracer = Tracing.getTracer();

    static Spanner spanner = null;
    static Map<String, DatabaseClient> dbClientMap = new HashMap();

    @Override
    public void setupTest(JavaSamplerContext context) {
        //super.setupTest(context);
        //System.out.println("SetupTest");
        //log.info("SetupTest");

        String project = context.getParameter("project");
        String instance = context.getParameter("instance");
        String database = context.getParameter("database");
        Integer minSessions = new Integer(context.getParameter("minSessions", "30"));
        Integer maxSessions = new Integer(context.getParameter("maxSessions", "30"));
        Integer grpcChannels = new Integer(context.getParameter("grpcChannels", "30"));

        // Instantiates a client
        try {
            //Instead of starting with 0, set min == max at startup for prod
            SessionPoolOptions sessionPoolOptions = SessionPoolOptions.newBuilder()
                .setMinSessions(minSessions)
                .setMaxSessions(maxSessions)
                .setBlockIfPoolExhausted()
                .setWriteSessionsFraction(1)
                .build();


            SpannerOptions.Builder builder = SpannerOptions.newBuilder();
            builder.setSessionPoolOption(sessionPoolOptions);
            TransportOptions transportOptions = GrpcTransportOptions.newBuilder().build();
            builder.setTransportOptions(((GrpcTransportOptions) transportOptions).toBuilder().build());
            builder.setNumChannels(grpcChannels);
            SpannerOptions options = builder.build();

            spanner = options.getService();
            // Creates a database client
            synchronized (database) {
                DatabaseClient dbClient = spanner.getDatabaseClient(DatabaseId.of(project, instance, database));
                dbClientMap.put(database, dbClient);

                //warm up connection
                ResultSet rs = dbClient.singleUse().executeQuery(Statement.of("SELECT 1;"));
                rs.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage());
        }


        try {
            String sampleName = context.getParameter("TestElement.name");

            // Next up let's  install the exporter for Stackdriver tracing.
            StackdriverTraceExporter.createAndRegister(
                StackdriverTraceConfiguration.builder()
                    .setProjectId(project)
                    .build());

            Set<String> stores = Tracing.getExportComponent().getSampledSpanStore().getRegisteredSpanNamesForCollection();
            if( !stores.contains(sampleName) ) {
                Tracing.getExportComponent().getSampledSpanStore().registerSpanNamesForCollection(Arrays.asList(sampleName));
                StackdriverStatsExporter.createAndRegister();
                RpcViews.registerAllGrpcViews();
            }

        } catch (Exception ex) {
            //ex.printStackTrace();
        }

    }


    @Override
    public void teardownTest(JavaSamplerContext context) {
        super.teardownTest(context);
        //System.out.println("TeardownTest");
        //log.info("TeardownTest");

        StackdriverTraceExporter.unregister();


        // Closes the client which will free up the resources used
        if (spanner != null) {
            spanner.close();
        }
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("staleReadSeconds", "0");
        defaultParameters.addArgument("project", "${project}");
        defaultParameters.addArgument("instance", "${spanner.instance}");
        defaultParameters.addArgument("database", "${spanner.database}");
        defaultParameters.addArgument("minSession", "30");
        defaultParameters.addArgument("maxSession", "30");
        defaultParameters.addArgument("grpcChannels", "30");
        defaultParameters.addArgument("samplePercentage", ".5");
        defaultParameters.addArgument("sql", "select  * from users limit 3;");
        return defaultParameters;
    }


    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        ArrayList<Struct> resultArray = new ArrayList();

        // pull parameters
        Integer staleReadSeconds = new Integer(context.getParameter("staleReadSeconds", "0"));
        String sql = context.getParameter("sql");
        String database = context.getParameter("database");

        String sampleName = context.getParameter("TestElement.name");
        Double samplePercentage = new Double(context.getParameter("samplePercentage"));
        Scope tracerScope = null;
        tracerScope = tracer.spanBuilder(sampleName).setSampler(Samplers.alwaysSample()).startScopedSpan();//Samplers.probabilitySampler(samplePercentage)).startScopedSpan();


        SampleResult result = new SampleResult();
        try {
            sql = resolveVariables(context, sql);

            result.setSampleLabel(context.getParameter("TestElement.name"));
            result.setSamplerData(sql);
            result.setDataType(SampleResult.TEXT);
            result.setContentType("text/plain");
            result.setThreadName(Thread.currentThread().getName());
            result.setDataEncoding("UTF-8");



            // Queries the database
            DatabaseClient dbClient = dbClientMap.get(database);
            result.sampleStart(); // start stopwatch

            if (sql.trim().toUpperCase().startsWith("SELECT")) {

                //get single use transaction scope
                ReadContext rc = null;
                try {
                    if (staleReadSeconds == 0) {
                        rc = dbClient.singleUse();
                    } else {
                        rc = dbClient.singleUse(TimestampBound.ofMaxStaleness(staleReadSeconds, TimeUnit.SECONDS));
                    }

                    try (ResultSet resultSet = rc.executeQuery(Statement.of(sql))) {
                        //pull data over the wire
                        while (resultSet.next()) {
                            resultArray.add(resultSet.getCurrentRowAsStruct());
                        }
                    }

                    result.sampleEnd();
                    result.setResponseMessage(resultArray.toString());
                }finally {
                    if( rc != null) rc.close();
                }

                //Span Attributes
                Span span = tracer.getCurrentSpan();
                span.setStatus(Status.OK);
                span.end();

            } else {
                throw new RuntimeException("Unsupport SQL - SELECT only queries are support");
            }


        } catch (Exception ex) {
            ex.printStackTrace();

            //set result
            result.sampleEnd(); // stop stopwatch
            result.setSuccessful(false);
            result.setResponseMessage(ex.getMessage());
            result.setResponseCode("500");

            //export error
            Span span = tracer.getCurrentSpan();
            span.setStatus(Status.UNKNOWN.withDescription(ex.getMessage()));
            span.end();

        } finally {
            if( tracerScope != null ) tracerScope.close();
        }

        result.setSuccessful(true);
        String resultsStr = serializeResults(resultArray);
        result.setResponseData(resultsStr, "UTF-8");
        result.setResponseCodeOK(); // 200 code

        return result;
}

    private String resolveVariables(JavaSamplerContext context, String sql) {
        JMeterVariables jMeterVariables = context.getJMeterVariables();
        Iterator itr = jMeterVariables.getIterator();
        while (itr.hasNext()){
            Map.Entry<String, Object> item = (Map.Entry<String, Object>)itr.next();
            sql = sql.replaceAll("\\$\\{" +item.getKey() +"}", item.getValue().toString());
        }
        return sql;
    }

    private String serializeResults(ArrayList<Struct> resultArray) {

        ArrayList<Map<String, Object>> rows = new ArrayList();

        for (Struct struct : resultArray) {
            Map<String, Object> row = new HashMap();

            List<Type.StructField> fields = struct.getType().getStructFields();
            for (Type.StructField field : fields) {

                if (!struct.isNull(field.getName())) {
                    if (field.getType().equals(Type.string())) {
                        row.put(field.getName(), struct.getString(field.getName()));
                    } else if (field.getType().equals(Type.float64())) {
                        row.put(field.getName(), struct.getDouble(field.getName()));
                    } else if (field.equals(Type.int64())) {
                        row.put(field.getName(), struct.getLong(field.getName()));
                    } else if (field.getType().equals(Type.bool())) {
                        row.put(field.getName(), struct.getBoolean(field.getName()));
                    } else if (field.getType().equals(Type.date())) {
                        row.put(field.getName(), struct.getDate(field.getName()));
                    } else if (field.getType().equals(Type.timestamp())) {
                        row.put(field.getName(), struct.getTimestamp(field.getName()));
                    } else {
                        row.put(field.getName(), null);
                    }
                } else {
                    row.put(field.getName(), null);
                }
            }
            rows.add(row);
        }

        try {
            return new ObjectMapper().writeValueAsString(rows);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}