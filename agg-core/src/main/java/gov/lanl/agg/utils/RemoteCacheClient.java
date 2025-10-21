package gov.lanl.agg.utils;

import gov.lanl.agg.BatchMap;
import gov.lanl.agg.Link;
import gov.lanl.agg.batch.RunMeBatchTask;
import gov.lanl.agg.cache.CacheStorage;

import java.io.IOException;
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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RemoteCacheClient {
	List caches;
	 public RemoteCacheClient(String configurl, String cacheself){
		 CacheConfig cc = new CacheConfig(configurl,cacheself);
		 caches = cc.load_cache();
	 }
	
	 public Map CheckCaches(HttpClient client,String id,String ihost,  RunMeBatchTask rmtask,CacheStorage storage){
		  if (caches.size()>0) {
			  for (int i=0;i<caches.size();i++){
				  String url = (String)caches.get(i);
				  Map rmap = FetchData(client, id, ihost,url, rmtask,storage);
				  if (rmap !=null)	{
					  return rmap;
					  }
				  }
		  }
		 return null;
	 }
	 
	
	public Map FetchData(HttpClient client,String id,String ihost,String cacheurl,  RunMeBatchTask rmtask,CacheStorage storage){
		
	   boolean nat = false;
	   System.out.println("fetch data:"+cacheurl+id);
	   GetMethod mGet = new GetMethod(cacheurl+id);
	   
	try {
		  int statusCode = client.executeMethod( mGet);
		  System.out.println("remotecacheclient:"+statusCode);
		  //404 not found of 404 NOT Found ....
		  //if( statusCode!=200) return null;
	      Header lastmodifiedheader = mGet.getResponseHeader("Last-Modified");
	      Date lastupd = null;
	      if  (lastmodifiedheader!=null) {
	      String updatedate = lastmodifiedheader.getValue();
	     
	      TimeZone tz = TimeZone.getTimeZone("GMT");
		  Calendar c = new GregorianCalendar(tz);		
		  c.setTime(new Date()); 
	 	  c.add(Calendar.DAY_OF_MONTH,-14);
	 	  Date cutoff =  c.getTime();
	 	  if (updatedate!=null){	 		  
	 		  lastupd = MementoUtils.httpformatter.parse(updatedate);  	 	  
              if (lastupd.before(cutoff)) {
            	  System.out.println("remotecacheclient:" +"record stale" );
            	 //information too old to fetch
            	  return null;
              }
	 	  }
	      }
	     
	      if (statusCode==200) {
			 String json = mGet.getResponseBodyAsString();			
	      if (json!=null) {
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
            	     System.out.println("hostname"+aid);
            	
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
			        } catch (ParseException e) {  e.printStackTrace();}
	    	        
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
	  		    
	            if (nat) {
	        	 rmtask.addArchive(ihost, "","",null,null);
	            }
	         
	            //tmp 
	           // Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();
		       // boolean fresh = false;
		        //BatchMap batchmap = gson.fromJson(json, BatchMap.class);
		        //String url = batchmap.getOriginalUrl();
		       // Date d = batchmap.getRequestDatetime(); 
		        //lastupd  = batchmap.getUpdateDatetime();
		  
	            
	            
	         // storage.checkCacheRelax(id);
	          rmtask.updateAllLinks(id, smap);	
	          System.out.println("remotecacheclient:"+ id +","+smap.size());
	          //SimpleDateFormat formatter_utc = new SimpleDateFormat("yyyy-MM-dd");
	          String ctime = MementoUtils.formatter_db.format(lastupd);
	          //String ctime = formatter_utc.format(lastupd);	
	          System.out.println("remotecacheclient:"+ctime);
	          rmtask.updateLinkmaster (id,ctime,null);
	          
	          return smap;
	         } //if
	        }
	  
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	finally { mGet.releaseConnection(); }
	return null;	
	}
	
}
