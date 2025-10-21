package gov.lanl.agg.helpers;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.LinkParser;
import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;
import gov.lanl.agg.resource.MyInitServlet;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

//import com.sun.jersey.api.client.ClientResponse;
//import com.sun.jersey.api.client.WebResource;
//import com.sun.jersey.client.apache.ApacheHttpClient;
//import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

public class TimeGateAggQuick {
	//static List <String> tglist;
	/**
	 * @param args
	 */
	//static ExecutorService exec;
	// LinkHeader linkheader = null;
	// static DefaultApacheHttpClientConfig cc;
	static ThreadSafeSimpleDateFormat  httpformatter = new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
	static { TimeZone tzo = TimeZone.getTimeZone("GMT");
	 httpformatter.setTimeZone(tzo);
	 MyInitServlet cl = MyInitServlet.getInstance();
	 Map params = (Map) cl.getAttribute("params");
	  // cc = new DefaultApacheHttpClientConfig();
		//if ( params.containsKey("web.proxyout")) {
			//String proxyurl = (String) params.get("web.proxyout");
			 //cc.getProperties().put(DefaultApacheHttpClientConfig.PROPERTY_PROXY_URI,proxyurl);
		//}
    // tglist =  (List) cl.getAttribute("timegatelist");	  		  
     //int  n= tglist.size();
    // exec = Executors.newFixedThreadPool(n);
		//newCachedThreadPool change to that
		 //exec = (ExecutorService )cl.getAttribute("MY_EXECUTOR");
	 }
	
	public NavigableMap <Long, String> getTimegateInfo (List  timegatelist,String url,String date) {
		 
		int  n = timegatelist.size();
	    //System.out.println("timegatelistsize"+n);
		// ExecutorService exec = Executors.newFixedThreadPool(n);
	     ExecutorService exec = (ExecutorService )MyInitServlet.getInstance().getAttribute("MY_EXECUTOR");
	     int largestPoolSize = ((ThreadPoolExecutor) exec).getLargestPoolSize();
	     int activecount = ((ThreadPoolExecutor) exec).getActiveCount();
	     int poolcount = ((ThreadPoolExecutor) exec).getPoolSize();
	    		System.out.printf("cached thread pool largest size was %d threadsn", largestPoolSize );
	    		System.out.printf("active  size was %d threadsn", activecount);
	    		System.out.printf("active  size was %d threadsn", poolcount);
		 CompletionService<LinkHeader> ecs = new ExecutorCompletionService<LinkHeader> (exec);
		 NavigableMap <Long, String> m = new TreeMap<Long, String>();
		 //long time = 180L; //60 sec
		 //TimeUnit unit = TimeUnit.SECONDS;
		 
		 //TimeUnit unit = TimeUnit.NANOSECONDS;
		  long TIME_BUDGET = 18L;
		  long endSeconds =  TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + TIME_BUDGET;
		 // System.out.println("endSeconds"+ endSeconds);
		  List <TimeGateTask> tasks = new ArrayList<TimeGateTask> (); 
	      //for (String timegate : timegatelist) {	 
		   	// String requrl = timegate + url;
	    	 //tasks.add(new TimeGateTask(requrl,date));
	    	 //}
	      
	      Iterator<ArchiveDescription> di =  timegatelist.iterator();
		    while (di.hasNext()) {
		    	ArchiveDescription sarchive = di.next();
		    	String tmap = sarchive.getTimegate();
		    	String turl = tmap+url;
		    	//Integer od = sarchive.getOrdernumber();
		    	//System.out.println("timemap +number" + turl +"," +od);
		    	tasks.add(new TimeGateTask(turl,date));
		    	 //tasks.add(new TimeMapTask(od, turl, client));
		       }
		    
	     
		    //ExecutorService service = Executors.newFixedThreadPool(N);

		    //ScheduledExecutorService canceller = Executors.newScheduledThreadPool(n);

		    //public void executeTask(Callable<?> c){
		      // final Future<?> future = service.submit(c);
		       //canceller.schedule(new Runnable(){
		         //  public void run(){
		           //   future.cancel(true);
		           //}
		        //}, SECONDS_UNTIL_TIMEOUT, TimeUnit.SECONDS);
		    //}   
		    
		    
	     LinkHeader result = null ;	   	 
	    	 List<Future<LinkHeader>> futures
	           = new ArrayList<Future<LinkHeader>>(n);
	    	 
	    	 try {
	                        for (TimeGateTask s : tasks) {
	                        	final Future f = ecs.submit(s);
	                            futures.add(f);	
	                           // canceller.schedule(new Runnable(){
	               		         //  public void run(){
	               		           //   f.cancel(true);
	               		           //}
	               		        //}, 8L, TimeUnit.SECONDS);
	    	                }
	                        int nn=0;
	                       for (int i = 0; i < n; ++i) {
	                             try {
	                	        long timeLeft = endSeconds-TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
	                	       // System.out.println("timegate_timeleft:"+timeLeft);
	                	        //if (timeLeft<0) break;
	                	         LinkHeader r = null;
	                	         if (result == null) {
	                             r = (LinkHeader) ecs.take().get();
	                             
	                                     if (r!=null) {
	                    	                      nn=nn+1;
	                    	                      result = r;
	                    	                      parsipolis (r,m);
	                    	                    
	                                      }
	                	          }
	                	         else {
	                	        	 System.out.println("timegate_timeleft2:"+timeLeft);
	                	        	 //if (timeLeft<0) timeLeft=0;
	                	         Future<LinkHeader> f = ecs.poll(timeLeft, TimeUnit.SECONDS);
	                	          nn=nn+1;
	                	              System.out.println("attemp" +nn);
	                	          if ( f!=null) {    r = (LinkHeader) f.get(); } else {
	                	        	 //if f null is it timeout?
	                	        	  break;
	                	          }
	                              if (r!=null) { parsipolis (r,m);}
	                	         }
	                	    
	                 } catch (ExecutionException ignore) {} 
	                   catch (InterruptedException e) {           	   
						// TODO Auto-generated catch block
						//e.printStackTrace();
	                	   break;
					} 
	             }
	             
	             
	    	 }
	    	 finally {
	    		 int count = 0;
	             for (Future<LinkHeader> f : futures) {
	            	 count=count+1;
	            	 f.cancel(true);
	            	 
	             }
	             //System.out.println ("cancelled:" + count +" threads");
	         }
	    	 
	    	 //exec.shutdown();
	    	
	    	/* 
	    	 try {
	    	//this aproach has timeout 
			List <Future<List <String>>> tfutures = exec.invokeAll(tasks,time,unit);
			
			
			Iterator<TimeGateTask> taskIter = tasks.iterator();
			
			for (Future <List <String>>f:tfutures) {
				TimeGateTask task = taskIter.next();
			  try {	
				       List <String> linkblob =    f.get();
				       
				       parsipolis (linkblob,m);
				            
			  }
			  catch (ExecutionException e) {
				  
			  }
			  catch (CancellationException e) {
				 
			  }
			}
			
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     */
	    	 
	    	// ok we have list of mementos
	    	 return m;
	    	 
	}
	
	
	public void parsipolis (LinkHeader linkheader,NavigableMap map) {
	List<Link> links = linkheader.getLinks();
	//System.out.println("size of links:"+links.size());
	  for (Link link:links) {
		 String datetime = link.getDatetime();
		 
		 if (datetime!=null) {
			 Date d ;
			try {
				d = httpformatter.parse(datetime);
			
				//d.getTime();
				//System.out.println("d" +d.getTime());
				//System.out.println("d" +d.getTime()+"mem url:"+link.getHref());
				map.put(d.getTime(), link.getHref());
				
		      } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		 }
		  
	  }
		
	}
	
	
	
	
	
