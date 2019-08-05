package com.google.swarm.samplers;

import java.io.Serializable;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.*;
import org.apache.jmeter.testelement.TestElement;

public class GCPSimpleSampler extends AbstractSampler implements Serializable {

	private static final long serialVersionUID = 254L;
	private Class<?> javaClass = null;
	private transient JavaSamplerContext context;
    private transient JavaSamplerClient javaClient;
	
	public GCPSimpleSampler() {}

	@Override
	public SampleResult sample(Entry entry) {

		Arguments args = new Arguments();
		args.addArgument(TestElement.NAME, getName());
		context = new JavaSamplerContext(args);

		if (javaClient == null) {
			try {
				javaClass = Class.forName(getPropertyAsString(TestElement.TEST_CLASS));
				javaClient = (JavaSamplerClient) javaClass.newInstance();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			javaClient.setupTest(context);
		}
    
		return javaClient.runTest(context);
	}

}
