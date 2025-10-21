package gov.lanl.agg.resource;


import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.CacheStats;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.RulesDescription;
import gov.lanl.agg.batch.RunMeBatchTask;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.helpers.ArchiveListConfig;
import gov.lanl.agg.helpers.TimeGateAggQuick;
import gov.lanl.agg.utils.AbbrSQLUtils;
import gov.lanl.agg.utils.CommonRuleMatcher;
import gov.lanl.agg.utils.MLClient;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.RemoteCacheClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
//import java.text.SimpleDateFormat;
import java.util.*;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.sun.jersey.api.client.ClientResponse.Status;

//import gov.lanl.aggregatorml.main.ClassifierPool;
/*
@author Lyudmila Balakireva

*/
@Path("/timegate/{id:.*}")
public class TimeGateResource {
  
    static FastDateFormat  httpformatter;
    static MementoUtils mc;
    protected final URI baseUri;
  
    static ArchiveListConfig config;
    static String proxybaseuri;
    static String filename;
    static List tglist;
    static List <ArchiveDescription> adesc;
  
    static  AbbrSQLUtils  autils;

    static RulesDescription tgrulesdynamic;
    static RulesDescription tgrulescache;
    static RemoteCacheClient rcache;
    static MLClient mlclient;
    String protocol = null;
    static String mlbaseurl ="";
    boolean nocache = false;
    static Logger logml ;
    static Logger statsloger;
    String srvdate;
    String tgdate="";

    //removed dependancy of ml service
    boolean noml = true;

    boolean after = false;

    //statsloger:url,req_date,service,hit,miss,stale,bypass,accept_datetime
   //logml:url,req_date,listofpredictedarchives,listofaddededarchivesviarules,accept_datetime
    static {
        MyInitServlet cl = MyInitServlet.getInstance();
       // initializeLogger();
        logml = Logger.getLogger("mlservice");
        statsloger = Logger.getLogger("stats");
      
        tglist =  (List) cl.getAttribute("timegatelist");
        adesc= (List<ArchiveDescription>) cl.getAttribute("archivedesc");
        autils = (AbbrSQLUtils) cl.getAttribute("autils");
        Map  params = (Map) cl.getAttribute("params");
     
        rcache = new RemoteCacheClient((String) params.get("config.cache.registry"),(String) params.get("config.cache.self"));	    
        if (params.containsKey("config.service.rules")) {
	        String srules = (String) params.get("config.service.rules");
	        tgrulesdynamic =  CommonRuleMatcher.load_rules(srules,"timegate_dynamic");
            tgrulescache =  CommonRuleMatcher.load_rules(srules,"timegate_cache");
        }
        if (params.containsKey("baseuri.proxy")) {
            proxybaseuri= (String) params.get("baseuri.proxy");
        }
        else {
            proxybaseuri =null;
        }
         
          

          mlclient = new MLClient();
          mlbaseurl= (String) MyInitServlet.getInstance().getAttribute("mlbaseurl");
    }



    public	TimeGateResource( @Context UriInfo uriInfo )
    {
        this.baseUri = uriInfo.getBaseUri();
        mc = new MementoUtils(baseUri);
        httpformatter = mc.httpformatter;
       

    }


    public void check_prefer_header(HttpHeaders hh){
		 List <String> cachecontrollst = hh.getRequestHeader("Prefer");
		 if (cachecontrollst!=null){
		 String preferstr = cachecontrollst.get(0).toLowerCase();
		 // if (preferstr.contains("no-ml")) {
		//	  noml = true;
		  //}
		  if (preferstr.contains("after")) {
			  after = true;
		  }
		 
		  }		 
		 
		
	}
    
    
    
    @POST

    public Response replytoPOST() {
        ResponseBuilder r = Response.status(405);
        r.header("Allow", "GET,HEAD");
        r.header("Vary","Accept-Datetime");
        return r.build();
    }

    @PUT
    public Response replytoPUT() {
        ResponseBuilder r = Response.status(405);
        r.header("Allow", "GET,HEAD");
        r.header("Vary","Accept-Datetime");
        return r.build();
    }

    @DELETE
    public Response replytoDELETE() {
        ResponseBuilder r = Response.status(405);
        r.header("Allow", "GET,HEAD");
        r.header("Vary","Accept-Datetime");
        return r.build();
    }


