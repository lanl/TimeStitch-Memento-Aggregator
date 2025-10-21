package gov.lanl.agg.helpers;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.CdxParser;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.LinkParser;
import gov.lanl.agg.MultivaluedMapImpl;
import gov.lanl.agg.batch.RunMeBatch;
import gov.lanl.agg.batch.RunMeBatchTask;
import gov.lanl.agg.resource.MyInitServlet;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.OriginalResource;
import gov.lanl.agg.utils.PagingUtils;
import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;
import gov.lanl.agg.utils.TimeGateClient;

import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;

/*
@author Lyudmila Balakireva

*/
public class TimemapAggQuick {
	//static List tglist;
	private Map hmap_= new HashMap();
	private Map plist;
	private List myglobal;
	private Date update;
	/**
	 * @param args
	 */
	
	static ThreadSafeSimpleDateFormat  httpformatter = new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
	static {
		TimeZone tzo = TimeZone.getTimeZone("GMT");
	       httpformatter.setTimeZone(tzo);
	        
	 }
	
	public void getTimeMapInfo (List <ArchiveDescription> timemaplist,String url) {
	      int  n = timemaplist.size();
	      ExecutorService exec = (ExecutorService )MyInitServlet.getInstance().getAttribute("MY_EXECUTOR");
		  CompletionService<LinkHeader> ecs = new ExecutorCompletionService<LinkHeader> (exec);
		
		               myglobal = new ArrayList(); 
		  Map <String,List>smap = new HashMap();
		  plist = new HashMap();
		  hmap_.putAll((Map) MyInitServlet.getInstance().getAttribute("hmap"));
		 //long time = 180L; //60 sec
		 //TimeUnit unit = TimeUnit.SECONDS;
		 
		 //TimeUnit unit = TimeUnit.NANOSECONDS;
		  long TIME_BUDGET = 160L;
		  long endSeconds =  TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + TIME_BUDGET;
		 // System.out.println("endSeconds"+ endSeconds);
		  List <TimeMapTask> tasks = new ArrayList<TimeMapTask> (); 
		  List tmlist = new ArrayList();
	     
	     //cdxindexer need url encorded, since it already can be encoded need decode it and than  encode again, so it is  extra fix:
		  //begin fix
		 //  String eurl = "";
		   String enurl = "";
		   Map http_result = new MultivaluedMapImpl();
		  // Map http_results = new HashMap();
	       String mhost=null;
		   try {
		    URL tturl = new URL(url);
		    mhost=tturl.getHost();
		   
		    StringBuilder sb = new StringBuilder();
		    StringBuilder ensb;
		  	sb.append(tturl.getProtocol()).append("://");
			sb.append(tturl.getHost());

			// omit port if scheme default:
		//	int defaultSchemePort = UrlOperations.schemeToDefaultPort(scheme);
			if(tturl.getPort() != 80 
					&& tturl.getPort() != -1) {

				sb.append(":").append(tturl.getPort());
			}

			sb.append(tturl.getPath());
			ensb = new StringBuilder(sb.toString()); 
			
			 if (tturl.getQuery() != null) {
				System.out.println("query:"+tturl.getQuery());
				String q = URIUtil.decode(tturl.getQuery(),"UTF-8");
				String enq = URLEncoder.encode("?"+q,"UTF-8");
				System.out.println("enquery:"+enq);
				ensb.append(enq);
				sb.append(URLEncoder.encode("?","UTF-8")).append(URIUtil.encodeQuery(q,"UTF-8"));
				
			      }
			  // eurl= sb.toString();
			   enurl = ensb.toString();
			   
		      }
		        catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			     }
		   		  
		  
		       int nat= -1;
	    	   Iterator<ArchiveDescription> di =  timemaplist.iterator();
			    while (di.hasNext()) {
			    	ArchiveDescription sarchive = di.next();
			    	String openwayback = sarchive.getOpenwaybackstatus();
		    	    String mprefix = sarchive.getMementotemplate();
			    	String tmap = sarchive.getTimemap();
			    	String turl = tmap+url;
			    	Integer od = sarchive.getOrdernumber();
			    	String paginstatus =  sarchive.getPagingstatus();
			    	
			    	tmlist.add(tmap);
			    	 plist.put(od, paginstatus);
			    	 
			    	   if (nat < od.intValue()) {
			            	nat=od.intValue();
			            }
			    	  if (openwayback!=null) {
			    		  //this is absolete
			    		  tasks.add(new TimeMapTask(od,tmap+enurl,mprefix,tmap));  
			    	  }
			    	  else {
			    	  tasks.add(new TimeMapTask(od,turl));
			    	  }
			    	 
			       } 	   
	    	   
