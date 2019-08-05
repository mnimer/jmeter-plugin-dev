package com.google.swarm.gui;

import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;

public interface SimpleSelector {

	public HashMap<String, AbstractJavaSamplerClient> initClientMap();
    
	public LinkedHashSet<String> initSelector();

    public String getStaticLabel();
    
	public String getLabelResource();
    
//    public TestElement createTestElement();
}
