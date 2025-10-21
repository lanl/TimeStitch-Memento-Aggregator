package gov.lanl.agg;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.core.util.MultivaluedMapImpl;

//import au.com.bytecode.opencsv.CSVParser;
//import au.com.bytecode.opencsv.CSVReader;

public class CdxParser {
	private LinkHeader header = new LinkHeader();
	//private int curr;
 	private String value;
 	public static final String DEFAULT_SEPARATOR = " ";
 	
 	//private InputStream inputStream;
	public CdxParser(String value)
	 	{
	 	this.value = value;
	 //	this.curr = 0;
	 	}
	//public CdxParser(InputStream inputStream)
 	//{
 	//this.inputStream =inputStream ;
 	//}
	
	public LinkHeader getHeader()
	 	{
	 	return header;
	 	}
	
	public void parse(String mementoprefix,String timemap, String url){
		MultivaluedMap<String, String> attributes = new MultivaluedMapImpl();
		//ia
		//timemapprefix="http://web.archive.org/web/";
		InputStream is = new ByteArrayInputStream(value.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = "";
		String ourl="";
		// CSVReader csvreader = new CSVReader(new InputStreamReader(is),' ',CSVParser.NULL_CHARACTER);
		int count = 1; 
		try {
			while ((line = br.readLine()) != null) {
				// System.out.println("line:"+line);
				 String[] row = line.split(DEFAULT_SEPARATOR);
				 //if (row!=null) {
				 //System.out.println("row:"+row.length);
				 //}
				// System.out.println("row" +count);
				 if (row.length==1) {
					 //somewhat tricky : use different url from IA
					 String href = timemap + ourl+"&resumeKey="+  br.readLine(); 
						System.out.println("next cdxtimemap href:"+href);
						Link link = new Link(null, "timemap", href, "", attributes);
						header.getSpecialLinks().add(link); 	
						//header.getLinksByRelationship().put("timemap", link);
						//System.out.println("size of links"+header.getLinks().size());
						//System.out.println("clean exit");
						 break;
				 }
				 
				  if (row.length==7) {
					       // System.out.println("clean e");
				    	    String 	href = mementoprefix + row[1]+"/"+row[2]; 
					       // System.out.println("href:"+href);
					        ourl=URLEncoder.encode(row[2],"UTF-8");
					        String dt =  row[1];
					       // System.out.println("dt:"+dt);
					        SimpleDateFormat formatter_utc = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
					        TimeZone tz = TimeZone.getTimeZone("GMT");
					        formatter_utc.setTimeZone(tz);
					       // SimpleDateFormat lformatter_utc = new java.text.SimpleDateFormat("yyyyMMdd HH:mm:ss");
					       // lformatter_utc.setTimeZone(tz);
					        java.text.SimpleDateFormat  httpformatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");	
							   httpformatter.setTimeZone(tz);
					        Date mdate = formatter_utc.parse(dt);
					        String datetime =  httpformatter.format(mdate);
					 
							Link link = new Link(datetime, "memento", href, "", attributes);
							header.getLinks().add(link);
							header.getLinksByDate().put(datetime, link);
							header.getLinksByRelationship().put("memento", link);
							count = count+1;
					}
				   //System.out.println("size of links"+header.getLinks().size());
					//else {
						//if (row[0]!=null) {
						//String href = timemap + "&resumeKey="+ row[0]; 
						//System.out.println("next cdxtimemap href:"+href);
						//Link link = new Link(null, "timemap", href, "", attributes);
						//header.getSpecialLinks().add(link);
						//}
					    //}
			 }
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		 finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		 
		 
	} 
}
