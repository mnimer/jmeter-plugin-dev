package com.google.swarm.gui;

import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;

import com.google.swarm.samplers.bigtable.*;


@GUIMenuSortOrder(12)
public class BigTableGui extends SimpleSelectorGui implements SimpleSelector {
	
    private static final long serialVersionUID = 234L;
    
    public BigTableGui() {
        super();
    }
    
    @Override
	public HashMap<String, AbstractJavaSamplerClient> initClientMap() {
    	HashMap<String, AbstractJavaSamplerClient> clientMap = new HashMap<String, AbstractJavaSamplerClient>();
		clientMap.put("RETRIEVE RANGE", new RetrieveRange());
		clientMap.put("RETRIEVE RANGE WITH FILTER", new RetrieveRangeWithFilter());
		clientMap.put("RETRIEVE SINGLE RESULT", new RetrieveSingleResult());
		clientMap.put("RETRIEVE SNAPSHOTS AND RAW EVENTS", new RetrieveSnapshotsAndRawEvents());
		clientMap.put("RETRIEVE WITH LIMIT", new RetrieveWithLimit());
		return clientMap;
	}
    
	@Override
	public LinkedHashSet<String> initSelector() {
		LinkedHashSet<String> selectorValues = new LinkedHashSet<String>(); 
		selectorValues.add("RETRIEVE RANGE");
		selectorValues.add("RETRIEVE RANGE WITH FILTER");
		selectorValues.add("RETRIEVE SINGLE RESULT");
		selectorValues.add("RETRIEVE SNAPSHOTS AND RAW EVENTS");
		selectorValues.add("RETRIEVE WITH LIMIT");
		return selectorValues;
	}

    @Override
    public String getStaticLabel() {
        return "GCP BigTable Request"; 
    }
    
	@Override
	public String getLabelResource() {
		return "bigtable_request";
	}

}