	public class TimeGateTask implements Callable<LinkHeader>{
         String url;
         String date;
         LinkHeader linkheader = null;
		public TimeGateTask(String url, String date) {
			this.url=url;
			this.date=date;
		}
		/*
		@Override
		public LinkHeader call() throws Exception {
			// TODO Auto-generated method stub
			
	         //cc.getProperties().put(DefaultApacheHttpClientConfig.PROPERTY_PROXY_URI,"http://proxyout.lanl.gov:8080/"); 
	        // ApacheHttpClient client = ApacheHttpClient.create(cc);
			HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
			
			// ClientResponse response = null;
			 LinkHeader linkheader = null;
			 GetMethod method = null;
			
			try {
		        method = new GetMethod(url);
		        method.setFollowRedirects(false);
		        method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
				Header header = new Header("Accept-Datetime", date);
				method.addRequestHeader(header);
				int status = client.executeMethod(method);
				//System.out.println("timegate:"+status);
				if (status==302) {
				Header link_msg= method.getResponseHeader("Link");
				String add_msg = link_msg.getValue();
				
				//WebResource webResource = client.resource(url);
						 //System.out.println("ReqDate:" +date);
			      //           webResource.header("Accept-Datetime", date);
			 //response = webResource.get(ClientResponse.class);
			// System.out.println("status :"+response.getStatus() + "url"+ url);
			// MultivaluedMap<String, String> hmap = response.getHeaders();
			 //System.out.println(hmap.size()+hmap.toString());
			  //if ( hmap.containsKey("Link")){
			  // List<String> add_msgs = hmap.get("Link");
				//StringBuffer sb = new StringBuffer();
				//for(int k=0;k< add_msgs.size();k++) {
					//sb.append(add_msgs.get(k));
				//}
				//String add_msg = add_msgs.get(0);
				//String add_msg=sb.toString();
			//	System.out.println("link header:"+add_msg);
				LinkParser parser = new LinkParser(add_msg);
				parser.parse();
				linkheader = parser.getHeader();
			  //}
				}	
	        }
	        finally { method.releaseConnection(); } // if (response!=null) response.close();}			
			return linkheader;
		}
		*/
		 @Override
	        public LinkHeader call() throws Exception {
	            HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
	            // LinkHeader linkheader = new LinkHeader();
	           
	            checkFeedUrl(client, url, date);
	            return linkheader;
	        }
		 
