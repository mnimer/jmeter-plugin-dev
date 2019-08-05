package com.google.swarm;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.jmeter.threads.JMeterContext;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;



public final class Utils {
	
    public static GoogleCredential authenticate(GoogleCredential credentials) {
    	if (credentials == null) {
			return createCredentials();
    	}
    	else if (credentials.getExpiresInSeconds() <= 0 || credentials.getExpiresInSeconds() == null) {
    		try {
    	        System.out.println("Credentials expired. Refreshing...");
				credentials.refreshToken();
			}
    		catch (IOException e) {
    			System.out.println("Credential refresh failed. Recreating...");
				e.printStackTrace();
				return createCredentials();
			}
    	}
    	return null;
    }
    
    private static GoogleCredential createCredentials() {
    	
    	GoogleCredential credentials = null;
    	
        ArrayList<String> scopes = new ArrayList<String>();
        scopes.add("https://www.googleapis.com/auth/cloud-healthcare");
        scopes.add("https://www.googleapis.com/auth/cloud-platform");

        try {
			credentials = GoogleCredential.getApplicationDefault();
		} 
        catch (IOException e) {
			System.out.println("Problem getting default credentials");
			e.printStackTrace();
		}
        
        credentials = credentials.createScoped(scopes);
        credentials.setExpiresInSeconds( new Long(864000 * 31) ); //1 month
        System.out.println("Created new credentials");
        
        return credentials;
    }
    
    public static Map<String, Object> getContextVariableMap(JMeterContext ctx) {
    	Set<Entry<String, Object>> varSet = ctx.getVariables().entrySet();
    	Map<String, Object> varMap = new HashMap<String, Object>();
    	
    	for(Entry<String, Object> entry : varSet)
        {
    		varMap.put(entry.getKey(), entry.getValue());
        }
    	return varMap;
    }

}
