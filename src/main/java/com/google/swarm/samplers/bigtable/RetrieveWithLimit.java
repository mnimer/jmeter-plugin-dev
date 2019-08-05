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
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

public class RetrieveWithLimit extends BigtableSamplerClient implements Serializable {

    @Override
    public Arguments getDefaultParameters() {
        // grab the parent's default parameters which include project, instance and appProfile
        Arguments defaultParameters = super.getDefaultParameters();

        // add the parameters we need here
        defaultParameters.addArgument( "columnFamily","" );
        defaultParameters.addArgument( "columns", "");
        defaultParameters.addArgument( "rowkeyStart", "");
        defaultParameters.addArgument( "limit","200");

        return defaultParameters;
    }


    @Override
    public SampleResult runTest(JavaSamplerContext context) {
//        System.out.println("runTest() - Retrieve With Limit");

        String rowkeyStart = context.getParameter("rowkeyStart");
        String columnFamily = context.getParameter( "columnFamily");
        String columns = context.getParameter("columns");
        String table = context.getParameter("table");
        Integer limit = context.getIntParameter("limit");

        String data = "RowKeyStart: " + rowkeyStart + " Limit: " + limit + " Columns " + columns;

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
//            throwExceptionIfMissing(rowkeyStart, "rowkeyStart");
//            throwExceptionIfMissing(columns, "columnFamily");
//            throwExceptionIfMissing(columns, "columns");
//            throwExceptionIfMissing(limit, "limit");

            try (Table dbTable = getConnection().getTable(TableName.valueOf(table)))
            {
                result.sampleStart();
                Scan scan = new Scan().withStartRow(Bytes.toBytes(rowkeyStart));
                scan.setLimit( limit );

                try(ResultScanner scanner = dbTable.getScanner(scan))
                {
                    Result[] scannerResults = scanner.next( limit );

                    if (scannerResults.length == 0)
                    {
                        result.setResponseData( "No records found starting at rowkey: " + rowkeyStart, "UTF-8");
                    }
                    else
                    {

                        ArrayList<Map<String, String>> resultSet = new ArrayList<Map<String, String>>();
                        for(int i=0;i<scannerResults.length;i++)
                        {
                            resultSet.add( processResult( scannerResults[i], columnFamilyName, columnMap));
                        }

                        result.setResponseData( objectToString(resultSet), "UTF-8");
                    }
                }

                result.sampleEnd();
                result.setSuccessful(true);
                result.setResponseCodeOK();
            }

        }
        catch (Exception ex)
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
