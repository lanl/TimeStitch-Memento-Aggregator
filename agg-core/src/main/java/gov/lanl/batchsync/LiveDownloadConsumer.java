package gov.lanl.batchsync;
import gov.lanl.agg.BatchMap;
import gov.lanl.agg.Link;
import gov.lanl.agg.batch.RunMeBatch;
import gov.lanl.agg.batch.RunMeBatchTask;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.utils.MementoUtils;

import java.net.MalformedURLException;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leansoft.bigqueue.IBigQueue;

public class LiveDownloadConsumer extends Thread {
		     //BlockingQueue q = null;
		     private boolean running = true;
		     URLPostClient postclient;
		     private boolean notify = false;
		     IBigQueue bigQueue  = null;
		     int interval = 1 * 30 * 1000*60*24;
		     RunMeBatch rmtask;
		     CacheStorage storage;
        public LiveDownloadConsumer(URLPostClient postclient,IBigQueue bigQueue,
    		RunMeBatch rmtask,CacheStorage storage){
		    	 this.postclient = postclient;
		    	 //this.bigQueue = task.getQue();
		    	 this.bigQueue = bigQueue;
		    	 this.rmtask = rmtask;
		    	 this.storage = storage;
				// this.q = q;
			 }
		     
		     public void setNotify(boolean refresh) {
		    	 notify = refresh;
		     }
		  
		     
		     public void setSleepInterval(int interval) {
		    	 interval = interval;
		     }
		    @Override
		    public void run() {
		        // Keeps running indefinitely, until the termination flag is set to false
		        while (running) {
		        	   try {
		        		    //System.out.println("q size before select jobs:"+q.size());
		        		    
		        		   //  if (notify) { 
		        		    	 if (bigQueue!=null) {
		        		    	 if (!bigQueue.isEmpty()){
		        		    		 byte[] data  = bigQueue.dequeue();
		        		    		  if (data != null) {
		        		    		  String item = new String(data);
		        		    		  System.out.println("from livepostproducer:"+item);
		        		    		  process_url(item);
		        		    		  }
		        		    	 }    
		        		    	 }
		        		    // }
		        		     
		        		    //int minuta = 1 * 30 * 1000;
		        		    // int interval = minuta*60*24;
		        		   // System.out.println("q size after select jobs:"+q.size());
							// Thread.sleep(interval);		// Sleep for 1 minutes			    	
							
						} catch (Exception e) {
							 //TODO Auto-generated catch block
							e.printStackTrace();
							  System.out.println("Counter Thread in run() - interrupted while sleeping");
						}
					   
		        }
		    }
		 
		    
		 public int   process_url(String id) {
		    	
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
				   
				  
			//	   RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
				//   CacheStorage storage =  (CacheStorage) MyInitServlet.getInstance().getAttribute("storage");
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
			                	return -1;
			                	//ResponseBuilder r = Response.status(204);
			                	//return r.build();
			                }
					  }
					   if (lastupdate==null){
				            storage.checkCacheRelax(id);
				          }
				  // URLPostClient  postclient = new URLPostClient();
				  // Map params = (Map) MyInitServlet.getInstance().getAttribute("params");
				  // postclient.setBatchDownloadUrl((String) params.get("resourcesync.live.updatefrom"));
				   String json = postclient.get_timemap(id);
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
				  		    
				        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();
				        boolean fresh = false;
				        BatchMap batchmap = gson.fromJson(json, BatchMap.class);
				        String url = batchmap.getOriginalUrl();
				        Date d = batchmap.getRequestDatetime(); 
				        Date udate = batchmap.getUpdateDatetime();
				  
				        System.out.println("json test:"+d);
				        //if new url first insert it to linkmaster; (this for backloading from batch)
				       
				         if (nat) {
				        	 rmtask.addArchive(ihost, "","",null,null);
				         }
				         
				           
				    rmtask.updateAllLinks(id, smap);
				   
				   // SimpleDateFormat formatter_utc = new SimpleDateFormat("yyyy-MM-dd");
				    String ctime = MementoUtils.formatter_db.format(udate);
				   // String ctime = formatter_utc.format(udate);
				    
				    rmtask.updateLinkmaster (id,ctime,null);
				    
				    } //if
				    }
				    
				    if (json.equals("404")) {
				         //only update records originated in life instance	
				     
				    	 //if (rmtask.checkLastUpdate(id)!=null){			          
				    	    Date udate = new Date();
				    	    //SimpleDateFormat formatter_utc = new SimpleDateFormat("yyyy-MM-dd");
				    	    String ctime = MementoUtils.formatter_db.format(udate);
						   // String ctime = formatter_utc.format(udate);				 
						    rmtask.updateLinkmaster (id,ctime,null);
						   				    
				    	 //}
				    }
				    else{
				    	System.out.println("json is null"+id);
				    }
		    	
		    	return 1;
		    }
		    
		    
		    
		    // Terminates thread execution
		    public void halt() {
		        this.running = false;
		    }
		}

	   

