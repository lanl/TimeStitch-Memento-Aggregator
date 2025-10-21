package gov.lanl.agg.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.jayway.jsonpath.JsonPath;

public class MLClient {
    	    
	    //this class looking for ml info json format
	// static Logger logml ;
	// static {
	// logml = Logger.getLogger("mlservice");
	 //}
	public List checkUrl(HttpClient client, String feedUrl){
		return checkUrl( client,  feedUrl,null);
	}
	    
	    public List checkUrl(HttpClient client, String feedUrl,List blist) {
  	   
  	       List<String> alist = null;
  	       //List<String> blist = null;
  	   
           //DefaultHttpMethodRetryHandler retryHandler = new DefaultHttpMethodRetryHandler(0, false);
  	       GetMethod method = new GetMethod(feedUrl);
  	      // method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);	    
  	      // method.setFollowRedirects(false);
  	        method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
         
  	    try {
  	        // request feed
  	            int statusCode = client.executeMethod(method);  	          
  	            if (statusCode==200) {
  	            	String jsonstr = method.getResponseBodyAsString();
  	            	String jsonpath = "$.recommendNoRequest[*].archive";
  	            	        alist  = JsonPath.read(jsonstr, jsonpath);
  	            	System.out.println("ml predict"+alist.toString());
  	            	String jsonpathrec ="$.recommendRequest[*].archive";
  	            	//this creates new arraylist:
  	            	ArrayList tlist = JsonPath.read(jsonstr, jsonpathrec);
  	            	//some trick to pass by reference
  	            	blist.addAll(tlist);
  	            	String liststring = String.join(":", blist);
  	                System.out.println("mlclient"+liststring);
  	                
  	            	// logml.info( logstring +","+ blist );	
  	            	
  	            }
  	            
  	           	           
  	    } catch (IOException ioe) {
  	       
  	    }
  	    finally { method.releaseConnection(); }
  	    if (alist==null)  alist = new ArrayList();
  	    return alist;
  	}
      
      
	
	
	
	
}