    @HEAD
    public Response  getHTimegate( @Context HttpHeaders hh, @Context UriInfo ui, @PathParam("id") String id ) throws ParseException, URISyntaxException {
        return getTimegate( hh, ui, id );
    }

    @GET
    // I may need to copy all  logic to @HEAD
    public Response  getTimegate( @Context HttpHeaders hh, @Context UriInfo ui, @PathParam("id") String idp ) throws ParseException, URISyntaxException {
        URI baseurl = ui.getBaseUri();
        URI ur = ui.getRequestUri();
        //System.out.println("request url:"+ur.toString());
        System.out.println("timegate headers:"+hh.toString());
        List <String> hscheme = hh.getRequestHeader("X-Forwarded-Proto");
        if (hscheme==null) {
        	protocol ="http://";
        }
        else {
        	protocol ="https://";
        }
        String id = ur.toString().replaceFirst(baseUri.toString()+"timegate/", "");
        System.out.println("get into get:"+id);
        //upper case?
        //removed Sept 16 //switched back nov 2015 
        if (id.startsWith("https:")) {
            id = id.replaceFirst("https:", "http:");
        }
        List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
        String rdate;

        if (hdatetime==null) {
            //no date time set to now
            //hdatetime = new ArrayList();
            //hdatetime.add(0,   httpformatter.format(new Date()));
            rdate = httpformatter.format(new Date());
            Date d = MementoUtils.httpformatter.parse(rdate);
            tgdate = MementoUtils.timeTravelMachineFormatter.format(d);
        }
        else {
            rdate = hdatetime.get(0);
            //reformat it 
            try {
            Date d = MementoUtils.httpformatter.parse(rdate);
            tgdate = MementoUtils.timeTravelMachineFormatter.format(d);
            }
            catch (Exception e) {
            	//skip it;
            	System.out.println("bad date");
            }
            
        }
       
	    boolean onlycached = false;
	    
        List <String> cachecontrollst = hh.getRequestHeader("Cache-Control");
		 if (cachecontrollst!=null){
		 String cacheconstrolstr = cachecontrollst.get(0).toLowerCase();
		 if (cacheconstrolstr.contains("no-cache")){
			 System.out.println("no-cache");
			 nocache = true;
		 }
		 if (cacheconstrolstr.contains("only-if-cached")){
			 System.out.println("only-if-cached");
			 onlycached = true;
		 }
		}
		 check_prefer_header(hh);
		 
        return  make_timegate_res(id,rdate,nocache,onlycached);

    }



