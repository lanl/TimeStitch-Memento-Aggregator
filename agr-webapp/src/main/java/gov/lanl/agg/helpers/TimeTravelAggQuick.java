package gov.lanl.agg.helpers;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.LinkParser;
import gov.lanl.agg.resource.MyInitServlet;
import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;



public class TimeTravelAggQuick {
    //static List <String> tglist;
    /**
     * @param args
     */
    private Map tr;
    int nat;
    static List num_name = new ArrayList();
    static List num_desc = new ArrayList();
    static List <ArchiveDescription> adesc;
    static ThreadSafeSimpleDateFormat  httpformatter = new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
    static { TimeZone tzo = TimeZone.getTimeZone("GMT");
        httpformatter.setTimeZone(tzo);
        MyInitServlet cl = MyInitServlet.getInstance();
        Map params = (Map) cl.getAttribute("params");
        adesc = (List<ArchiveDescription>) cl.getAttribute("archivedesc");
        Iterator<ArchiveDescription> ait = adesc.iterator();
        while (ait.hasNext()) {
            ArchiveDescription ad = ait.next();
            //String aname = ad.getLongname();
            String shortName = ad.getName();
            int k=ad.getOrdernumber();
            num_name.add(k, shortName);
            num_desc.add(k, ad);
        }

    }

    public NavigableMap <Long, Link> getTimegateInfo (List  timegatelist,String url,String date,boolean getNative) {

        int  n = timegatelist.size();

        ExecutorService exec = (ExecutorService )MyInitServlet.getInstance().getAttribute("MY_EXECUTOR");
        CompletionService<LinkHeader> ecs = new ExecutorCompletionService<LinkHeader> (exec);
        NavigableMap <Long, Link> m = new TreeMap<Long, Link>();
        tr = new HashMap();
        // num_name = new ArrayList();
        //long time = 180L; //60 sec
        //TimeUnit unit = TimeUnit.SECONDS;
        //TimeUnit unit = TimeUnit.NANOSECONDS;
        long TIME_BUDGET = 7L;
        long endSeconds =  TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + TIME_BUDGET;
        // System.out.println("endSeconds"+ endSeconds);
        List <TimeTravelTask> tasks = new ArrayList<TimeTravelTask> ();
        Iterator<ArchiveDescription> di =  timegatelist.iterator();
        nat=-1;
        while (di.hasNext()) {
            ArchiveDescription sarchive = di.next();
            String tmap = sarchive.getTimegate();
            //String aname = sarchive.getLongname();
            String turl = tmap+url;
            Integer od = sarchive.getOrdernumber();
            if (nat < od.intValue()) {
                nat=od.intValue();
            }
            //num_name.add(od.intValue(),aname);
            //System.out.println("timegate +number" + turl +"," + od);
            tasks.add(new TimeTravelTask(od,turl,date));
        }
        if (getNative) {
            //adding one more task native memento;
        	 
        	
            nat = nat + 1;
            //System.out.println("timegate +number" + url + "," + nat);
            //how to get maxnumber ?
            tasks.add(new TimeTravelTask(nat, url, date));
            n = n + 1;
        	 
        }
        else {
            nat = -1;
        }

        LinkHeader result = null ;
        List<Future<LinkHeader>> futures
                = new ArrayList<Future<LinkHeader>>(n);

        try {
            for (TimeTravelTask s : tasks) {
                final Future f = ecs.submit(s);
                futures.add(f);

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
                        // System.out.println("timegate_timeleft2:"+timeLeft);
                        //if (timeLeft<0) timeLeft=0;
                        Future<LinkHeader> f = ecs.poll(timeLeft, TimeUnit.SECONDS);
                        nn=nn+1;
                        // System.out.println("attemp" +nn);
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
        // ok we have list of mementos
        return m;

    }


    public void parsipolis (LinkHeader linkheader,NavigableMap map) {
        List<Link> links = linkheader.getLinks();
        int i = linkheader.getHostId();
        if (i== nat) {
            ArchiveDescription ad = new ArchiveDescription();
            ad.setName("Native");
            ad.setOrdernumber(i);
            //System.out.println("hostid native, size of links:"+links.size());
            tr.put(ad, links);
        }
        else {
            tr.put(num_desc.get(i),links);
            //tr.put( num_name.get(i),links);

            //System.out.println("hostid "+num_name.get(i)+" size of links:"+links.size());
        }
        for (Link link:links) {
            String datetime = link.getDatetime();

            if (datetime!=null) {
                Date d ;
                try {
                    d = httpformatter.parse(datetime);

                    //d.getTime();
                    //System.out.println("d" +d.getTime());
                    //System.out.println("d" +d.getTime()+"mem url:"+link.getHref());
                    //map.put(d.getTime(), link.getHref());

                    map.put(d.getTime(), link);

                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

    }

    public Map getPerArchive() {
        //Comparator comparator = new MyComparator();
        //Collections.sort(myglobal, comparator);
        return tr;
    }

    public class TimeTravelTask implements Callable<LinkHeader>{
        String url;
        String date;
        int ihost;
        LinkHeader linkheader = null;

        public TimeTravelTask(Integer I,String url, String date) {
            this.url=url;
            this.date=date;
            this.ihost=I.intValue();
        }


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
                //System.out.println("status: " +statusCode);
               // System.out.println("dyn update timegate: "+statusCode +"url: "+feedUrl + " : "+ date);
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
                        //System.out.println("redirect" + location.getValue());
                        String iloc = location.getValue();
                     /* if (iloc.startsWith("https:")) {
                            iloc = iloc.replaceFirst("https:", "http:");

                        } */
                        response = checkFeedUrl(client,iloc,date);
                    }
                    else {
                        Header link_msg = method.getResponseHeader("Link");
                        String add_msg = link_msg.getValue();
                        System.out.println(add_msg);
                        LinkParser parser = new LinkParser(add_msg);
                        parser.parse();
                        LinkHeader linkheadertmp = parser.getHeader();
                        Link lmemento =   linkheadertmp.getLinkByRelationship("memento");
                        if (lmemento!=null) {
                        	linkheader = linkheadertmp;
                        	linkheader.setHostId(ihost);
                        	Link fmemento =   linkheadertmp.getLinkByRelationship("first memento");
                        	if (fmemento==null) {
                        		//we can try to see if we have more info at the memento level
                        		response = checkFeedUrl(client,location.getValue(),date);
                        	}
                        }
                        else {
                        	//not sufficient info at timegate
                        	response = checkFeedUrl(client,location.getValue(),date);
                        }
                        //linkheader.setHostId(ihost);
                    }

                } else {
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
                  	         linkheader.setHostId(ihost);
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
                                                	System.out.println("timegate 200"); 
                                                	linkheader = parser.getHeader();
                                                	linkheader.setHostId(ihost);
                            		             }
                                                 else {
                                                	// System.out.println(" 200"); 
                                                	   LinkHeader linkheadertmp = parser.getHeader();
                                                       Link lmemento = linkheadertmp.getLinkByRelationship("memento");
                                                   if (lmemento!=null) {
                                                	   System.out.println("trying to get link header info from  200"); 
                                                	   linkheader = linkheadertmp;
                                                	   linkheader.setHostId(ihost);
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
