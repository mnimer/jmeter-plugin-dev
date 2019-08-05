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
import java.util.Iterator;

import org.apache.jmeter.JMeter;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSampler;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.*;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;


/**
 * Java Sampler for the different Spanner Queries tied to the GUI
 *
 * @Author Mike Nimer
 * @Author John-Paul Zebrowski
 */
public class SpannerSDKSampler extends JavaSampler implements Serializable {

	// following JavaSampler
	private static transient JavaSamplerClient javaClient = null;
	private static transient JavaSamplerContext context = null;
	private String queryType = "READ";

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public SpannerSDKSampler() {
        super();
        super.setClassname(SpannerSDKSampler.class.getName());
    }

    @Override
    public void testStarted() {
        super.testStarted();


        //Create Context with GUI properties
        Arguments args = new Arguments();
        args.addArgument(TestElement.NAME, getName());
        Iterator props = this.propertyIterator();
        while( props.hasNext() ) {
            JMeterProperty property = (JMeterProperty)props.next();
            if( !property.getName().startsWith("Test")) {
                args.addArgument(property.getName(), property.getStringValue());
            }
        }
        context = new JavaSamplerContext(args);


        //Create Matching Client
        if( this.queryType.equals("READ")){
            javaClient = new ReadQuery();
            javaClient.setupTest(context);
        }else if( this.queryType.equals("INSERT")){
            javaClient = new InsertQuery();
            javaClient.setupTest(context);
        }
    }




    @Override
	public SampleResult sample(Entry entry) {
		return javaClient.runTest(context);
	}




    @Override
    public void testEnded() {
        super.testEnded();
        javaClient.teardownTest(context);
    }


    public void teardownTest(JavaSamplerContext context) {
        //do nothing
        System.out.println("teardownTest");
    }

}