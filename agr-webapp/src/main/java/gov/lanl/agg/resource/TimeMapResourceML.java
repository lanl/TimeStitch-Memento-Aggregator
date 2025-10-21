package gov.lanl.agg.resource;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.CacheStats;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.RulesDescription;
import gov.lanl.agg.TimeMapLinkDesc;
import gov.lanl.agg.batch.RunMeBatchTask;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.helpers.TimeTravelAggQuick;
import gov.lanl.agg.helpers.TimemapAggQuick;
import gov.lanl.agg.utils.CommonRuleMatcher;
import gov.lanl.agg.utils.MLClient;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.OriginalResource;
import gov.lanl.agg.utils.RemoteCacheClient;
import gov.lanl.agg.utils.TimeGateClient;

import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.httpclient.HttpClient;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.leansoft.bigqueue.IBigQueue;

import org.apache.commons.validator.routines.UrlValidator;

/*
@author Lyudmila Balakireva
@Path("/timemap/*")
*/

@Path("/timemapml/")
//@Path("/timemap/link/{page:[0-9]+}/{id:.*}")
public class TimeMapResourceML {
	  static URI baseUri;
	  static String proxybaseuri;
	
	  static CacheStorage storage;
	  static String defaulttimemap;
	  static int pagesize;
	  static List<String> tmlist;
	  // static  AbbrSQLUtils  autils;
	  static List <ArchiveDescription> adesc;
	  static List <ArchiveDescription> defaultarchives;
	 
	  static RemoteCacheClient rcache;
	  static RulesDescription tgrulesdynamic;
	  static RulesDescription tgrulescache;
      static String livestatus;
      boolean nocache = false;
	  boolean onlycached = false;
	  static String extimegate;
	  MementoUtils mc ;
	  String protocol = null;
	  static String mlbaseurl ="";
	  static MLClient mlclient;
	  static {
		    System.out.println("timemap service init");
	        MyInitServlet cl = MyInitServlet.getInstance();
	        Map  params = (Map) cl.getAttribute("params");
	        rcache = new RemoteCacheClient((String) params.get("config.cache.registry"),(String) params.get("config.cache.self"));
	        tmlist = (List) cl.getAttribute("timemaplist");
	        // aug 19
	        //livestatus = (String) cl.getAttribute("liveclient");
	                        
	        livestatus = (String) cl.getAttribute("timemapstyle");
	      //System.out.println("timemapstatic:"+livestatus);
	        if (params.containsKey("config.service.rules")) {
	        String srules = (String) params.get("config.service.rules");
	        tgrulesdynamic =  CommonRuleMatcher.load_rules(srules,"timemap_dynamic");
	        tgrulescache =  CommonRuleMatcher.load_rules(srules,"timemap_cache");
	        }
	        if (params.containsKey("timegate.external.uri")) {
	        	extimegate=(String) params.get("timegate.external.uri");
	        }
	        else {
	        	extimegate = null;
	        }
	       // autils = (AbbrSQLUtils) cl.getAttribute("autils");
	        storage =  (CacheStorage) MyInitServlet.getInstance().getAttribute("storage");
	        //storage = new StorageImpl(params);
	        if (params.containsKey("baseuri.proxy")) {
	        	proxybaseuri= (String) params.get("baseuri.proxy");	
	        }
	        else {
	        	proxybaseuri =null;
	        }
	        if (params.containsKey("timemap.default.redirect")) {
	        	defaulttimemap = (String) params.get("timemap.default.redirect");
	        }
	        else {
	        	//temporary choice 
	        	defaulttimemap ="http://megalodon.lanl.gov/iipc/timemap/link/";
	        }
	        if (params.containsKey("timemap.page.size")) {
	        	String pagestr = (String) params.get("timemap.page.size");
	        	pagesize= Integer.parseInt(pagestr);
	        }
	        else {
	        	//temporary choice 
	        	pagesize = 500;
	        }
	        adesc= (List<ArchiveDescription>) cl.getAttribute("archivedesc");
	        mlbaseurl= (String) MyInitServlet.getInstance().getAttribute("mlbaseurl");
	        mlclient = new MLClient();
	  }
	  
	  
	public	TimeMapResourceML( @Context UriInfo uriInfo )
    {
        this.baseUri = uriInfo.getBaseUri();
        mc = new MementoUtils(baseUri);
    }
  
