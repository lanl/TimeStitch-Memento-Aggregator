package gov.lanl.agg.utils;

import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.LinkParser;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.URIUtil;

public class TimeGateClient {

	   OriginalResource oresource;
	   public  Integer redirectCount = 0;  
	    public TimeGateClient() {
	    	oresource  = new  OriginalResource();
	    }
	    
	    public  OriginalResource getOriginalResource() {
	    	return oresource;
	    }
	    
	    //this class looking for timegate,timemap info
	    
	    public String checkFeedUrl(HttpClient client, String feedUrl,String date) {
  	      String response = feedUrl;
  	     // HostConfiguration hf = new HostConfiguration();
  	    
  	   /*   
  	    HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {
  	      public boolean retryMethod(
  	          final HttpMethod method, 
  	          final IOException exception, 
  	          int executionCount) {
  	          if (executionCount >= 1) {
  	              // Do not retry if over max retry count
  	              return false;
  	          }
  	          if (exception instanceof NoHttpResponseException) {
  	              // Retry if the server dropped connection on us
  	              return true;
  	          }
  	          if (!method.isRequestSent()) {
  	              // Retry if the request has not been sent fully or
  	              // if it's OK to retry methods that have been sent
  	              return true;
  	          }
  	          // otherwise do not retry
  	          return false;
  	      }
  	  };
  	       */   
  	      
  	      
           DefaultHttpMethodRetryHandler retryHandler = new DefaultHttpMethodRetryHandler(0, false);
  	       HttpMethod method = new HeadMethod(feedUrl);
  	       method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
  	     
  	       method.setFollowRedirects(false);
  	       method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
           Header header = new Header("Accept-Datetime", date);
           method.addRequestHeader(header);
        
  	    try {
  	        // request feed
  	            int statusCode = client.executeMethod(method);
  	            //System.out.println("status" +statusCode);
  	            System.out.println("update memento batch:"+statusCode +"url:"+feedUrl +date);

  	            if ((statusCode == 301) | (statusCode == 302) | (statusCode == 303)) {
  	        	     Header varyheader = method.getResponseHeader("Vary");
  	                 //check if timegate
  	        	     boolean addredirect = false;
  	        	     boolean tgflag= false;
  	                 Header location = method.getResponseHeader("Location");
  	                   if (varyheader==null){ addredirect = true;}
                         if (varyheader!=null){
                             if ( !varyheader.getValue().toLowerCase().contains("accept-datetime")) {
                                addredirect = true;
                             }
                             else {
                            	 tgflag = true;
                             }
                          }
  	                    if (addredirect && !location.getValue().equals("")) {
  	                    // recursively check URL until it's not redirected any more
  	                     System.out.println("redirect" + location.getValue());
  	                      Header link_msg = method.getResponseHeader("Link");
                		        if (link_msg!=null){
                			        String add_msg = link_msg.getValue();
                			        System.out.println("add_msg_index:"+add_msg);
                                    LinkParser parser = new LinkParser(add_msg);
                                    parser.parse();
                                    LinkHeader linkheadertmp = parser.getHeader();
                             //Link ltimegate = linkheadertmp.getLinkByRelationship("timegate");
                                    
                                    List<Link> slinks = linkheadertmp.getSpecialLinks();
                                     Iterator<Link> sli = slinks.iterator();
                                    while( sli.hasNext()){
                                    	Link lk= sli.next();
                                    	System.out.println(lk.getRelationship());
                                    	if (lk.getRelationship().equals("timemap index")){
                                    		oresource.setTimeMapIndexURI(lk.getHref());
                                    	}
                                    	
                                    }
                                    
                                    //Link ptimegate = linkheadertmp.getLinkByRelationship("index");
                                 // if (ptimegate!=null) {
                            	   // System.out.println("index timemap:"+ptimegate.getHref());  
                            	    //oresource.setTimeMapIndexURI(ptimegate.getHref());
                                   //}
                		          }
                		          String nexturl = location.getValue();
                		           // this creates infinit loop
                		           //if (nexturl.startsWith("https:")) {
          					        // nexturl = nexturl.replaceFirst("https:", "http:");
          					       //}
                		          
                		          if (!nexturl.startsWith("http")){
                		        	  //relative url //will not work if protocol relative
                		        	  URL fu= new URL(feedUrl);
                		        	  StringBuilder sb = new StringBuilder();
                		        	    sb.append(fu.getProtocol()).append("://");
                						sb.append(fu.getHost());

                							if(fu.getPort() != 80 
                								&& fu.getPort() != -1) {

                							sb.append(":").append(fu.getPort());
                						}
                		        	  
                		        	  nexturl = sb.toString()+nexturl;
                		        	
                		        	   //decoding problems in location field
                		        	  
                		        	    URL fuuu= new URL(nexturl);
                		        	    StringBuilder sbn = new StringBuilder();
                					  
                					  	sbn.append(fuuu.getProtocol()).append("://");
                						sbn.append(fuuu.getHost());
                			
                						if(fuuu.getPort() != 80 
                								&& fuuu.getPort() != -1) {
                			
                							sbn.append(":").append(fuuu.getPort());
                						 }
                			
                						 sbn.append(fuuu.getPath());               					
                						
                						if(fuuu.getQuery() != null) {
                							//System.out.println("query:"+tturl.getQuery());
                							String qu = URIUtil.decode(fuuu.getQuery(),"UTF-8");
                							sbn.append( URLEncoder.encode("?","UTF-8")).append(URIUtil.encodeQuery(qu,"UTF-8"));
                							
                						}
                			
                						 nexturl = sbn.toString();
                		        	  
                		        	  
                		        	  System.out.println(feedUrl+" vs relative url reconstruction:"+nexturl);
                		        	 
                		          }
                		          redirectCount++;
                                  if (redirectCount <= 10) {
                                      response = checkFeedUrl(client, nexturl,date);
                                  }
  	                             // response = checkFeedUrl(client,nexturl,date);
  	                     }
  	                    else {
  	            	     Header link_msg = method.getResponseHeader("Link");
                           String add_msg = link_msg.getValue();
                           System.out.println(add_msg);
                           LinkParser parser = new LinkParser(add_msg);
                           parser.parse();
                           LinkHeader  linkheader = parser.getHeader();
                          // linkheader.setHostId(ihost);
                           Link ltimegate = linkheader.getLinkByRelationship("timegate");
                           
                           if (ltimegate != null) {
                           oresource.setTimeGateURI(ltimegate.getHref());
                           }
                           
                           Link tm = linkheader.getLinkByRelationship("timemap");
                           if (tm!=null) {
                        	   System.out.println("set timemap"+tm.getHref());
                           oresource.setTimeMapURI(tm.getHref());
                           }
                           oresource.setOriginalURI(feedUrl);
                           oresource.setTimegateFlag();
                           Link ptimegate = linkheader.getLinkByRelationship("index");
                             if (ptimegate!=null) {
                          	 oresource.setTimeMapIndexURI(ptimegate.getHref());
                             }
  	                     }
  	            
  	                } else {
  	                	//other codes original resource as input parameter
  	                	if (statusCode==200) {
  	                		//System.out.println("timegate orig resource:"+feedUrl);
  	                		 Header link_msg = method.getResponseHeader("Link");
  	                		 if (link_msg!=null){
  	                			 String add_msg = link_msg.getValue();
  	                             LinkParser parser = new LinkParser(add_msg);
  	                             
  	                             parser.parse();
  	                             LinkHeader linkheadertmp = parser.getHeader();
  	                             Link ltimegate = linkheadertmp.getLinkByRelationship("timegate");
  	                             Link ptimegate = linkheadertmp.getLinkByRelationship("index");
  	                               if (ptimegate!=null) {
  	                            	 System.out.println("index timegate:"+ptimegate.getHref());  
  	                            	 oresource.setTimeMapIndexURI(ptimegate.getHref());
  	                               }
  	                               if (ltimegate!=null) {
  	                                String timegate = ltimegate.getHref();
  	                                oresource.setTimeGateURI(ltimegate.getHref());
  	                               response = checkFeedUrl(client,timegate,date);
  	                               }
  	                               
  	                		 }
  	                	}
  	            response = feedUrl;
  	         }
  	           
  	    } catch (Exception ioe) {
  	        response = feedUrl;
  	    }
  	    finally { method.releaseConnection(); }
  	    return response;
  	}
      
      
	
	
	
	
}
