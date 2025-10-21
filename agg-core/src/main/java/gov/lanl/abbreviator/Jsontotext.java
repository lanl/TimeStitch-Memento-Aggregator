package gov.lanl.abbreviator;

import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.CompositeType.Builder;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.io.sstable.SSTableSimpleUnsortedWriter;
import org.apache.cassandra.service.StorageService;

import static org.apache.cassandra.utils.ByteBufferUtil.bytes;

import org.json.JSONObject;
/*
@author Lyudmila Balakireva

*/


public class Jsontotext {
    static Date sday;
    static ThreadSafeSimpleDateFormat hformatter;
	static {
		 String startday = "1995-01-01 00:00:00";
	     TimeZone tz = TimeZone.getTimeZone("GMT");
			  ThreadSafeSimpleDateFormat httpformatter = new ThreadSafeSimpleDateFormat("yyyy-mm-dd HH:mm:ss");	
			  hformatter = new ThreadSafeSimpleDateFormat("yyyymmdd");
			  
			  httpformatter.setTimeZone(tz);
			  hformatter.setTimeZone(tz);
			  
	          try {
				sday = httpformatter.parse(startday);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	//create keyspace abbreviator  with replication = {'class':'SimpleStrategy','replication_factor':1};
	//use abbreviator;
	//CREATE TABLE summary (url varchar, mdate timestamp,archive_id varchar, mcount int,PRIMARY KEY (url, mdate,archive_id));

	

	//create keyspace abbreviator
	//CREATE TABLE summary (url varchar, mdate timestamp,archive_id varchar, mcount int PRIMARY KEY (url, mdate,archive_id));
	 // ./bin/cassandra-cli -h 127.0.0.1
	//./bin/cqlsh
	//./run.sh
	// /home/ludab/cassandra/dsc-cassandra-1.2.2/bin/sstableloader  -d localhost -debug /home/ludab/cassandra/dsc-cassandra-1.2.2/test/abbreviator/summary
	public static void main(String[] args) {
		//try {
		     Args margs = new Args( args );
		     String mfilename =  margs.get("fname", "cdx_0.json");
		     String pathtomap  =  margs.get("fmap", "/smnt/proto_space/iipc/cdx-id-hash.json"); 
		     MyJParser mparser = new MyJParser();
		              java.util.Map fmap = mparser.getMappings(pathtomap);
		              
		              
	         IPartitioner<?> partitioner = StorageService.getPartitioner();
	         String keyspace = "abbreviator";
	         BufferedReader br = null;
	        Jsontotext maker = new Jsontotext();
	        try {
	        File directory = new File(keyspace+"/summary");
	        
	        
	         if (!directory.exists()){
	           directory.mkdir();}
	      
	     	 //  final List<AbstractType<?>> compositeTypes = new ArrayList<>();
		         //compositeTypes.add(LongType.instance); //or LongType.instance ? 
		       //  compositeTypes.add(DateType.instance);
		         //compositeTypes.add(AsciiType.instance);
		         //compositeTypes.add(UTF8Type.instance);
		         //compositeTypes.add(UTF8Type.instance);
		         //compositeTypes.add(IntegerType.instance);
		         //CompositeType compositeType = CompositeType.getInstance(compositeTypes);
		         
		//        SSTableSimpleUnsortedWriter simpleUnsortedWriter = new SSTableSimpleUnsortedWriter(directory,partitioner, 
	//			keyspace,"summary",compositeType,null,32);
		       StringBuffer musor = new StringBuffer();
	     	 
		        for (int ii=0;ii<137;ii++) {
			    	  String nfilename = mfilename + "cdx_" +ii+".json";
			    	   br=new BufferedReader(new FileReader(nfilename));   
	     	           
			    	  String arch_name=(String) fmap.get(new Integer(ii));
			    	  System.out.println("filename:"+ nfilename+ " , archivename:"+arch_name );
			    	  
			    	  File oufile = new File("/smnt/proto_space/iipc/sum_flat/"+arch_name+ii+".txt");
			    	  if (!oufile.exists()) {
							oufile.createNewFile();
						}
			    	  FileWriter fw = new FileWriter(oufile.getAbsoluteFile());
			    	  BufferedWriter bw = new BufferedWriter(fw);
	     	          String line = null;
		              while ((line = br.readLine()) != null) {
		        	   if (line.length()>2) {
		        	//	"0.0-360.com/b2":{"mt":{"image":1},"t":{"748":1,"774":1,"800":1,"722":1},"ttl":1},	
		        		 //int j = line.lastIndexOf(",");
		        		 line = line.trim();
		        		 if (line.charAt(line.length()-1)==(',')) {
		        		 int j = line.lastIndexOf(",");
		        		 line = (String) line.substring(0, j);
		        		 }
		        		 System.out.println(nfilename+"line" +line);
		        		 JSONObject JObject =null;
		        		 try {
		        		  JObject = new JSONObject("{"+line+"}");	
		        		 
		     		    
		        		 
		        		 Iterator it = JObject.keys();
		        		 while( it.hasNext() ) {
		        		 
		        		 String url =	(String) it.next();
		        		 //System.out.println("keyss:"+url);
		        		  JSONObject jjobject = JObject.getJSONObject(url);
		        		  
		        		
		        		  
		        		 Iterator iter =  jjobject.keys();
		        		  while (iter.hasNext())
		        		  {
		        		      String key = (String) iter.next();
		        		     
							      if (key.equals("t")) {
							    	  JSONObject vslue;
		        		                vslue = jjobject.getJSONObject(key);
		        		                Iterator it2 = vslue.keys(); 
		        		                   while (it2.hasNext()) {
		        		                	String a = (String) it2.next();
		        		                	int nweek = Integer.parseInt(a);
		        		                	Long total =  vslue.getLong(a);
		        		                	int mcount=total.intValue();
		        		                	maker.write_record(bw,url, nweek, mcount);
		        		                	// maker.insert_record( simpleUnsortedWriter,compositeType, url, nweek,  mcount, arch_name);
		        		                     System.out.println(url+","+ a +"," +total);
		        		                    }
		        		                }//if
		        		  }//while
		        		 
		        		 
		        		}//while
		        		 
		        		 
		        	   }
		        		 catch (Exception e) {
			     				// TODO Auto-generated catch block
			     				musor.append(ii+","+line);
			     		} 
		        		 
		        		 
		     	            
		        	}
		        	
		        }
	         
	       bw.close();
	         
	      
	       }//for loop
	       // simpleUnsortedWriter.close();
	       
	        System.out.println("bad lines"+musor.toString());
	        
	        System.exit(0);
	        
	}
    catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	        
	        
	}
     
	public static Date addDaysToDate(final Date date, int noOfweeks) {
	    Date newDate = new Date(date.getTime());
	    int  noOfdays= noOfweeks*7;
	    GregorianCalendar calendar = new GregorianCalendar();
	    calendar.setTime(newDate);
	    calendar.add(Calendar.DATE, noOfdays);
	    newDate.setTime(calendar.getTime().getTime());

	    return newDate;
	}
	public static long addDaysToDate2(final Date date, int noOfweeks) {
	long thedate = date.getTime() + (86400L * 7L * 1000L)*noOfweeks;
	return thedate;
	 
	}
	
	public void write_record(BufferedWriter bw,String url,int nweek,int mcount) {
		  long timestamp0 =addDaysToDate2(sday,nweek);
		  Date newDate = new Date(timestamp0);
		  String wdate = hformatter.format(newDate);
		  String content = url +" "+wdate +" " +mcount;
		  try {
			bw.write(content+"\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
	}
	public void insert_record( SSTableSimpleUnsortedWriter simpleUnsortedWriter,CompositeType compositeType,String url,int nweek, int mcount,String _arc) {
		 
        long timestamp = System.currentTimeMillis();  
        long nanotimestamp = timestamp * 1000;
        try {
			simpleUnsortedWriter.newRow(bytes(url));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //String _arc = "bnf";
       // long timestamp0 = 1372321637000L;
        long timestamp0 =addDaysToDate2(sday,nweek);
        ByteBuffer date = bytes(timestamp0);
        ByteBuffer archive_id = bytes(_arc);
       // int mcount = 10;
        
        Builder builder =
      	      new CompositeType.Builder(compositeType);
      builder.add(date);
      builder.add(archive_id);
      builder.add(bytes("mcount"));
      System.out.println("here");
      simpleUnsortedWriter.addColumn(builder.build(),bytes(mcount), timestamp);
        
        
	}
	
}