	public void check_cache_header(HttpHeaders hh){
		 List <String> cachecontrollst = hh.getRequestHeader("Cache-Control");
		 if (cachecontrollst!=null){
		 String cacheconstrolstr = cachecontrollst.get(0).toLowerCase();
		 if (cacheconstrolstr.contains("no-cache")){
			 System.out.println("no-cache");
			 nocache = true;
		 }
		 if (cacheconstrolstr.contains("only-if-cached")){
			// System.out.println("only-if-cached");
			 onlycached = true;
		 }
		}
	}
	
	
	
	
	@GET
	@Path("link/{page:[0-9]+}/{id:.*}")
	//@Produces("application/link-format", )
	//@Produces({"application/link-format",MediaType.TEXT_PLAIN})
	public  Response getMyLinks(@PathParam("id") String idp, @PathParam("page") String page, @Context UriInfo ui, @Context HttpHeaders hh) throws ParseException {
		 URI ur = ui.getRequestUri(); 
		 System.out.println("request timemap url:"+ur.toString());
		
		 List <String> hscheme = hh.getRequestHeader("X-Forwarded-Proto");
			
			if (hscheme==null) {
	        	protocol ="http://";
	        }
	        else {
	        	protocol ="https://";
	        }
			
		 
		 
		// String protocol = "http://";
		
		 URI baseurl = ui.getBaseUri();
		 //if (baseurl.getScheme().equals("https")) {
			// protocol = "https://";
		 //}
		 System.out.println("baseurl"+baseurl.toString());
		 String id;
		 boolean paging =false;
		 String noindex = page;
		 if (page.equals("a")) {
			  id = ur.toString().replaceFirst(baseurl.toString()+"timemapml/link/", "");	 
			  page = "1";
		 }
		 else {
		  id = ur.toString().replaceFirst(baseurl.toString()+"timemapml/link/"+page+"/", "");
		 }
		 System.out.println("page"+page);
		// System.out.println("livestatus:"+livestatus);
		
		if (id.startsWith("https:")) {
			 id = id.replaceFirst("https:", "http:");
		 }
		 
		 check_cache_header(hh);
		
	//	String sthost="";
		boolean skip = false;
	
		boolean redirect = false;
		boolean refuse = false;
		   CacheStats stats = (CacheStats) MyInitServlet.getInstance().getAttribute("timemapstats");
		   CommonRuleMatcher urlmatcher = new CommonRuleMatcher();
		   List  archivelist = urlmatcher.getArchives(id,tgrulesdynamic);
		   System.out.println("tm:"+archivelist.toString());
				
			 String redlocation ="";
		        if (archivelist.size()==1)  {
		        	   Iterator it = adesc.iterator();
		        	   while (it.hasNext()) {
		        	   ArchiveDescription ad = (ArchiveDescription) it.next();
		        	              if (archivelist.contains(ad.getName())){
		        	              String mredirect = ad.getTimemapRedirect();
		        	              if (mredirect.equals("yes")) redirect = true;
		        	              if (mredirect.equals("refuse")) refuse = true;
		        	            
		        	               redlocation = ad.getTimemap();
		        	               break;
		        	              }
		        	  }
		        }
			
			String sthost=null;
			
			
			 label: try {
	        	 
	        	  id = mc.validateAndEncodeUrl(id);
	        	      if (id==null) {
	        	         skip = true;
	        	         break label;
	        	       }
	        	  
	                URL turl = new URL(id);
	                  sthost = turl.getHost();
				     if (sthost.indexOf("web.archive.org")>0) {
	                  skip = true; break label;
	                 }
	                if (sthost.indexOf("http://")>0) {
	                 skip = true; break label;
	                }
	                if (id.length()>1024) skip = true; break label;
	         }
	         catch ( Exception ignore) {
	            ignore.printStackTrace();
	            System.out.println("url bad tg:"+id);
	            skip=true;
	         }
		     
		 		  
			 
	        if (MementoUtils.isDomainBlacklisted(sthost, (HashMap<String, String>) MyInitServlet.getInstance().getAttribute("blacklistDomains"))) {
	            // Blacklisted Domain -- 403
	            System.out.println("Blacklisted Domain: " + id);
	           
	            Response.ResponseBuilder responseBuilder = Response.status(403);
	            responseBuilder.entity(MementoUtils.composeErrorPage403(id));
	            return responseBuilder.build();
	        }
			
			
		         if (refuse) {
				    	//bad url
				    	   ResponseBuilder r = Response.status(404).entity("service not provided for url: " + id);
				    	   r.header("Content-Type", "text/plain");
				    	   return r.build();           
				    }
		  
		        if (skip) {
		    	//bad url
		    	   ResponseBuilder r = Response.status(400).entity("bad url: " + id);
		    	   r.header("Content-Type", "text/plain");
		    	   return r.build();     
		         }
		      ////notify batch about request
		        System.out.println("no notify");
		      //notify_batch(id);
		      if (redirect) {
		    	   do_redirect(redlocation,id);
		       }
		   		  		    
		      String bu=baseUri.toString();
			   if (proxybaseuri!=null){
				bu = protocol+proxybaseuri;
			  }   
		    
			   int istart=0;
			   if (isNumeric(page)) {
				istart =Integer.parseInt(page)-1;
			   }
			   
			     List f_archives = urlmatcher.getArchives(id,tgrulescache);
		         if (sthost.startsWith("www.")) {
		         sthost=sthost.replaceFirst("www.", "");	 
		         }
		         List filtered_archives = new ArrayList();
		         filtered_archives.addAll(f_archives);
		         if (!filtered_archives.contains(sthost)){
			         filtered_archives.add(sthost);
			         }
		         System.out.println("tm cache list:"+filtered_archives.toString());
		         //filtered_archives.add(sthost);
			    //for live always return static
		       //  if (livestatus.equals("true")){
		        	  stats.incrementHit();
    	        	  System.out.println("timemap live:"+id);
    	        	  return compose_internal_page( bu, id, archivelist,sthost);
    	        	 //return make_503(id); 
    	         //}
		        // storage.
			 //if stale issue 503
    	        	 /*
		     RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
	        //lastmodified header
		     Date lastupdate = rmtask.checkLastUpdate(id);
		  	 HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
		     
	          TimeZone tz = TimeZone.getTimeZone("GMT");
			  Calendar c = new GregorianCalendar(tz);		
			  c.setTime(new Date()); 
		 	  c.add(Calendar.DAY_OF_MONTH,-15);
			  Date cutoff =  c.getTime();
			 
			  if (lastupdate!=null){
	                if (lastupdate.before(cutoff)){            
	        	         stats.increamentStale();
	        	         System.out.println("timemap stale:"+id);
	        	         if (livestatus.equals("true")){
	        	        	// System.out.println("timemap stale:"+id);
	        	        	 return compose_static_page( bu, id, archivelist,sthost);
	        	        	 //return make_503(id); 
	        	         }
	        	         else {
	        	        	 if (onlycached){
	         	        		return make_504(id);
	         	        	 }
	        	        	 ResponseBuilder rb = do_dynamic_lookup(id,archivelist,istart, bu,page,filtered_archives);
	        	        	// rb.header("Last-Modified", MementoUtils.httpformatter.format(new Date()));
	        				  
	        	         return rb.build();
	        	         }
	        	         
	                  }
	                
	          
	          }	 
			  Date gracedate = getDayOffSet(Calendar.MINUTE,10);
			
		      int maxres = storage.getMatchingCount(id);
		     System.out.println("matching count:"+maxres);
		   //check cache first
		   
		  
		  
	         LinkHeader hlinks = 
				   storage.getTimeMapInfo(id, null, null, istart, pagesize +1, filtered_archives);
		   
		    Date next = null;
		    
	     if (hlinks==null) {	
	  	    	  stats.incrementMiss(); 
	  	    	 if (livestatus.equals("true")){
	  	    		 System.out.println("No records:");
    	        	// return make_503(id); 
    	        	 return compose_static_page( bu, id, archivelist,sthost);
    	         }
    	         else {
    	        	 if (onlycached){
      	        		return make_504(id);
      	        	 }
    	        	  ResponseBuilder rb = do_dynamic_lookup(id,archivelist,istart, bu,page, filtered_archives);
    	        	 // rb.header("Last-Modified", MementoUtils.httpformatter.format(new Date()));
    	        	 return rb.build();
    	         }
	  	    	  //return make_503(id); 
	  	    	 
		 }
		 else {
		    //links from storage	
		
			List  <Link> links = hlinks.getLinks();
			System.out.println( "links from db" + links.size());
			//NavigableMap<Long, Link> m = hlinks.getOrderedLinksByDate();
			 if (links.size()==0) {
				 //??
				// if (onlycached){
  	        		//return make_504(id);
  	        	 //}
  			   //no links found in archives or network error 
				 
				 if (lastupdate!=null){	   
				       if  (lastupdate.before(gracedate)){ 
				    	   ResponseBuilder rb = do_dynamic_lookup(id,archivelist,istart, bu,page, filtered_archives);
				       }}
				       
  			   ResponseBuilder r = Response.status(404).entity("not found for url: " + id);
  			   r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
  			   r.header("Link","<"+id+">;rel=\"original\"\n");
  			   r.header("Content-Type", "text/plain");
  			   return  r.build();
      	     }
			 //changed m.size() to links.size() //not accurate now
			    if ( links.size()>pagesize) {
			    	String nnext = links.get(links.size()-1).getDatetime();
			    	next  = MementoUtils.formatter_utc.parse(nnext);
       	            //next = new Date(m.pollLastEntry().getKey());
       	            System.out.println("size from db:"+links.size());
       	            int lindex = links.size()-1;
       	            links.remove(lindex);
       	           // System.out.println("size after removal from db:"+links.size());
       	         
       	        }
			     
			     String lm = links.get(links.size()-1).getDatetime();
          	     String fm = links.get(0).getDatetime();
          	  	        
			     Date fmd = MementoUtils.formatter_utc.parse(fm);
			     Date lmd = MementoUtils.formatter_utc.parse(lm);
			     //Date nmd = lmd;
			     Date nmd = next;
			     if (nmd==null) {
				 //this should be dead code, not used one
				 nmd=lmd;
			    // int s = nmd.getSeconds();
		       	 //nmd.setSeconds(s+1);
			      }
			     StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"");
					      
			   String type="";
			   //need to remove last entry
			
			  for ( Link l : links) {
			
				String ldate = l.getDatetime();
				Date fd = MementoUtils.formatter_utc.parse(ldate);
				sb.append(",\n <"+ l.getHref()+">;rel=\"memento"+ type+"\"; datetime=\"" + 
				MementoUtils.httpformatter.format(fd)+ "\"");
			  }
			
			 if (extimegate!=null) {
				 sb.append(",\n <"+extimegate + id+">;rel=\"timegate\"");
			 }else {
			  sb.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
			 }
			 String selfp = page +"/";
			 if (noindex.equals("a")){
				 selfp="";
			 }
			 
			 sb.append (",\n <"+bu +"timemap/link/" +selfp + id+
					">;rel=\"self\"; type=\"application/link-format\"; from =\""+
					MementoUtils.httpformatter.format(fmd)+"\";until=\""+
					MementoUtils.httpformatter.format(lmd)+"\"");
			  if (!noindex.equals("a")){
			            sb.append (",\n <"+bu +"timemap/link/"+ id+
						">;rel=\"index\"; type=\"application/link-format\"");
			  }
			  //nuznho calculate if page numeric +1 if date +1sec
			  if (paging==true) {
			  istart = istart + pagesize;
			   if (maxres>istart) {			
			      sb.append (",\n <"+bu +"timemap/link/" +istart +"/"+ id+
					">;rel=\"timemap\"; type=\"application/link-format\"; from =\""+
					MementoUtils.httpformatter.format(nmd)+"\"");
			   }
			  }
			  ResponseBuilder r = Response.ok(sb.toString());
			  r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
			  r.header("Content-Type", "application/link-format");
			  return  r.build(); 
			 
			
		}
		  */        
		               
		
		             
	}
	
	    public  Response make_503(String id) { 
	     ResponseBuilder r = Response.status(503).entity("Retry-After: 30 sec for url: " + id);
	     r.header("Link","<"+id+">;rel=\"original\"\n");
	     r.header("Content-Type", "text/plain");
	     r.header("Retry-After", 30);
	     return  r.build();  
	    }
	    public  Response make_504(String id) { 
		     ResponseBuilder r = Response.status(504).entity("Not cached: " + id);
		     r.header("Link","<"+id+">;rel=\"original\"\n");
		     r.header("Content-Type", "text/plain");		    
		     return  r.build();  
		    }
	    
	  //notify batch about request
	    public void notify_batch(String id){
	      try {
           IBigQueue ressyncque = (IBigQueue) MyInitServlet.getInstance().getAttribute("ResSyncQue");
           Date now = new Date();
           if  (ressyncque!=null) {
           String pload = now.getTime()+"|"+ id;
           System.out.println("posting to Q:"+pload);
           ressyncque.enqueue(pload.getBytes());
           
           }
           }
           catch (Exception e) {
			      // TODO Auto-generated catch block
			        e.printStackTrace();
		            }
	    }
	    
	@GET
	@Path("json/{page:[0-9]+}/{id:.*}")
	//@Produces("application/json" )
	
