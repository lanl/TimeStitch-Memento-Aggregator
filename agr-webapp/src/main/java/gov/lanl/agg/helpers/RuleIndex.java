package gov.lanl.agg.helpers;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RuleIndex {

   // public static  String DOMAIN_PATTERN = "\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}github\\.com";
    public static  String DOMAIN_PATTERN1 = "\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}\\.fr";
    public static  String DOMAIN_PATTERN2 = "\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}wikipedia\\.";
    public static  String DOMAIN_PATTERN3 = "\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}\\.parliament\\.uk";
    public static String timegate_default = new String();
    
    public static  String DOMAIN_PATTERN="\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}";
     
    static Map regindex = new TreeMap();

    public static String getDomain(String url) {
        if (url == null || url.equals("")) {
            return "";
        }
        Pattern p = Pattern.compile(DOMAIN_PATTERN3);
        // Pattern pattern = p.compile("http://([a-z0-9]*\\.)?github\\.com");
     
        Matcher m = p.matcher(url);

        if (m.find()) {
            return m.group();
        }
        return "";
    }

    public static void main(String[] args) {
        //System.out.println("domain"+getDomain("http://www.wikipedia.com/wiki/Myalgia"));
        System.out.println("domain"+getDomain("http://www.polis.parliament.uk/a"));
        
    }



    public RuleIndex(){

    }

    //load rules for each service once
    public static  void load_index(String filename) {
        InputStream in= null;
      //  RulesDescription rd = new RulesDescription();
        Map<Pattern, List<String>> map = new HashMap<>();
       // Map regindex = new TreeMap();
        String proxyset = System.getProperty("proxySet");
        Proxy proxy = null;
        if(proxyset.equals("true")){
        
		   String iport = System.getProperty("http.proxyPort");
		   System.out.println("port:"+iport);
		   
		   InetSocketAddress proxyInet = new InetSocketAddress((String)System.getProperty("http.proxyHost"),Integer.parseInt(iport));
		   proxy = new Proxy(Proxy.Type.HTTP, proxyInet);
			   
        }
        
        try {
        	 URLConnection openConnection;
        	 if (proxy!=null) {
        	 openConnection = new URL(filename).openConnection(proxy);	
        	 }else{
        	  openConnection = new URL(filename).openConnection(); 
        	 }
     		 openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        	 in = openConnection.getInputStream();
		
			//in = new URL(filename).openStream();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
       /* 
        ClassLoader cl = CommonRuleMatcher.class.getClassLoader();
        if (cl != null) {
            in = cl.getResourceAsStream(filename);
        } else {
            in = ClassLoader.getSystemResourceAsStream(filename);
        }
        */
        if (in != null) {
            Gson gson = new Gson();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in));
            JsonElement jelement = new JsonParser().parse(reader);
            JsonObject  jobject = jelement.getAsJsonObject();
           // System.out.println("rulematcher:"+jobject.getAsString());
           
            //String defaultlist = jobject.get(service+"default").toString().replace("\"", "");

            //System.out.println("defaultlist"+defaultlist);
            // defaultlist = defaultlist0;

           // List<String> defaultarchives = new ArrayList(Arrays.asList(defaultlist.split("\\s*,\\s*")));
            // List defaultarchives =  Arrays.asList(defaultlist.split("\\s*,\\s*"));
           // rd.setDefaultArchives(defaultarchives);
            //System.out.println("def1"+defaultarchives.size());
            //System.out.println("defa"+defaultarchives.toString());

            JsonArray rules = jobject.getAsJsonArray("rules");
            for (int i=0; i <rules.size();i++) {
                JsonObject entry = rules.get(i).getAsJsonObject();
                String urls = entry.get("url").toString().replace("\"", "");
                // System.out.println(urlpattern);
                //String archivelist =  entry.get(service).toString().replace("\"", "");
                //archivelist = archivelist.replace(service+"default", defaultlist);
                // System.out.println("archivelist:"+archivelist);\\
                String urlpattern;
                if (urls.startsWith("http://")){
                	 //urlpattern = "^" + urls +"$"; // or 
                	 urlpattern = urls;
                String collection = entry.get("collection").toString().replace("\"", "");
                
                }else{
                     urlpattern = DOMAIN_PATTERN + urls;
                }
                Pattern p = Pattern.compile(urlpattern);
                String[] strArray = urls.split("\\.");
                int strlen = strArray.length;
                if (strArray.length>1) {
                	strlen = strArray.length-1;
                }
                for (int j=0;j<strArray.length;j++){
                	String word = strArray[j];
                	if (regindex.containsKey(word)) {
                		ArrayList a = (ArrayList)regindex.get(word);
                		a.add(p);
                		regindex.put(word,a);
                		
                	}
                	else {
                		ArrayList a = new ArrayList();
                		a.add(p);
                		regindex.put(word,a);
                	}
                }
                
               // map.put(p, new ArrayList<>(Arrays.asList(archivelist.split("\\s*,\\s*"))));
            }
           // rd.setUrlRules(map);
            Iterator it = regindex.keySet().iterator();
           
            while(it.hasNext()) {
            	String word = (String) it.next();
            	
            	System.out.println (word);
            	
            }
            
            
        }
        try {
            in.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    
}
