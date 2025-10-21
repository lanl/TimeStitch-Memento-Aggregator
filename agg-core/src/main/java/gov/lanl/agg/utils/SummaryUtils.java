package gov.lanl.agg.utils;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.net.InternetDomainName;

public class SummaryUtils {
	/*
	@author Lyudmila Balakireva

	*/
	
	 public static String extractSuff(String u) {
		 URL url;
		try {
			 url = new URL(u);
			
			 //int port = url.getPort();
		     String host = url.getHost();
		     InternetDomainName domainName = InternetDomainName.fromLenient(host);
		     //String shost = domainName.topPrivateDomain().name();
		     String suff = domainName.publicSuffix().name();
		     return suff;
		     
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		  
	   
		return null;
	 }
	
	
	public static String shortin(String u,String type) {
		try {
			
			
			URL url=  new URL(u);
			
			   int port = url.getPort();
		       String host = url.getHost();
		       //host to lowercase
		       System.out.println("host"+host);
		       InternetDomainName domainName = InternetDomainName.fromLenient(host);
		       String shost = domainName.topPrivateDomain().name();
		       //String shost;
		       System.out.println("shost"+shost);
		       String khost=host;
		       if (!shost.equals(host)) {
		    	   khost = host.substring(0,host.lastIndexOf(shost)-1);
		       }
		       if (khost.length() > 0) {
		       String[] tokens = khost.split("\\.");
		       int tokencount = tokens.length;
		       shost = tokencount + "."+shost;
		       }
		       else {
		    	   shost="0." + shost;
		       }
		       String path = url.getPath();
		       int p=0;
		       String[] ptokens=null;
		       if (path.length()>0) {
		       System.out.println("path:" +path);
		       String s = "\\";
		       if (path.startsWith(s)) {
		    	   path.replaceFirst(s, "");   
		       }
		       System.out.println("path1:"+path);
		       if (path.endsWith(s)){
		    	   path.substring(0,path.length()-1);
		    	   System.out.println("path2:"+path);
		       }
		       
		       //if / at front just get rid of it
		       
		        ptokens = path.split("/");
		        p = ptokens.length;
		       }
		       String fs = "-";
		       if (type.equals("include1char")) {
		       if (p > 0) {
		       fs=ptokens[0].substring(0,1);
		       fs = fs.toLowerCase();
		       if (!Character.isLetter(fs.charAt(0))) {
             		fs="-";    	   
		       }
		       }
		       }
		       
		       String q = url.getQuery();
		       System.out.println("query:"+q);
		       int j = 0;
		       if (q!=null) {
		    	   String[] qtokens = q.split("&");
		    	    j = qtokens.length;
		    	   
		       }
		       
		       int pcount=p+j;
		       //one more rule
		       if (pcount>15) {
		    	   return  shost +"/" +fs +"*";
		       }
		       return  shost +"/" +fs +pcount;
		       
			 
		       
		       
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	 
	   
	
}