			    System.out.println("size of tasks:"+tasks.size());
			    //check if native resource exists
	               
	          
	               TimeGateClient tgclient = new TimeGateClient();
	               RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
		              
	               HttpClient    httpclient =(HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
	               tgclient.checkFeedUrl(  httpclient, url,MementoUtils.httpformatter.format(new Date()));
	               OriginalResource ores = tgclient.getOriginalResource();
	               if (ores.getTimeMapIndexURI()!=null) {
	            	   String tmstr = ores.getTimeMapIndexURI();
                 	  tmstr =  tmstr.replace(url,"");
                 	  tmstr =  tmstr.replace(":80","");
                 	 if (!tmlist.contains(tmstr)) {
                 	
	            	   String ihost= mhost.replaceFirst("www.", "");
	            	   System.out.println("dynamic timeindex:"+ores.getTimeMapIndexURI());
	            	   nat=nat+1;
	            	   hmap_.put(nat, ihost);
	            	   plist.put(nat,"1");
	            	   String name = null;
       	        	try { URL tm = new URL(ores.getTimeMapIndexURI());
       	               name =   tm.getHost();
       	        	}
       	        	catch (Exception e) {
  							//e.printStackTrace();
  						} 
	            	   rmtask.addArchive(ihost, ores.getTimeGateURI(),tmstr,name,null); 
	            	   tasks.add(new TimeMapTask(nat, ores.getTimeMapIndexURI()));
	            	   n=n+1;
                 	 }
	            	  
	               }
	               else {
	                      if (ores.getTimeMapURI()!=null) {
	                    	  //Sep2 2015 fix -- remove duplicates
	                    	 
	                    		  String tmstr = ores.getTimeMapURI();
		                    	  tmstr =  tmstr.replace(url,"");
		                    	  tmstr =  tmstr.replace(":80","");
		                    	 if (!tmlist.contains(tmstr)) {
		                    	 
	                    		  //Sep2 2015 fix -- 
	                    	  System.out.println("batch timemap:"+ores.getTimeMapURI());
	                    	  String ihost=mhost.replaceFirst("www.", ""); 
	            	          nat=nat+1;
	            	          hmap_.put(nat, ihost);
	            	          plist.put(nat,"-1");
	            	          System.out.println("nat:"+nat);
	            	          String name = null;
	            	        	try { URL tm = new URL(ores.getTimeMapURI());
	            	               name =   tm.getHost();
	            	               name=name.replaceFirst("www.", ""); 
	            	        	}
	            	        	catch (Exception e) {
	    		                	   
	       							// TODO Auto-generated catch block
	       							//e.printStackTrace();
	       						} 
	            	          rmtask.addArchive(ihost, ores.getTimeGateURI(),tmstr,name,null);
	            	          tasks.add(new TimeMapTask(nat, ores.getTimeMapURI()));
	            	          n=n+1;
	                    	  }
	            	         
	                    }
	               }
	               
	               
	           			    
			    System.out.println("in timemap agg quik:"+url);			    
			    
			    PagingUtils putils = new PagingUtils(plist); 
	    	   
	     LinkHeader result = null ;
	    	
	    	 List<Future<LinkHeader>> futures
	           = new ArrayList<Future<LinkHeader>>();
	    	 
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
	                	         LinkHeader r = null;
	                	         if (result == null) {
	                	        	
	                             r = (LinkHeader) ecs.take().get();
	                            // String from = null;
	                            // String until = null;
	                                 //change --- r is never null now
	                                int h_id =  r.getHostId();
       	                            Integer hostid = new Integer(h_id);
       	                            String host = (String) hmap_.get(hostid);
       	                    
	                                     int http_status = r.getStatus();
	                                     System.out.println("http_status"+http_status);
   	                                     // http_results.put(host, http_status);
   	                                     String hstatus = putils.composeStatus(http_status);
   	                                     http_result.put(hstatus, host);
   	                       
	                                     //if (r!=null) {
	                                            if (r.getStatus()==200){
	                    	                      nn=nn+1;
	                    	                      result = r;
	                    	                     // int h_id =  r.getHostId();
	                    	                      //Integer hostid = new Integer(h_id);
	                    	                      //String host = (String) hmap.get(hostid);
	                    	                      System.out.println("host" +host+ "size:"+r.getLinks().size());
	                    	                     
	                    	                      String pstatus="0";
	        	                                   if (plist!=null) {
	             	                    	               pstatus = (String) plist.get(hostid);
	             	                                  }
	        	                                   
	        	                                   if (pstatus.equals("3")) {
	                 	                    		  String ntimemap= putils.timemap_paging_backward (r.getSpecialLinks());
	                 	                    		 if (ntimemap!=null) {
	                 	                    		  System.out.println(ntimemap);
	                 	                    		  TimeMapTask task = new TimeMapTask(hostid,ntimemap);
	                 	                    		  futures.add(ecs.submit(task));
	                 	                    		  n=n+1;
	                 	                    		 }
	                 	                    		}
	                 	                    	   if (pstatus.equals("1")) {
	                 	                    		   System.out.println("index timemap");
	                 	                    		 List <String>ntimemaps= putils.timemap_paging_index  (r.getSpecialLinks());
	               	                    		    if (ntimemaps.size()>0) {
	               	                    			  for(String ntimemap : ntimemaps){	 
	               	                    				  System.out.println("submiting task:"+ntimemap);
	               	                    		          TimeMapTask task = new TimeMapTask(hostid,ntimemap);
	                	                    		      futures.add(ecs.submit(task)); 
	                	                    		      n=n+1;
	               	                    			  }
	               	                    			  //tmp fix need to investigate
	               	                    			  //pstatus="2";
	               	                    			   plist.put(hostid, "2");
	               	                    		     }
	                 	                    		               		
	                 	        	              	   }
	                 	                    	   
	                 	                    	   
	                 	                    	  //special fix
	                 	                    	  if (pstatus.equals("666")) {
	                 	                    		  //use logic to get special link 
	                 	                    		 List<Link> ttlist = r.getSpecialLinks();
	                 	                    		 String ntimemap = null;
	                 	                    		  for (Link temp : ttlist) {
	                 	                    			  if (temp.getRelationship().equals("timemap")) {
	                 	                    			  ntimemap = temp.getHref();
	                 	                    			  }
	                 	                    		  }
	                 	                    		  //String ntimemap= timemap_paging_forward (lh.getSpecialLinks());
	                 	                    		  System.out.println("timemappage666"+ntimemap);
	                 	                    		  //tasks.add(new TimeMapTask(hostid,ntimemap, client));
	                 	                    		  if (ntimemap!=null) {
	                 	                    			  //hostid find mprefix,tmap
	                 	                    		    Iterator<ArchiveDescription> dia =  timemaplist.iterator();
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
	                 	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap,mprefix, tmap);
	                 	                    		  futures.add(ecs.submit(task));
	                 	                    		  n=n+1;
	                 	                    		  }
	                 	                    	  }
	                 	                    	  
	                 	                    	   
	                 	                    	 if (pstatus.equals("-1")) {
	               	                    		  System.out.println("Dynamic timemap: case -1");
	               	                    		  //String ntimemap= timemap_paging_forward (lh.getSpecialLinks());
	               	                    		  String ntimemap = putils.timemap_paging_unknown (r.getSpecialLinks(),hostid);
	               	                    		   System.out.println("timemappage/-1"+ntimemap);
	               	                    	 	  if (ntimemap!=null) {
	               	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap);
	               	                    		  futures.add(ecs.submit(task));
	               	                    		  n=n+1;
	               	                    		 }
	               	                    		 // tasks.add(new TimeMapTask(hostid,ntimemap, client));
	               	                    		  
	               	                    	  }
	                 	                    	  
	                 	                    	   if (pstatus.equals("2")) {
	                 	                    		   //this case is not working need to return List
	                 	                    		  String ntimemap = putils.timemap_paging_forward (r.getSpecialLinks());
	                 	                    		  if (ntimemap!=null){
	                 	                    		  System.out.println("case2:next timemap" + ntimemap);
	                 	                    		  TimeMapTask task = new TimeMapTask(hostid,ntimemap);
	                 	                    		  futures.add(ecs.submit(task));
	                 	                    		  n=n+1;
	                 	                    		  }
	                 	                    		  }
	        	                                                                
	                    	                     
	                    	                      //smap.put(host,r.getLinks());
	                    	                                          
	                    	                      if (smap.containsKey(host)) {
	                    	                    		List flinks = (List) smap.get(host);
	                    	                    		flinks.addAll(r.getLinks());
	                    	                    		smap.put(host, flinks);
	                    	                    	  }
	                    	                    	  else {
	                    	                    		  smap.put(host,r.getLinks());  
	                    	                    	  }
	                    	                      parsipolis (r);
	                                      }
	                	          }
	                	         else {
	                	         Future<LinkHeader> f = ecs.poll(timeLeft, TimeUnit.SECONDS);
	                	          nn=nn+1;
	                	              System.out.println("attemp" +nn);
	                	          if ( f!=null) {    r = (LinkHeader) f.get(); } else {
	                	        	 //if f null is it timeout?
	                	        	  break;
	                	          }
	                             // if (r!=null) { 
	                	              
	                            	  int h_id =  r.getHostId();
	                            	  Integer hostid = new Integer(h_id);
        	                          String host = (String) hmap_.get(hostid); 
        	                            int http_status = r.getStatus();
	                                     // http_results.put(host, http_status);
	                                     String hstatus = putils.composeStatus(http_status);
	                                     http_result.put(hstatus, host);
	                       
        	                          
        	                          if (r.getStatus()==200) {
        	                          System.out.println("host" +host+ "size:"+r.getLinks().size());
        	                          
        	                        //  smap.put(host,r.getLinks());
        	                         // parsipolis (r,m);
        	                          String pstatus="0";
        	                                   if (plist!=null) {
             	                    	               pstatus = (String) plist.get(hostid);
             	                                  }
        	                            	                        	            	                          
        	                        	  if (pstatus.equals("3")) {
            	                    		  String ntimemap= putils.timemap_paging_backward (r.getSpecialLinks());
            	                    		  System.out.println("from 3:"+ntimemap);
            	                    		  if (ntimemap!=null) {
            	                    		  TimeMapTask task = new TimeMapTask(hostid,ntimemap);
             	                    		  futures.add(ecs.submit(task));
             	                    		  n=n+1;
            	                    		  }
            	                    		  }
            	                    	  if (pstatus.equals("1")) {
            	                    		  //this is not tested as case doesnot exists yet
            	                    		  List <String>ntimemaps= putils.timemap_paging_index  (r.getSpecialLinks());
            	                    		  if (ntimemaps.size()>0) {
            	                    			  for(String ntimemap : ntimemaps){	
            	                    			  System.out.println("submiting task:"+ntimemap);
            	                    		      TimeMapTask task = new TimeMapTask(hostid,ntimemap);
             	                    		      futures.add(ecs.submit(task)); 
             	                    		      n=n+1;
            	                    			  }
            	                    			  //pstatus="2";
            	                    			 
            	                    			    plist.put(hostid, "2");
            	                    		  }
            	                    	  }
            	                    	  //special fix
            	                    	  if (pstatus.equals("666")) {
            	                    		  //use logic to get special link 
            	                    		 List<Link> ttlist = r.getSpecialLinks();
            	                    		 String ntimemap = null;
            	                    		  for (Link temp : ttlist) {
            	                    			  if (temp.getRelationship().equals("timemap")) {
            	                    			  ntimemap = temp.getHref();
            	                    			  }
            	                    		  }
            	                    		  //String ntimemap= timemap_paging_forward (lh.getSpecialLinks());
            	                    		  System.out.println("timemappage666"+ntimemap);
            	                    		  //tasks.add(new TimeMapTask(hostid,ntimemap, client));
            	                    		  if (ntimemap!=null) {
            	                    			  //hostid find mprefix,tmap
            	                    		    Iterator<ArchiveDescription> dia =  timemaplist.iterator();
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
            	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap,mprefix, tmap);
            	                    		  futures.add(ecs.submit(task));
            	                    		  n=n+1;
            	                    		  }
            	                    	  }
            	                    	  
            	                    	  
            	                    	  if (pstatus.equals("-1")) {
            	                    		  System.out.println("Dynamic timemap: case -1");
            	                    		  //String ntimemap= timemap_paging_forward (lh.getSpecialLinks());
            	                    		  String ntimemap = putils.timemap_paging_unknown (r.getSpecialLinks(),hostid);
            	                    		   System.out.println("timemappage/-1"+ntimemap);
            	                    		if (ntimemap!=null) {
            	                    		  TimeMapTask task= new TimeMapTask(hostid,ntimemap);
            	                    		  futures.add(ecs.submit(task));
            	                    		  n=n+1;
            	                    		}
            	                    		 // tasks.add(new TimeMapTask(hostid,ntimemap, client));
            	                    		  
            	                    	  }
            	                    	  
            	                    	  if (pstatus.equals("2")) {
            	                    		  //System.out.println("doing paging forward");
            	                    		  
            	                    		  String ntimemap = putils.timemap_paging_forward(r.getSpecialLinks());
            	                    		  if (ntimemap!=null) {
            	                    		  TimeMapTask task = new TimeMapTask(hostid,ntimemap);
             	                    		  futures.add(ecs.submit(task));
             	                    		  n=n+1;
            	                    		  }
            	                    	  }
            	                    	          	                          
                	                 
                	                  
                	                  if (smap.containsKey(host)) {
          	                    		List flinks = (List) smap.get(host);
          	                    		flinks.addAll(r.getLinks());
          	                    		 smap.put(host, flinks);
          	                    	  }
          	                    	  else {
          	                    		  smap.put(host, r.getLinks());  
          	                    	  }
        	                          parsipolis (r);}
	                	         }
	                	    
	                 } catch (ExecutionException ignore) {} 
	                   catch (InterruptedException e) {
	                	   
						// TODO Auto-generated catch block
						e.printStackTrace(); break;
					} 
	             }
	             
	             
	    	 }
	    	 finally {
	    		 int count = 0;
	             for (Future<LinkHeader> f : futures) {
	            	 count=count+1;
	            	 f.cancel(true);
	            	 
	             }
	             
	         }
	    	 
	    	
	    	
	    
	    	 
	    	// ok we have list of mementos
	    	 //put stuff to db  removed july 10 2013
	    	 //since it is partial list do not do it
	    	 //oct 10 2013 update insert partial list to allow paging, set magic date to allow immediate update 
	    	//no need to filter native, since it is not going after it
	    	 RunMeBatch btask = (RunMeBatch) MyInitServlet.getInstance().getAttribute("task");
	    	//now with stale policy that will ruin second request 
	    	 //String ctime = "1996-12-31";
	    	  update = new Date();
	    	// SimpleDateFormat formatter_utc = new SimpleDateFormat("yyyy-MM-dd");
	    	  String ctime = MementoUtils.formatter_db.format(update);
	    	 //String ctime = formatter_utc.format(update);
	    	 if (smap.size()>0) {  
	    	    btask.updateAllLinks(url, smap); 
               // Date reqtime = new Date();
	    	    //System.out.println("formtatinh");
				//SimpleDateFormat formatter_utc = new SimpleDateFormat("yyyy-MM-dd");		
				btask.updateLinkmaster (url,ctime,null);
	    	 }
	    	 //nov 13 2014 
	    	 if ( smap.size()==0 &&  http_result.containsKey("5XX")){	    		     		  
	    		  btask.updateLinkmaster (url,ctime,"F");
		    	  //System.out.println(sb.toString());
		       }
	    	  else if (smap.size()==0) {
	    		 //we can mark this as special status too 
	    		 btask.updateLinkmaster (url,ctime,null);
	    	 }
	    	// return m;
	    	 
	}
	
	
    public List getGlobal() {
    	Comparator comparator = new MyComparator();
    	Collections.sort(myglobal, comparator);
    	return myglobal;
    }
	public Date getUpdate(){
		return update;
	}
	public void parsipolis (LinkHeader linkheader) {
	List<Link> links = linkheader.getLinks();
	
	  for (Link link:links) {
		 String datetime = link.getDatetime();
		 
		 if (datetime!=null) {
				myglobal.add(link);
				
		     
		 }
		  
	  }
		
	}
	
	
	public class MyComparator <Link> implements Comparator <Link> {

	    public int compare(Link link1, Link link2){
	    	Date d1 =null;
	    	Date d2 =null;
	    	try {
	    	 d1 = httpformatter.parse(((gov.lanl.agg.Link) link1).getDatetime());
	    	 d2 = httpformatter.parse(((gov.lanl.agg.Link) link2).getDatetime());
	    	} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
	           
	    	return d1.compareTo(d2);
	      
	    }
	}
	
	
	public class TimeMapTask implements Callable<LinkHeader>{
         String url;
         int ihost;
         String mementoprefix=null;
         String timemap=null;
         
        // String date;
		public TimeMapTask(int i,String url) {
			this.url=url;
			this.ihost=i;
			//this.date=date;
		}
		
		public TimeMapTask(int i,String url,String mementoprefix,String timemap) {
			this.url=url;
			this.ihost=i;
			this.mementoprefix=mementoprefix;
			this.timemap = timemap;
			//this.date=date;
		}
	
		@Override
		public LinkHeader call()  {
	 		 //ApacheHttpClient client = (ApacheHttpClient) MyInitServlet.getInstance().getAttribute("httpclient");
	 		 HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
	 		GetMethod method = null;
	 		LinkHeader linkheader = null;
	 		 try {
	 		           method = new GetMethod(url);
	 		           method.setFollowRedirects(false);
                       method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
	 	
	 		         
	 		           System.out.println("url in timemap call():"+url);
	 		           int statusCode=0;
             
			            statusCode = client.executeMethod(method);
            	    
			       
			           // System.out.println("response"+ statusCode);
            	   
			             if (statusCode == 200) {
				         //String ms = response.getEntity(String.class);
			    	
				         String ms = method.getResponseBodyAsString();
				         if (mementoprefix==null) {
				            LinkParser parser = new LinkParser(ms);
				                       parser.parse();
				           linkheader = parser.getHeader();
				           linkheader.setHostId(ihost);
				           linkheader.setStatus(statusCode);
				           }
				         else {
				    	     CdxParser parser = new CdxParser(ms);
					    	
							 parser.parse(mementoprefix,timemap,url);
							 linkheader = parser.getHeader();	
							 linkheader.setHostId(ihost);
							 linkheader.setStatus(statusCode);
							 }
			         }
			       else {
			    	linkheader = new LinkHeader();
			    	linkheader.setHostId(ihost);
			    	linkheader.setStatus(statusCode);
			       }
             }
             catch (Exception ignore) {
            	    linkheader = new LinkHeader();
			    	linkheader.setHostId(ihost);
			    	linkheader.setStatus(0);
            	 //e.printStackTrace();
             } 
			 finally {   method.releaseConnection();}
			return 	 linkheader;
	}
	}
}
