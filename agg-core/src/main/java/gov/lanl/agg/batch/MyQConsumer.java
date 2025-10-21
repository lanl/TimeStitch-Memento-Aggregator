package gov.lanl.agg.batch;




import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.CacheStats;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.MultivaluedMapImpl;
import gov.lanl.agg.RulesDescription;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.utils.CommonRuleMatcher;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.OriginalResource;
import gov.lanl.agg.utils.PagingUtils;
import gov.lanl.agg.utils.RemoteCacheClient;
import gov.lanl.agg.utils.TimeGateClient;
import gov.lanl.agg.utils.TimeMapTask;
//import gov.lanl.agg.utils.TimeGateClientIP;
//import gov.lanl.agg.utils.TimeMapTaskIP;
import gov.lanl.batchsync.URLPostClient;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
//import javax.ws.rs.core.MultivaluedMap;
import org.apache.log4j.Logger;

//import org.apache.commons.httpclient.HttpClient;
//import org.apache.http.client.methods.CloseableHttpResponse;






//import org.apache.http.client.HttpClient;






import com.leansoft.bigqueue.IBigQueue;
/*
@author Lyudmila Balakireva

*/
public class MyQConsumer extends Thread {
	 
	
	 RulesDescription   tgrules;
	
	     Logger batchloger = Logger.getLogger("batch");
		 BlockingQueue q = null;
		 private boolean running = true;
	     RunMeBatch task = null;
	   
	     Map hmap_ =new HashMap();
	     List<String> tmlist;
	     List<ArchiveDescription> adesc;
	     ExecutorService exec;
	     gov.lanl.agg.cache.CacheStorage storage;
	     HttpClient client;
	     List<ArchiveDescription> defaultarchives;
	     Map paginglist;
	     long TIME_BUDGET = 250L;
	     IBigQueue bigQueue=null;
	     URLPostClient poclient=null;
	     CacheStats batchstats=null;
	     IBigQueue mainq = null;
	     Map archivebyName;
	     String rulesurl=null;
	     RemoteCacheClient rcache;
	     
	     public void setTimeBudget(long budget) {
	    	  TIME_BUDGET = budget;
	    	  
	      }
	     
	     public void setRules(String rulesurl){
	    	 tgrules = CommonRuleMatcher.load_rules(rulesurl,"batch");
	    	 
	     }
	     
	     public void setCacheRegistry(String fileurl,String selfcache){
	    	   rcache = new gov.lanl.agg.utils.RemoteCacheClient(fileurl,selfcache);
			    
	     }
	     public void setURLPostClient(URLPostClient client) {
	      poclient = client;	 
	     }
	     public void setBatchQue(IBigQueue bigQueue) {
	    	bigQueue=bigQueue;
	     }
	 
	     public MyQConsumer(PriorityBlockingQueue q,Map hmap,List <ArchiveDescription> adesc,List tmlist,RunMeBatch task, ExecutorService exec,  CacheStorage storage,  HttpClient client,CacheStats batchstats){
			 this.q = q;
			   
			 this.tmlist = tmlist;
			 this.task = task;
			 this.exec=exec;
			 this.storage = storage;
			 this.batchstats=batchstats;
			 hmap_.putAll(hmap);
			 this.client=client;
		
			 paginglist = new HashMap();
			 this.adesc=adesc; //not changing other lists yet 
			 
		  }		 
	    
	  
	    @Override
	    public void run() {
	        // Keeps running indefinitely, until the termination flag is set to false
	        while (running) {
	        	//if (mainq!=NULL) {
	        	//if (!mainq.isEmpty()){
	        	   dprocess();
	        	//}
	        	//}
	        	//else {
	        		// dprocess();
	        	//}
				   
	        }
	    }
	 
	    // Terminates thread execution
	    public void halt() {
	        this.running = false;
	    }
	    
