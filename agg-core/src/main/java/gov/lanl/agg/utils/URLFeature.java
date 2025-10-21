package gov.lanl.agg.utils;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.net.InternetDomainName;

public class URLFeature {
 String url;
 String hostname="";
 int url_len=0;
 int host_len=0;
 int slash_num=0;
 int dot_num=0;
 int amp_num=0;
 int amp_num_path=0;
 int dash_num=0;
 int path_len=0;
 int query_len=0;
 int dot_num_host=0;
 int dash_num_host= 0;
 int  _num_path = 0;
 String suffix="";
 String main_word="";
 String[] PathWords;
 String extention;
 
 
 public  URLFeature(String url) {
	 this.url = url;
	 init();
 }
public String getHostname(){
	return hostname;
}
 
public int getUrl_len() {
	return url_len;
}
 
public int getHost_len(){
	return host_len;
}
 
public int getSlash_num(){
	return  slash_num;
}

public int getDot_num(){
	return dot_num;
}

public int getAmp_num(){
	return amp_num;
}

public int getAmp_num_path(){
	return amp_num_path;
}

public int getDash_num(){
	return  dash_num;	
}

public int getDash_num_host(){
	return  dash_num_host;	
}

public int getPath_len(){
	return path_len;
}
public int getQuery_len(){
	return query_len;
}

public int getDot_num_host(){
	return  dot_num_host;	
}

public int get__num_path(){
	return  _num_path;	
}

public String getSuffix(){
	return suffix;
}
public String getMainWord(){
	return  main_word;
}

public  String[] getPathWords(){
	return PathWords;
}


public 
void init() {
	 URL Url;
		try {
			 url=url.toLowerCase();
					
					
			  Url = new URL(url);
			  //System.out.println(url);
			 //int port = url.getPort();
		       hostname = Url.getHost();
		      // System.out.println("host:"+hostname);
		       String scheme = Url.getProtocol();
		       host_len = hostname.length();
		       String[] tokens = hostname.split("\\.");
		       dot_num_host = tokens.length-1;
		       String[] tokens2 = hostname.split("-");
               dash_num_host = tokens2.length-1;
		       String path = Url.getPath();
		      
		       
		       if (path!=null) {
		    	  
		    	   path_len = path.length();
		    	   
		    	   while(path.contains("//")) {
						path = path.replace("//","/");
					} 
		    	   
		    	   String test = path;
		    	   slash_num = 0;
		    	   while (test.contains("/")) {
		    		   test = test.replaceFirst("/", "#");
		    		   slash_num=slash_num+1;
		    	   }
		    	  
		    	   
		            String s = "/";
		            if (path.startsWith(s)) {
		    	       path = path.replaceFirst(s, "");   
		            }
		           // System.out.println(path);
		     

		            PathWords = path.split("/");
		           // System.out.println("length:"+PathWords.length);
		            // slash_num   = PathWords.length-1;
		        
		       
		            amp_num_path = path.toLowerCase().split("=").length-1;
		            _num_path = path.toLowerCase().split("_").length-1;
		       }
		       else{
		    	   PathWords = null; 
		       }
		        String query = Url.getQuery();
		        if (query !=null){
		        query_len=query.length();
		        String [] qtokens = query.toLowerCase().split("=");
		        amp_num = qtokens.length-1;
		        }
		     
		      InternetDomainName domainName = InternetDomainName.fromLenient(hostname);
		     
		     //String shost = domainName.topPrivateDomain().name();
		      suffix = domainName.publicSuffix().name();
		      dash_num = url.toLowerCase().split("-").length-1;
		      dot_num =   url.toLowerCase().split("\\.").length-1;
		      main_word = hostname.replace(suffix, "");
		      String[] mtokens = main_word.split("\\.");
		      main_word = mtokens[mtokens.length-1];
		      url_len = url.length();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
}

}
