package gov.lanl.abbreviator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class MyJParser {

	public static void main(String[] argsArray) {
		// TODO Auto-generated method stub
		  String jsonText = null;
		  Args args = new Args( argsArray );
	        //need to add path to config
	        //System.setProperty( "ta.storage.basedir", args.get( "path", "target/db" ) );
	        //System.out.println("dir to"+System.getProperty("ta.storage.basedir"));
	      //  System.setProperty( "index", args.get("indexClass",  "gov.lanl.archive.index.cassandra.Index"));
	       // System.setProperty( "warcdir", args.get( "warcpath", "wa-db" ) );
	         String mfilename =  args.get("fname", "/smnt/proto_space/iipc/cdx-id-hash.json");
	         System.out.println(mfilename);
	       // int port = args.getNumber( "port", AggServer.DEFAULT_PORT ).intValue();
	         BufferedReader br = null;
	        
	    try  {
	    	br=new BufferedReader(new FileReader(mfilename));
	        StringBuilder sb = new StringBuilder();
	       // String line = br.readLine();
	        String line = null;
	        while ((line = br.readLine()) != null) {
             sb.append(line);
	            //sb.append('\n');
	          //  line = br.readLine();
	        
	        }
	        
	        jsonText = sb.toString().trim().trim();
	       // One answer JSONArray jArray = new JSONArray("["+result+"]");
	        System.out.println(jsonText);
	        JSONObject jobject=new JSONObject(jsonText);
	       
	        JSONArray mJsonArr = jobject.getJSONArray("list");
			//JSONArray mJsonArr = new JSONArray("["+jsonText+"]");
			
	        for (int i = 0; i <  mJsonArr.length(); i++) { 
	            JSONObject entry =  mJsonArr.getJSONObject(i); 
	            String ofile = entry.getString("file");
	           
	            ofile = ofile.substring(("/Volumes/LaCie/").length());
	            int k= ofile.indexOf("/"); 
	            ofile = ofile.substring(0,k);
	           
	            int id = entry.getInt("id");
                System.out.println (id+","+ofile);
	        }	        
	        
	        
			 //  Iterator<String> mIteratorList =
				//	   mJsonArr.getJSONObject(0).keys();
			   //String text = "Key-Value pairs: ";
			    //if (mIteratorList.hasNext())
                  // while (mIteratorList.hasNext()) {
                    //       String key = mIteratorList.next().toString();
                      //     System.out.println("key"+key);
                        //  System.out.println("value:" + mJsonArr.getJSONObject(0).get(key));
                          // text += "\n " + key + " -> " + mJsonArr.getJSONObject(0).(key);
                   //}
                  //else
                //{  text = "No iterators retrieved...";}
			    //br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}

	 public Map getMappings(String mfilename) {
		 Map map = new HashMap();
		  String jsonText = null;
		  BufferedReader br = null;
	        
		    try  {
		    	br=new BufferedReader(new FileReader(mfilename));
		        StringBuilder sb = new StringBuilder();
		       // String line = br.readLine();
		        String line = null;
		        while ((line = br.readLine()) != null) {
	             sb.append(line);
		            //sb.append('\n');
		          //  line = br.readLine();
		        
		        }
		        
		        jsonText = sb.toString().trim().trim();
		       // One answer JSONArray jArray = new JSONArray("["+result+"]");
		        System.out.println(jsonText);
		        JSONObject jobject=new JSONObject(jsonText);
		       
		        JSONArray mJsonArr = jobject.getJSONArray("list");
				//JSONArray mJsonArr = new JSONArray("["+jsonText+"]");
				
		        for (int i = 0; i <  mJsonArr.length(); i++) { 
		            JSONObject entry =  mJsonArr.getJSONObject(i); 
		            String ofile = entry.getString("file");
		           
		            ofile = ofile.substring(("/Volumes/LaCie/").length());
		            int k= ofile.indexOf("/"); 
		            ofile = ofile.substring(0,k);
		           
		            Integer id = entry.getInt("id");
	                System.out.println (id+","+ofile);
	                map.put(id, clean_archive_name(ofile));
	                
		        }	        
		        
		        
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		return map;
	}
	
	 public String clean_archive_name(String aname) {
		 //some naming discrepancies
		 if (aname.equals("bl")) {
			 return "blarchive";
		 }
		 		
		 if (aname.equals("lc")) {
			 return "loc";
		 }
		 return aname;
		 
	 }
	 
}
