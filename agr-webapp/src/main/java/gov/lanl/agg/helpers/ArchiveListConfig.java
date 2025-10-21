package gov.lanl.agg.helpers;

import gov.lanl.agg.ArchiveDescription;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.DefaultExpressionEngine;

public class ArchiveListConfig {

    static List timemapindex;
    static List timegateindex;
    static Map archive_map;
    static Map params;
    static List timemappaging;
    static List archivedesclist;



    public List getTGList() {
        return timegateindex;
    }
    public List getTMList() {
        return timemapindex;
    }
    public Map getMap(){
        return archive_map;
    }
    public Map getParams(){
        return params;
    }
    public List getPagingList() {
        return timemappaging;
    }

    public List getArchiveList() {
        return archivedesclist;
    }


    private  static ArchiveListConfig singleton = null;
    
    public static Map<String, String> loadConfigFile( )
    {

        ClassLoader cl = ArchiveListConfig.class.getClassLoader();

        java.io.InputStream in;

        if (cl != null) {
            in = cl.getResourceAsStream("agg.properties");
        } else {
            in = ClassLoader.getSystemResourceAsStream("agg.properties");

        }
        System.out.println( "Using configuration from classpath");

        return in!=null ? loadProperties( in )
                : new HashMap<String, String>();


    }
    public static Map<String,String> loadProperties( InputStream stream)
    {
        Properties props = new Properties();
        try
        {

            try
            {
                props.load( stream );
            }
            finally
            {
                stream.close();
            }
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "Unable to load agg.properties", e );
        }
        Set<Entry<Object,Object>> entries = props.entrySet();
        Map<String,String> stringProps = new HashMap<String,String>();
        for ( Entry<Object,Object> entry : entries )
        {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            stringProps.put( key, value );

        }
        return stringProps;
    }

    
    
    public static ArchiveListConfig getInstance() {
    	
		if (singleton == null) {
			singleton = new ArchiveListConfig();
			singleton.processConfig();			
		}
		return singleton;
		
    }
	
    
    public static void processConfig()  {
		 /*
		  <timegates>
         <timegate uri=" base uri of timegate ">
         <start> datetime </start>
         <end> datetime </end>
         <regex> URI regular expression </regex>
         <regex> URI regular expression </regex>
         <timemap  uri="http://www.theresourcedepot.com/000010/timemap/link/"/>
         </timegate>
         </timegates>
         
         <links>
         <link id="hostname">
         <timegate uri="baseuri of timegate" pagestatus="0">
         <timemap  uri="baseuri of timemap">
         
         </link>  
         <links>
         
         //pagestatus 0 paging is not implemented
         // 1 timemap style aka index
         // 2 timemap forward (old records first , next new one)
         // 3 timemap backward(new records first, next older one)
         
		  * 
		  */
        String baseUri = "";
        String privateproxyhostname=null;
        try {
            params = loadConfigFile( );
            // ProxySelector.setDefault(null);
            Proxy proxy = null;
            System.setProperty("proxySet", "false");
            if (params.containsKey("hostname.private.proxy")) {
            	privateproxyhostname = (String) params.get("hostname.private.proxy");
            }
            
            if (params.containsKey("baseuri.proxy")) {
                baseUri = (String) params.get("baseuri.proxy");
            }

            if (params.containsKey("web.proxyout.host")) {
                System.out.println("proxy detected");
                Properties p = System.getProperties();
                System.setProperty("proxySet", "true");
                System.setProperty("http.proxyHost",(String) params.get("web.proxyout.host"));
                System.setProperty("http.proxyPort",(String) params.get("web.proxyout.port"));
                String iport = (String) params.get("web.proxyout.port");
                System.out.println("port:"+iport);

                InetSocketAddress proxyInet = new InetSocketAddress((String) params.get("web.proxyout.host"),Integer.parseInt(iport));
                proxy = new Proxy(Proxy.Type.HTTP, proxyInet);

            }

            ClassLoader cl = ArchiveListConfig.class.getClassLoader();
            XMLConfiguration config = new XMLConfiguration();

            String  archive_registry = (String) params.get("config.archive.registry");
            System.out.println(archive_registry);
            InputStream in;
            if (archive_registry!=null) {
                URLConnection openConnection;
                if (proxy!=null) {
                    openConnection = new URL(archive_registry).openConnection(proxy);
                }else{
                    openConnection = new URL(archive_registry).openConnection();
                }
                openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
                in = openConnection.getInputStream();
            }
            else {
                if (cl != null) {
                    in = cl.getResourceAsStream("archiveslinks.xml");
                } else {
                    in = ClassLoader.getSystemResourceAsStream("archiveslinks.xml");
                }
            }

            //System.out.println("before load");

            config.load(in);

            StringWriter stringWriter = new StringWriter();
            config.save(stringWriter);
            //System.out.println(stringWriter.toString());
            DefaultExpressionEngine engine = new DefaultExpressionEngine();
            engine.setAttributeEnd(null);
            engine.setAttributeStart(engine.getPropertyDelimiter());

            config.setExpressionEngine(engine);
            // config.setExpressionEngine(new XPathExpressionEngine());

            //System.out.println("after load");
            Object prop = config.getProperty("timegate");
            // list = new ArrayList();
            // map = new TreeMap();
            timemapindex = new ArrayList();
            timegateindex = new ArrayList();
            archive_map = new HashMap();
            timemappaging = new ArrayList();
            archivedesclist = new ArrayList();
            List ltimegates = config.getList("link.id");
            System.out.println("list size from tg-director config:"+ltimegates.size());
           
            if (ltimegates.size()>0) {
            	
                for (int i=0;i<ltimegates.size();i++) {
                    String url = "";
                    String timegateRedirect = "";
                    if (config.getProperty("link("+i+").timegate.uri") instanceof String) {
                        url  =  (String) config.getProperty("link("+i+").timegate.uri");
                        timegateRedirect = (String) config.getProperty("link("+i+").timegate.redirect");
                    }
                    else if (config.getProperty("link("+i+").timegate.uri") instanceof List) {
                        List<String> urls = (List<String>) config.getProperty("link("+i+").timegate.uri");
                        List<String> redirects = (List<String>) config.getProperty("link("+i+").timegate.redirect");
                        for (int u=0; u<urls.size(); u++) {
                            if (urls.get(u).startsWith(baseUri)) {
                                url = urls.get(u);
                                timegateRedirect = redirects.get(u);
                                break;
                            }
                        }
                        if (url.equals("")) {
                            url = urls.get(0);
                            timegateRedirect = redirects.get(0);
                        }
                    }
                    String memst = (String) config.getProperty("link("+i+").archive.memento-status");
                    String timegateGlobal = url;
                    if(privateproxyhostname!=null&&memst.equals("no")){
                  	   URL tgg = new URL(url);
                  	   url = tgg.getProtocol() +"://"+privateproxyhostname+ tgg.getFile();
                  	   
                     }
                    System.out.println("url"+url);
                    String host =  (String) config.getProperty("link("+i+").id");
                    String longname =  (String) config.getProperty("link("+i+").longname");
                    System.out.println("host" +host);
                    String timemap = "";
                    String pagingstatus = "";
                    String timemapRedirect = "";
                    if (config.getProperty("link("+i+").timemap.uri") instanceof String) {
                        timemap  =  (String) config.getProperty("link("+i+").timemap.uri");
                        pagingstatus = (String) config.getProperty("link("+i+").timemap.paging-status");
                        timemapRedirect =  (String) config.getProperty("link("+i+").timemap.redirect");
                    }
                    else if (config.getProperty("link("+i+").timemap.uri") instanceof List) {
                        List<String> urls = (List<String>) config.getProperty("link("+i+").timemap.uri");
                        List<String> paging_statuses= (List<String>) config.getProperty("link("+i+").timemap.paging-status");
                        List<String> redirects = (List<String>) config.getProperty("link("+i+").timemap.redirect");
                        for (int r=0; r<urls.size(); r++) {
                            if (urls.get(r).startsWith(baseUri)) {
                                timemap = urls.get(r);
                                pagingstatus = paging_statuses.get(r);
                                timemapRedirect = redirects.get(r);
                                break;
                            }
                        }
                        if (timemap.equals("")) {
                            timemap = urls.get(0);
                            pagingstatus = paging_statuses.get(0);
                            timemapRedirect = redirects.get(0);
                        }
                        
                      
                    }
                    String global=timemap;
                    System.out.println("timemap_global" + timemap);
                    if(privateproxyhostname!=null&&memst.equals("no")){
                 	   URL tm = new URL(timemap);
                 	   timemap = tm.getProtocol() +"://"+privateproxyhostname+ tm.getFile();
                 	   
                    }
                    System.out.println("timemap" +timemap);
                    String avatar = (String) config.getProperty("link("+i+").icon.uri");

                    System.out.println("paging" +pagingstatus);
                        String rw = (String) config.getProperty("link("+i+").archive.rewritten-urls");
                    //String ast = (String) config.getProperty("link("+i+").accessPolicy.type");
                    String ast = (String) config.getProperty("link("+i+").archive.access-policy");
                    //String memst = (String) config.getProperty("link("+i+").accessPolicy.mementostatus");
                   // String memst = (String) config.getProperty("link("+i+").archive.memento-status");
                    // #TODO unused currently... but useful
                    String openwaybackst = (String) config.getProperty("link("+i+").accessPolicy.openwayback");
                    String memuri = (String) config.getProperty("link("+i+").memento.uri");
                      String archiveType = (String) config.getProperty("link("+i+").archive.type");
                   
                    ArchiveDescription archivedesc = new ArchiveDescription();
                    archivedesc.setTimemapGlobal(global);
                    archivedesc.setTimegateGlobal(timegateGlobal);
                    archivedesc.setName(host);
                    archivedesc.setTimegate(url);
                    archivedesc.setTimemap(timemap);
                    archivedesc.setOrdernumber(i);
                    archivedesc.setPagingstatus(pagingstatus);
                    //archivedesc.setDelaystatus(dp);
                    archivedesc.setAccesstatus(ast);
                    archivedesc.setMementostatus(memst);
                    archivedesc.setRewritestatus(rw);
                    archivedesc.setMementotemplate(memuri);
                    archivedesc.setLongname(longname);
                    archivedesc.setArchiveType(archiveType);
           

                    archivedesc.setTimegateRedirect(timegateRedirect);
                    archivedesc.setTimemapRedirect( timemapRedirect);

                    String calpage = (String) config.getProperty("link("+i+").calendar.uri");
                    System.out.println("calpage"+calpage);
                    
                    archivedesc.setCalendarUrl(calpage);
                    archivedesc.setIcon(avatar);
                    if  (openwaybackst!=null) {
                        archivedesc.setOpenwaybackstatus(openwaybackst);
                    }
                   

                    archivedesclist.add(archivedesc);
                    archive_map.put(host,i);
                    timemapindex.add (i,timemap);
                    timegateindex.add(i,url);
                    timemappaging.add(i,pagingstatus);

                }


            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


}
