package gov.lanl.agg.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import com.jolbox.bonecp.BoneCP;





public class AbbrSQLUtils {
	
	  BoneCP cPool= null;
	  public AbbrSQLUtils(BoneCP cPool) {
	    	this.cPool = cPool;
			
		    }
	  
	  
	
		    public String formatDate(String date) {
		        date = date.replace('T', ' ');
		        date = date.replace('Z', ' ');
		        date = date.trim();
		         return date;
		    }
	    
	       
		    
	public Map getArchivesInfo(String url, Date reqtime) {
		 Map map = new HashMap();
		 System.out.println("AbbrInfo from Cassandra");
		 //java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	
		// java.text.SimpleDateFormat  lformatter_utc = new java.text.SimpleDateFormat("yyyyMMdd HH:mm:ss");	
		 //String mysqlreqdate = formatter_utc.format(reqtime);
		//check that thing in cache
		   Statement s = null;
		   PreparedStatement st = null;
		   PreparedStatement st1 = null;
		   //different strategy -fisrst select upper boundary
		   //interval [a b]->
		  // [ omiiting boundary a logic now ] 
		   //boundary a
			//String sqlp = "select mdate,url,archive_id,mcount from summary where url = ? " +
				//	" and  mdate  <= ?  order by mdate desc limit 1; ";
			//use mdate from boundary a equal desc or asc doesnot matter.
			//String sqll = "select mdate,url,archive_id,mcount from summary where url = ? " +
				  //   "  and  mdate=? order by mdate ; ";
			
			//select boundary b with limit
			String sqlf = "select mdate from summary where url = ? " +
					     "  and  mdate >= ? order by mdate asc limit 1; ";
			//use mdate from boundary b without limit equal desc or asc doesnot matter.
			String sqll = "select mdate,archive_id,mcount from summary where url = ? " +
				     "  and  mdate = ?  ; ";
			
			
		   Connection conn = null;
		   ResultSet rs0 = null;
		   ResultSet rs = null;
			
		  // if (checkCache(url, reqtime)) {
			    
		      //  System.out.println("in cache");
			    try {
					//conn = startConnection();
					 conn = cPool.getConnection();
					// s = conn.createStatement();
						st = conn.prepareStatement(sqlf);				
						st.setString(1,url);
						st.setLong(2,reqtime.getTime());
						//st.setString(2,url);
						rs0 = st.executeQuery();
			  
			           if (rs0.next()) {
				               String mdate = null;
				               java.sql.Date _date = rs0.getDate(1);
				               
				                System.out.println("cassandra mdate="+_date);
				               
				                st1 = conn.prepareStatement(sqll);				
								st1.setString(1,url);
								st1.setDate(2, _date);
							    rs = st1.executeQuery();
							      while( rs.next()) {
							    	   Date d_update = rs.getTimestamp(1);
					    	          // System.out.println("Date:" +d_update);
					    	           String arch_id = rs.getString(2);
					    	          //ommit date for now
					    	           System.out.println("cassandra Date:" +d_update+","+arch_id);
					    	           Integer count = rs.getInt(3);
					    	           map.put(arch_id, count);			    	   			    	   
					                  }
							    
							    
				              
				                 }
			        
			                  
			    		            
			 return map;
			 
		   } catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			    finally {
			    	       try {
			    	            if (rs0 != null) rs0.close();
			    	            if (rs != null) rs.close();
			    	            if (st != null) st.close();
			    	            if (st1 != null) st1.close();
			    	            if (conn != null) conn.close();
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                 }//finally
		                 
						
	       // }//cache if?
		 return null;
		   		
	}


	
	
	
}