	public  Response getMyJsonLinks(@PathParam("id") String idp, @PathParam("page") String page, @Context UriInfo ui, @Context HttpHeaders hh) throws ParseException {
		 URI ur = ui.getRequestUri(); 
		 System.out.println("request timemap url:"+ur.toString());
		 URI baseurl = ui.getBaseUri();
		 System.out.println("baseurl"+baseurl.toString());
		 String id;
		 boolean paging =false;
		// boolean nocache = false;
		// boolean onlycached = false;
		 
		 List <String> hscheme = hh.getRequestHeader("X-Forwarded-Proto");
			
			if (hscheme==null) {
	        	protocol ="http://";
	        }
	        else {
	        	protocol ="https://";
	        }
			
		 
		 
		// String protocol = "http://";
		 //if (baseurl.getScheme().equals("https")) {
			// protocol = "https://";
		 //}
		 if (page.equals("a")) {
			  id = ur.toString().replaceFirst(baseurl.toString()+"timemapml/json/", "");	 
			  page = "1";
		 }
		 else {
		  id = ur.toString().replaceFirst(baseurl.toString()+"timemapml/json/"+page+"/", "");
		 }
		 System.out.println("page"+page);
		 //id = id.indexOf("/")
		 //System.out.println("get into get:"+id);
		
		//System.out.println("id"+id);
		 if (id.startsWith("https:")) {
			 id = id.replaceFirst("https:", "http:");
		 }
		 check_cache_header(hh);
		
		 
	//	String sthost="";
		boolean skip = false;
	
		boolean redirect = false;
		boolean refuse = false;
		   CacheStats stats = (CacheStats) MyInitServlet.getInstance().getAttribute("timemapstats");
		   CommonRuleMatcher urlmatcher = new CommonRuleMatcher();
		   List  archivelist = urlmatcher.getArchives(id,tgrulesdynamic);
		   System.out.println("tm:"+archivelist.toString());
		
			
			 String redlocation ="";
		        if (archivelist.size()==1)  {
		        	   Iterator it = adesc.iterator();
		        	   while (it.hasNext()) {
		        	   ArchiveDescription ad = (ArchiveDescription) it.next();
		        	              if (archivelist.contains(ad.getName())){
		        	              String mredirect = ad.getTimemapRedirect();
		        	              if (mredirect.equals("yes")) redirect = true;
		        	              if (mredirect.equals("refuse")) refuse = true;
		        	            
		        	               redlocation = ad.getTimemap();
		        	               break;
		        	              }
		        	  }
		        }
			
			String sthost=null;
			
			
			  label: try {
		        	 
	        	  id = mc.validateAndEncodeUrl(id);
	        	      if (id==null) {
	        	         skip = true;
	        	         break label;
	        	       }
	        	  
	                URL turl = new URL(id);
	                  sthost = turl.getHost();
				     if (sthost.indexOf("web.archive.org")>0) {
	                  skip = true; break label;
	                 }
	                if (sthost.indexOf("http://")>0) {
	                 skip = true; break label;
	                }
	                if (id.length()>1024) skip = true; break label;
	         }
	         catch ( Exception ignore) {
	            ignore.printStackTrace();
	            System.out.println("url bad tm:"+id);
	            skip=true;
	         }
			
			
			
			
			
			 if (MementoUtils.isDomainBlacklisted(sthost, (HashMap<String, String>) MyInitServlet.getInstance().getAttribute("blacklistDomains"))) {
		            // Blacklisted Domain -- 403
		            System.out.println("Blacklisted Domain: " + id);
		           
		            Response.ResponseBuilder responseBuilder = Response.status(403);
		            responseBuilder.entity(MementoUtils.composeErrorPage403(id));
		            return responseBuilder.build();
		        }
		    
		  
		  
		         if (refuse) {
				    	//bad url
				    	   ResponseBuilder r = Response.status(404).entity("service not provided for url: " + id);
				    	   r.header("Content-Type", "text/plain");
				    	   return r.build();
			                  
				    }
		  
		      if (skip) {
		    	//bad url
		    	   ResponseBuilder r = Response.status(400).entity("bad url: " + id);
		    	   r.header("Content-Type", "text/plain");
		    	   return r.build();
	                  
		     }
		    //notify batch about request
		     // notify_batch(id);
		      
		      if (redirect) {
		    	   do_redirect(redlocation,id);
		       }
		  
		  
		    
		    String bu = baseUri.toString();
			if (proxybaseuri!=null){
				bu = protocol+proxybaseuri;
			}   
		    
			 List f_archives = urlmatcher.getArchives(id,tgrulescache);
	         if (sthost.startsWith("www.")) {
	         sthost=sthost.replaceFirst("www.", "");	 
	         }
	         List filtered_archives = new ArrayList();
	         filtered_archives.addAll(f_archives);
	        
	         if (!filtered_archives.contains(sthost)){
	         filtered_archives.add(sthost);
	         }
	         int istart=0;
		     if (isNumeric(page)) {
			     istart =Integer.parseInt(page)-1;
		      }
	         
		    // if (livestatus.equals("true")){
		    	  stats.incrementHit();
	        	 System.out.println("timemap stale:"+id);
	        	 //return make_503(id);
	        	return  compose_internal_json_page( bu, id,  archivelist,sthost);
	        // }
		     /*
			 //if stale issue 503
		     RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
		     HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
	         Date lastupdate = rmtask.checkLastUpdate(id);
	         TimeZone tz = TimeZone.getTimeZone("GMT");
			  Calendar c = new GregorianCalendar(tz);		
			  c.setTime(new Date()); 
		 	  c.add(Calendar.DAY_OF_MONTH,-15);
			  Date cutoff =  c.getTime();
			  // only do it at index page
			  //if (!nocache) {
				//  if (lastupdate==null) {
				  //    Map m =(Map) rcache.CheckCaches(client, id, sthost, rmtask, storage);
				    //  if (m!=null){
				    //	  lastupdate = rmtask.checkLastUpdate(id);
				     // }
				    // }
				  //else {
					//  if (lastupdate.before(cutoff)){   
						//  Map m =(Map) rcache.CheckCaches(client, id, sthost, rmtask, storage);
					     // if (m!=null){
					    	//  lastupdate = rmtask.checkLastUpdate(id);
					      //}  
					  //}
				  //}
				  //}	
			  
			  
			  
			  if (lastupdate!=null){
	                if (lastupdate.before(cutoff)){            
	        	         stats.increamentStale();
	        	         if (livestatus.equals("true")){
	        	        	 System.out.println("timemap stale:"+id);
	        	        	 //return make_503(id);
	        	        	return  compose_json_page( bu, id,  archivelist,sthost);
	        	         }
	        	         else {
	        	        	  if (onlycached){
		         	        		return make_504(id);
		         	        	 }
	        	         return json_dynamic_lookup(id,archivelist,istart, bu,page,filtered_archives);
	        	         }
	        	        // return make_503(id); 
	        	         }
	             }	 
			  Date gracedate = getDayOffSet(Calendar.MINUTE,10);
			   //Calendar c1 = new GregorianCalendar(tz);		
			   //c1.setTime(new Date()); 
		 	   //c.add(Calendar.MINUTE,-30);
			   //Date gracedate =  c.getTime();
			  // only do it at index page
			 
			   //if (nocache){
				 //  if (lastupdate!=null){
			      // if  (lastupdate.before(gracedate)){  
				   // stats.incrementHit();
				   ////ResponseBuilder rb = json_dynamic_lookup(id,archivelist,istart, bu,page,filtered_archives);
				   //return json_dynamic_lookup(id,archivelist,istart, bu,page,filtered_archives);
			     //}
			   //}
			   //}
			   
			   
			   
		     int maxres = storage.getMatchingCount(id);
		// System.out.println("matching count:"+maxres);
		   //check cache first
		   		  
	         LinkHeader hlinks = 
				   storage.getTimeMapInfo(id, null, null, istart, pagesize +1, filtered_archives);
		   
		    Date next = null;
		    
	  	if (hlinks==null) {	
	  	    	  stats.incrementMiss(); 
	  	  	      //do_dynamic_lookup(id,archivelist,istart, bu,page);
	  	    	 if (livestatus.equals("true")){
	  	    		return  compose_json_page( bu, id,  archivelist,sthost);
    	        	// return make_503(id); 
    	         }
    	         else {
    	        	 if (onlycached){
       	        		return make_504(id);
       	        	 }
	  	    	 return json_dynamic_lookup(id,archivelist,istart, bu,page,filtered_archives);
    	         }
	  	    	 // return make_503(id); 
	  	    	  	    	
		}
		else {
		    //links from storage	
			//List <Link> links = hlinks.getSpecialLinks();
			List  <Link> links = hlinks.getLinks();
			System.out.println( "links from db" + links.size());
			//NavigableMap<Long, Link> m = hlinks.getOrderedLinksByDate();
			 if (links.size()==0) {
  			   //no links found in archives or network error 
				 if (lastupdate!=null){	   
				       if  (lastupdate.before(gracedate)){ 
				    	   ResponseBuilder rb = do_dynamic_lookup(id,archivelist,istart, bu,page, filtered_archives);
				       }}
  			   ResponseBuilder r = Response.status(404).entity("not found for url: " + id);
  			   r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
  			   r.header("Link","<"+id+">;rel=\"original\"\n");
  			   r.header("Content-Type", "text/plain");
  			   return  r.build();
      	     }
			 //changed m.size() to links.size() //not accurate now
			    if ( links.size()>pagesize) {
			    	String nnext = links.get(links.size()-1).getDatetime();
			    	next  = MementoUtils.formatter_utc.parse(nnext);
       	            //next = new Date(m.pollLastEntry().getKey());
       	            System.out.println("size from db:"+links.size());
       	            int lindex = links.size()-1;
       	            links.remove(lindex);
       	           // System.out.println("size after removal from db:"+links.size());
       	         
       	        }
			     
			     String lm = links.get(links.size()-1).getDatetime();
          	     String fm = links.get(0).getDatetime();
          	  	        
			     Date fmd = MementoUtils.formatter_utc.parse(fm);
			     Date lmd = MementoUtils.formatter_utc.parse(lm);
			     //Date nmd = lmd;
			     Date nmd = next;
			     if (nmd==null) {
				 //this should be dead code, not used one
				 nmd=lmd;
			    // int s = nmd.getSeconds();
		       	 //nmd.setSeconds(s+1);
			      }
			    // StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"\n");
			     StringBuffer sb = new StringBuffer();
				   sb.append("{");
				   sb.append("\"original_uri\":\""+id+"\",\n");
				   
				  
				   if (extimegate!=null) {
					   sb.append("\"timegate_uri\":\""+extimegate + id+"\",\n");
				   } else {
				   sb.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
				   }
			    String type="";
			   //need to remove last entry
			    sb.append("\"mementos\": {\n");
			    sb.append("\"list\": [\n");
			    int count = 0;
			    int total = links.size();
			    for (int i=0;i<links.size();i++) {
			  //for ( Link l : links) {
			    	  Link l = links.get(i);
				  count=count+1;
				  sb.append("{");
				  String ldate = l.getDatetime();
				  Date fd = MementoUtils.formatter_utc.parse(ldate);
				  sb.append( "\"datetime\":\""+ MementoUtils.timeTravelJsFormatter.format(fd)+"\",\n");
				  sb.append("\"uri\":\""+ l.getHref()+"\"\n");
								
				  //sb.append(", <"+ l.getHref()+">;rel=\"memento"+ type+"\"; datetime=\"" + 
				  //MementoUtils.httpformatter.format(fd)+ "\"\n");
				 // sb.append("},\n");
				  
				  if ((i==links.size()-1) && (count==total)){
	            	  sb.append("}\n");
	              }else {
	              sb.append("},\n");
	              }
			  }
			      sb.append("]\n");
				  sb.append( " },");
			
			//  sb.append(" , <"+bu +"timegate/"+ id+">;rel=\"timegate\"\n");
				 sb.append("\"timemap_uri\": {\n");
				 sb.append("\"json_format\": \""+bu +"timemap/json/" +page +"/"+ id+"\"");
		         sb.append("}\n");
			    // sb.append (" , <"+bu +"timemap/link/" +page +"/"+ id+
					//">;rel=\"self\"; type=\"application/link-format\"; from =\""+
					//MementoUtils.httpformatter.format(fmd)+"\";until=\""+
					//MementoUtils.httpformatter.format(lmd)+"\"");
			  //nuznho calculate if page numeric +1 if date +1sec
			  if (paging==true) {
			  istart = istart + pagesize;
			   if (maxres>istart) {			
			      sb.append (" , <"+bu +"timemap/link/" +istart +"/"+ id+
					">;rel=\"timemap\"; type=\"application/link-format\"; from =\""+
					MementoUtils.httpformatter.format(nmd)+"\"");
			   }
			  }
			  
			  sb.append( " }");
			  
			  ResponseBuilder r = Response.ok(sb.toString());	
			  r.header("Content-Type", "application/json");
			  r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
			  return  r.build(); 
			
		}
		 */         
		               
		
		             
	}
	
	
	
