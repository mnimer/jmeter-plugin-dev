/*
 * Copyright 2019 Google LLC
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

package com.google.swarm.samplers.bigtable;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.Serializable;
import java.util.Map;


public class RetrieveSingleResult extends BigtableSamplerClient implements Serializable {

    @Override
    public Arguments getDefaultParameters() {
        // grab the parent's default parameters which include project, instance and appProfile
        Arguments defaultParameters = super.getDefaultParameters();

        // add the parameters we need here
        defaultParameters.addArgument( "rowkey", "");
        defaultParameters.addArgument( "columnFamily","" );
        defaultParameters.addArgument( "columns", "");

        return defaultParameters;
    }


    // setup occurs in the base class
    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        String rowkey = context.getParameter("rowkey");
        String columnFamily = context.getParameter( "columnFamily");
        String columns = context.getParameter("columns");
        String table = context.getParameter("table");

        String data = "RowKey: " + rowkey + " Columns " + columns;

        byte[] columnFamilyName = Bytes.toBytes(columnFamily);

        Map<String, byte[]> columnMap = buildColumnMap(columns);

        SampleResult result = new SampleResult();
        result.setSampleLabel(context.getParameter("TestElement.name"));
        result.setSamplerData(data);
        result.setContentType("text/plain");
        result.setDataType(SampleResult.TEXT);
        result.setThreadName(Thread.currentThread().getName());
        result.setDataEncoding("UTF-8");

        try {

//            throwExceptionIfMissing(table, "table");
//            throwExceptionIfMissing(rowkey, "rowkey");
//            throwExceptionIfMissing(columns, "columnFamily");
//            throwExceptionIfMissing(columns, "columns");

            try (Table dbTable = getConnection().getTable(TableName.valueOf(table)))
            {
                result.sampleStart();
                Result getResult = dbTable.get(new Get(Bytes.toBytes(rowkey)));

                if (getResult.isEmpty())
                {
                    result.setResponseData( "No records found matching rowkey: " + rowkey, "UTF-8");
                }
                else
                {
                    Map<String, String> rowResult = processResult(getResult, columnFamilyName, columnMap);
                    result.setResponseData( objectToString(rowResult), "UTF-8");
                }

                result.sampleEnd();
                result.setSuccessful(true);
                result.setResponseCodeOK();
            }
            // find the value by rowkey
            //Table dbTable = getTable();

        } catch (Exception ex)
        {
            ex.printStackTrace();;
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseCode("500");
            result.setResponseMessage("Exception " + ex);
        }


        return result;
    }


}
