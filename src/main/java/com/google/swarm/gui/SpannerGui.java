package com.google.swarm.gui;

import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;

import com.google.swarm.samplers.spanner.InsertQuery;
import com.google.swarm.samplers.spanner.ReadQuery;


@GUIMenuSortOrder(12)
public class SpannerGui extends SimpleSelectorGui implements SimpleSelector {
	
    private static final long serialVersionUID = 234L;
    
    public SpannerGui() {
        super();
    }
    
    @Override
	public HashMap<String, AbstractJavaSamplerClient> initClientMap() {
    	HashMap<String, AbstractJavaSamplerClient> clientMap = new HashMap<String, AbstractJavaSamplerClient>();
		clientMap.put("INSERT", new InsertQuery());
		clientMap.put("READ", new ReadQuery());
		return clientMap;
	}
    
	@Override
	public LinkedHashSet<String> initSelector() {
		LinkedHashSet<String> selectorValues = new LinkedHashSet<String>(); 
		selectorValues.add("INSERT");
		selectorValues.add("READ");
		return selectorValues;
	}

    @Override
    public String getStaticLabel() {
        return "GCP Spanner Request"; 
    }
    
	@Override
	public String getLabelResource() {
		return "spanner_sdk_request";
	}

}
