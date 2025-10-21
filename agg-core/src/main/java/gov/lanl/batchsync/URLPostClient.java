package gov.lanl.batchsync;

import gov.lanl.agg.utils.MementoUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class URLPostClient {

	
	  String postpoint = null; 
	  String timemapurl="";
	  String batchdownloadurl="";
	  HttpClient mClient = new HttpClient(new MultiThreadedHttpConnectionManager());
	   
	  // HttpClient mClient;
	   
	  /*  public URLPostClient(String endpoint,HttpClient mclient) {
	    	 postpoint=endpoint;
	    	 mClient=mclient;
	    }
	    public URLPostClient(HttpClient mclient) {
	    	 mClient=mclient;
	    }
	   */
	    public URLPostClient(String endpoint) {
	    	 postpoint=endpoint;
	    	
	    }
	    public URLPostClient() {
	    	
	    }
	    public void setTimeMapUrl(String url) {
	    	timemapurl=url;
	    }
	    
	    public void setBatchDownloadUrl(String url) {
	    	batchdownloadurl=url;
	    }
	    
	    
	    public String get_timemap(String url) {
	    	System.out.println("download:"+batchdownloadurl+url);
	    	GetMethod mGet = new GetMethod(batchdownloadurl+url);
	    	try {
	    		 int statusCode = mClient.executeMethod( mGet);
			     if (statusCode==200) {
				 String result = mGet.getResponseBodyAsString();
				 return result;
			     }
			     if (statusCode==404) {
			    	 return "404";
			     }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	finally{
	    	 mGet.releaseConnection(); }
	    	return null;
	    	
	    }
	    
	    
	    
	    public void seedurl_put(String item) {
	    	  PostMethod mPut = new PostMethod(postpoint);
	    	  mPut.addRequestHeader("Content-Type", "application/xml");
	    	  
	    	   try {
	    		   		  
	 	    	  int k = item.indexOf("|");
	 	    	  String idate = item.substring(0, k);
	 	    	 // System.out.println("seed date:"+idate);
	 	    	  String url = item.substring(k+1);
	 	    	 
	 	    	 // long g = Long.parseLong(idate);
	 	    	 // Date ds = new Date(Long.parseLong(idate));
	 	    	  Date ds = new Date();
	 	    	  ds.setTime(Long.parseLong(idate));
	 	    	  String pload=composeXML(url,  MementoUtils.timeTravelJsFormatter.format(ds),timemapurl);
	 	  	      //System.out.println("test notify:"+pload);	
	 	  	      InputStream is = new ByteArrayInputStream(pload.getBytes());
	 	    	  mPut.setRequestBody(is);
	 	    	  int returnCode = mClient.executeMethod(mPut);
	 	    	  System.out.println("publisher returncode:"+  returnCode);
	 	    	  is.close();
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	   // Header[] headers = mPut.getResponseHeaders();
			    // for ( int i = 0; i < headers.length; ++i){
				  //    System.out.println( headers[i]);
				    //  }
			     
	    	 finally{  mPut.releaseConnection();
	    	 }
	    }
	    
	    public void resultsurl_put(String url,String udate) {
	    	  PostMethod mPut = new PostMethod(postpoint);
	    	  mPut.addRequestHeader("Content-Type", "application/xml");
	    	  
	    	   try {
	    		    String pload=composeXML(url, udate,batchdownloadurl);
	 	  	       // System.out.println("batch test notify:"+pload);	
	 	  	        InputStream is = new ByteArrayInputStream(pload.getBytes());
	 	    	    mPut.setRequestBody(is);
	    		   
	    		   
	    		   
	 	    	   int returnCode = mClient.executeMethod( mPut);
				  
	 	    	   System.out.println(" batch publish returncode:"+  returnCode);
	 	    	   is.close();
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	   // Header[] headers = mPut.getResponseHeaders();
			    // for ( int i = 0; i < headers.length; ++i){
				  //    System.out.println( headers[i]);
				    //  }
			 finally {    
	    	   mPut.releaseConnection();
			 }
	    	
	    }
	    
	   
	   /* public void resultsurl_put(String url,String udate, Map <String,List> result) {
	    	
	    	 
	    	  //String item = new String(bigQueue.dequeue());
	    	  //int k = item.indexOf("|");
	    	  //String idate = item.substring(0, k-1);
	    	  
	    	  //date to http format
	    	  PostMethod mPost = new PostMethod(postpoint+"/"+udate+"/"+url);
	    	  //String url = item.substring(k+1);
	    	  //iterate result. fil payload
	    	  Iterator it = result.entrySet().iterator();
	    	  StringBuffer sb=new StringBuffer();
	    	  while (it.hasNext()) {
	    	        Map.Entry pairs = (Map.Entry)it.next();
	    	        String archive = (String) pairs.getKey();
	    	        List ltmp = (List) pairs.getValue();
	    	        Iterator lt= ltmp.iterator();
	    	        while (lt.hasNext()) {
	    	        	Link link = (Link) lt.next();
	    	        	if (link.getDatetime()!=null) {
	    	        	sb.append(archive +","+ link.getDatetime() + "," +link.getHref()+"\n");
	    	        	}
	    	        }
	    	  }
	    	  String pload = sb.toString();	
	    	  System.out.println("batch test post results:"+pload);
	  	      InputStream is = new ByteArrayInputStream(pload.getBytes());
	    	  mPost.setRequestBody(is);
	    	   try {
				mClient.executeMethod( mPost);
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	   
			     
	    	   mPost.releaseConnection();
	    	
	    }
	    */
	    
	    
	  
      
	    public String composeXML(String url,String mdate,String timemapurl){
	    	
	    	StringBuffer sb = new StringBuffer();
	    	sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" ");
	        sb.append("xmlns:rs=\"http://www.openarchives.org/rs/terms/\">\n");
	        sb.append("<url>");
	        sb.append("<rs:ln rel=\"about\"  href=\""+url+"\"/>");
	        String turl = timemapurl +url;
	        sb.append("<loc>"+turl+"</loc>");
	        sb.append("<lastmod>"+mdate+"</lastmod>");
	        sb.append("<rs:md change=\"updated\"/>");
	        sb.append("</url>");
	        sb.append("</urlset>");
	        return sb.toString();
	    	}
	
	
	
	
}
