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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.cloud.bigtable.hbase.BigtableOptionsFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public abstract class BigtableSamplerClient extends AbstractJavaSamplerClient implements Serializable {

    private Connection connection;
    private static Table dbTable = null;

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("project", "");
        defaultParameters.addArgument("instance", "");
        defaultParameters.addArgument("appProfile", "");
        defaultParameters.addArgument("table", "");

        return defaultParameters;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);

        if (connection != null)
            return;

        // grab the connection properties
        String projectId = context.getParameter( "project");
        String instance = context.getParameter( "instance");
        String appProfileId = context.getParameter( "appProfile");
        String table = context.getParameter("table");

        try {
            Configuration config = BigtableConfiguration.configure( projectId, instance);
            config.setBoolean("google.bigtable.use.cached.data.channel.pool", true);

            // If we need App Profiles then configure that
            if (null != appProfileId && !appProfileId.isEmpty())
                config.set(BigtableOptionsFactory.APP_PROFILE_ID_KEY, appProfileId);

            connection =  BigtableConfiguration.connect(config);
            // dbTable = connection.getTable(TableName.valueOf(table));

        } catch (Exception e) {
            System.out.println("Exception while setting test up");
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        super.teardownTest(context);

        try {
            if (connection != null)
            {
                System.out.println("Closing the bigtable connection");
                connection.close();
                connection = null;
            }
        } catch(IOException ex)
        {
            System.out.println("Exception while closing bigtable connection");
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }

    public Connection getConnection()
    {
        return connection;
    }
    // public Table getTable() { return dbTable; }

    protected Table getTable(String tableName) throws IOException
    {
        // find the value by rowkey
        Table table = connection.getTable(TableName.valueOf(tableName));
        return table;
    }

    //
    protected Filter buildColumnExistsFilter(String colName)
    {
        ColumnPrefixFilter filter = new ColumnPrefixFilter(Bytes.toBytes(colName));
        return filter;
    }

    protected List<Map<String,String>> searchRangeWithFilter(Table table, byte[] rowkeyStart, byte[] rowkeyEnd, Filter filter,
                                                             byte[] columnFamilyName, Map<String,byte[]> columnMap) throws java.io.IOException
    {
        Scan scan = new Scan().withStartRow(rowkeyStart).withStopRow(rowkeyEnd, true);
        scan.setFilter(filter);

        ArrayList<Map<String, String>> resultSet = new ArrayList<Map<String, String>>();
        try (ResultScanner scanner = table.getScanner(scan)) {
            long count = 0;

            for (Result scannerResult = scanner.next(); scannerResult != null; scannerResult = scanner.next()) {
                count++;
                Map<String, String> result = processResult(scannerResult, columnFamilyName, columnMap);
                resultSet.add(result);
            }
        }

        return resultSet;
    }


    protected Filter buildFilter(byte[] columnFamilyName, String colName, String colValue)
    {
        byte[] colNameAsBytes = Bytes.toBytes(colName);
        byte[] colValueAsBytes = Bytes.toBytes(colValue);

        SingleColumnValueFilter filter = new SingleColumnValueFilter(columnFamilyName, colNameAsBytes, CompareFilter.CompareOp.EQUAL, colValueAsBytes );
        // Note: if the field is null and you don't include the line below, the row will be returned.
        filter.setFilterIfMissing(true);

        return filter;
    }

    protected FilterList buildFilterWithRegEx( byte[] columnFamilyName, String col1Name, String col1Value, String col2Name, String col2RegEx)
    {
        byte[] col1NameAsBytes = Bytes.toBytes(col1Name);
        byte[] col1ValueAsBytes = Bytes.toBytes(col1Value);
        byte[] col2NameAsBytes = Bytes.toBytes(col2Name);

        FilterList filter = new FilterList(FilterList.Operator.MUST_PASS_ONE);
        SingleColumnValueFilter filter1 = new SingleColumnValueFilter(columnFamilyName,
                col1NameAsBytes, CompareFilter.CompareOp.EQUAL, col1ValueAsBytes);
        // we don't want null (or missing) values
        filter1.setFilterIfMissing(true);
        filter.addFilter( filter1 );

        if (!isEmpty(col2Name))
        {
            SingleColumnValueFilter filter2 = new SingleColumnValueFilter(columnFamilyName, col2NameAsBytes, CompareFilter.CompareOp.EQUAL,
                    new RegexStringComparator( col2RegEx));
            filter2.setFilterIfMissing(true);
            filter.addFilter(filter2);
        }

        return filter;
    }

    protected FilterList buildFilter(byte[] columnFamilyName, String col1Name, String col1Value, String col2Name, String col2Value)
    {
        byte[] col1NameAsBytes = Bytes.toBytes(col1Name);
        byte[] col1ValueAsBytes = Bytes.toBytes(col1Value);
        byte[] col2NameAsBytes = Bytes.toBytes(col2Name);
        byte[] col2ValueAsBytes = Bytes.toBytes(col2Value);


        FilterList filter = new FilterList(FilterList.Operator.MUST_PASS_ONE);
        SingleColumnValueFilter filter1 = new SingleColumnValueFilter(columnFamilyName,
                col1NameAsBytes, CompareFilter.CompareOp.EQUAL, col1ValueAsBytes);
        // we don't want null (or missing) values
        filter1.setFilterIfMissing(true);
        filter.addFilter( filter1 );

        // do we have another filter?
        if (!isEmpty(col2Name))
        {
            SingleColumnValueFilter filter2 = new SingleColumnValueFilter(columnFamilyName,
                    col2NameAsBytes, CompareFilter.CompareOp.EQUAL, col2ValueAsBytes);
            // we don't want null (or missing) values
            filter2.setFilterIfMissing(true);
            filter.addFilter( filter2 );
        }

        return filter;
    }

    protected Map<String, String> processResult(Result result, byte[] columnFamilyName, Map<String, byte[]> columnMap)
    {
        Map<String, String> rowResult = new HashMap<>();

        String rowKey = Bytes.toString(result.getRow());
        rowResult.put( "rowkey", rowKey);

        Iterator it = columnMap.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, byte[]> pair = (Map.Entry) it.next();
            pair.getKey();

            // getValue returns the latest version
            String columnValue = Bytes.toString(result.getValue( columnFamilyName, pair.getValue()));
            rowResult.put(pair.getKey(), columnValue);
        }

        return rowResult;
    }

    // columnNames are comma delimeted without quotes
    protected Map<String, byte[]> buildColumnMap(String columnNames)
    {
        if (columnNames == null)
            return null;

        String[] columnArray = columnNames.split(",");
        Map<String, byte[]> columnMap = new HashMap<>();

        for(String column : columnArray)
        {
            columnMap.put(column, Bytes.toBytes(column));
        }

        return columnMap;
    }

    protected String objectToString(Object object)
    {
        try {
            return new ObjectMapper().writeValueAsString(object);
        }
        catch(JsonProcessingException e)
        {
            return "{}";
        }
    }

    protected void throwExceptionIfMissing(String str, String name)
    {
        if (isEmpty(str))
            throw new RuntimeException("Empty or missing parameter: " + name);
    }

    protected void throwExceptionIfMissing(Integer value, String name)
    {
        if (value == null)
            throw new RuntimeException("Missing parameter: " + name);
    }

    protected static boolean isEmpty(String str)
    {
        return (str == null || str.trim() == "");
    }

}
