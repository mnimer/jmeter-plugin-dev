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

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.*;



/**
 * The specific logic required to run INSERT data using the Google Spanner Mutation API
 *
 * @Author Mike Nimer
 * @Author John-Paul Zebrowski
 */
public class InsertQuery extends AbstractJavaSamplerClient implements Serializable {
    private static final Logger log = LoggingManager.getLoggerForClass();

    Spanner spanner = null;
    static DatabaseClient dbClient = null;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);

        String project = context.getParameter("project");
        String instance = context.getParameter("instance");
        String database = context.getParameter("database");

        // Instantiates a client
        try {

            //Instead of starting with 0, set min == max at startup for prod
            SessionPoolOptions sessionPoolOptions = SessionPoolOptions.newBuilder()
                .setMinSessions(2)
                .setMaxSessions(context.getJMeterContext().getThreadGroup().getNumberOfThreads() +10)
                .setWriteSessionsFraction(0)
                .build();

            SpannerOptions.Builder builder = SpannerOptions.newBuilder();
            builder.setSessionPoolOption(sessionPoolOptions);
            SpannerOptions options = builder.build();

            //spanner = options.getService();
            // Creates a database client
            synchronized (database) {
                dbClient =  options.getService().getDatabaseClient(DatabaseId.of(project, instance, database));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage());
        }


    }


    @Override
    public void teardownTest(JavaSamplerContext context) {
        super.teardownTest(context);
        //System.out.println("TeardownTest");
        //log.info("TeardownTest");

        // Closes the client which will free up the resources used
        if (spanner != null) {
            spanner.close();
        }
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("project", "${project}");
        defaultParameters.addArgument("instance", "${spanner.instance}");
        defaultParameters.addArgument("database", "${spanner.database}");
        defaultParameters.addArgument("table", "");
        defaultParameters.addArgument("columns", "col1,col2,col3");
        defaultParameters.addArgument("values", "a,b,c");
        defaultParameters.addArgument("types", "STRING,STRING,STRING");
        defaultParameters.addArgument("dateParsePattern", "yyyy-MM-dd");
        defaultParameters.addArgument("timestampParsePattern", "yyyy-MM-dd HH:mm:ss");
        return defaultParameters;
    }


    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        // pull parameters
        String[] columns = context.getParameter("columns").split(",");
        String[] values = context.getParameter("values").split(",");
        String[] types = context.getParameter("types").split(",");
        String datePattern = context.getParameter("dateParsePattern");
        String timestampPattern = context.getParameter("timestampParsePattern");
        String table = context.getParameter("table");
        String database = context.getParameter("database");

        SampleResult result = new SampleResult();
        result.setSampleLabel(context.getParameter("TestElement.name"));
        result.setSamplerData(columns +" | " +types +" | " +values);
        result.setDataType(SampleResult.TEXT);
        result.setContentType("text/plain");
        result.setThreadName(Thread.currentThread().getName());
        result.setDataEncoding("UTF-8");

        try {

            //get Spanner mutation
            Mutation mutation = getMutation(table, columns, values, types, datePattern, timestampPattern);

            //execute write
            result.sampleStart(); // start stopwatch
            Timestamp commitTimestamp = dbClient.write(Arrays.asList(mutation));
            result.sampleEnd();


            result.setSuccessful(true);
            result.setResponseCodeOK(); // 200 code
            result.setResponseData("Commit Timestamp: " +commitTimestamp.toString(), "UTF-8");
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            result.sampleEnd(); // stop stopwatch
            result.setSuccessful(false);
            result.setResponseCode("500");
            result.setResponseMessage("Exception: " + e);
            return result;
        }


    }

    private Mutation getMutation(String table, String[] columns, String[] values, String[] types, String datePattern, String timestampPattern) {

        if( columns.length != values.length && columns.length != types.length ){
            throw new RuntimeException("Columns, Values, and Types are all required for all fields.");
        }
        if (table == null) {
            throw new RuntimeException("Table name is required");
        }


        Mutation.WriteBuilder builder = Mutation.newInsertBuilder(table);
        for (int i = 0; i < columns.length; i++) {
            String _column = columns[i];
            String _value = values[i];
            String _type = types[i];


            if (_type.equals("INT64")) {
                builder.set(_column).to(parseLong(_column, _value));
            } else if (_type.equals("FLOAT64")) {
                builder.set(_column).to(parseFloat(_column, _value));
            } else if (_type.equals("BOOL")) {
                builder.set(_column).to(parseBoolean(_column, _value));
            } else if (_type.equals("DATE")) {
                builder.set(_column).to(parseDate(_column, _value, datePattern));
            } else if (_type.equals("TIMESTAMP")) {
                builder.set(_column).to(parseTimestamp(_column, _value, timestampPattern));
            } else if (_type.equals("STRING")) {
                builder.set(_column).to(parseString(_column, _value));
            }else{
                throw new RuntimeException("Unknown data type: " +_type);
            }

        }
        return builder.build();
    }



    Long parseLong(String key, Object o)
    {
        if( o == null ){
            return (Long)o;
        }else {
            try {
                return new Long(o.toString());
            } catch (Exception ex) {
                throw new RuntimeException("Parse Error for '" + key + "' LONG '" + o.toString() + "'");
            }
        }
    }

    Float parseFloat(String key, Object o)
    {
        if( o == null ){
            return (Float)o;
        }else {
            try {
                return new Float(o.toString());
            } catch (Exception ex) {
                throw new RuntimeException("Parse Error for '" + key + "'  FLOAT '" + o.toString() + "'");
            }
        }
    }

    Boolean parseBoolean(String key, Object o)
    {
        if( o == null ) {
            return (Boolean) o;
        }else {
            try {
                return new Boolean(o.toString());
            } catch (Exception ex) {
                throw new RuntimeException("Parse Error for '" + key + "' BOOLEAN '" + o.toString() + "'");
            }
        }
    }


    SimpleDateFormat dateFormat = null;
    com.google.cloud.Date parseDate(String key, Object o, String pattern)
    {
        if( o == null ){
            return (com.google.cloud.Date)o;
        }else if (o.toString().startsWith("0000-00-00")) { //return null for special date format
            return null;
        }else {
            try {
                if (dateFormat == null) {
                    dateFormat = new SimpleDateFormat(pattern);
                }
                java.util.Date d = dateFormat.parse(o.toString());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(d);
                return com.google.cloud.Date.fromYearMonthDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_WEEK));
            } catch (ParseException ex) {
                throw new RuntimeException("Parse Error for '" + key + "' DATE '" + o.toString() + "'");
            }
        }
    }


    SimpleDateFormat timestampFormat = null;
    com.google.cloud.Timestamp parseTimestamp(String key, Object o, String pattern)
    {
        if( o == null ){
            return (com.google.cloud.Timestamp)o;
        }else if (o.toString().startsWith("0000-00-00")) { //return null for special date format
            return null;
        }else {
            try {
                if (timestampFormat == null) {
                    timestampFormat = new SimpleDateFormat(pattern);
                }

                return com.google.cloud.Timestamp.of(timestampFormat.parse(o.toString()));
            } catch (ParseException ex) {
                throw new RuntimeException("Parse Error for '" + key + "' TIMESTAMP '" + o.toString() + "'");
            }
        }
    }

    String parseString(String key, Object o)
    {
        if( o == null ){
            return (String)o;
        }else {
            try {
                return o.toString();
            } catch (Exception ex) {
                throw new RuntimeException("Parse Error for '" + key + "' STRING '" + o.toString() + "'");
            }
        }
    }

}