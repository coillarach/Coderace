package net.coderace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import android.net.http.AndroidHttpClient;
import android.util.Log;

public class GameJSON{
	
	public String body = "";
	
	private static final String TAG = "GameJSON";

	GameJSON(String url) {
		final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
	    final HttpGet getRequest = new HttpGet(url);
	    
	    try {
	        Log.d(TAG, "Trying " +  url); 
	        HttpResponse response = client.execute(getRequest);
	        final int statusCode = response.getStatusLine().getStatusCode();
	        if (statusCode != HttpStatus.SC_OK) { 
	            Log.w(TAG, "Error " + statusCode + " while fetching from " + url);
	            body = "{'message':'Error'}";
	        }
	        
	        final HttpEntity entity = response.getEntity();
	        if (entity != null) {
	        	InputStreamReader isr = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
	            try {
	            	BufferedReader reader = new BufferedReader(isr);
	            	body = reader.readLine();
	            	Log.i(TAG, "Found: " + body);
	            } 
	            finally {
	                if (isr != null) {
	                    isr.close();  
	                }
	            }
	        }
	    } catch (SocketTimeoutException e) {
	        getRequest.abort();
	        Log.w(TAG, "Timeout while fetching from " + url, e);
	        body = "{'message':'Login attempt timed out. Try again'}";
	    } catch (Exception e) {
	        // Could provide a more explicit error message for IOException or IllegalStateException
	        getRequest.abort();
	        Log.w(TAG, "Error while fetching from " + url, e);
	        body = "{'message':'Error'}";
	    } finally {
	        if (client != null) {
	            client.close();
	        }
	    }
	}
}
