package gov.lanl.agg.utils;



import gov.lanl.agg.RulesDescription;

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
import java.util.Arrays;
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
import com.jayway.jsonpath.JsonPath;

public class CommonRuleMatcher {

   // public static  String DOMAIN_PATTERN = "\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}github\\.com";
    public static  String DOMAIN_PATTERN1 = "\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}\\.fr";
    public static  String DOMAIN_PATTERN2 = "\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}wikipedia\\.";
    public static  String DOMAIN_PATTERN3 = "\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}\\.parliament\\.uk";
    public static  String DOMAIN_PATTERN="\\p{L}{0,10}(?:://)?[\\p{L}\\.]{0,50}";
     


    public static String timegate_default = new String();



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



    public CommonRuleMatcher(){

    }

    public static InputStream read_file(String filename){
    	 InputStream in = null;
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
          
         
          
          return in;
          
    }
    
    //load rules for each service once
    public static  RulesDescription load_rules(String filename,String service) {
        InputStream in = read_file(filename);
        RulesDescription rd = new RulesDescription();
        Map<Pattern, List<String>> rmap =  new HashMap();
        		
        //Map regindex = new TreeMap();
      
        if (in != null) {
            Gson gson = new Gson();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in));
            JsonElement jelement = new JsonParser().parse(reader);
            JsonObject  jobject = jelement.getAsJsonObject();
           // System.out.println("rulematcher:"+jobject.getAsString());
           
            String defaultlist = jobject.get(service+"default").toString().replace("\"", "");

           

            List<String> defaultarchives = new ArrayList(Arrays.asList(defaultlist.split("\\s*,\\s*")));
            // List defaultarchives =  Arrays.asList(defaultlist.split("\\s*,\\s*"));
            rd.setDefaultArchives(defaultarchives);
          

            JsonArray rules = jobject.getAsJsonArray("rules");
            for (int i=0; i <rules.size();i++) {
                JsonObject entry = rules.get(i).getAsJsonObject();
                String urls = entry.get("url").toString().replace("\"", "");
                JsonObject api = entry.getAsJsonObject("api");
                List exturls = null;
                if (api!=null) {
                	 String apiurl = api.get("url").toString().replace("\"", "");
                	 String jsonpath = api.get("jsonpath").toString().replace("\"", "");
                	 System.out.println("apiurl:"+apiurl);
                	 System.out.println("jsonpath"+jsonpath);
                	 InputStream inpsr = read_file(apiurl);
                	 if (inpsr!=null) {
                	 BufferedReader breader = new BufferedReader(new InputStreamReader(inpsr));
                	 StringBuilder out = new StringBuilder();
                	    //String newLine = System.getProperty("line.separator");
                	    String line;
                	    try {
							while ((line = breader.readLine()) != null) {
							    out.append(line);
							   // out.append(newLine);
							}
							
							  String jsonstr = out.toString();
							  exturls = JsonPath.read(jsonstr, jsonpath);
							  
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                	 }
                }
                //String api = entry.get("api").toString().replace("\"", "");
                // System.out.println(urlpattern);
                String archivelist =  entry.get(service).toString().replace("\"", "");
                archivelist = archivelist.replace(service+"default", defaultlist);
                // System.out.println("archivelist:"+archivelist);\\
                String urlpattern;
                if (urls.startsWith("http://")){
                	 //urlpattern = "^" + urls +"$"; // or 
                	if ( exturls!= null) {
                		Iterator itt = exturls.iterator();
                		while(itt.hasNext()) {
                			
                		 String eurl= (String) itt.next();
                		 System.out.println(eurl);
                		 urlpattern = eurl;               	
                    	 Pattern p = Pattern.compile(urlpattern);
                         rmap.put(p, new ArrayList<>(Arrays.asList(archivelist.split("\\s*,\\s*"))));
                		}
                		
                		
                	} else{
                	 urlpattern = urls;               	
                	 Pattern p = Pattern.compile(urlpattern);
                     rmap.put(p, new ArrayList<>(Arrays.asList(archivelist.split("\\s*,\\s*"))));
                	}
                	 
                	 
                }else{
                     urlpattern = DOMAIN_PATTERN + urls;
                     Pattern p = Pattern.compile(urlpattern);
                     rmap.put(p, new ArrayList<>(Arrays.asList(archivelist.split("\\s*,\\s*"))));
                }
               
               
            }
            rd.setUrlRules(rmap);
          
            
            
        }
        try {
            in.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return rd;
    }

    public static List<String> getArchives(String url,RulesDescription rd) {
        //or return null?
        Map<Pattern, List<String>> map = rd.getUrlRules();
        System.out.println("mapsize"+map.size());
        List def = rd.getDefaultArchives();

        System.out.println("def"+def.size());
        Iterator it = map.keySet().iterator();
        while(it.hasNext()){
            Pattern p = (Pattern) it.next();
            Matcher m = p.matcher(url);
            if (m.find()) {
                List<String> archives = map.get(p);
                System.out.println("found archives:"+archives);
                //List  items = Arrays.asList(archives.split("\\s*,\\s*"));
                //return Arrays.asList(archives.split("\\s*,\\s*"));
                return archives;
            }
        }//while

        return def;
    }

}
