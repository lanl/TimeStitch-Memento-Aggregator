package gov.lanl.agg.resource;

import gov.lanl.agg.BatchMap;
import gov.lanl.agg.Link;
import gov.lanl.agg.batch.RunMeBatchTask;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.batchsync.URLPostClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

@Path("/batchcachein/{id:.*}")


//LIVE
//this is for downloading timemaps from remote  batch cache 
//post url  to this end point

public class LiveUpdateResource {
	
	  Date lastupd = null;
	
	 @POST
	 public Response notifyPOST(@Context UriInfo ui, @PathParam("id") String idp) {
		   URI baseurl = ui.getBaseUri();
		   URI ur = ui.getRequestUri(); 
		   String id = ur.toString().replaceFirst(baseurl.toString()+"batchcachein/", "");
		
		   
		    /*                                                                                                                                                       
           IBigQueue liveque = (IBigQueue) MyInitServlet.getInstance().getAttribute("LiveDownloadQue");                                                           
   if  (liveque==null) {                                                                                                                                          
   System.out.println("liveque is null");                                                                                                                         
   }                                                                                                                                                              
           if  (liveque!=null) {                                                                                                                                  
                                                                                                                                                                  
      // System.out.println(pload);                                                                                                                               
       try {                                                                                                                                                      
           System.out.println("get live notified:"+id);                                                                                                           
                        liveque.enqueue(id.getBytes("UTF-8"));                                                                                                    
                } catch (UnsupportedEncodingException e) {                                                                                                        
                        // TODO Auto-generated catch block                                                                                                        
                        e.printStackTrace();                                                                                                                      
                } catch (IOException e) {                                                                                                                         
                        // TODO Auto-generated catch block                                                                                                        
                        e.printStackTrace();                                                                                                                      
                }                                                                                                                                                 
                                                                                                                                                                  
   }                                                                                                                                                              
   */

		   
		   
		   
		   String mhost="";
		   
			    URL tturl;
				try {
					 tturl = new URL(id);
					 mhost=tturl.getHost();
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			    
				String ihost = mhost.replaceFirst("www.", "");
		        boolean nat = false;
		  // Date srdate = MementoCommons.checkSrDateValidity(rdate);
		   //String cdate = MementoCommons.httpformatter.format(srdate);
		   
		  
		   RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
		   CacheStorage storage =  (CacheStorage) MyInitServlet.getInstance().getAttribute("storage");
		   //if new url first insert it to linkmaster; (this for backloading from batch)
		     Date lastupdate = rmtask.checkLastUpdate(id);
	        
		   
	          TimeZone tz = TimeZone.getTimeZone("GMT");
			  Calendar c = new GregorianCalendar(tz);		
			  c.setTime(new Date()); 
		 	  c.add(Calendar.DAY_OF_MONTH,-10);
		 	  
			  Date cutoff =  c.getTime();
			  System.out.println("cuttof:"+cutoff);
			  			  if (lastupdate!=null){
	                if (lastupdate.after(cutoff)) {
	                	
	                	System.out.println("already fresh:"+id +"lastupdate"+lastupdate);
	                	ResponseBuilder r = Response.status(204);
	                	return r.build();
	                }
			  }
			    if (lastupdate==null){
		            storage.checkCacheRelax(id);
		          }
			   
		   HttpClient cli = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");			   
		  // URLPostClient  postclient = new URLPostClient();
		   Map params = (Map) MyInitServlet.getInstance().getAttribute("params");
		 //  postclient.setBatchDownloadUrl((String) params.get("resourcesync.live.updatefrom"));
		   String json = get_timemap(cli,id,(String) params.get("resourcesync.live.updatefrom"));
		   
		   
		   
		   
		    if (json!=null) {
		  // System.out.println("json"+json);
		    	if ( !json.equals("404")) {
		        JsonElement jelement = new JsonParser().parse(json);
		        JsonObject  jobject = jelement.getAsJsonObject();
		        Map smap = new HashMap();
		        jobject = jobject.getAsJsonObject("mementos");
		        JsonArray mementos = jobject.getAsJsonArray("list");
		        String prev_ai=""; 
		        List links =null;
		        
		         for (int i=0; i <mementos.size();i++) {
		    	 JsonObject entry = mementos.get(i).getAsJsonObject();
		    	 String aid = entry.get("archive_id").toString().replace("\"", "");
		    	        if (aid.equals(ihost)) nat = true;
		    	       if (!aid.equals(prev_ai)) {
                 	    // System.out.println("hostname"+aid);
                 	
                       if (links!=null) {
                 	   System.out.println("prev_hostname"+prev_ai);
                       smap.put(prev_ai,links);
                       }
                        links = new ArrayList<Link>();
                      }
		    	 
		    	 String mdate = entry.get("datetime").toString().replace("\"", "");
		    	 Link link = new Link();
		    	    try {
					Date d = MementoUtils.timeTravelJsFormatter.parse(mdate);
					 String fdate = MementoUtils.httpformatter.format(d);
					link.setDatetime(fdate);
				     } catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				     }
		    	 String uri = entry.get( "uri").toString().replace("\"", "");
		    	 //System.out.println(aid+","+mdate+","+uri);
		    	
		    	 
		    	 link.setHref(uri);
		    	
		    	 links.add(link);
		    	 prev_ai=aid;
		    	 
		    	 
		    }//for
		    //last one
		         if ( !prev_ai.equals("")){
		         smap.put(prev_ai,links);
		         }
		  		    
		     /*   Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();
		        boolean fresh = false;
		        BatchMap batchmap = gson.fromJson(json, BatchMap.class);
		        String url = batchmap.getOriginalUrl();
		        Date d = batchmap.getRequestDatetime(); 
		        Date udate = batchmap.getUpdateDatetime();
		      
		        System.out.println("json test:"+d);
		      */
		        //if new url first insert it to linkmaster; (this for backloading from batch)
		       
		         if (nat) {
		        	 rmtask.addArchive(ihost, "","",null,null);
		         }
		         
		           
		    rmtask.updateAllLinks(id, smap);
		   
		    //SimpleDateFormat formatter_utc = new SimpleDateFormat("yyyy-MM-dd");
		    //String ctime = formatter_utc.format(lastupd);
		    String ctime = MementoUtils.formatter_db.format(lastupd);
		    rmtask.updateLinkmaster (id,ctime,null);
		    
		    } //if
		    }
		    
		    if (json.equals("404")) {
		         //only update records originated in life instance	
		     
		    	 //if (rmtask.checkLastUpdate(id)!=null){			          
		    	    //Date udate = new Date();
		    	   // SimpleDateFormat formatter_utc = new SimpleDateFormat("yyyy-MM-dd");
				  //  String ctime = formatter_utc.format(udate);	
		    	//if lastupd exists then it id not found in archives
			        if (lastupd!=null){
			        	  //String ctime = formatter_utc.format(lastupd);	
			        	  String ctime = MementoUtils.formatter_db.format(lastupd);
			        	  rmtask.updateLinkmaster (id,ctime,null);
			        } 	    
		    	 //}
		    }
		    ResponseBuilder r = Response.status(204);
		 
		 return r.build();
	 }
	
	
	  public String get_timemap( HttpClient mClient,String url, String batchdownloadurl ) {
	    	System.out.println("download:"+batchdownloadurl+url);
	    	GetMethod mGet = new GetMethod(batchdownloadurl+url);
	    	try {
	    		 int statusCode = mClient.executeMethod( mGet);
	    		 
	    		   Header lastmodifiedheader = mGet.getResponseHeader("Last-Modified");
	    		     
	    		      if  (lastmodifiedheader!=null) {
	    		      String updatedate = lastmodifiedheader.getValue();
	    		      if (updatedate!=null){	 		  
	    		 		  lastupd = MementoUtils.httpformatter.parse(updatedate);  	 	  
	    		      }
	    		      }
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
	    	 mGet.releaseConnection();
	    	}
	    	return null;
	    	
	    }
	    
	
}