		 public String checkFeedUrl(HttpClient client, String feedUrl,String date) {
	            String response = feedUrl;
	            DefaultHttpMethodRetryHandler retryHandler = new DefaultHttpMethodRetryHandler(0, false);
	           // HttpMethod method = new HeadMethod(feedUrl);
	            HttpMethod method = new GetMethod(feedUrl);
	            method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
	            //HttpMethod method = new GetMethod(feedUrl);
	            method.setFollowRedirects(false);
	            method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
	            Header header = new Header("Accept-Datetime", date);
	            method.addRequestHeader(header);

	            try {
	                // request feed
	                int statusCode = client.executeMethod(method);
	               /*
	                Header[] httpHeaders = method.getResponseHeaders();
	                for (Header hdr : httpHeaders) {
	                    System.out.println("Headers.. name,value:"+hdr.getName() + "," + hdr.getValue());
	                }
	                */
	               
	               // System.out.println("status: " +statusCode);
	                System.out.println("dyn update timegate: "+statusCode +"url: "+feedUrl + " : "+ date);
	                if ((statusCode == 301) | (statusCode == 302)|(statusCode == 303)|(statusCode == 307)) {
	                    Header varyheader = method.getResponseHeader("Vary");
	                    //check if timegate
	                    boolean addredirect = false;
	                    Header location = method.getResponseHeader("Location");
	                    if (varyheader==null){ addredirect = true;}
	                    if (varyheader!=null){
	                        if ( !varyheader.getValue().toLowerCase().contains("accept-datetime")) {
	                            addredirect = true;
	                        }
	                    }
	                    if (addredirect && !location.getValue().equals("")) {
	                        // recursively check URL until it's not redirected any more
	                       // System.out.println("redirect" + location.getValue());
	                        String iloc = location.getValue();
	                    
	                        response = checkFeedUrl(client,iloc,date);
	                    }
	                    else {
	                        Header link_msg = method.getResponseHeader("Link");
	                        String add_msg = link_msg.getValue();
	                      //  System.out.println(add_msg);
	                        LinkParser parser = new LinkParser(add_msg);
	                        parser.parse();
	                        LinkHeader linkheadertmp = parser.getHeader();
	                        Link lmemento =   linkheadertmp.getLinkByRelationship("memento");
	                        if (lmemento!=null) {
	                        	linkheader = linkheadertmp;
	                        }
	                        else {
	                        	//not sufficient info at timegate
	                        	response = checkFeedUrl(client,location.getValue(),date);
	                        }
	                        //linkheader.setHostId(ihost);
	                    }

	                } else {
	                    //other codes memento resource as input parameter
	                	
	                	 //other codes memento resource as input parameter
	                	
	                	if (statusCode!=200){
	                		//if it is archived memento of 404 etc
	                		  Header link_msg = method.getResponseHeader("Link");
	                		  if (link_msg!=null){
	                              String add_msg = link_msg.getValue();
	                             // System.out.println(add_msg);
	                		        LinkParser parser = new LinkParser(add_msg);
	                                            parser.parse();
	                        
	                  	          LinkHeader linkheadertmp = parser.getHeader();
	                              Link lmemento = linkheadertmp.getLinkByRelationship("memento");
	                                    if (lmemento!=null) {
	                  	                  System.out.println("trying to get link header info from  non - 200"); 
	                  	                   linkheader = linkheadertmp;
	                  	        
	                                       }
	                		  }
	                	}
	                	
	                	
	                    if (statusCode==200) {
	                        //System.out.println("timegate orig resource:"+feedUrl);
	                        Header link_msg = method.getResponseHeader("Link");
	                        Header varyheader = method.getResponseHeader("Vary");
	                        boolean istimegate = false;
	                        if  (varyheader!=null) { if (varyheader.getValue().toLowerCase().contains("accept-datetime")) istimegate=true; }
	                        if (link_msg!=null){
	                            String add_msg = link_msg.getValue();
	                            //System.out.println("200:"+add_msg);
	                            LinkParser parser = new LinkParser(add_msg);
	                            parser.parse();
	                                                 if (istimegate){
	                                                	//System.out.println("timegate 200"); 
	                                                	linkheader = parser.getHeader();
	                                                    //linkheader.setHostId(ihost);
	                            		             }
	                                                 else {
	                                                	 //System.out.println(" 200"); 
	                                                	   LinkHeader linkheadertmp = parser.getHeader();
	                                                       Link lmemento = linkheadertmp.getLinkByRelationship("memento");
	                                                   if (lmemento!=null) {
	                                                	  // System.out.println("trying to get link header info from  200"); 
	                                                	   linkheader = linkheadertmp;
	                                                     // String timegate = ltimegate.getHref();
	                                                     // response = checkFeedUrl(client,timegate,date);
	                                                    }
	                                                 }
	                        }//linkmsg
	                    } //code 200
	                    response = feedUrl;
	                }

	            } catch (IOException ioe) {
	                response = feedUrl;
	            }
	            finally { method.releaseConnection(); }
	            return response;
	        }

	
	}
}