	    public void dprocess() {
			String aa;
			String url;
			boolean delete = false;
			try {
				aa = (String) q.take();
				String priority =aa.substring(0, 1);
				//System.out.println("priority:"+priority);
				aa = aa.substring(1); 
				  String ipid = aa.substring(0,aa.indexOf("|")); 
				  String rest = aa.substring(aa.indexOf("|")+1);
				  String reqdate = rest.substring(0,rest.indexOf("|"));
				  reqdate = reqdate.substring(0, 19);
				 // System.out.println("reqdate:"+reqdate);
				  
				//aa = new String(mainq.dequeue());
				//Map resultlinks = new HashMap();
				  Map resultlinks = new ConcurrentHashMap();
				  Map http_result = new MultivaluedMapImpl();
				  Map http_results = new HashMap();
			    	
			    long time0 = System.currentTimeMillis();
			    //I am losing requested date  or need to use this date instead of reqtime?
				url = "http://"+ rest.substring(rest.indexOf("|")+1);
				
				//check that url is good
				boolean proceed = true;
				Date updtime = new Date();
				String updtimelog = MementoUtils.timeTravelMachineFormatter.format(updtime);
				String ctime =  MementoUtils.formatter_db.format(updtime);
		
				Date d = MementoUtils.formatter_db.parse(reqdate);
	            String reqdatelog = MementoUtils.timeTravelMachineFormatter.format(d);
	          
				System.out.println("ctime"+ctime);
				String suffix="";
				String mhost="";
				
				String eurl = "";
				String enurl = "";
				   
			    	
				try {
					System.out.println(url);
					
				    URL tturl = new URL(url);
				    mhost = tturl.getHost();				   		
				    eurl = url;
				   enurl = url;
				}
				catch ( Exception ignore) {
					if (url !=null) {
					//need to update jobs if bad url
					 proceed = false;	
					 System.out.println("bad id:"+url);
					}
		        	 //ignore.printStackTrace(); 
		         }
				
				 if (mhost.indexOf("http://")>0) {
					  proceed = false;
				  }
				 if (url.length()>1024) proceed = false;
				
	
			     			     
			     String aurl = eurl;
			     String aenurl = enurl;
			      
				 
				CommonRuleMatcher  urlmatcher = new CommonRuleMatcher();
			    List  archivelist = urlmatcher.getArchives(aurl,tgrules);
			    
			    List <ArchiveDescription>   tgdyn = new ArrayList();
			    Iterator itdefault = adesc.iterator();
			    
			    while (itdefault.hasNext()) {
			    	ArchiveDescription ad = (ArchiveDescription) itdefault.next();
			    	String name = ad.getName(); 
			    	//System.out.println("batch 1 step, name:"+name);
			    	if ( archivelist.contains(name)) {
			    	  tgdyn.add(ad);
			    	}
			    }
			    
				System.out.println("batch job:" +url);
				   OriginalResource ores= null;
				   if (proceed) {
				   TimeGateClient tgclient = new TimeGateClient();
	              // System.out.println( "batch:original"+aenurl);
	               try {
	               tgclient.checkFeedUrl(client, aenurl,MementoUtils.httpformatter.format(new Date()));
	               }
	               catch (Exception e) {
               	   
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
	               
	                ores = tgclient.getOriginalResource();
	                String ourl = ores.getOriginaURI();
	               //if (ourl!=null) {
	               // if (!ourl.equals(url)) proceed=false;
	               // System.out.println("from batch url is timegate:"+ url);
				   //}
				   }
				   
			       if (!proceed){
			    	   
			          task.updateLinkmaster (url,ctime,"B");
			          //delete from jobs?
			      }
			      
				if (proceed) {
				   
			    CompletionService<Map> ecs = new ExecutorCompletionService<Map> (exec);		
			    List <TimeMapTask> tasks = new ArrayList<TimeMapTask> (); 
			   
			               Iterator<ArchiveDescription> di =  tgdyn.iterator();
			               int nat = -1;
			               while (di.hasNext()) {
			    	           ArchiveDescription sarchive = di.next();
			    	                         String aname = sarchive.getName();
			    	                          String tmap = sarchive.getTimemap();
			    	                    String pagestatus = sarchive.getPagingstatus();
			    	      
			    	                    String openwayback = sarchive.getOpenwaybackstatus();
			    	                        String mprefix = sarchive.getMementotemplate();
			    	       			    	        
			    	            
			    	              Integer od = sarchive.getOrdernumber();
			    	           
			    	                            paginglist.put(od, pagestatus);
			    	                              if (od.intValue()>nat) {
			    	            	                 nat = od;
			    	                              }
			    	     
			    	        if (openwayback!=null) {
			    	        	 //by index service  fix        
			    	        	  String enqurl= tmap+ aenurl;
			    	        	  tasks.add(new TimeMapTask(od,enqurl,client,mprefix, tmap));
			    	        } else {
			    	        	 String turl= tmap+ aurl;
			    	        	 //System.out.println("od and url"+od+turl);
			    	        	//adding tasks based on file
			    	             tasks.add(new TimeMapTask(od, turl, client));
			    	        }
			                }
			               
			               
			    		   //check if native resource exists
			               
			               int n = tgdyn.size();
			               
			              // TimeGateClient tgclient = new TimeGateClient();
			              // System.out.println( "batch:original"+aenurl);
			               //try {
			               //tgclient.checkFeedUrl(client, aenurl,MementoUtils.httpformatter.format(new Date()));
			               //}
			               //catch (Exception e) {
		                	   
      							// TODO Auto-generated catch block
      							//e.printStackTrace();
      						//}
			               
			               //OriginalResource ores = tgclient.getOriginalResource();
			               //if (ores.isTimeGate()) 
			               if (ores.getTimeMapIndexURI()!=null) {
			            	   //adding index timemap frpm original resource
			            	   String tmstr = ores.getTimeMapIndexURI();
		                    	  tmstr =  tmstr.replace(aenurl,"");
		                    	  tmstr =  tmstr.replace(":80","");
			            		   if (!tmlist.contains(tmstr)) {
			            	          String ihost= mhost.replaceFirst("www.", "");
			            	          System.out.println("batch timeindex:"+ores.getTimeMapIndexURI());
			            	          nat=nat+1;
			            	          hmap_.put(nat, ihost);
			            	          paginglist.put(nat,"1");
			            	   
			            	          String name = "";
			            	           try { URL tm = new URL(ores.getTimeMapIndexURI());
	            	                         name =   tm.getHost();
	            	                         name = name.replaceFirst("www.", ""); 
	            	        	            }
	            	        	       catch (Exception e) {
	       							  //e.printStackTrace();
	       						      }
	            	        	
			            	         task.addArchive(ihost, ores.getTimeGateURI(),tmstr,name,null); 
			            	         tasks.add(new TimeMapTask(nat, ores.getTimeMapIndexURI(), client));
			            	         n=n+1;
			            		   
			            	      }
			            	  
			                }
			                else {
			            	      //adding timemap from original resource
			                      if ((ores.getTimeMapURI())!=null) {
			                    	  //simple check not going to work always
			                    	  String tmstr = ores.getTimeMapURI();
			                    	  tmstr =  tmstr.replace(aenurl,"");
			                    	  tmstr =  tmstr.replace(":80","");
			                    	 if (!tmlist.contains(tmstr)) {
			                    	  System.out.println("batch timemap:"+ores.getTimeMapURI());
			                    	  String ihost=mhost.replaceFirst("www.", ""); 
			            	         nat=nat+1;
			            	         System.out.println( nat+ihost);
			            	         hmap_.put(nat, ihost);
			            	         paginglist.put(nat,"-1");
			            	         //System.out.println("nat:"+nat);
			            	            String name = "";
			            	        	try { URL tm = new URL(ores.getTimeMapURI());
			            	        	  
			            	               name =   tm.getHost();
			            	               name = name.replaceFirst("www.", ""); 
			            	        	}
			            	        	catch (Exception e) {
			    		                	   
			       							//e.printStackTrace();
			       						} 
			            	        	//System.out.println( "batch native step:nat,host,name"+nat+"," +ihost+","+name +","+tmstr);
			            	        		
			            	         
			            	         task.addArchive(ihost, ores.getTimeGateURI(),tmstr,name,null);
			            	         tasks.add(new TimeMapTask(nat, ores.getTimeMapURI(), client));
			            	         n=n+1;
			                    	 }
			            	         
			                    }
			               }
			               
			               
			               
			              
		                   PagingUtils putils = new PagingUtils(paginglist);
		                   Map result = null ;
		                   // int  n = tmlist.size(); 
		                  // int n = tgdyn.size();
		    	           List<Future<Map>> futures
		                    = new ArrayList<Future<Map>>();
		    	          // long TIME_BUDGET = 250L;
				           long endSeconds =  TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + TIME_BUDGET;
				                   System.out.println("total tasks:"+ tasks.size());
		    	                try {
		                            for (TimeMapTask s : tasks) {
		                            futures.add(ecs.submit(s));	 
		    	                    }
		                        
		                            int nn=0;
		                            for (int i = 0; i < n; ++i) {
		                             try {
		                	        long timeLeft = endSeconds-TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		                	       // System.out.println("timeleft:"+timeLeft);
		                	        //if (timeLeft<0) break;
		                	         Map r = null;
		                	         if (result == null) {
		                             r = (Map) ecs.take().get();
		                             
		                                     if (r!=null) {
		                    	                      nn=nn+1;
		                    	                      result = r;
		                    	                    //  long time_1 = System.currentTimeMillis();
		                    	                      Integer hostid = (Integer) r.keySet().iterator().next();
		                    	                     
		                    	                      LinkHeader lh= (LinkHeader) r.get(hostid.intValue());
		                    	                      String host =(String) hmap_.get(hostid);
		                    	                      String pstatus="0";
		                    	                      if (paginglist!=null) {
		                    	                    	 pstatus = (String) paginglist.get(hostid);
		                    	                    	// System.out.println("status:" +pstatus);
		                    	                      }
		                    	                     // if (lh!=null) {
		                    	                          int http_status=lh.getStatus();
		                    	                          http_results.put(host, http_status);
		                    	                          String hstatus = putils.composeStatus(http_status);
		                    	                          http_result.put(hstatus, host);
		                    	                          
		                    	                    	  if (lh.getStatus()==200) {
		                    	                    	// Map<String, Link> tmap = lh.getLinksByRelationship();
		                    	                    	  //by hostid find what time map style we have
		                    	                    	  //no paging implemented
		                    	                    	  //timemap index 
		                    	                    	  //timemap backward
		                    	                    	  //timemap forward
		                    	                    	  //timemap unknown -define type in first timemap and set backward or forward.
		                    	                    	 // List<Link> ttlist = lh.getSpecialLinks();
		                    	                    	  //if status 0 no paging implemented 
		                    	                    	  //resultlinks.put(host, lh.getLinks());
		                    	                    	  if (resultlinks.containsKey(host)) {
		                    	                    		List flinks = (List) resultlinks.get(host);
		                    	                    		flinks.addAll(lh.getLinks());
		                    	                    		// System.out.println("resultf:"+host+flinks.size());
		                    	                    		resultlinks.put(host, flinks);
		                    	                    	  }
		                    	                    	  else {
		                    	                    		 	  resultlinks.put(host, lh.getLinks());  
		                    	                    	  }
		                    	                    	  if (pstatus.equals("3")) {
		                    	                    		  String ntimemap= putils.timemap_paging_backward (lh.getSpecialLinks());
		                    	                    		 // tasks.add(new TimeMapTask(hostid,ntimemap, client));
		                    	                    		  if (ntimemap!=null) {
		                    	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap, client);
		                    	                    		  futures.add(ecs.submit(task));
		                    	                    		  n=n+1;
		                    	                    		  }
		                    	                    	  }
		                    	                    	 // if (pstatus.equals("1")) {
		                    	                    		//  String ntimemap= timemap_paging_index  (lh.getSpecialLinks());
		                    	                    		  //tasks.add(new TimeMapTask(hostid,ntimemap, client));
		                    	                    		 // if (ntimemap!=null) {
		                    	                    		  //TimeMapTask task= new TimeMapTask(hostid,ntimemap, client);
		                    	                    		  //futures.add(ecs.submit(task));
		                    	                    		  //n=n+1;
		                    	                    		  //}
		                    	                    	  //}
		                    	                    	  if (pstatus.equals("1")) {
		                    	                    		  //this is not tested as case doesnot exists yet
		                    	                    		  List <String>ntimemaps= putils.timemap_paging_index (lh.getSpecialLinks());
		                    	                    		  if (ntimemaps.size()>0) {
		                    	                    			  for(String ntimemap : ntimemaps){	  
		                    	                    		      TimeMapTask task = new TimeMapTask(hostid,ntimemap,client);
		                     	                    		      futures.add(ecs.submit(task)); 
		                     	                    		      n=n+1;
		                    	                    			  }
		                    	                    			  paginglist.put(hostid, "2");
		                    	                    			  
		                    	                    		  }
		                    	                    	  }
		                    	                    	  
		                    	                    	  
		                    	                    	  //special fix //not existing case
		                    	                    	  if (pstatus.equals("666")) {
		                    	                    		  //use logic to get special link 
		                    	                    		 List<Link> ttlist = lh.getSpecialLinks();
		                    	                    		 String ntimemap = null;
		                    	                    		  for (Link temp : ttlist) {
		                    	                    			  if (temp.getRelationship().equals("timemap")) {
		                    	                    			  ntimemap = temp.getHref();
		                    	                    			  System.out.println("timemappage/666"+ntimemap);
		                    	                    			  }
		                    	                    		  }
		                    	                    		     String mprefix="";
			                    	                    		 String tmap = "";
		                    	                    		  //String ntimemap= timemap_paging_forward (lh.getSpecialLinks());
		                    	                    		 // System.out.println("timemappage"+ntimemap);
		                    	                    		  //tasks.add(new TimeMapTask(hostid,ntimemap, client));
		                    	                    		  if (ntimemap!=null) {
		                    	                    			  //hostid find mprefix,tmap
		                    	                    		       Iterator<ArchiveDescription> dia =  tgdyn.iterator();
		                    	              
		                    	       			                    while (dia.hasNext()) {
		                    	       			    	                   ArchiveDescription aarchive = dia.next();
		                    	       			    	                   Integer oid = aarchive.getOrdernumber();
		                    	       			    	                    if (oid.equals(hostid)) {
		                    	       			    	        	        mprefix = aarchive.getMementotemplate();
		                    	       			    	        	        //System.out.println("mprefix"+mprefix);
		                    	       			    	        	        tmap = aarchive.getTimemap();
		                    	       			    	                   }
		                    	       			                     }
		                    	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap, client,mprefix, tmap);
		                    	                    		  futures.add(ecs.submit(task));
		                    	                    		  n=n+1;
		                    	                    		  }
		                    	                    	  }
		                    	                    	  
		                    	                    	  
		                    	                    	  
		                    	                    	  if (pstatus.equals("-1")) {
		                    	                    		  System.out.println("from MyQConsumer:case -1");
		                    	                    		  //String ntimemap= timemap_paging_forward (lh.getSpecialLinks());
		                    	                    		  String ntimemap = putils.timemap_paging_unknown (lh.getSpecialLinks(),hostid);
		                    	                    		   System.out.println("timemappage/-1"+ntimemap);
		                    	                    		if (ntimemap!=null) {
		                    	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap, client);
		                    	                    		  futures.add(ecs.submit(task));
		                    	                    		  n=n+1;
		                    	                    		}
		                    	                    		 // tasks.add(new TimeMapTask(hostid,ntimemap, client));
		                    	                    		  
		                    	                    	  }
		                    	                    	  
		                    	                    	  
		                    	                    	  
		                    	                    	  if (pstatus.equals("2")) {
		                    	                    		  System.out.println("from MyQConsumer:case2");
		                    	                    		  String ntimemap= putils.timemap_paging_forward (lh.getSpecialLinks());
		                    	                    		 // System.out.println("timemappage/2"+ntimemap);
		                    	                    		if (ntimemap!=null) {
		                    	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap, client);
		                    	                    		  futures.add(ecs.submit(task));
		                    	                    		  n=n+1;
		                    	                    		}
		                    	                    		 // tasks.add(new TimeMapTask(hostid,ntimemap, client));
		                    	                    		  
		                    	                    	  }
		                    	                    	  
		                    	                    	 
		                    	                      }
		                    	                    //  task.updateAllLinks(url,host,lh);
		                    	                    // long time_2 = System.currentTimeMillis();
		                    	                     // System.out.println("Results insert Took : " + ((time_2 - time_1) / 1000));
		                    	                     // parsipolis (r,m);
		                                      }
		                	          }
		                	         else {
		                	         r = null;
		                	         Future<Map> f = ecs.poll(timeLeft, TimeUnit.SECONDS);
		                	          nn=nn+1;
		                	            // System.out.println("attemp" +nn);
		                	          if ( f!=null) {    r = (Map) f.get(); } else {
		                	        	 //if f null is it timeout?
		                	        	  break;
		                	          }
		                              if (r!=null) { 
		                            	 // long time_1 = System.currentTimeMillis();
		                            	  Integer hostid = (Integer) r.keySet().iterator().next();
		        	                      LinkHeader lh= (LinkHeader) r.get(hostid.intValue());
		        	                      String host = (String) hmap_.get(hostid);
		        	                      String pstatus="0";
		        	                      if (paginglist!=null) {
		        	                    	 pstatus = (String) paginglist.get(hostid);
		        	                    	// System.out.println("status2:" +pstatus);
		        	                      }
		        	                       int http_status=lh.getStatus();
            	                           http_results.put(host, http_status);
            	                           String hstatus = putils.composeStatus(http_status);
            	                           http_result.put(hstatus, host);
            	                         
		        	                       if (lh.getStatus()==200) {
		        	                    	  //resultlinks.put(host, lh.getLinks());
		        	                    	   if (resultlinks.containsKey(host)) {
		          	                    		List flinks = (List) resultlinks.get(host);
		          	                    		flinks.addAll(lh.getLinks());
		          	                    		 //System.out.println("resultf:"+host+flinks.size());
		          	                    		resultlinks.put(host, flinks);
		          	                    	   }
		          	                    	   else {
		          	                    		
		          	                    		   
		          	                    		 System.out.println("result:"+host+lh.getLinks().size());
		          	                    		  resultlinks.put(host, lh.getLinks());  
		          	                    	   }
		        	                    	  if (pstatus.equals("3")) {
		        	                    		  String ntimemap= putils.timemap_paging_backward (lh.getSpecialLinks());
		        	                    		  //tasks.add(new TimeMapTask(hostid,ntimemap, client));
		        	                    		  if (ntimemap!=null) {
		        	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap, client);
		        	                    		  futures.add(ecs.submit(task));
		        	                    		  n=n+1;
		        	                    		  }
		        	                    	  }
		        	                    	  if (pstatus.equals("1")) {
		        	                    		  //this is not tested as case doesnot exists yet
		        	                    		  List <String>ntimemaps= putils.timemap_paging_index  (lh.getSpecialLinks());
		        	                    		  if (ntimemaps.size()>0) {
		        	                    			  for(String ntimemap : ntimemaps){	  
		        	                    		      TimeMapTask task = new TimeMapTask(hostid,ntimemap,client);
		         	                    		      futures.add(ecs.submit(task)); 
		         	                    		      n=n+1;
		        	                    			  }
		        	                    			 
		        	                    			  paginglist.put(hostid, "2");
		        	                    		  }
		        	                    	  }
		        	                    	  //special fix //not existing case any more
		        	                    	  if (pstatus.equals("666")) {
		        	                    		  //use logic to get special link 
		        	                    		 List<Link> ttlist = lh.getSpecialLinks();
		        	                    		 String ntimemap = null;
		        	                    		  for (Link temp : ttlist) {
		        	                    			  if (temp.getRelationship().equals("timemap")) {
		        	                    			  ntimemap = temp.getHref();
		        	                    			  }
		        	                    		  }
		        	                    		  //String ntimemap= timemap_paging_forward (lh.getSpecialLinks());
		        	                    		 // System.out.println("timemappage666"+ntimemap);
		        	                    		  //tasks.add(new TimeMapTask(hostid,ntimemap, client));
		        	                    		  if (ntimemap!=null) {
		        	                    			  //hostid find mprefix,tmap
		        	                    		    Iterator<ArchiveDescription> dia =  tgdyn.iterator();
		        	                    		    String mprefix="";
		        	                    		    String tmap = "";
		        	       			                  while (dia.hasNext()) {
		        	       			    	          ArchiveDescription aarchive = dia.next();
		        	       			    	           Integer oid = aarchive.getOrdernumber();
		        	       			    	            if (oid.equals(hostid)) {
		        	       			    	        	   mprefix = aarchive.getMementotemplate();
		        	       			    	        	   tmap = aarchive.getTimemap();
		        	       			    	              }
		        	       			                  }
		        	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap, client,mprefix, tmap);
		        	                    		  futures.add(ecs.submit(task));
		        	                    		  n=n+1;
		        	                    		  }
		        	                    	  }
		        	                    	  
		        	                    	  
		        	                    	  if (pstatus.equals("-1")) {
		        	                    		  System.out.println("from MyQConsumer:case -1");
		        	                    		  //String ntimemap= timemap_paging_forward (lh.getSpecialLinks());
		        	                    		  String ntimemap = putils.timemap_paging_unknown (lh.getSpecialLinks(),hostid);
		        	                    		   System.out.println("timemappage/-1"+ntimemap);
		        	                    		if (ntimemap!=null) {
		        	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap, client);
		        	                    		  futures.add(ecs.submit(task));
		        	                    		  n=n+1;
		        	                    		}
		        	                    		 // tasks.add(new TimeMapTask(hostid,ntimemap, client));
		        	                    		  
		        	                    	  }
		        	                    	  
		        	                    	  if (pstatus.equals("2")) {
		        	                    		  String ntimemap= putils.timemap_paging_forward (lh.getSpecialLinks());
		        	                    		 //System.out.println("timemappage2"+ntimemap);
		        	                    		  //tasks.add(new TimeMapTask(hostid,ntimemap, client));
		        	                    		  if (ntimemap!=null) {
		        	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap, client);
		        	                    		  futures.add(ecs.submit(task));
		        	                    		  n=n+1;
		        	                    		  }
		        	                    	  }
		        	                    	  
		          	                    	   
		          	                    	  
		        	                      }
		        	                     // long time_1 = System.currentTimeMillis();
		        	                      //task.updateAllLinks(url,host,lh);
		        	                     // long time_2 = System.currentTimeMillis();
		        	                     // System.out.println("Results insert Took : " + ((time_2 - time_1) / 1000));
		        	                     
		                            	  //parsipolis (r,m);
		                            	  }
		                              }
		                	        // }
		                	    
		                 } catch (ExecutionException ignore) {
		                	 //ignore.printStackTrace(); 
		                 } 
		                   catch (Exception e) {
		                	   
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
		             }
		             
		             
		    	 }
		    	 finally {
		    		 int count = 0;
		             for (Future<Map> f : futures) {
		            	 count=count+1;
		            	 f.cancel(true);
		            	 
		             }
		             //System.out.println ("cancelled:" + count +" threads");
		         } 
		    	   
		    		 if (resultlinks.size()>0) {
		    			 boolean iaonly=false;
		    			 Set keys = resultlinks.keySet();
		    				String listString ="";
		    				
		    			//if (keys.size()>0) {
		    				//	listString=String.join(":", keys);
		    			//}
		    			//batchloger.info(url + " "+ listString+" "+reqdatelog+" "+updtimelog);
		    			 if (keys.size()==1){
		    				 if (keys.contains("ia")) {
		    					 iaonly=true;
		    				 }
		    			 }
		    			 long time_2 = System.currentTimeMillis();	 
		                 List archives = task.updateAllLinks(url, resultlinks); 
		                 long time_3 = System.currentTimeMillis();
		                  System.out.println("batch Link msql insert Took : " + ((time_2 - time_3) / 1000)+"sec");
		                  batchstats.incrementHit();  
		                  if (archives.size()>0){
		                	  listString = String.join(":", archives);
		                      batchloger.info(url + " "+ listString+" "+reqdatelog+" "+updtimelog);
		                  }
		                  else {
		                	  //can be 200 empty timemaps
		                	  batchloger.info(url + " null"+" "+reqdatelog+" "+updtimelog); 
		                  }
		                      //if it remotebatch operation post results
		                      // if (poclient!=null) {
		                    //	String resdate = MementoUtils.timeTravelJsFormatter.format(reqtime);
		                    	//no need to send results 
		                      //  poclient.resultsurl_put(url, resdate);
		                       // }	                    
		                   
		    		        }
					   else {
						   //no records	
						   batchloger.info(url + " null"+" "+reqdatelog+" "+updtimelog);
						   batchstats.incrementMiss();
						   if (priority.equals("2")) {
							   delete = true;
						   }
					   System.out.println("links not found:"+url);
					    }
		             //long time_2 = System.currentTimeMillis();
		           //  System.out.println("Link insert Took : " + ((time_2 - time_1) / 1000)+"sec");
		    		 
		    		  
		    		 
		              } //proceed
				
				
				   // new code as Sept 16 2016 comment out for now
				
				 if  (http_results.size()==0){
					 // update table from archive list 
					 // task.updateurlarchivesummary(url,ctime,
				 }
				 else {
					 Iterator ki = http_results.keySet().iterator();
		    		  while (ki.hasNext()){
				        	 String archive = (String) ki.next();
				        	 int flag = 0;
				        	// System.out.println("archive"+archive);
				        	 int code = (int) http_results.get(archive);
				        	 if (resultlinks.size()>0){
				        		 if (resultlinks.containsKey(archive)) {
				        			 List<Link> lm = (List<Link>) resultlinks.get(archive);
				        			 
				        			 flag = lm.size();
				        		 }
				        	 }
				        	// System.out.println("code"+code);
				        	 
				        	 if (code==404) {
				        		 flag = 0;
				        	 }
				        	 if (resultlinks.size()==0){
				        	 flag=0;
				        	 }
				        	 else if (code!=200&&code!=404) {
				        		 flag = -1;
				        	 }
				        	 
				        	// System.out.println("flag"+flag);
				        	 //comment out update for now
				        	// task.updateLinkSummary( url, archive, ctime, Integer.toString(code),Integer.toString(flag));
				        	
		    		  }
					 
				 }
				
				
				
				
			 	       //this is just test 
				       //  StringBuffer sb= new StringBuffer();
				        // Iterator ki = http_results.keySet().iterator();
				        // while (ki.hasNext()){
				        	// String archive = (String) ki.next();
				        	 //int code = (int) http_results.get(archive);
				        	 //sb.append(archive+":"+code+",");
				         //}
				         
				         //sb.delete(sb.length()-1, sb.length()-1);
				         //System.out.println("archivecheck:"+sb.toString());
				         //end of test
				
				 long time_lm1 = System.currentTimeMillis();	
				       if ( resultlinks.size()==0 &&  http_result.containsKey("5XX")){
				    	  task.updateLinkmaster (url,ctime,"F");
				    	  //System.out.println(sb.toString());
				       }
				       else if(resultlinks.size()==0)
				       {
				    	   if (priority.equals("2")){
				    		   System.out.println("deleting url:"+url);
				    		   task.deleteLinkmaster(url);
				    	   }
				    	   else {
				    	   task.updateLinkmaster (url,ctime,"N");
				    	   }
				       }
				       else {
					   task.updateLinkmaster (url,ctime,null);
				       }
				       long time_lm2 = System.currentTimeMillis();     
				       //if it remotebatch operation post results
				       System.out.println("Linkmaster msql insert Took : " + ((time_lm2 - time_lm1) / 1000)+"sec");
					   
				        // if (poclient!=null) {
	                    	//String resdate = MementoUtils.timeTravelJsFormatter.format(reqtime);
	                    	//no need to send results 
	                        //poclient.resultsurl_put(url, resdate);
	                      //  }	 
					  
					  
				   //} //proceed
				       // String ipid = aa.substring(0,aa.indexOf("|")); 
				    // System.out.println("pid" +ipid);
				      
					// System.out.println("updated linkmaster_2:"+url);
				         long time_j1 = System.currentTimeMillis();
					   task.updateJobs(ipid, ctime);
					   long time_j2 = System.currentTimeMillis();
					   System.out.println("job delete Took : " + ((time_j1 - time_j2) / 1000)+"sec,for " + url);
			             
					   long time10 = System.currentTimeMillis();
					   
					  System.out.println("Link insert Took : " + ((time0 - time10) / 1000)+"sec,for " + url);
					   	 
		      }
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
						
		}

	    
	
	}

	
	