    public Response make_timegate_res(String id, String rdate,  boolean nocache,  boolean onlycached) {

        String bu=baseUri.toString();
        //String protocol = "http://";
        //if (baseUri.getScheme().equals("https")) {
			// protocol = "https://";
		 //}
        if (proxybaseuri!=null){
            bu = protocol+proxybaseuri;
        }
        StringBuffer sb = new StringBuffer("<").append(id).append(">;rel=\"original\"\n");
        String origlink = sb.toString();
        // String origlink ="<"+id+">;rel=\"original\"";
        String timemap = (",<"+bu +"timemap/link/"+ id+">;rel=\"timemap\"; type=\"application/link-format\"");
        //sb.append(",<").append(bu).append("timemap/link/1/").append(id).append(">;rel=\"timemap\"; type=\"application/link-format\"");
        String timegate =(",<"+bu +"timegate/"+ id+">;rel=\"timegate\"");

        boolean skip = false;
   	  
        String suffix="";
        String sthost="";
      
        boolean redirect = false;
        boolean refuse = false;
        CommonRuleMatcher urlmatcher = new CommonRuleMatcher();
        CacheStats stats = (CacheStats) MyInitServlet.getInstance().getAttribute("timegatestats");
       
        List  archivelist = urlmatcher.getArchives(id,tgrulesdynamic);
        //System.out.println("tg:"+archivelist.toString());
        String redlocation ="";
        if (archivelist.size()==1)  {
        	   Iterator it = adesc.iterator();
        	   while (it.hasNext()) {
        	   ArchiveDescription ad = (ArchiveDescription) it.next();
        	             if (archivelist.contains(ad.getName())){
        	               String mredirect = ad.getTimegateRedirect();
        	               if (mredirect.equals("yes")) redirect = true;
        	               if (mredirect.equals("refuse")) refuse = true;
        	               redlocation = ad.getTimegateGlobal();
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

        HashMap<String, String> bl = (HashMap<String, String>) MyInitServlet.getInstance().getAttribute("blacklistDomains");
        if (MementoUtils.isDomainBlacklisted(sthost, bl)) {
            // Blacklisted Domain -- 403
            System.out.println("Blacklisted Domain: " + id);
           
            Response.ResponseBuilder responseBuilder = Response.status(403);
            responseBuilder.entity(MementoUtils.composeErrorPage403(id));
            return responseBuilder.build();
        }
         

        if (skip) {
            //bad url
            ResponseBuilder r = Response.status(400).entity(composeErrorPage(id));
            r.header("Vary","Accept-Datetime");
            r.header("Link", origlink );
            return r.build();
        }
        if (refuse) {
            //bad url
            ResponseBuilder r = Response.status(404).entity(composeErrorPage(id));
            r.header("Vary","Accept-Datetime");
            r.header("Link", origlink );
            return r.build();
        }

        // String timemap = " , <"+baseurl.toString() +"timemap/link/" + id+">;rel=\"timemap\"; type=\"application/link-format\"";
      
        //if (datetime!=null) {
        System.out.println(rdate);
        // Date dtdate = mc.checkDtDateValidity(hdatetime.get(0)) ;
        Date dtdate = mc.checkDtDateValidity(rdate) ;
        if (dtdate==null) {
            ResponseBuilder r = Response.status(400).entity(composeErrorPage400(id));
            r.header("Vary","Accept-Datetime");
            r.header("Link", origlink );
            return r.build();
        }
        
       //notify batch about request
    /*    try {
            Date now = new Date();
            IBigQueue ressyncque = (IBigQueue) MyInitServlet.getInstance().getAttribute("ResSyncQue");
            if  (ressyncque!=null) {
                String pload = now.getTime()+"|"+ id;
               // System.out.println(pload);
                ressyncque.enqueue(pload.getBytes("UTF-8"));
                System.out.println("added to Q:"+pload);
            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
*/
        if (redirect) {
            //this is temporary fix  to redirect to wikipedia proxy
            //call to db to initiate putting of wikipedia url to cache
            ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage")).checkCacheRelax(id);
           // String location = "http://mementoweb.org/proxy/wiki/timegate/" +id;
            redlocation = redlocation+id;
            ResponseBuilder r = Response.status(Status.FOUND);
            r.header("Location",redlocation);
            r.header("Link","<"+id+">;rel=\"original\"\n");
            return  r.build();
        }

       
         long reqtime = dtdate.getTime();
         Date service_date = new Date();
         srvdate = MementoUtils.timeTravelMachineFormatter.format(service_date);
        //    System.out.println("long reqtime" + reqtime);
         List f_archives = urlmatcher.getArchives(id,tgrulescache);
         List filtered_archives = new ArrayList();
         filtered_archives.addAll(f_archives);
        
         if (sthost.startsWith("www.")) {
         sthost=sthost.replaceFirst("www.", "");	 
         }
         filtered_archives.add(sthost);
         
         RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
         Date lastupdate = rmtask.checkLastUpdate(id);
        // ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage")).checkCacheRelax(id);
          CacheStorage storage= ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage"));
          LinkHeader lh = storage.getTimegateInfo(id, dtdate, filtered_archives,nocache,service_date);
          HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
          
          
          TimeZone tz = TimeZone.getTimeZone("GMT");
		  Calendar c = new GregorianCalendar(tz);		
		  c.setTime(new Date()); 
	 	  c.add(Calendar.DAY_OF_MONTH,-30);
		  Date cutoff =  c.getTime();
		  
		  if (!nocache) {  //unlikly other cache has 30 minute updated info
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
		  
		  
		  boolean statsincreamented = false;
		  boolean dynamic = false;
		  if (lastupdate!=null){
                if (lastupdate.before(cutoff)){            
        	         stats.increamentStale();
        	     	 statsloger.info(id+" "+srvdate+" tg 0 0 1 0"+" "+tgdate);
        	         statsincreamented = true;
        	         //added Aug 19
        	         dynamic=true;
                  }
                // removing memento date checking Aug 19
                // if (dtdate.after(lastupdate)){
			     //dynamic = true;	  
			     //}
          
          }	 
		  //more strict grace period for no-cache
		   Calendar c1 = new GregorianCalendar(tz);		
		   c1.setTime(new Date()); 
	 	   c1.add(Calendar.MINUTE,-10);
		   Date gracedate =  c1.getTime();
		   
		   if (nocache){
			   if (lastupdate!=null){
			   if  (lastupdate.before(gracedate)){ 
				   statsincreamented = true;
				   statsloger.info(id+" "+srvdate+" tg 0 0 0 1"+" "+tgdate);
		        dynamic = true;
		       }
			   }
			   else {
				   statsloger.info(id+" "+srvdate+" tg 0 0 0 1"+" "+tgdate);
				   statsincreamented = true;
				dynamic = true;   
			   }
		   }
		  
          if  (!dynamic) {
        	 // fresh cache:
         // LinkHeader lh = ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage"))
        	//	  .getTimegateInfo(id, dtdate, filtered_archives);
      
          if (lh!=null) {
        	  // stats.incrementHit(); 
             //from db
               NavigableMap<Long, Link> m = lh.getOrderedLinksByDate();
               
                 if (m.size()==0) {
                 //no links fond in archives
                	 //Aug 19,2015 refresh  
                	 if (!onlycached) {                		 
                	     if (lastupdate.before(gracedate)) {
                		     stats.incrementMiss(); 
                		     statsloger.info(id+" "+srvdate+" tg 0 1 0 0"+" "+tgdate);
                		     do_dynamic(archivelist, reqtime, rdate, id, origlink,timemap);
                	       }
                	 }
                	 else{
                	 stats.incrementHit(); 
                	 statsloger.info(id+" "+srvdate+" tg 1 0 0 0"+" "+tgdate);
                	 }
                 ResponseBuilder r = Response.status(404).entity(composeErrorPage(id));
                 r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
                 r.header("Link",origlink);
                 return  r.build();
                }
                 statsloger.info(id+" "+srvdate+" tg 1 0 0 0"+" "+tgdate);
                 stats.incrementHit(); 
                 System.out.println ("size of map:"+m.size());
                 //StringBuffer sb = new StringBuffer("<").append(id).append(">;rel=\"original\"\n");
              long key;
              if (reqtime < m.firstKey() || reqtime == m.firstKey()  || m.size()==1) {
                System.out.println("firstkey as location:"+reqtime + m.firstKey());
                key = m.firstKey();
              }
              else {
                key = m.floorKey(reqtime);
                Long hkey = m.higherKey(reqtime);
                if (hkey!=null) {
                    Long rdelta = Math.abs(reqtime-key);
                    Long ldelta = Math.abs(hkey-reqtime);
                       if (rdelta<ldelta) {
                        key=key;
                       }
                       else {
                        key=hkey;
                       }
                }
              }
             String location = m.get(key).getHref() ;
             Long next = m.higherKey(key);
             String nextstr="";
             if (next!=null) {
           	 nextstr=",<"+m.get(next).getHref()+">;rel=\""+"memento next"+"\"; datetime=\""+httpformatter.format( new Date(next))+"\"";
 
             }
             Long prev = m.lowerKey(key);              
             String prevstr="";
             if (prev!=null) {
            	 prevstr=",<"+m.get(prev).getHref()+">;rel=\""+"memento prev"+"\"; datetime=\""+httpformatter.format( new Date(prev))+"\"";
  
              }
             String links = composeLinkHeader(key, m.lastKey(),m.firstKey(),m.get(m.lastKey()).getHref(),m.get(m.firstKey()).getHref(),location);
             ResponseBuilder r = Response.status(Status.FOUND);

            //ResponseBuilder r = Response.ok("here:"+location);
             r.header("Location",location);
             r.header("Vary","Accept-Datetime");
             r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
             r.header("Link",origlink+ timemap + links  +nextstr +prevstr);
             return  r.build();
            }
           }
           if (!statsincreamented) {
        	   statsloger.info(id+" "+srvdate+" tg 0 1 0 0"+" "+tgdate);
            stats.incrementMiss(); 
           }
            if (onlycached){
        		return make_504(id);
        	 }
            
            
            return do_dynamic(archivelist, reqtime, rdate, id, origlink,timemap);
            
          
    }

   
    public  Response do_dynamic(List archivelist, long reqtime,String rdate,String id,String origlink,String timemap){
    	
    	 TimeGateAggQuick tgq = new TimeGateAggQuick();
    	 HttpClient httpclient = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
         /* machine learning 
    	 List <String>b = ListUtils.intersection(mlArchives, archivelist);
    	 String[] archivesToConsider = b.toArray(new String[b.size()]);
    	 System.out.println("intersect in timegate:" +  Arrays.asList(archivesToConsider).toString());
    	 String[] potentialHolders = null;
          try {
        	 ClassifierPool pool =  (ClassifierPool) MyInitServlet.getInstance().getAttribute("classifierpool");
             potentialHolders =  pool.predictNecessaryRequests(id, archivesToConsider );
         } catch(Exception e){
             Logger.getLogger(this.getClass()).error(e);
             
         }
         
         System.out.println("potential holders in timegate:" +  Arrays.asList(potentialHolders).toString());
          List excludeArchives = ListUtils.subtract(mlArchives, Arrays.asList(potentialHolders));
          System.out.println("exclude list in timegate:" + excludeArchives.toString());
          */
       //	boolean wiki = false;
    	   //System.out.println( "ml"+mlbaseurl+id);
    	   List recommended  = new ArrayList();
    	   List excludeArchives=new ArrayList();
    	   //removed dependancy on ml for release
    	  // List excludeArchives = mlclient.checkUrl(httpclient, mlbaseurl+id,recommended);
    	   String recString = String.join(":", recommended);
    	   //System.out.println("recomended in timegate:"+recString);
    	   boolean donotlog = false;
    	   if (nocache){
    		  excludeArchives = new ArrayList();
    		  donotlog = true;
    	   }
    	   if (noml){
    		   excludeArchives = new ArrayList();
    		   donotlog = true;
    	   }
           Iterator ait = adesc.iterator();
           List tglistp = new ArrayList();
           List all_a = new ArrayList();
           
		   	 while(ait.hasNext()) {
            ArchiveDescription ard = (ArchiveDescription) ait.next();
            String name = ard.getName();
              if ( archivelist.contains(name)){          	  
            	  if (!excludeArchives.contains(name)) {
            		 // System.out.println("actual list:"+  name);
            		   all_a.add(name);
            		  tglistp.add(ard);
            	 }
              }
            }
//java 8 warning
		   	// would print actual list 
		   //	all_a.removeAll(recommended);
		  	String listString = String.join(":", all_a);
		    System.out.println("actuallist:"+listString);
		  	//req timeis still missing
		    if (!donotlog) {
		  	logml.info(id+" "+srvdate+" "+ recString +" "+ listString +" "+tgdate);	
		    }
        NavigableMap<Long, String> linkmap = tgq.getTimegateInfo( tglistp, id, rdate);

        if (linkmap.size()>0 ) {        	
       	 ResponseBuilder r = process_links( linkmap,reqtime,origlink,timemap);
       	 r.header("Last-Modified", mc.httpformatter.format(new Date()));
           // r.header("Link",origlink+ timemap + links  );
            return  r.build();
         }
        else {
           ResponseBuilder r = Response.status(404).entity(composeErrorPage(id));
           r.header("Link",origlink);
           return r.build();
        }
    	
    }

     public  ResponseBuilder process_links(NavigableMap<Long, String> linkmap,long reqtime,String origlink,String timemap) {
    	
              Long key;
              if (linkmap.size()==1 ||  reqtime == linkmap.firstKey() || reqtime < linkmap.firstKey()){
                  key = linkmap.firstKey();
                  //System.out.println("firstkey as location:"+reqtime +"," + linkmap.firstKey());

              }
              else {
                  //System.out.println("floorkey as location:"+reqtime +"," + linkmap.floorKey(reqtime));
                  key = linkmap.floorKey(reqtime);

                  Long hkey = linkmap.higherKey(reqtime);
                  if (hkey!=null) {
                      Long rdelta = Math.abs(reqtime-key);
                      Long ldelta = Math.abs(hkey-reqtime);
                      if (rdelta<ldelta) {
                          key=key;
                      }
                      else {
                          key=hkey;
                      }
                  }      
              }
              String location = linkmap.get(key) ;
              String links = composeLinkHeader(key, linkmap.lastKey(),linkmap.firstKey(),linkmap.get(linkmap.lastKey()), linkmap.get(linkmap.firstKey()), location);
              
              Long next = linkmap.higherKey(key);
              String nextstr="";
              if (next!=null) {
            	 nextstr=",<"+linkmap.get(next)+">;rel=\""+"memento next"+"\"; datetime=\""+httpformatter.format( new Date(next))+"\"";
  
              }
              Long prev = linkmap.lowerKey(key);              
              String prevstr="";
              if (prev!=null) {
             	 prevstr=",<"+linkmap.get(prev)+">;rel=\""+"memento prev"+"\"; datetime=\""+httpformatter.format( new Date(prev))+"\"";
   
               }
              
              ResponseBuilder r = Response.status(Status.FOUND);
              r.header("Location",location);
              //r.header("Vary","negotiate,accept-datetime");
              r.header("Vary","Accept-Datetime");
              r.header("Link",origlink+ timemap+ links +nextstr +prevstr );
              return  r;
     }
    
     public void filter_date (List arcvl,final Date sdate) {
	
 		Predicate<ArchiveDescription> dateFiltering = new Predicate<ArchiveDescription>() {
 		    @Override
 		    public boolean apply(ArchiveDescription input) {
 		        return input.getOriginated().before(sdate);		              
 		    }

 		};
 		Iterable<ArchiveDescription> filtered = Iterables.filter(arcvl, dateFiltering);
 	    }
     
     //call to cassandra database
    public List Summary_check(String sumurl,Date dnow) {
        // add internet archive by default;
        List tglistcas = new ArrayList();
        tglistcas.add("ia");
        long time_3 = System.currentTimeMillis();
        //this is for testing only
        TimeZone tz = TimeZone.getTimeZone("GMT");
        Calendar c = new GregorianCalendar(tz);
        //Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        //c.add(Calendar.HOUR_OF_DAY , -2);
        c.add(Calendar.YEAR,-2);
        Date now =  c.getTime();
        try {
            Map <String,Integer> amap = autils.getArchivesInfo(sumurl, now);
            if (amap!=null) {
                //cassandra  filtering
                Iterator aait = adesc.iterator();
                System.out.println("amap size:" +amap.size());
                while(aait.hasNext()) {
                    ArchiveDescription aad = (ArchiveDescription) aait.next();
                    String name = aad.getName();
                    if (amap.containsKey(name)) {
                        tglistcas.add(aad);
                    }
                }

            }
        }
        catch ( Exception ignore) {
            ignore.printStackTrace();

        }

        long time_4 = System.currentTimeMillis();
        System.out.println("CassandraResponse Took : " + ((time_4 - time_3) / 1000));
        return tglistcas;
    }

    public  Response make_504(String id) { 
	     ResponseBuilder r = Response.status(504).entity("Not cached: " + id);
	     r.header("Link","<"+id+">;rel=\"original\"\n");
	     r.header("Content-Type", "text/plain");		    
	     return  r.build();  
	    }
   

    public String composeErrorPage(String url) {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>").append("<head><title>The requested URI was not found in any archive</title><head>\n");
        sb.append("<body>");
        sb.append("<div align=\"center\" style=\"margin-top:20ex;margin-left:20ex;margin-right:20ex;border:1px double gray;font-weight:bold;font-family:monospace;font-size:120%;padding:2em;background-color:#eeeeee\">\n");
        sb.append("<table bgcolor=\"#eeeeee\" border=\"0\" width=\"100%\">\n");
        sb.append("<tr style=\"font-weight:bold;font-family:monospace;font-size:120%\">\n");
        sb.append("<td width=\"10%\" align=\"left\">\n");
        sb.append("<a href=\"http://www.mementoweb.org\"><img src=\"http://www.mementoweb.org/mementologo.png\" alt=\"memento logo\" width=\"100\" height=\"100\" style=\"border-style: none\"/></a>\n");
        sb.append("</td><td width=\"90%\" align=\"center\">");
        sb.append("Error, the requested URI:<br><br><a href=\"").append(url).append("\">").append(url).append("</a><br><br>is not available in an archive.\n");
        sb.append("</td></tr></table>\n");
        sb.append("</div></body></html>");
        return sb.toString();
    }
   
    
    public String composeErrorPage400(String url) {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>").append("<head><title>The requested date is  not found in any calendar</title><head>\n");
        sb.append("<body>");
        sb.append("<div align=\"center\" style=\"margin-top:20ex;margin-left:20ex;margin-right:20ex;border:1px double gray;font-weight:bold;font-family:monospace;font-size:120%;padding:2em;background-color:#eeeeee\">\n");
        sb.append("<table bgcolor=\"#eeeeee\" border=\"0\" width=\"100%\">\n");
        sb.append("<tr style=\"font-weight:bold;font-family:monospace;font-size:120%\">\n");
        sb.append("<td width=\"10%\" align=\"left\">\n");
        //sb.append("<body><!--<br><br>--><div align=\"center\" style=\"background-color:#eeeeee;margin-top:20ex;margin-left:20ex;margin-right:20ex;padding:2em;border:1px double gray;font-weight:bold;font-family:monospace;font-size:120%\">\n");
        sb.append("<a href=\"http://www.mementoweb.org\"><img src=\"http://www.mementoweb.org/mementologo.png\" alt=\"memento logo\" width=\"100\" height=\"100\" style=\"border-style: none\"/></a>\n");
        sb.append("</td><td width=\"90%\" align=\"center\">");

        sb.append("Error, the requested URI:<br><br><a href=\"").append(url).append("\">").append(url).append("</a><br><br>is not available in an archive.\n");
        sb.append("</td></tr></table>\n");
        sb.append("</div></body></html>");
        return sb.toString();
    }


    public String composeLinkHeader(long memento, long l,long f, String lhref, String fhref,String mhref) {
        StringBuffer sb = new StringBuffer();
        //String mem = composeLink(memento,id,"memento");
        sb.append( ",<").append(mhref).append(">;rel=\"memento\"; datetime=\"").append(httpformatter.format( new Date(memento))).append("\"");
        String mem = sb.toString();
        //String mem = ",<"+ mhref   +">;rel=\"memento\"; datetime=\"" +httpformatter.format( new Date(memento))+ "\"";

        ////String mfl = null;
        StringBuffer lb= new StringBuffer();
        lb.append(mem);
        if ( memento==f && memento==l) {
            //// mfl = ",<"+ mhref   +">;rel=\""+"memento first last"+"\"; datetime=\"" +httpformatter.format( new Date(memento))+ "\"";
            lb.append(",<").append(mhref).append(">;rel=\""+"memento first last"+"\"; datetime=\"").append(httpformatter.format( new Date(memento))).append("\"");

            //mfl = composeLink(memento,id,"memento first last");
        }
        else if (memento==f){
            //// mfl = ",<"+ mhref  +">;rel=\""+"memento first"+"\"; datetime=\"" +httpformatter.format( new Date(memento))+ "\"";

            lb.append(",<").append(mhref).append(">;rel=\""+"memento first"+"\"; datetime=\"").append(httpformatter.format( new Date(memento))).append("\"");
            //mfl = composeLink(memento,id,"memento first");
            //// mfl = mfl + ",<"+ lhref  +">;rel=\""+"memento last"+"\"; datetime=\"" +httpformatter.format( new Date(l))+ "\"";
            lb.append(",<").append(lhref).append(">;rel=\""+"memento last"+"\"; datetime=\"").append(httpformatter.format( new Date(l))).append("\"");
            // mfl = mfl + composeLink(l,id,"memento last");


        }
        else if (memento==l) {

            //// mfl = ",<"+ mhref   +">;rel=\""+"memento last"+"\"; datetime=\"" +httpformatter.format( new Date(memento))+ "\"";
            lb.append( ",<").append(mhref).append(">;rel=\""+"memento last"+"\"; datetime=\"").append(httpformatter.format( new Date(memento))).append("\"");
            //	mfl = composeLink(memento,id,"memento last");
            //// mfl = mfl + ",<"+ fhref  +">;rel=\""+"memento first"+"\"; datetime=\"" +httpformatter.format( new Date(f))+ "\"";
            lb.append(",<").append(fhref).append(">;rel=\""+"memento first"+"\"; datetime=\"").append(httpformatter.format( new Date(f))).append("\"");

            //mfl = mfl + composeLink(f,id,"memento first");

        }
        else  {

            ////mfl = mem ;
            //lb.append(mem);
            ////mfl = mfl + ",<"+ fhref  +">;rel=\""+"memento first"+"\"; datetime=\"" +httpformatter.format( new Date(f))+ "\"";
            lb.append(",<").append(fhref).append(">;rel=\""+"memento first"+"\"; datetime=\"").append(httpformatter.format( new Date(f))).append("\"");
            ////mfl = mfl + ",<"+ lhref  +">;rel=\""+"memento last"+"\"; datetime=\"" +httpformatter.format( new Date(l))+ "\"";
            lb.append(",<").append(lhref).append(">;rel=\""+"memento last"+"\"; datetime=\"").append(httpformatter.format( new Date(l))).append("\"");
            //mfl = mfl +composeLink(l,id,"memento last");
            //mfl = mfl + composeLink(f,id,"memento first");


        }


        //return mfl;
        return lb.toString();
    }


}