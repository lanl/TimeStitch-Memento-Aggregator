package gov.lanl.agg.utils;



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
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CacheConfig {
	static String cacheurl;
	static String cacheself;
     public CacheConfig(String filename,String cacheself){
	 cacheurl = filename;
	 cacheself = cacheself;
     }

    //load rules for each service once
    public static  List load_cache() {
         InputStream in = null;
         List  cachelt = new ArrayList();
        // ClassLoader cl = CacheConfig.class.getClassLoader();
        //if (cl != null) {
          //  in = cl.getResourceAsStream(filename);
        //} else {
          //  in = ClassLoader.getSystemResourceAsStream(filename);
        //}
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
        	 openConnection = new URL(cacheurl).openConnection(proxy);	
        	 }else{
        	  openConnection = new URL(cacheurl).openConnection(); 
        	 }
     		 openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        	 in = openConnection.getInputStream();
			//in = new URL(cacheurl).openStream();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        if (in != null) {
            Gson gson = new Gson();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in));
            JsonElement jelement = new JsonParser().parse(reader);
            JsonObject  jobject = jelement.getAsJsonObject();
           // System.out.println("rulematcher:"+jobject.getAsString());
           
           
            JsonArray caches = jobject.getAsJsonArray("caches");
            for (int i=0; i <caches.size();i++) {
                JsonObject entry = caches.get(i).getAsJsonObject();
                if (entry.has("uri")){
                String urls = entry.get("uri").toString().replace("\"", "");
                if (!urls.equals(cacheself)) {
                cachelt.add(urls);
                }
                }
            }
           
        }
        try {
            in.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cachelt;
    }

  

}
