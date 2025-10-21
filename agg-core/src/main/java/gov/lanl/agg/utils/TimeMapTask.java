package gov.lanl.agg.utils;

import gov.lanl.agg.CdxParser;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.LinkParser;


//import com.sun.jersey.api.client.ClientResponse;
//import com.sun.jersey.api.client.WebResource;
//import com.sun.jersey.client.apache.ApacheHttpClient;
//import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;












import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;


public class TimeMapTask implements Callable<Map>{
    String url;
   // String timemap;
    int hostid;
    //DefaultApacheHttpClientConfig cc;
    HttpClient client;
    String mementoprefix=null;
    String timemap=null;
   // String date;
	public TimeMapTask(int hostid,String url,HttpClient client) {
		this.url = url;
		//this.cc =cc;
		this.hostid = hostid;
		this.client = client;
		//this.date=date;
	}
	public TimeMapTask(int hostid,String url,HttpClient client,String mementoprefix,String timemap) {
		this.url = url;
		//this.cc =cc;
		this.hostid = hostid;
		this.client = client;
		this.mementoprefix = mementoprefix;
		this.timemap = timemap;
		//this.date=date;
	}
	
	@Override
	public Map call() throws Exception  {
		// TODO Auto-generated method stub
		 Map map = new HashMap();
		 GetMethod method = null;
    	 //long time_1 = System.currentTimeMillis();
		 //System.out.println("paging testing timemap " + url );
		//HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli")
         //WebResource webResource = client.resource(url);
		 //long time_2 = System.currentTimeMillis();
        // System.out.println("Webresource Took : " + ((time_2 - time_1) / 1000) +"sec for hostid" +hostid);
       //  long time_3 = System.currentTimeMillis();
         //ClientResponse response=null;
		try {
			
			    method = new GetMethod(url);
		        method.setFollowRedirects(false);
		        method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
		        method.addRequestHeader("Cache-Control", "no-cache");
		       // method.setRequestContentLength(method.);
		        //method.setStrictMode(true);
		        int status = 0;
				//try {
					status = client.executeMethod(method);
				//} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				//}
		       // try {
				//	method.getResponseBodyAsString();
				//} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				//}
         //response = webResource.get(ClientResponse.class);
		 //long time_4 = System.currentTimeMillis();
         //System.out.println("ClientResponse Took : " + ((time_4 - time_3) / 1000) +"sec for hostid" +hostid);       
		 //System.out.println("status :"+response.getStatus() + "url"+ url);
		 //MultivaluedMap<String, String> hmap = response.getHeaders();
		// int status = response.getStatus();
		 if (status!=200) {
			 LinkHeader lh = new LinkHeader();
			 lh.setStatus(status);
			/// map.put(hostid, null);
			 map.put(hostid, lh);
			 
		 }
		 else {
		    //String ms = response.getEntity(String.class);
		    String ms="";
			
				//try {
		   
					ms = method.getResponseBodyAsString();
					/*InputStream in = method.getResponseBodyAsStream();
					//System.out.println("max_int"+Integer.MAX_VALUE);
					long l = method.getResponseContentLength();
					//System.out.println(hostid+"response length:"+l);
					BufferedReader bufReader = new BufferedReader((new InputStreamReader(in)));
					String line=null;
					
					StringBuilder responseData = new StringBuilder();
					while((line =  bufReader.readLine()) != null) {
						System.out.println(line);
					    responseData.append(line);
					}
					*/
					//byte[] responseBody = method.getResponseBody();
					//String encoding = c.getContentEncoding();
					//encoding = encoding == null ? "UTF-8" : encoding;
					 //ms = IOUtils.toString(in, encoding);
					//System.out.println(new String(responseBody,"UTF-8"));
					//ms = responseData.toString();
					/*String[] parts = ms.split(",\\s*<");
					//String[] parts = ms.split(",\\<");
					int u = parts.length;
					for (int ii=0;ii<u; ii++) {
						System.out.println(parts[ii]);
						//String tokens[]=parts[ii].split(";");
						//tokens[]
					}
					*/
					//System.out.println("split"+u);
					
					
					//System.out.println(hostid+"string length:"+ms.length());
					//System.out.println(ms);
					/*InputStream in = method.getResponseBodyAsStream();
					BufferedReader bufReader = new BufferedReader((new InputStreamReader(in)));
					String line=null;
					
					
					List bus = new ArrayList();
                    int count=0;
					StringBuilder responseData = new StringBuilder();
					while((line =  bufReader.readLine()) != null) {
						
					    responseData.append(line);
					    count=count+1;
					    if (count==5000) {
					    	String buf = responseData.toString();
					    	System.out.println(hostid+", buf"+count);
					    	bus.add(buf);
					    	responseData = new StringBuilder();
					    	count=0;
					    }
					}
					if (responseData.length()>0) {
						bus.add(responseData.toString());
					}
					
					System.out.println("host length:"+ hostid +","+ bus.size());
					*/
					
					
				//} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				//}
			
		   // System.out.println(ms);
		    if (mementoprefix!=null) {
		    	// System.out.println("cdxparser:");
		    	 CdxParser parser = new CdxParser(ms);
		    	// String ourl =url.substring(timemap.length()+1);
		    	// System.out.println("mementoprefix:"+mementoprefix);
		    	// System.out.println("timemap:"+timemap);
				 parser.parse(mementoprefix,timemap,url);
				 LinkHeader linkheader = parser.getHeader();
				 linkheader.setStatus(status);
				 map.put(new Integer(hostid),linkheader);
				// System.out.println ("clean" +hostid);
		    }
		    else {
		    	// System.out.println(ms);
		    	// System.out.println("linkparser:");
		    	
		    	   LinkParser parser = new LinkParser(ms); 
		    	   parser.parse();
			       LinkHeader linkheader = parser.getHeader();
			       linkheader.setStatus(status);
			       map.put(new Integer(hostid),linkheader); 
		    	/*
		    	 List <Link> alllinks = new ArrayList();
		    	// synchronized(this) {
		    	 Iterator it = bus.iterator(); 
		    	    while (it.hasNext()) {
		    	    String buff = (String) it.next();
		            LinkParser parser = new LinkParser(buff); 
			        parser.parse();
			        LinkHeader linkheader = parser.getHeader();
			      
			        List<Link> portion = linkheader.getLinks();
			        System.out.println("one portion"+portion.size());
			        alllinks.addAll(portion);
			        // map.put(new Integer(hostid),linkheader); 
		    	    }
		    	       LinkHeader lh = new LinkHeader();
		    	       System.out.println("alllinks"+hostid+alllinks.size());
		    	       Iterator it0 = alllinks.iterator() ;
		    	       while(it0.hasNext()) {
		    	    	   Link link = (Link) it.next();
		    	    	   lh.addLink(link);
		    	    	   
		    	       }
		    	   
		    	   
		    	 //  lh.addLinks(alllinks);
		    	   map.put(new Integer(hostid),lh); 
		    	   */
			     }
		        //}
		 }
		}
		
		 finally { 
			
			 method.releaseConnection();
			//if (response!=null) response.close();
			   
			}
			
			return map;
		
	}
	


}