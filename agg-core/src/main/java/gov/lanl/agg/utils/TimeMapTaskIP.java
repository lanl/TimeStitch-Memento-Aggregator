package gov.lanl.agg.utils;

import gov.lanl.agg.CdxParser;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.LinkParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

public class TimeMapTaskIP implements Callable<Map>{
    String url;
 
    int hostid;
   
    HttpClient client;
    String mementoprefix=null;
    String timemap=null;
   // String date;
	public TimeMapTaskIP(int hostid,String url,HttpClient client) {
		this.url = url;
		//this.cc =cc;
		this.hostid = hostid;
		this.client = client;
		//this.date=date;
	}
	public TimeMapTaskIP(int hostid,String url,HttpClient client,String mementoprefix,String timemap) {
		this.url = url;
		//this.cc =cc;
		this.hostid = hostid;
		this.client = client;
		this.mementoprefix = mementoprefix;
		this.timemap = timemap;
		//this.date=date;
	}
	
	@Override
	public Map call() throws Exception  {
		
		 Map map = null;
		 RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).
				 setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		 //.setLocalAddress(InetAddress.getByAddress(ip1)).build();
		 HttpGet httpget = new HttpGet(url);
		 httpget.setHeader("Cache-Control", "no-cache");
         //httpget.setConfig(requestConfig);
       
		try { map = client.execute(httpget,handler);}
		 finally {  
			 httpget.releaseConnection();
			}
			
			return map;
		
	}
	
	 private final ResponseHandler<Map> handler = new ResponseHandler<Map>() {
	        @Override
	        public Map handleResponse(final HttpResponse response)
	                throws ClientProtocolException, IOException {
	            return sortResponse(response);
	        }
	    };

	    
	    private Map sortResponse(final HttpResponse httpResponse) throws IOException {
	        StringBuilder builder = null;
	        Map map = new HashMap();
	        
	        if (httpResponse != null) {
	        	int status = httpResponse.getStatusLine().getStatusCode();
	            switch (status) {
	                case HttpStatus.SC_OK:
	                    final HttpEntity entity = httpResponse.getEntity();
	                    if (entity != null) {

	                        final InputStreamReader instream = new InputStreamReader(entity.getContent());
	                        try {
	                            final BufferedReader reader = new BufferedReader(instream);
	                            builder = new StringBuilder();
	                            String currentLine = null;
	                            currentLine = reader.readLine();
	                            while (currentLine != null) {
	                                builder.append(currentLine).append("\n");
	                                currentLine = reader.readLine();
	                            }
	                           String  ms = builder.toString();
	                            if (mementoprefix!=null) {
	                    		    
	               		    	 CdxParser parser = new CdxParser(ms);
	               		    	 parser.parse(mementoprefix,timemap,url);
	               				 LinkHeader linkheader = parser.getHeader();
	               				 linkheader.setStatus(status);
	               				 map.put(new Integer(hostid),linkheader);
	               				
	               		    }
	               		    else {	    	
	               		    	   LinkParser parser = new LinkParser(ms); 
	               		    	   parser.parse();
	               			       LinkHeader linkheader = parser.getHeader();
	               			       linkheader.setStatus(status);
	               			       map.put(new Integer(hostid),linkheader); 
	               		    	
	               			     }
	                            
	                            
	                        } finally {
	                            instream.close();
	                        }
	                    }
	                    break;
	                default:
	                	LinkHeader lh = new LinkHeader();
	       			    lh.setStatus(status);	       		
	       			    map.put(hostid, lh);
	                    
	            }
	        }
	        
	        
	        return map;
	    }
	}    
	    