	//we use this again
	
	public  ResponseBuilder do_dynamic_lookup(String id,List archivelist,int istart,String bu,String page,List filtered_archives) throws ParseException{
		  TimemapAggQuick tgq = new TimemapAggQuick();
          Iterator<ArchiveDescription> ait = adesc.iterator();
		       List<ArchiveDescription> tglistp = new ArrayList<ArchiveDescription>();		   	
		              while(ait.hasNext()) {
			                ArchiveDescription ard =  ait.next();
			                String name = ard.getName(); 
			                if ( archivelist.contains(name)){
				            tglistp.add(ard);
			                }
		                }
		 //grab links		   	
     tgq.getTimeMapInfo(tglistp, id);	                          
     List  <Link> links = tgq.getGlobal();
     Date update =  tgq.getUpdate();
     StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"");
           if (links.size() ==0) {
	              ResponseBuilder r = Response.status(404).entity(" not found for url: " + id);
	              r.header("Last-Modified", MementoUtils.httpformatter.format(update));
	              System.out.println("no timemaps in archives"); 
	              return  r;  
            }
           if (links.size() < pagesize+1){
        	   
            System.out.println("dynamic less 1000");
            long lastm = 0;
            long nexttm = 0;            
	        int nif = 0;
	  
	        String lastlink = links.get(links.size()-1).getDatetime();
	            // "Tue, 26 Nov 2013 20:33:33 GMT"
	        Date lastlinkd = MementoUtils.httpformatter.parse(lastlink);
	        String firstlink=links.get(0).getDatetime();
	        Date firstlinkd = MementoUtils.httpformatter.parse(firstlink);
	        long firstm = firstlinkd.getTime();
	        lastm = lastlinkd.getTime();
	        String type="";
	   
		    for ( Link l : links) {
			   nif=nif+1;
			   String ldate = l.getDatetime();
			   Date fd = MementoUtils.httpformatter.parse(ldate);
			         if(nif==pagesize) { 
   		        lastm = fd.getTime();
   		        if (lastm==lastlinkd.getTime()) break;
			            }
		    	     else if(nif==pagesize+1L) { 
				     nexttm = fd.getTime();
				     break;
			          }
			sb.append(",\n <"+ l.getHref()+">;rel=\"memento"+ type+"\"; datetime=\"" + 
			MementoUtils.httpformatter.format(fd)+ "\"");
		   }
		    if (extimegate!=null) {
				 sb.append(",\n <"+extimegate + id+">;rel=\"timegate\"");
			 }else {
			  sb.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
			 }
           //sb.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
		   //removed page from here april 6 2016
	       sb.append (",\n <"+bu +"timemap/link/" +
           id+">;rel=\"self\"; type=\"application/link-format\"; from =\""+
	            MementoUtils.httpformatter.format(new Date(firstm))+"\";until=\""+ 
           MementoUtils.httpformatter.format(new Date(lastm))+"\"\n");
       
           ResponseBuilder r = Response.ok(sb.toString());
           r.header("Content-Type", "application/link-format");
           r.header("Last-Modified", MementoUtils.httpformatter.format(update));
		
       return  r;
           }
           else {
        	
           Map tmmap = storage.getTimeMapIndexInfo(id, pagesize,filtered_archives);
    	  	    //if (hlinks==null) {	
    		    if( tmmap.size()==0) {  		    	
    		    //no records 		    	
    		    	   ResponseBuilder r = Response.status(404).entity("not found for url: " + id);
    	  			   r.header("Link","<"+id+">;rel=\"original\"\n");
    	  			   r.header("Content-Type", "text/plain");
    	  			   return  r;
    		    
    		     }
    		    else {
    		    //links from storage	
    			//List <Link> links = hlinks.getSpecialLinks();
    		   // stats.incrementHit(); 
    			   Iterator it = tmmap.keySet().iterator();
    			   StringBuffer sbn = new StringBuffer("<"+id+">;rel=\"original\"");
    			   if (extimegate!=null) {
    					 sbn.append(",\n <"+extimegate + id+">;rel=\"timegate\"");
    				 }else {
    				  sbn.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
    				 }
    			   //sbn.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
    			   Date fromself = null;
    			    while (it.hasNext()) {
    				String  key= (String) it.next();
    				TimeMapLinkDesc tml = (TimeMapLinkDesc) tmmap.get(key);
    				Date nmd = tml.getFromdate();
    				if (key.equals("0")) {
    					fromself=nmd;
    					key="1";
    				}
    				 Date lmd = tml.getUntildate();
    			     sbn.append (",\n <"+bu +"timemap/link/" +key +"/"+ id+
    							">;rel=\"timemap\"; type=\"application/link-format\"; from =\""+
    							MementoUtils.httpformatter.format(nmd)+"\"");
    							if (lmd!=null) { 
    							sbn.append(";until=\""+MementoUtils.httpformatter.format(lmd)+"\"");
    							}
    							
    			 }
    			    
    			  sbn.append (",\n <"+bu +"timemap/link/" + id+
    						">;rel=\"self\"; type=\"application/link-format\"; from =\""+
    						MementoUtils.httpformatter.format(fromself)+"\"\n");
    			  
    			  ResponseBuilder r = Response.ok(sbn.toString());
    			  r.header("Last-Modified", MementoUtils.httpformatter.format(update));
    			  r.header("Content-Type", "application/link-format");
    			  return  r; 
    		    }
        	                
        	  // return getIndexLinks(id,"a", ui) ;
           }
	}
	
	
	//we use this again
	
		public  Response json_dynamic_lookup(String id,List archivelist,int istart,String bu,String page,List filtered_archives) throws ParseException{
			  TimemapAggQuick tgq = new TimemapAggQuick();
	          Iterator<ArchiveDescription> ait = adesc.iterator();
			       List<ArchiveDescription> tglistp = new ArrayList<ArchiveDescription>();		   	
			              while(ait.hasNext()) {
				                ArchiveDescription ard =  ait.next();
				                String name = ard.getName(); 
				                if ( archivelist.contains(name)){
					            tglistp.add(ard);
				                }
			                }
			 //grab links		   	
	     tgq.getTimeMapInfo(tglistp, id);
	     Date update =  tgq.getUpdate();
	     List  <Link> links = tgq.getGlobal();
	 
	    // StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"\n");
	     
	        StringBuffer sb = new StringBuffer();
		    sb.append("{");
		    sb.append("\"original_uri\":\""+id+"\",\n");
		    //sb.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
		    if (extimegate!=null) {
				   sb.append("\"timegate_uri\":\""+extimegate + id+"\",\n");
			   } else {
			   sb.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
			   }
		    
		    sb.append("\"mementos\": {\n");
		    sb.append("\"list\": [\n");
	     
	           if (links.size() ==0) {
		              ResponseBuilder r = Response.status(404).entity(" not found for url: " + id);
		              System.out.println("no timemaps in archives"); 
		              return  r.build();  
	            }
	           if (links.size() < pagesize+1){
	        	   
	            System.out.println("dynamic less 1000");
	            long lastm = 0;
	            long nexttm = 0;            
		        int nif = 0;
		  
		        String lastlink = links.get(links.size()-1).getDatetime();
		            // "Tue, 26 Nov 2013 20:33:33 GMT"
		        Date lastlinkd = MementoUtils.httpformatter.parse(lastlink);
		        String firstlink=links.get(0).getDatetime();
		        Date firstlinkd = MementoUtils.httpformatter.parse(firstlink);
		        long firstm = firstlinkd.getTime();
		        lastm = lastlinkd.getTime();
		        String type="";
		   
			   // for ( Link l : links) {		    	
			    	    int count = 0;
					    int total = links.size();
					    for (int i=0;i<links.size();i++) {
					  //for ( Link l : links) {
					    	  Link l = links.get(i);
						  count=count+1;
					
			    	sb.append("{");
				   nif=nif+1;
				   String ldate = l.getDatetime();
				   Date fd = MementoUtils.httpformatter.parse(ldate);
				         if(nif==pagesize) { 
	   		        lastm = fd.getTime();
	   		        if (lastm==lastlinkd.getTime()) break;
				            }
			    	     else if(nif==pagesize+1L) { 
					     nexttm = fd.getTime();
					     break;
				          }
				          sb.append( "\"datetime\":\""+ MementoUtils.timeTravelJsFormatter.format(fd)+"\",\n");
						  sb.append("\"uri\":\""+ l.getHref()+"\"\n");
						
				//sb.append(", <"+ l.getHref()+">;rel=\"memento"+ type+"\"; datetime=\"" + 
				//MementoUtils.httpformatter.format(fd)+ "\"\n");
						  if ((i==links.size()-1) && (count==total)){
			            	  sb.append("}\n");
			              }else {
			              sb.append("},\n");
			              }
			   }
			    
					    sb.append("]\n");
						  sb.append( " },");
					
					//  sb.append(" , <"+bu +"timegate/"+ id+">;rel=\"timegate\"\n");
						 sb.append("\"timemap_uri\": {\n");
						 sb.append("\"json_format\": \""+bu +"timemap/json/" +page +"/"+ id+"\"");
				         sb.append("}\n");
				         
	          
				         
				         
				         sb.append( " }");
						  
						  ResponseBuilder r = Response.ok(sb.toString());	
						  r.header("Content-Type", "application/json");
						  r.header("Last-Modified", MementoUtils.httpformatter.format(update));
	                    return  r.build();
	           }
	           else {
	        	
	        	    Map tmmap = storage.getTimeMapIndexInfo(id, pagesize,filtered_archives);
	    	  	    //if (hlinks==null) {	
	    		    if( tmmap.size()==0) {
	    		    	
	    		    //no records should be dead code
	    		    	
	    		    	   ResponseBuilder r = Response.status(404).entity("not found for url: " + id);
	    	  			   r.header("Link","<"+id+">;rel=\"original\"\n");
	    	  			   r.header("Content-Type", "text/plain");
	    	  			   return  r.build();
	    		    	
	    	  	    	 	  	    
	    		     }
	    		    else {
	    		    //links from storage	
	    			//List <Link> links = hlinks.getSpecialLinks();
	    		   // stats.incrementHit(); 
	    		    	
	    		    	    		    	
	    					  Iterator it = tmmap.keySet().iterator();
	    					  // StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"\n");
	    					   StringBuffer sbn = new StringBuffer();
	    					   sbn.append("{");
	    					   sbn.append("\"original_uri\":\""+id+"\",\n");
	    					   if (extimegate!=null) {
	    						   sbn.append("\"timegate_uri\":\""+extimegate + id+"\",\n");
	    					   } else {
	    					   sbn.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
	    					   }
	    					   //sbn.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
	    					  // sb.append(" , <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
	    					   Date fromself = null;
	    					   sbn.append("\"timemap_index\": [\n");
	    					   int count = 0;
	    					   
	    					    while (it.hasNext()) {
	    						String  key= (String) it.next();
	    						TimeMapLinkDesc tml = (TimeMapLinkDesc) tmmap.get(key);
	    						Date nmd = tml.getFromdate();
	    						if (key.equals("0")) {
	    							fromself=nmd;
	    							key="1";
	    						}
	    						 Date lmd = tml.getUntildate();
	    						 sbn.append( "{");
	    						 sbn.append( "\"from\":\""+ MementoUtils.timeTravelJsFormatter.format(nmd)+"\",\n");
	    						 if (lmd!=null){
	    						 sbn.append( "\"until\":\""+ MementoUtils.timeTravelJsFormatter.format(lmd)+"\",\n");
	    						 }
	    						 sbn.append("\"uri\":\""+bu +"timemap/json/" +key +"/"+ id+"\"\n");
	    						 sbn.append( "},");
	    					    // sb.append ("\n, <"+bu +"timemap/link/" +key +"/"+ id+
	    							//		">;rel=\"timemap\"; type=\"application/link-format\"; from =\""+
	    								//	MementoUtils.httpformatter.format(nmd)+"\"");
	    									//if (lmd!=null) { 
	    									//sb.append(";until=\""+MementoUtils.httpformatter.format(lmd)+"\"");
	    									//}
	    									
	    					 }
	    					    
	    					   sbn.replace(sbn.lastIndexOf(","),sbn.lastIndexOf(",")+1,"");
	    					   sbn.append("],");
	    					   sbn.append( "\"timemap_uri\": {");
	    					   
	    					   sbn.append( "\"json_format\": \""+  bu +"timemap/json/" + id+"\",\n");
	    					   sbn.append( "\"link_format\": \""+  bu +"timemap/link/" + id+"\"\n");
	    					     
	    					   sbn.append("}");
	    					  // sb.append ("\n, <"+bu +"timemap/link/" + id+
	    						//		">;rel=\"self\"; type=\"application/link-format\"; from =\""+
	    							//	MementoUtils.httpformatter.format(fromself)+"\"\n");
	    					   sbn.append("}");
	    					  ResponseBuilder r = Response.ok(sbn.toString());	
	    					  r.header("Content-Type", "application/json");
	    					  r.header("Last-Modified", MementoUtils.httpformatter.format(update));
	    					  return  r.build(); 
	    					
	    		    	
	    		    }
	        	   
	        	   	              
	        	  // return getIndexLinks(id,"a", ui) ;
	           }
		}
		
		
	
	
	
	@GET
	@Path("link/{id:.*}")
	//@Produces("application/link-format" )
	public  Response getDefaultLinks(@PathParam("id") String idp, @Context UriInfo ui, @Context HttpHeaders hh) throws ParseException {
		String page = "a";
		List <String> hscheme = hh.getRequestHeader("X-Forwarded-Proto");
			
		if (hscheme==null) {
        	protocol ="http://";
        }
        else {
        	protocol ="https://";
        }
		//return getMyLinks(idp,page, ui) ;
		return getIndexLinks(idp,page, ui,hh) ;
			
	}
	
	@GET
	@Path("json/{id:.*}")
	//@Produces("application/json" )
	public  Response getJLinks(@PathParam("id") String idp, @Context UriInfo ui, @Context HttpHeaders hh) throws ParseException {
		String page = "a";
		//return getMyLinks(idp,page, ui) ;
		 List <String> hscheme = hh.getRequestHeader("X-Forwarded-Proto");
		 if (hscheme==null) {
	        	protocol ="http://";
	        }
	        else {
	        	protocol ="https://";
	        }
		return getIndexLinksJson(idp,page, ui,hh) ;
			
	}
	
	
	public Response getIndexLinksJson(@PathParam("id") String idp, @PathParam("page") String page, @Context UriInfo ui, @Context HttpHeaders hh) throws ParseException {
		 URI ur = ui.getRequestUri(); 
		 System.out.println("request timemap url:"+ur.toString());
		 URI baseurl = ui.getBaseUri();
		 System.out.println("baseurl"+baseurl.toString());
		// String protocol = "http://";
		 //if (baseurl.getScheme().equals("https")) {
			// protocol = "https://";
		 //}
		 String id;
		 id = ur.toString().replaceFirst(baseurl.toString()+"timemapml/json/", "");	 
		 page = "1";
		 System.out.println("page"+page);
		 if (id.startsWith("https:")) {
			id = id.replaceFirst("https:", "http:");
		 }
		check_cache_header(hh);
		String sthost="";
		boolean skip = false;
		boolean redirect = false;
		boolean refuse = false;
		   CacheStats stats = (CacheStats) MyInitServlet.getInstance().getAttribute("timemapstats");
		   CommonRuleMatcher urlmatcher = new CommonRuleMatcher();
		   List  archivelist = urlmatcher.getArchives(id,tgrulesdynamic);
		   System.out.println("tm: dynamic"+archivelist.toString());
					
			 String redlocation ="";
		        if (archivelist.size()==1)  {
		        	   Iterator it = adesc.iterator();
		        	   while (it.hasNext()) {
		        	   ArchiveDescription ad = (ArchiveDescription) it.next();
		        	              if (archivelist.contains(ad.getName())){
		        	              String mredirect =  ad.getTimemapRedirect();
		        	              if (mredirect.equals("yes")) redirect = true;
		        	              if (mredirect.equals("refuse")) refuse = true;        	            
		        	               redlocation = ad.getTimemap();
		        	               break;
		        	              }
		        	  }
		        }
			
		        label: try {
		        	 
		        	  id = mc.validateAndEncodeUrl(id);
		        	      if (id==null) {
		        	         skip = true;
		        	         break label;
		        	       }
		        	  
		                URL turl = new URL(id);
		                  sthost = turl.getHost();
					     if (sthost.indexOf("web.archive.org")>0) {
		                  skip = true; break label;
		                 }
		                if (sthost.indexOf("http://")>0) {
		                 skip = true; break label;
		                }
		                if (id.length()>1024) skip = true; break label;
		         }
		         catch ( Exception ignore) {
		            ignore.printStackTrace();
		            System.out.println("url bad tm:"+id);
		            skip=true;
		         }
		     	
		        if (MementoUtils.isDomainBlacklisted(sthost, (HashMap<String, String>) MyInitServlet.getInstance().getAttribute("blacklistDomains"))) {
		            // Blacklisted Domain -- 403
		            System.out.println("Blacklisted Domain: " + id);
		           
		            Response.ResponseBuilder responseBuilder = Response.status(403);
		            responseBuilder.entity(MementoUtils.composeErrorPage403(id));
		            return responseBuilder.build();
		        }
		        
		        
		        
		        
		         if (refuse) {
				    	//bad url
				    	   ResponseBuilder r = Response.status(404).entity("service not provided for url: " + id);
				    	   r.header("Content-Type", "text/plain");
				    	   return r.build();                  
				    }
		  
		        if (skip) {
		    	//bad url
		    	   ResponseBuilder r = Response.status(400).entity("bad url: " + id);
		    	   return r.build();
	               }
		    //notify batch about request
		     //   notify_batch(id);
		      
		        if (redirect) {
			    	   do_redirect(redlocation,id);
			    }
		    		
		        String bu=baseUri.toString();
				if (proxybaseuri!=null){
					bu = protocol+proxybaseuri;
				}  
		        
				     List f_archives = urlmatcher.getArchives(id,tgrulescache);
			         if (sthost.startsWith("www.")) {
			              sthost = sthost.replaceFirst("www.", "");	 
			         }
			         List filtered_archives = new ArrayList();
			         filtered_archives.addAll(f_archives);
			        
			         if (!filtered_archives.contains(sthost)){
				         filtered_archives.add(sthost);
				         
				         }
			         System.out.println("tm: cache list"+filtered_archives.toString());
			 		
			         //filtered_archives.add(sthost);
			    
			        // if (livestatus.equals("true")){
		     	        	
	     	        	 //return make_503(id);
	     	        	  // ResponseBuilder r = Response.status(405);
	     	  			   //r.header("Link","<"+id+">;rel=\"original\"\n");
	     	  			 //  r.header("Content-Type", "text/plain");
	     	  			   //return  r.build();
			        	  stats.incrementHit();
			        	 return compose_internal_json_page( bu, id, archivelist,sthost);
	     	        	
	     	       //  }

			       /* 	 
		     //if stale issue 503
		     RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
		     HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
	         Date lastupdate = rmtask.checkLastUpdate(id);
	         TimeZone tz = TimeZone.getTimeZone("GMT");
			  Calendar c = new GregorianCalendar(tz);		
			  c.setTime(new Date()); 
		 	  c.add(Calendar.DAY_OF_MONTH,-15);
			  Date cutoff =  c.getTime();
			  if (lastupdate==null) {
			      Map m =(Map) rcache.CheckCaches(client, id, sthost, rmtask, storage);
			      if (m!=null){
			    	  lastupdate = rmtask.checkLastUpdate(id);
			      }
			     }
			  else {
				  if (lastupdate.before(cutoff)){   
					  Map m =(Map) rcache.CheckCaches(client, id, sthost, rmtask, storage);
				      if (m!=null){
				    	  lastupdate = rmtask.checkLastUpdate(id);
				      }  
				  }
			  }
			  if (!nocache){
			  if (lastupdate!=null){
	                if (lastupdate.before(cutoff)){            
	        	         stats.increamentStale(); 
	        	         if (livestatus.equals("true")){
	        	        	// return make_503(id); 
	        	        	 return  compose_json_page( bu, id,  archivelist,sthost);
	        	         }
	        	         else {
	        	        	    if (onlycached){
		         	        		return make_504(id);
		         	        	 }
	        	         return json_dynamic_lookup(id,archivelist,0, bu,page,filtered_archives);
	        	         }
	        	         //return make_503(id); 
	        	         
	                  }
	             }	 
			  }
			  
			 
			  Date gracedate = getDayOffSet(Calendar.MINUTE,10);
			   if (nocache){
				   if (lastupdate!=null) {
			       if  (lastupdate.before(gracedate)){  
				   stats.incrementHit();
				   //ResponseBuilder rb = json_dynamic_lookup(id,archivelist,istart, bu,page,filtered_archives);
				   return json_dynamic_lookup(id,archivelist,0, bu,page,filtered_archives);
			   }
				   }
			   }
			  
			  
		    int maxres = storage.getMatchingCount(id);
		    System.out.println("matching count:"+maxres);
		    if ( maxres<pagesize) {
		    	System.out.println( "pagesize"+pagesize);
			    page = "a";
			    return getMyJsonLinks(idp,page, ui,hh) ;
			 }
		   
		     Date next = null;		    
		     Map tmmap = storage.getTimeMapIndexInfo(id, pagesize,filtered_archives);
	  	    //if (hlinks==null) {	
		    if( tmmap.size()==0) {
		    	  stats.incrementMiss(); 
		    	  if (livestatus.equals("true")){
		    		  return  compose_json_page( bu, id,  archivelist,sthost);
     	        	 //return make_503(id); 
     	         }
     	         else {
     	        	 if (onlycached){
       	        		return make_504(id);
       	        	 }
		    	    return json_dynamic_lookup(id,archivelist,0, bu,page,filtered_archives);
     	         }
		    	//return make_503(id); 
  	    	 
		     }
		    else {
		    //links from storage	
			//List <Link> links = hlinks.getSpecialLinks();
		    stats.incrementHit(); 
			Iterator it = tmmap.keySet().iterator();
			  // StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"\n");
			   StringBuffer sb = new StringBuffer();
			   sb.append("{");
			   sb.append("\"original_uri\":\""+id+"\",\n");
			   if (extimegate!=null) {
				   sb.append("\"timegate_uri\":\""+extimegate + id+"\",\n");
			   } else {
			   sb.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
			   }
			   
			  // sb.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
			  // sb.append(" , <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
			   Date fromself = null;
			   sb.append("\"timemap_index\": [\n");
			   int count = 0;
			   
			    while (it.hasNext()) {
				String  key= (String) it.next();
				TimeMapLinkDesc tml = (TimeMapLinkDesc) tmmap.get(key);
				Date nmd = tml.getFromdate();
				if (key.equals("0")) {
					fromself=nmd;
					key="1";
				}
				 Date lmd = tml.getUntildate();
				 sb.append( "{");
				 sb.append( "\"from\":\""+ MementoUtils.timeTravelJsFormatter.format(nmd)+"\",\n");
				 if (lmd!=null){
				 sb.append( "\"until\":\""+ MementoUtils.timeTravelJsFormatter.format(lmd)+"\",\n");
				 }
				 sb.append("\"uri\":\""+bu +"timemap/json/" +key +"/"+ id+"\"\n");
				 sb.append( "},");
			    // sb.append ("\n, <"+bu +"timemap/link/" +key +"/"+ id+
					//		">;rel=\"timemap\"; type=\"application/link-format\"; from =\""+
						//	MementoUtils.httpformatter.format(nmd)+"\"");
							//if (lmd!=null) { 
							//sb.append(";until=\""+MementoUtils.httpformatter.format(lmd)+"\"");
							//}
							
			 }
			    
			   sb.replace(sb.lastIndexOf(","),sb.lastIndexOf(",")+1,"");
			   sb.append("],");
			   sb.append( "\"timemap_uri\": {");
			   
			   sb.append( "\"json_format\": \""+  bu +"timemap/json/" + id+"\",\n");
			   sb.append( "\"link_format\": \""+  bu +"timemap/link/" + id+"\"\n");
			     
			   sb.append("}");
			  // sb.append ("\n, <"+bu +"timemap/link/" + id+
				//		">;rel=\"self\"; type=\"application/link-format\"; from =\""+
					//	MementoUtils.httpformatter.format(fromself)+"\"\n");
			   sb.append("}");
			  ResponseBuilder r = Response.ok(sb.toString());	
			  r.header("Content-Type", "application/json");
			  r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
			  return  r.build(); 
			
		}
		   */       		
		             
	}
	
	public Date getDayOffSet(int type,int days){
		 TimeZone tz = TimeZone.getTimeZone("GMT");
		  Calendar c = new GregorianCalendar(tz);		
		  c.setTime(new Date()); 
		 
	 	  c.add(type,-days);
		  Date cutoff =  c.getTime();
		 return cutoff;
	}
	
	public  Response getIndexLinks(@PathParam("id") String idp, @PathParam("page") String page, @Context UriInfo ui, @Context HttpHeaders hh) throws ParseException {
		 URI ur = ui.getRequestUri(); 
		 System.out.println("request timemap url:"+ur.toString());
		 URI baseurl = ui.getBaseUri();
		 System.out.println("baseurl"+baseurl.toString());
		// String protocol="http://";
		 //if (baseurl.getScheme().equals("https")) {
			// protocol = "https://";
		 //}
		 String id;
		// if (page.equals("a")) {
			  id = ur.toString().replaceFirst(baseurl.toString()+"timemapml/link/", "");	 
			  page = "1";
		
		 System.out.println("page"+page);
		  //removed  Sept 16, 2016
		   if (id.startsWith("https:")) {
			 id = id.replaceFirst("https:", "http:");
		   }
		   check_cache_header(hh);
		   String sthost="";
		   boolean skip = false;
		   boolean redirect = false;
		   boolean refuse = false;
		   CacheStats stats = (CacheStats) MyInitServlet.getInstance().getAttribute("timemapstats");
		   CommonRuleMatcher urlmatcher = new CommonRuleMatcher();
		   List  archivelist = urlmatcher.getArchives(id,tgrulesdynamic);
		   System.out.println("tm:"+archivelist.toString());
					
			 String redlocation ="";
		        if (archivelist.size()==1)  {
		        	   Iterator it = adesc.iterator();
		        	   while (it.hasNext()) {
		        	   ArchiveDescription ad = (ArchiveDescription) it.next();
		        	              if (archivelist.contains(ad.getName())){
		        	              String mredirect = ad.getTimemapRedirect();
		        	              if (mredirect.equals("yes")) redirect = true;
		        	              if (mredirect.equals("refuse")) refuse = true;        	            
		        	               redlocation = ad.getTimemap();
		        	               break;
		        	              }
		        	  }
		        }
				
		        
		        label: try {
		        	 
		        	  id = mc.validateAndEncodeUrl(id);
		        	      if (id==null) {
		        	         skip = true;
		        	         break label;
		        	       }
		        	  
		                URL turl = new URL(id);
		                  sthost = turl.getHost();
					     if (sthost.indexOf("web.archive.org")>0) {
		                  skip = true; break label;
		                 }
		                if (sthost.indexOf("http://")>0) {
		                 skip = true; break label;
		                }
		                if (id.length()>1024) skip = true; break label;
		         }
		         catch ( Exception ignore) {
		            ignore.printStackTrace();
		            System.out.println("url bad tg:"+id);
		            skip=true;
		         }
		        
		        if (MementoUtils.isDomainBlacklisted(sthost, (HashMap<String, String>) MyInitServlet.getInstance().getAttribute("blacklistDomains"))) {
		            // Blacklisted Domain -- 403
		            System.out.println("Blacklisted Domain: " + id);
		           
		            Response.ResponseBuilder responseBuilder = Response.status(403);
		            responseBuilder.entity(MementoUtils.composeErrorPage403(id));
		            return responseBuilder.build();
		        }
		   
		  		 if (refuse) {
				    	//bad url
				    	   ResponseBuilder r = Response.status(404).entity("service not provided for url: " + id);
				    	   r.header("Content-Type", "text/plain");
				    	   return r.build();                  
				    }
		        if (skip) {
		    	  //bad url
		    	   ResponseBuilder r = Response.status(400).entity("bad url: " + id);
		    	   r.header("Content-Type", "text/plain");
		    	   return r.build();
	               }		      
		          //notify batch about request
		         // notify_batch(id);
		     
		          if (redirect) {
		    	   do_redirect(redlocation,id);
		    
		          }
		    
		     //if stale issue 503
		          
		          if (sthost.startsWith("www.")) {
		              sthost = sthost.replaceFirst("www.", "");	 
		         }     
		          
		        List f_archives = urlmatcher.getArchives(id,tgrulescache);
		        List filtered_archives = new ArrayList();
		         filtered_archives.addAll(f_archives);
		        
	            if (!filtered_archives.contains(sthost)){
		            filtered_archives.add(sthost);
		         }
	            System.out.println("tm cache list:"+filtered_archives.toString());
	         //filtered_archives.add(sthost);
	         
	         String bu=baseUri.toString();
				if (proxybaseuri!=null){
					bu = protocol+proxybaseuri;
				}   
	         
				 // if (livestatus.equals("true")){
					   stats.incrementHit();
     	        	 //return make_503(id);
					  return compose_internal_page( bu, id, archivelist,sthost);
     	        	  // ResponseBuilder r = Response.status(405);
     	  			  // r.header("Link","<"+id+">;rel=\"original\"\n");
     	  			 //  r.header("Content-Type", "text/plain");
     	  			  // return  r.build();
     	        	
     	         //}
				
		    /* RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
		     HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
	         Date lastupdate = rmtask.checkLastUpdate(id);
	         Date cutoff = getDayOffSet(Calendar.DAY_OF_MONTH,15);
	          if (!nocache) {
			  if (lastupdate==null) {
			      Map m =(Map) rcache.CheckCaches(client, id, sthost, rmtask, storage);
			      if (m!=null){
			    	  lastupdate = rmtask.checkLastUpdate(id);
			      }
			     }
			  else {
				  if (lastupdate.before(cutoff)){   
					  Map m =(Map) rcache.CheckCaches(client, id, sthost, rmtask, storage);
				      if (m!=null){
				    	  lastupdate = rmtask.checkLastUpdate(id);
				      }  
				  }
			  }
			  }
			
			  if (lastupdate!=null){
	                if (lastupdate.before(cutoff)){            
	        	         stats.increamentStale();
	        	         //istart not used
	        	         if (livestatus.equals("true")){
	        	        	 return  compose_static_page( bu, id,  archivelist,sthost);
	        	        	 //return make_503(id); 
	        	         }
	        	         else {
	        	        	 if (onlycached){
		         	        		return make_504(id);
		         	        	 }
	        	         ResponseBuilder rb= do_dynamic_lookup(id,archivelist,0, bu,page,filtered_archives);
	        	         return rb.build(); 
	        	         }
	        	         // return make_503(id); 
	        	        }	                
	          }	 
			  

			    
			   Date gracedate = getDayOffSet(Calendar.MINUTE,10);
			   
			   if (nocache){
				  
				   if (lastupdate!=null){	   
			           if  (lastupdate.before(gracedate)){
			    	        stats.incrementHit();
				            System.out.println("dynamic lookup");
				             ResponseBuilder rb = do_dynamic_lookup(id,archivelist,0, bu,page,filtered_archives);
				            return rb.build();
			             }
				   }
				   else {
					   
					   ResponseBuilder rb = do_dynamic_lookup(id,archivelist,0, bu,page,filtered_archives);
				   }
			   }
			  
			  
		        int maxres = storage.getMatchingCount(id);
		        System.out.println("matching count:"+maxres);
		   // if (maxres>0) {  //need more to test remove for now
		    if ( maxres<pagesize ) {
		    	System.out.println( "pagesize"+pagesize);
			    page = "a";
			    return getMyLinks(idp,page, ui,hh) ;
			 }
		    //}
		     Date next = null;		    		    
	         filtered_archives.add(sthost);
	         
		     Map tmmap = storage.getTimeMapIndexInfo(id, pagesize,filtered_archives);
	  	    //if (hlinks==null) {	
		     if( tmmap.size()==0) {
		    	 stats.incrementMiss();
		    	 //what need to be here?
		    	 if (livestatus.equals("true")){
		    		 return  compose_static_page( bu, id, archivelist,sthost);
    	        	 //return make_503(id); 
    	         }
    	         else {
    	        	 if (onlycached){
       	        		return make_504(id);
       	        	 }
    	        	 System.out.println("dynamic lookup");
    	        	  ResponseBuilder rb = do_dynamic_lookup(id,archivelist,0, bu,page,filtered_archives);
    	        	  return rb.build(); 
    	         }
		    	// return make_503(id); 	  	    	 	  	    
		     }
		    else {
		    //links from storage	
			//List <Link> links = hlinks.getSpecialLinks();
		    stats.incrementHit(); 
			Iterator it = tmmap.keySet().iterator();
			   StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"");
			   if (extimegate!=null) {
					 sb.append(",\n <"+extimegate + id+">;rel=\"timegate\"");
				 }else {
				  sb.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
				 }
			  // sb.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
			   Date fromself = null;
			    while (it.hasNext()) {
				String  key= (String) it.next();
				TimeMapLinkDesc tml = (TimeMapLinkDesc) tmmap.get(key);
				Date nmd = tml.getFromdate();
				if (key.equals("0")) {
					fromself=nmd;
					key="1";
				}
				 Date lmd = tml.getUntildate();
			     sb.append (",\n <"+bu +"timemap/link/" +key +"/"+ id+
							">;rel=\"timemap\"; type=\"application/link-format\"; from =\""+
							MementoUtils.httpformatter.format(nmd)+"\"");
							if (lmd!=null) { 
							sb.append(";until=\""+MementoUtils.httpformatter.format(lmd)+"\"");
							}
							
			 }
			    
			  sb.append (",\n <"+bu +"timemap/link/" + id+
						">;rel=\"self\"; type=\"application/link-format\"; from =\""+
						MementoUtils.httpformatter.format(fromself)+"\"\n");
			  
			  ResponseBuilder r = Response.ok(sb.toString());	
			  r.header("Content-Type", "application/link-format");
			  r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
			  return  r.build(); 
			
		}
		 */         		
		             
	}
	
	public  Response do_redirect(String redlocation,String id) {
	
	    	//this is temporary fix  to redirect to wikipedia proxy
			  //String location = "http://mementoweb.org/proxy/wiki/timemap/link/" +id;
	    	  redlocation = redlocation+id;
			  ResponseBuilder r = Response.status(302);
			  r.header("Location",redlocation);
			  r.header("Link","<"+id+">;rel=\"original\"\n");
			  return  r.build();
	   
	}
	
	public String getNativeTimemap(String id){
		HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
		  TimeGateClient tgclient = new TimeGateClient();
		   tgclient.checkFeedUrl(client, id,MementoUtils.httpformatter.format(new Date()));
	        OriginalResource ores = tgclient.getOriginalResource();
	        if (ores.getTimeMapIndexURI()!=null) {
	        	return ores.getTimeMapIndexURI();
	        }
	        else {
	        	if (ores.getTimeMapURI()!=null) {
	        		 return ores.getTimeMapURI();
	        	}
	        }
	        return null;
	}
	
	public void filter_memento (List arcvl) {
		
		Predicate<ArchiveDescription> mementoFiltering = new Predicate<ArchiveDescription>() {
		    @Override
		    public boolean apply(ArchiveDescription input) {
		        return input.getMementostatus().equals("yes");		              
		    }

		};
		Iterable<ArchiveDescription> filtered = Iterables.filter(arcvl, mementoFiltering);
	    }
	
	public  Response compose_internal_page(String bu,String id, List archivelist,String ihost){
		//timemap , checking if memento exist in every archive
		
		 
		 List arcvl= new ArrayList();
		   StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"");
		   sb.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\""); 
		  
	     Iterator<ArchiveDescription> ait = adesc.iterator();
	     while (ait.hasNext()) {
	    	   ArchiveDescription ard = ait.next();
	    	  // String mstatus = ard.getMementostatus();
	    	   String name = ard.getName();
	    	   if (archivelist.contains(name)){	
	    	  // if (mstatus.equals("yes")) {
	    		   System.out.println("name"+name);
	    		   arcvl.add(ard);
	    	   /* }
	    	   else {
	    		   sb.append (",\n <"+ ard.getTimemapGlobal() + id+
	                		 ">;rel=\"timemap\"; "
	                		 + "type=\"application/link-format\"; "
	                		 + "title=\"memento_compliant:no|archive_id:"+name +"\"");	 
	    	   }
	    	   */
	    	   }
	     }
	     
	     		  		   
		     TimeTravelAggQuick tgq = new TimeTravelAggQuick();
		    //need to filter memento complient
		     Date now = new Date();
		     String cdate = MementoUtils.httpformatter.format(now);
		     
		     		     
	         tgq.getTimegateInfo( arcvl, id, cdate , false); //

	         // reduce linkmap to the "summary", Links and add it to archiveLinks
	         Map<ArchiveDescription, List<Link>> archivesLinks =  tgq.getPerArchive();
	         //test the values
	         Iterator<ArchiveDescription> it = archivesLinks.keySet().iterator();

	         while(it.hasNext()) {
		                ArchiveDescription ard =  it.next();
		                String name = ard.getName(); 
		                System.out.println("name"+name);
		                String mstatus = ard.getMementostatus();
		                //if (archivelist.contains(name)){	
		                     if ( mstatus.contains("yes")){			          
		                         sb.append (",\n <"+ ard.getTimemapGlobal() + id+
		                		 ">;rel=\"timemap\"; "
		                		 + "type=\"application/link-format\"; "
		                		 + "title=\"memento_compliant:yes|archive_id:"+name +"\"");	
		                       }
		                      else{
		                         sb.append (",\n <"+ ard.getTimemapGlobal() + id+
			                	 ">;rel=\"timemap\"; "
			                	  + "type=\"application/link-format\"; "
			                	  + "title=\"memento_compliant:no|archive_id:"+name +"\"");	 
		                          }
		                //}
	                }
	              String nattm = getNativeTimemap(id);
	             
	             if (nattm!=null){
	              
	            	 
	            	  sb.append (",\n <"+ nattm + 
		                		 ">;rel=\"timemap\"; type=\"application/link-format\"; title=\"memento_compliant:yes|archive_id:" +ihost+"\"");
	              }
	              sb.append (",\n <"+bu +"timemap/link/" +
	                      id+">;rel=\"self\"; type=\"application/link-format\"\n");
	              ResponseBuilder r = Response.ok(sb.toString());	
				  r.header("Content-Type", "application/link");
				  return  r.build();
	}
	
	
	
	public  Response compose_static_page(String bu,String id, List archivelist,String ihost){
		//timemap based on machine learning
		 HttpClient httpclient = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
		 List excludeArchives = mlclient.checkUrl(httpclient, mlbaseurl+id);
		 
		// List arcvl= new ArrayList();
		  StringBuffer sb = new StringBuffer("<"+id+">;rel=\"original\"");
		   sb.append(",\n <"+bu +"timegate/"+ id+">;rel=\"timegate\""); 
		  
	     Iterator<ArchiveDescription> ait = adesc.iterator();
	     while (ait.hasNext()) {
	    	   ArchiveDescription ard = ait.next();
	    	   String mstatus = ard.getMementostatus();
	    	   String name = ard.getName();
	    	   if (archivelist.contains(name)){	
	    		   if (!excludeArchives.contains(name)) {
	    	         if (mstatus.equals("yes")) {
	    		//arcvl.add(ard);
	    		        sb.append (",\n <"+ ard.getTimemapGlobal() + id+
                		 ">;rel=\"timemap\"; "
                		 + "type=\"application/link-format\"; "
                		 + "title=\"memento_compliant:yes|archive_id:"+name +"\"");	
	    		
	    	         }
	    	        else {
	    		        sb.append (",\n <"+ ard.getTimemapGlobal() + id+
	                		 ">;rel=\"timemap\"; "
	                		 + "type=\"application/link-format\"; "
	                		 + "title=\"memento_compliant:no|archive_id:"+name +"\"");	 
	    	   }
	    		   }
	    	   }
	     }
	     
	     		  		   
		    /* TimeTravelAggQuick tgq = new TimeTravelAggQuick();
		    //need to filter memento complient
		     Date now = new Date();
		     String cdate = MementoUtils.httpformatter.format(now);
		     
		     		     
	         tgq.getTimegateInfo( arcvl, id, cdate , false); //

	         // reduce linkmap to the "summary", Links and add it to archiveLinks
	         Map<ArchiveDescription, List<Link>> archivesLinks =  tgq.getPerArchive();
	         //test the values
	         Iterator<ArchiveDescription> it = archivesLinks.keySet().iterator();

	         while(it.hasNext()) {
		                ArchiveDescription ard =  it.next();
		                String name = ard.getName(); 
		                
		               // if ( archivelist.contains(name)){			          
		                 sb.append (",\n <"+ ard.getTimemapGlobal() + id+
		                		 ">;rel=\"timemap\"; "
		                		 + "type=\"application/link-format\"; "
		                		 + "title=\"memento_compliant:yes|archive_id:"+name +"\"");	
		                //}
	                }
	         
	         */
	              String nattm = getNativeTimemap(id);
	             
	             if (nattm!=null){
	              
	            	 
	            	  sb.append (",\n <"+ nattm + 
		                		 ">;rel=\"timemap\"; type=\"application/link-format\"; title=\"memento_compliant:yes|archive_id:" +ihost+"\"");
	              }
	              sb.append (",\n <"+bu +"timemap/link/" +
	                      id+">;rel=\"self\"; type=\"application/link-format\"\n");
	              ResponseBuilder r = Response.ok(sb.toString());	
				  r.header("Content-Type", "application/link-format");
				  return  r.build();
	}
	
	public Response compose_internal_json_page(String bu,String id, List archivelist,String ihost){
		
		 List arcvl= new ArrayList();
		 StringBuffer sb = new StringBuffer();
		
		   sb.append("{");
		   sb.append("\"original_uri\":\""+id+"\",\n");
		   sb.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
		  // sb.append(" , <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
		   sb.append("\"timemap_index\": [\n");
		 
		 
		   Iterator<ArchiveDescription> ait = adesc.iterator();
		     while (ait.hasNext()) {
		    	   ArchiveDescription ard = ait.next();
		    	   String mstatus = ard.getMementostatus();
		    	   String name = ard.getName();
		    	   if (archivelist.contains(name)){	
		    	   //if (mstatus.equals("yes")) {
		    		arcvl.add(ard);
		    	   //}
		    	   //else {
		    		 //  sb.append( "{");				 
						// sb.append("\"uri\":\""+ ard.getTimemapGlobal() + id+"\",\n");
						// sb.append("\"memento_compliant\":\"no\",\n");
						// sb.append("\"archive_id\":\""+name+"\"\n");
						// sb.append( "},");
		    		 
		    	   //}
		    	   }
		     }
		 
		 
		     TimeTravelAggQuick tgq = new TimeTravelAggQuick();
			    //need to filter memento complient
			     Date now = new Date();
			     String cdate = MementoUtils.httpformatter.format(now);			     			     		     
		         tgq.getTimegateInfo( arcvl, id, cdate , false); //

		         // reduce linkmap to the "summary", Links and add it to archiveLinks
		         Map<ArchiveDescription, List<Link>> archivesLinks =  tgq.getPerArchive();
		         //test the values
		         Iterator<ArchiveDescription> it = archivesLinks.keySet().iterator();

		 
		 //Iterator<ArchiveDescription> ait = adesc.iterator();
		 
		   
		   while(it.hasNext()) {
              ArchiveDescription ard =  it.next();
              String name = ard.getName(); 
              String mstatus = ard.getMementostatus();
              if (archivelist.contains(name)){	
              if ( mstatus.equals("yes")) {		                       
                sb.append( "{");				 
				 sb.append("\"uri\":\""+ ard.getTimemap() + id+"\",\n");
				 sb.append("\"memento_compliant\":\"yes\",\n");
				 sb.append("\"archive_id\":\""+name+"\"\n");
				
				 sb.append( "},");
              
              }
              else{
            	  sb.append( "{");				 
					 sb.append("\"uri\":\""+ ard.getTimemapGlobal() + id+"\",\n");
					 sb.append("\"memento_compliant\":\"no\",\n");
					 sb.append("\"archive_id\":\""+name+"\"\n");
					 sb.append( "},");
              }
              }
          }
		   String nattm = getNativeTimemap(id);
		   if (nattm!=null){
			
			   
			   
			   sb.append( "{");				 
				 sb.append("\"uri\":\""+ nattm +"\",\n");
				 sb.append("\"memento_compliant\":\"yes\",\n");
				 sb.append("\"archive_id\":\""+ihost+"\"\n");
				
				 sb.append( "},");
		   }
		   sb.replace(sb.lastIndexOf(","),sb.lastIndexOf(",")+1,"");
		   sb.append("],");
		   sb.append( "\"timemap_uri\": {");
		   
		   sb.append( "\"json_format\": \""+  bu +"timemap/json/" + id+"\",\n");
		   sb.append( "\"link_format\": \""+  bu +"timemap/link/" + id+"\"\n");
		     
		   sb.append("}");
		   sb.append("}");
		  ResponseBuilder r = Response.ok(sb.toString());	
		  r.header("Content-Type", "application/json");
		  return  r.build(); 
		   
	}
	
	public Response compose_json_page(String bu,String id, List archivelist,String ihost){
		
		// List arcvl= new ArrayList();
		 StringBuffer sb = new StringBuffer();
		 HttpClient httpclient = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
		 List excludeArchives = mlclient.checkUrl(httpclient, mlbaseurl+id);

		   sb.append("{");
		   sb.append("\"original_uri\":\""+id+"\",\n");
		   sb.append("\"timegate_uri\":\""+bu +"timegate/"+ id+"\",\n");
		  // sb.append(" , <"+bu +"timegate/"+ id+">;rel=\"timegate\"");
		   sb.append("\"timemap_index\": [\n");
		 
		 
		   Iterator<ArchiveDescription> ait = adesc.iterator();
		     while (ait.hasNext()) {
		    	   ArchiveDescription ard = ait.next();
		    	   String mstatus = ard.getMementostatus();
		    	   String name = ard.getName();
		    	   if (archivelist.contains(name)){	
		    	   if (!excludeArchives.contains(name)) {
		    	   if (mstatus.equals("yes")) {
		    		//arcvl.add(ard);
		    		   sb.append( "{");				 
					   sb.append("\"uri\":\""+ ard.getTimemap() + id+"\",\n");
					   sb.append("\"memento_compliant\":\"yes\",\n");
					   sb.append("\"archive_id\":\""+name+"\"\n");
						
					   sb.append( "},");
		    	   }
		    	   else {
		    		   sb.append( "{");				 
						 sb.append("\"uri\":\""+ ard.getTimemapGlobal() + id+"\",\n");
						 sb.append("\"memento_compliant\":\"no\",\n");
						 sb.append("\"archive_id\":\""+name+"\"\n");
						 sb.append( "},");
		    		 
		    	   }
		    	   }
		    	   }
		     }
		 
		 
		  /*   TimeTravelAggQuick tgq = new TimeTravelAggQuick();
			    //need to filter memento complient
			     Date now = new Date();
			     String cdate = MementoUtils.httpformatter.format(now);
			     
			     		     
		         tgq.getTimegateInfo( arcvl, id, cdate , false); //

		         // reduce linkmap to the "summary", Links and add it to archiveLinks
		         Map<ArchiveDescription, List<Link>> archivesLinks =  tgq.getPerArchive();
		         //test the values
		         Iterator<ArchiveDescription> it = archivesLinks.keySet().iterator();

		 
		 //Iterator<ArchiveDescription> ait = adesc.iterator();
		 
		   
		   while(it.hasNext()) {
               ArchiveDescription ard =  it.next();
               String name = ard.getName(); 
              // if ( archivelist.contains(name)){			                       
                 sb.append( "{");				 
				 sb.append("\"uri\":\""+ ard.getTimemap() + id+"\",\n");
				 sb.append("\"memento_compliant\":\"yes\",\n");
				 sb.append("\"archive_id\":\""+name+"\"\n");
				
				 sb.append( "},");
               
               //}
           }
           */
		   String nattm = getNativeTimemap(id);
		   if (nattm!=null){
			
			   
			   
			   sb.append( "{");				 
				 sb.append("\"uri\":\""+ nattm +"\",\n");
				 sb.append("\"memento_compliant\":\"yes\",\n");
				 sb.append("\"archive_id\":\""+ihost+"\"\n");
				
				 sb.append( "},");
		   }
		   sb.replace(sb.lastIndexOf(","),sb.lastIndexOf(",")+1,"");
		   sb.append("],");
		   sb.append( "\"timemap_uri\": {");
		   
		   sb.append( "\"json_format\": \""+  bu +"timemap/json/" + id+"\",\n");
		   sb.append( "\"link_format\": \""+  bu +"timemap/link/" + id+"\"\n");
		     
		   sb.append("}");
		   sb.append("}");
		  ResponseBuilder r = Response.ok(sb.toString());	
		  r.header("Content-Type", "application/json");
		  return  r.build(); 
		   
	}
	public static boolean isNumeric(String str)
	{
	 // return str.matches("-?\\d+(\\.\\d+)?");
		//return str.matches("-?\\d");
		return str.matches("\\d*");
	}
	public static boolean isNumb(String str)
	{
	String s=str;
	for (int i = 0; i < s.length(); i++) {
	//If we find a non-digit character we return false.
	if (!Character.isDigit(s.charAt(i)))
	return false;
	}
	return true;
	}
	
}
