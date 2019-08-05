# JMeter GCP Plugins
This is a collection of JMeter classes to help load test the different GCP Services. 


## JMeter Setup
*Build and tested with JMeter 5.1*

 

When you create a new jmx test, you need to link this library. 

In the root node of the JMeter *.jmx test add two libraries
- The "jmeter-gcp-plugins-0.1.0-SNAPSHOT.jar" in the /target folder
- Add the "/target/lib" folder. 

Increase the memory used by JMeter
```bash
export HEAP="-Xms2g -Xmx2g -XX:MaxMetaspaceSize=512m"
```

You might need to set the credentials for the  Client to have permissions to access your instance via the GOOGLE_APPLICATION_CREDENTIALS via a service key.
For Example:
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/serviceaccount.json
```



## Samples
- examples\SpannerTest.jmx - show the different Spanner test variations
- more coming soon



##Listeners & Reports

### BQ Listener
tbd

- Sample DataStudio Report (todo)




##Test Classes

## Spanner
- Read Queries
- Insert Queries
- Update Queries (todo)
- Delete Queries (todo)




## Bigtable

To build and install in JMeter

Exit/quit out of JMeter

```bash
(Set to your specific location)
export JMETER_HOME=/Users/rossrick/tools/apache-jmeter-5.1

mvn clean package dependency:copy-dependencies
cp target/jmeter-bigtablesdk-1.0-SNAPSHOT.jar $JMETER_HOME/lib/ext/
rm target/dependency/ApacheJMeter_*

rm target/dependency/groovy-all-2.4.15.jar
rm target/dependency/Saxon-HE-9.8.0-12.jar
cp target/dependency/* $JMETER_HOME/lib/ext/
```



Start JMeter

To use the RetrieveSingleResult class, do the following:
* Add a thread group (skip if you already have a thread group set up)
* Add a Java Request (Under Sampler)
* Change the class to point to **com.google.swarm.samplers.bigtable.RetrieveSingleResult**
* Fill in the approipriate parameters, which include
* project (id)
* instance
* appProfile (if used)
* table
* rowkey - the search value
* columnFamily
* columns - a comma delimited list that you want returned

To use the RetrieveRange class do the following:
* Add a thread group (skip if you already have a thread group set up)
* Add a Java Request (Under Sampler)
* Change the class to point to **com.google.swarm.samplers.bigtable.RetrieveRange**
* Fill in the approipriate parameters, which include
* project (id)
* instance
* appProfile (if used)
* table
* columnFamily
* columns - a comma delimited list that you want returned
* rowkeyStart - the first row to start the search on
* rowkeyEnd - the last row to include in the search

To use the RetrieveRangeWithFilter class do the following:
* Add a thread group (skip if you already have a thread group set up)
* Add a Java Request (Under Sampler)
* Change the class to point to **com.google.swarm.samplers.bigtable.RetrieveRangeWithFilter**
* Fill in the approipriate parameters, which include
* project (id)
* instance
* appProfile (if used)
* table
* columnFamily
* columns - a comma delimited list that you want returned
* rowkeyStart - the first row to start the search on
* rowkeyEnd - the last row to include in the search
* filterColumn1Name - The name of the column you want to filter on
* filterColumn1Value - The value of the first column you want to include
* filterColumn2Name - (Optional) the name of the second column you want to filter on
* filterColumn2Value - (Optional) the value of the second column that you want to include
If you use both filter1 and filter2, the operation is treated as an OR operation, meaning that if the record includes the value for filter1 or filter2 it will be returned

To use the RetrieveRangeWithLimit class do the following:
* Add a thread group (skip if you already have a thread group set up)
* Add a Java Request (Under Sampler)
* Change the class to point to **com.google.swarm.samplers.bigtable.RetrieveWithLimit**
* Fill in the approipriate parameters, which include
* project (id)
* instance
* appProfile (if used)
* table
* columnFamily
* columns - a comma delimited list that you want returned
* rowkeyStart - the first row to start the search on
* limit - the number of records to limit the search to

To use the RetrieveSnapshotsAndRawEvents class do the following:
* Add a thread group (skip if you already have a thread group set up)
* Add a Java Request (Under Sampler)
* Change the class to point to **com.google.swarm.samplers.bigtable.RetrieveSnapshotsAndRawEvents**
* Fill in the approipriate parameters, which include
* project (id)
* instance
* appProfile (if used)
* table
* columnFamily
* columns - a comma delimited list that you want returned
* rowkeyStart - the first row to start the search on
* rowkeyEnd - the last row to include in the search
* filterColumn1Name - The name of the column you want to filter on
* filterColumn1Value - The value of the first column you want to include
