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
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class RetrieveSnapshotsAndRawEvents extends BigtableSamplerClient implements Serializable {

    @Override
    public Arguments getDefaultParameters()
    {
        // grab the parent's default parameters which include project, instance and appProfile
        Arguments defaultParameters = super.getDefaultParameters();

        // add the parameters we need here
        defaultParameters.addArgument( "columnFamily","" );
        defaultParameters.addArgument( "columns", "");
        defaultParameters.addArgument( "rowkeyStart", "");
        defaultParameters.addArgument("rowkeyEnd", "");
        defaultParameters.addArgument( "filterColumn1Name", "");
        defaultParameters.addArgument("filterColumn1Value", "");
        defaultParameters.addArgument( "filterColumn2Name", "");
        defaultParameters.addArgument( "filterColumn2RegEx", "");

        return defaultParameters;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        String rowkeyStart = context.getParameter("rowkeyStart");
        String rowkeyEnd   = context.getParameter("rowkeyEnd");
        String columnFamily = context.getParameter( "columnFamily");
        String columns = context.getParameter("columns");
        String table = context.getParameter("table");
        String filterColumn1Name = context.getParameter("filterColumn1Name");
        String filterColumn1Value = context.getParameter("filterColumn1Value");
        String filterColumn2Name = context.getParameter("filterColumn2Name");
        String filterColumn2RegEx = context.getParameter( "filterColumn2RegEx");

        String data = "RowKeyStart: " + rowkeyStart + " RowKeyEnd: " + rowkeyEnd + " Columns " + columns +
                " FilterCol1Name/Value: " + filterColumn1Name + "/" + filterColumn1Value;

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
//            throwExceptionIfMissing(rowkeyEnd, "rowkeyEnd");
//            throwExceptionIfMissing(columns, "columnFamily");
//            throwExceptionIfMissing(columns, "columns");



            try (Table dbTable = getConnection().getTable(TableName.valueOf(table)))
            {
                result.sampleStart();

                Filter filter = buildFilterWithRegEx(columnFamilyName, filterColumn1Name, filterColumn1Value,
                        filterColumn2Name, filterColumn2RegEx);

                List<Map<String, String>> resultSet = searchRangeWithFilter(dbTable,
                        Bytes.toBytes(rowkeyStart), Bytes.toBytes(rowkeyEnd), filter, columnFamilyName, columnMap);

                result.setResponseData(objectToString(resultSet), "UTF-8");

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
