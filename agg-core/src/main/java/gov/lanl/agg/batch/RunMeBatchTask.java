package gov.lanl.agg.batch;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.CacheStats;
import gov.lanl.agg.Link;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;
import gov.lanl.batchsync.URLPostClient;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.PriorityBlockingQueue;

import com.jolbox.bonecp.BoneCP;
/*
@author Lyudmila Balakireva

*/
public class RunMeBatchTask implements RunMeBatch {
	BoneCP cPool= null;
	 URLPostClient poclient=null;
		    public RunMeBatchTask(BoneCP cPool) {
		    	this.cPool = cPool;
				
			    }
		  	
		    
		    public void setURLPostClient(URLPostClient client) {
		    	
			      poclient = client;	 
			     }
		    
		    public void fupdateArchives(List tmlist,List tglist,Map<String,Integer> mymap) {
		    	   //Statement s = null;
				   //Connection conn = null;
				   //ResultSet rs0 =null;
				   try (Connection conn = cPool.getConnection(); 
						   Statement  s = conn.createStatement();) {
			    		
			    		 conn.setAutoCommit(false);
			    		    // s = conn.createStatement();
				   for (String key: mymap.keySet()) {
						 Integer in = mymap.get(key);
						
						 String tmgate = (String) tglist.get( in.intValue());
						 String tmmap = (String) tmlist.get( in.intValue());
				         String sql1 ="insert  into archive_register(hostname,timegate,timemap)"+ 
						 " values ('"+key+"','"+tmgate+"','"+ tmmap +"') ON DUPLICATE KEY UPDATE " +
						 " hostname='"+key+"',timegate='"+tmgate+"',timemap='"+ tmmap +"', id=LAST_INSERT_ID(id);";
				         System.out.println("sql1" +sql1);
				          int status = s.executeUpdate(sql1);	
				          conn.commit();
					 }
				   }
				   catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    	
			    }
					
		  public ArchiveDescription  getArchiveInfo(String archive_id) {
		    	String sql = "select timegate,timemap,name,cal_page from archive_register where hostname=?;";
		    	ArchiveDescription ad = null;
		    	 try (  Connection conn = cPool.getConnection();
		                PreparedStatement st = conn.prepareStatement(sql);
		             ){
		    		 ResultSet rs0 = st.executeQuery();
		             while (rs0.next()) {
		            	  ad = new ArchiveDescription();
		            	  String timegate = rs0.getString(1);
		            	  String timemap = rs0.getString(2);
		            	  String name = rs0.getString(3);
		            	  String cal_page =rs0.getString(4);
		            	  ad.setTimegate(timegate);
		            	  ad.setName(archive_id);
		            	  ad.setCalendarUrl(cal_page);
		            	  ad.setLongname(name);
		             }
		    	 }
		    	 catch (Exception e) {
		             // TODO Auto-generated catch block
		             e.printStackTrace();
		         }
		    	 
		    	return ad;
		     }
		    
		    //now not only native archive, but new  archive from batch archive can be added
		    public void addArchive(String hostname,String timegate,String timemap,String name,String cal_page) {
		    	 
		    	   int count = 0;
			    	String sql ="select count(*) from  archive_register where hostname='" + hostname + "'; ";
			    	try (  Connection conn = cPool.getConnection();
			    			 Statement  s = conn.createStatement();) {
			    		// conn = startConnection();
			    		/// conn = cPool.getConnection();
			    		 conn.setAutoCommit(false);
			    		     //s = conn.createStatement();
			    		     ResultSet rs0 = s.executeQuery(sql);
			    		     System.out.println(sql);
						      while (rs0.next()) {
							  count = rs0.getInt(1); 
							  
						 }
						    rs0.close();
						    if (count==0) {
			                     if (name==null) {
			                    	 name=hostname;
			                     }
			                     if (cal_page==null) {
			                    	 cal_page="";
			                     }
						    	String sql1 ="insert  into archive_register(hostname,timegate,timemap,name,cal_page)"+ 
										 " values ('"+hostname+"','"+timegate+"','"+ timemap +"','"+name+"','"+cal_page+"') ON DUPLICATE KEY UPDATE " +
										 " hostname='"+hostname+"',timegate='"+timegate+"',timemap='"+ timemap +"',name='"+name+"' ,cal_page='"+cal_page+"', id=LAST_INSERT_ID(id);";						      									 
								          System.out.println("sql1" +sql1);
								          int status = s.executeUpdate(sql1);
						    }
					     
						    conn.commit(); 
					 
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		    
		    public void updateArchives(List tmlist,List tglist,Map<String,Integer> map) {
		    	  //Statement s = null;
				  // Connection conn = null;
				  // ResultSet rs0 =null;
				  
				   int count = 0;
		    	String sql ="select count(*) from  archive_register; ";
		    	try (  Connection conn = cPool.getConnection();
		    			 Statement  s = conn.createStatement();) {
		    		// conn = startConnection();
		    		/// conn = cPool.getConnection();
		    		 conn.setAutoCommit(false);
		    		     //s = conn.createStatement();
		    		     ResultSet rs0 = s.executeQuery(sql);
					 while (rs0.next()) {
						  count = rs0.getInt(1); 
						  
					 }
					    rs0.close();
					 if (count<tmlist.size()) {
						 //do updates;
						 for (String key: map.keySet()) {
							 Integer in = map.get(key);							
							 String tmgate = (String) tglist.get( in.intValue());
							 String tmmap = (String) tmlist.get( in.intValue());
					         String sql1 ="insert  into archive_register(hostname,timegate,timemap)"+ 
							 " values ('"+key+"','"+tmgate+"','"+ tmmap +"') ON DUPLICATE KEY UPDATE " +
							 " hostname='"+key+"',timegate='"+tmgate+"',timemap='"+ tmmap +"', id=LAST_INSERT_ID(id);";						      									 
					         System.out.println("sql1" +sql1);
					          int status = s.executeUpdate(sql1);
					          
						 }
						conn.commit(); 
					 }
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    			
		    }
		    
	  public Map selectJob () {
		  Map m = new HashMap();
		  // Statement s = null;
		  // Connection conn = null;
		   ResultSet rs0 =null;
		   String url="";
		   
		  // PreparedStatement pst = null;
		   //String sql = "select id,url from jobs where process_id is null order by id asc limit 1 FOR UPDATE;";
		   String sql = "select id, url, priority from jobs where process_id is null order by priority, id asc limit 1;";
		   String sqlp = "update jobs set process_id = ? where  process_id is null and url = ?;";
			
		   try ( Connection conn = cPool.getConnection();
				 Statement s = conn.createStatement(); 
				   PreparedStatement pst = conn.prepareStatement(sqlp);)
				 {
			
			   conn.setAutoCommit(true);
		       rs0 = s.executeQuery(sql);
		
		       Integer id=0;
		       String priority;
			 while (rs0.next()) {
				    id = rs0.getInt(1); 
				    url = rs0.getString(2);
				    priority = rs0.getString(3);
				    if (rs0.wasNull()) {
				    	priority = "1";
				    	} 
				 m.put("url", url);
				 m.put("id", id);
				 m.put("priority", priority);
			 }
		    rs0.close();
		    //all urls in the table mark as  
		    if (id!=0) {
		    //String sql2 = "update jobs set process_id = " +id + " where  process_id is null and url = '"+url+ "';";
			//int st = s.executeUpdate(sql2);
			   pst.setInt(1, id);
			   pst.setString(2, url);
			   pst.execute();
		    }
		         
		  
	  } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
		 return m;  
	  }
	
	  
	

	  
	  
	  
	  public Map getLastMementos(String url) {
		  url= MementoUtils.RemoveProtocol (url);
		  Map map = new HashMap();
		// Connection conn = null;
		// ResultSet rs0 =null;
		 //PreparedStatement check=null;
		 String sql="select hostname,max(mdate) from links a, archive_register b where a.archive_id=b.id and a.id=md5(?) group by hostname";
		 try (  Connection conn = cPool.getConnection(); 
				 PreparedStatement check = conn.prepareStatement(sql);
				 ) {
				//conn = cPool.getConnection();
				//check = conn.prepareStatement(sql);				
				check.setString(1,url);
				ResultSet rs = check.executeQuery();
				 while(rs.next()) {
					   // String Date = rs.getString("updtime");
					 String name = rs.getString(1);
					 Date   d_update = rs.getTimestamp(2);
					 map.put(name,d_update);
					}
					rs.close();	
		 
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 return map;
	  }
	  
	  
	  public long check_Que(){
		  long q_count=0;
		  String sql = "select count(*) from jobs;";
		  try (  Connection conn = cPool.getConnection();
					PreparedStatement check = conn.prepareStatement(sql);		) {
			  ResultSet rs = check.executeQuery();
			  while(rs.next()) {
				 q_count = rs.getLong(1);
			  }
	      } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 	
		  
		return q_count;  
		  
	  }
	  public Date checkLastUpdate(String url) {
		  url= MementoUtils.RemoveProtocol (url);
		 // Statement s = null;
		  // Connection conn = null;
		 //  ResultSet rs0 =null;
		  // PreparedStatement check=null;
		   String sql ="select cast(updtime as CHAR)  from linkmaster " +
			  		"where id = md5(?);";
			Date d_update=null;
			try (  Connection conn = cPool.getConnection();
					PreparedStatement check = conn.prepareStatement(sql);		) {
				//conn = cPool.getConnection();
				//check = conn.prepareStatement(sql);				
				check.setString(1,url);
				ResultSet rs = check.executeQuery();
				
				while(rs.next()) {
				   // String Date = rs.getString("updtime");
					 
					 String  _datestr=rs.getString(1);
					 if (_datestr!=null) {
						 _datestr=_datestr+" GMT";
	    		    	// System.out.println("second qstr:"+rowcount+","+_datestr);
	    		    	 //System.out.println("first q"+rowcount+","+_datestr);				 
					     d_update = MementoUtils.formatter_db.parse(_datestr);
					 }
					 
				   // d_update = rs.getTimestamp(1);
				}
				rs.close();			  
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 	
			return d_update;
	  }
	  
	  public Date checkLastRequest(String url) {
		  url= MementoUtils.RemoveProtocol (url);
		
		   String sql ="select cast(reqtime as CHAR)  from linkmaster " +
			  		"where id = md5(?);";
			Date d_update=null;
			try (  Connection conn = cPool.getConnection();
					PreparedStatement check = conn.prepareStatement(sql);		) {
							
				check.setString(1,url);
				ResultSet rs = check.executeQuery();
				
				while(rs.next()) {
				    
					 String  _datestr=rs.getString(1);
					 if (_datestr!=null) {
						 _datestr=_datestr+" GMT";
	    		    				 
					     d_update = MementoUtils.formatter_db.parse(_datestr);
					 }
					    
				}
				rs.close();			  
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		 	
			return d_update;
	  }
	  
	  
	  
	  //delete Q daily
	  public void cleanQue() {
		  String sql="delete from jobs where process_id is not null and compltime < (SUBDATE(now(), INTERVAL 3 month));"; 
		  try ( Connection conn = cPool.getConnection();
				 
				  PreparedStatement s = conn.prepareStatement(sql);){
			      s.executeUpdate(sql);
		  }
		  catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  }
	  
	  public void rearangeQue() {
		
		  try ( Connection conn = cPool.getConnection();
				  CallableStatement cs =  conn.prepareCall("{call rearrange_que}");
				  ){
			      cs.executeQuery();
		  }
		  catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  }
	  
	  
	  
	 //delete
	  
	  public void cleanCache() {
		  //need index on updtime and reqtime
		 String sqlc = "select FLOOR((count(*) /100)*10) from linkmaster where  updtime <  (SUBDATE(now(), INTERVAL 2 week)) and reqtime < (SUBDATE(now(), INTERVAL 2 week)) ;"; 
		 //should I add updtime = '1996-12-31'
		 //String sql= "select url from linkmaster where  updtime <  (SUBDATE(now(), INTERVAL 2 week))  and reqtime < (SUBDATE(now(), INTERVAL 2 week)) order by numreq limit ?; ";
		 String sql= "select url from linkmaster where  updtime <  (SUBDATE(now(), INTERVAL 12 week))   limit 40000; ";
			
		 //check this
		 String sqlm="delete from linkmaster  where id=md5(?);";
		 String sqlj="delete from jobs where url=?;";
		 String sqll="delete from links where id = md5(?);";
				 
		  // Connection conn = null;
		  // ResultSet rs0 =null;
		  // PreparedStatement tcount = null;
		   //PreparedStatement s = null;
		   //PreparedStatement deletem = null;
		  // PreparedStatement deletej = null;
		   //PreparedStatement deletel = null;
		   
		 try ( Connection conn = cPool.getConnection();
				// PreparedStatement tcount = conn.prepareStatement(sqlc);
				 PreparedStatement s = conn.prepareStatement(sql);
				 PreparedStatement deletem = conn.prepareStatement(sqlm);
				 PreparedStatement deletej = conn.prepareStatement(sqlj);
				 PreparedStatement deletel = conn.prepareStatement(sqll);
				 ){
	    	 //conn = startConnection();
			 //conn = cPool.getConnection();
			 conn.setAutoCommit(false);
			 //tcount = conn.prepareStatement(sqlc);
			// s = conn.prepareStatement(sql);
			// deletem = conn.prepareStatement(sqlm);
			// deletej = conn.prepareStatement(sqlj);
			// deletel = conn.prepareStatement(sqll);
			 
			/* ResultSet rs0 = tcount.executeQuery();
	    	 int limnum = 0;
	    	  while (rs0.next()) {
	    		  limnum = rs0.getInt(1);
	    		  System.out.println("top urls number" +limnum);
	    	  }
	         	  
	    	  s.setInt(1, limnum);
	    	  rs0.close();
			  */
			 ResultSet  rs0 = s.executeQuery();
			 
			 String url="";
		       while (rs0.next()) {
			       url = rs0.getString(1);
			       deletem.setString(1,url);
			       deletej.setString(1,url);
			       deletel.setString(1,url);
			       
			       deletem.executeUpdate();
			       deletej.executeUpdate();
			       deletel.executeUpdate();
			       conn.commit();
			    									
		             }
	    	  
		  }
		    catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			   
			 
			 }
	  
	  
	 //extra thread to update timemaps if they stale
	  public void populateJobs() {
		   TimeZone tz = TimeZone.getTimeZone("GMT");
		   Calendar c = new GregorianCalendar(tz);
		   c.setTime(new Date()); 
		   Date dor = c.getTime();
		  // c.add(Calendar.WEEK_OF_MONTH,-2);
		  // Connection conn = null;
		  // ResultSet rs0 =null;
		  // PreparedStatement insertjobs = null;
		   //PreparedStatement s = null;
		   //PreparedStatement q = null;
		   //PreparedStatement tcount = null;
		  // PreparedStatement top = null;
		   
		 //ok defect that I can delete proccess_id already in Q. but if i check for last update and did not process those no harm. 
		   String sqli1 = "insert  into  jobs (url,hashkey,reqtime,priority)   select "
		   		+ "  url,id,reqtime,'2' from linkmaster where " 
		   		+  " updtime <  (SUBDATE(now(), INTERVAL 2 week)) on duplicate key update priority='2', process_id = NULL;";
	
		   /*
		   
		   
		   String sql = "select url from linkmaster where numreq>4 " +
				   " and (updtime is null or updtime < ( SUBDATE(now(), INTERVAL 2 week)));";
		   		   
		   //new relative aproach
		  // String  sqli1 = "insert into  jobs (url,reqtime,priority) values (?,?,'2');";	
		   // String sqli1 = "update jobs set process_id = NULL, priority='2' where url in (select url from linkmaster where " +
			//	   "updtime <  (SUBDATE(now(), INTERVAL 2 week)) order by updtime asc ) and reqtime < ( (SUBDATE(now(), INTERVAL 2 week)) and compltime < ( (SUBDATE(now(), INTERVAL 2 week));";
		   //first calculate count
		   String sqlc = "select FLOOR((count(*) /100)*20) from linkmaster where updtime is null or updtime <  (SUBDATE(now(), INTERVAL 2 week));";
		   String sqlp = "select url,reqtime from linkmaster where " +
				   					" updtime is null or " +
				    " updtime <  (SUBDATE(now(), INTERVAL 2 week)) order by numreq asc limit ?;";
				    */
		    //s = conn.prepareStatement(sql);
		    //insertjobs = conn.prepareStatement(sqli1);
		   // java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//	formatter_utc.setTimeZone(tz);
			//String rdate = formatter_utc.format(dor);
		    try (   Connection conn = cPool.getConnection();
		    		  PreparedStatement insertjobs = conn.prepareStatement(sqli1);
		    		 // PreparedStatement tcount = conn.prepareStatement(sqlc);
		    		 // PreparedStatement top = conn.prepareStatement(sqlp);
		    		){
		    	 //conn = startConnection();
				 //conn = cPool.getConnection();
				 conn.setAutoCommit(true);
				// insertjobs = conn.prepareStatement(sqli1);
		    	// s = conn.prepareStatement(sql);
		    	// tcount = conn.prepareStatement(sqlc);
		    	// top = conn.prepareStatement(sqlp);
				 /*
				 ResultSet  rs0 = tcount.executeQuery();
		    	 int limnum = 0;
		    	  while (rs0.next()) {
		    		  limnum=rs0.getInt(1);
		    		  System.out.println("top urls number" +limnum);
		    	  }
		    	  top.setInt(1, limnum);
		    	  rs0.close();
				  rs0 = top.executeQuery();
				 
				  String url="";
			       while (rs0.next()) {
				       url = rs0.getString(1);
			       
			       
				       insertjobs.setString(1,url);
				       insertjobs.setString(2,rdate);
				       insertjobs.executeUpdate();
				       // System.out.println("from select jobs;"+id+"|"+url);
				   // q.add(id+"|"+url);									
			             }
			       
			       */
			       insertjobs.executeUpdate();
		    }
		    catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			  
			    	 		    
	  }
	 
	  public void rollbackJobs(PriorityBlockingQueue q) {
		  String tmp;
		  String sql = "update jobs set process_id = NULL  where  process_id = ?  and hashkey =md5(?);";
		  while((tmp = (String) q.poll()) != null) {
			tmp = tmp.substring(1); 
			String process_id = tmp.substring(0,tmp.indexOf("|"));   
			System.out.println(process_id);
			String urlString=tmp.substring(tmp.indexOf("|")+1);
			  // urlString = RemoveProtocol (urlString); ??
			  try (   Connection conn = cPool.getConnection();
					  PreparedStatement updatejobs = conn.prepareStatement(sql);					  					 
				){
				  updatejobs.setInt(1, Integer.parseInt(process_id));
		          updatejobs.setString(2, urlString);		
	              int a = updatejobs.executeUpdate();
				  
				  
			  } catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			
		  }
		
		 
	  }
	  
	  /*
	   * (non-Javadoc)
	   * @see gov.lanl.agg.batch.RunMeBatch#selectJobs(java.util.concurrent.BlockingQueue)
	   * 
	   *  //jobs table structure
		   // id auto_increment not null primary key,
		   // url varchar(1024),
		    //reqtime DATETIME,
		    //process_id INT null default null
		    //compltime DATETIME
	   */
	  public void selectJobs (PriorityBlockingQueue q) {
		  //aprox now date
		  TimeZone tz = TimeZone.getTimeZone("GMT");
		  Calendar c = new GregorianCalendar(tz);
		  //Calendar c = Calendar.getInstance(); 
		  c.setTime(new Date()); 
		  //c.add(Calendar.HOUR_OF_DAY , -2);
		  //c.add(Calendar.DAY_OF_MONTH,-2);
		 // c.add(Calendar.DAY_OF_MONTH,-14);
		  
		   c.add(Calendar.MINUTE, 10);
		   Date now =  c.getTime();
		   c.add(Calendar.MONTH, -6);
		   Date yearold=c.getTime();
				  
		   java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		   formatter_utc.setTimeZone(tz);
		 
		   String sql =   "select id, url, priority,reqtime from jobs where process_id is null order by priority,id asc limit 30;";
		   //String psql =  "update jobs set process_id = ? where  process_id is null and hashkey =md5(?);";
		   String psql =  "update jobs set process_id = ? where  id = ?;";
 //String psql1 = "update jobs set process_id = ?, compltime=? where  process_id is null and url = ?;";
		  // String psql1 = "delete from jobs where process_id is null and hashkey =md5(?);";
		   String psql1 = "delete quick from jobs where id = ?;";
		
		   try (  
				   Connection conn = cPool.getConnection();
				   PreparedStatement updatejobs = conn.prepareStatement(psql);
				   PreparedStatement deletejobs= conn.prepareStatement(psql1);
				   PreparedStatement s =conn.prepareStatement(sql);
				   
				   ) {
		
			conn.setAutoCommit(true);
		 
		   // int count = 1200-q.size(); //eto teper ne sovsem tochno
			//System.out.println("count:"+count);
		    //for (int i=1;i<count;i++){
			    while(q.size()<1185) {
		    	 ResultSet rs0 = s.executeQuery(sql);
		         Integer id=0;
		         String url="";
		         String priority="2";
			       while (rs0.next()) {
				         id = rs0.getInt(1); 
				         url = rs0.getString(2);
				         priority = rs0.getString(3);
				         
				         if (rs0.wasNull()) {
						    	priority = "1";
						    	}
				         String reqtime = rs0.getString(4);
			           //  } //april8 2016 change
		               // rs0.close();
		     System.out.println("select job"+url);
		       if (!url.equals("")) {
		        Date m = checkLastUpdate(url);
		        Date r = checkLastRequest(url);
		        
		        Boolean remove = false;
		       
		        if (r==null||r.before(yearold)){
		    	 remove = do_eviction(url);
		         }
		        
		     
		             if( (m==null || m.before(now))&& remove==false){		    	
		    	     updatejobs.setInt(1,id);
		    	     updatejobs.setInt(2,id);
		          	
	                 int a = updatejobs.executeUpdate();
	                 System.out.println("update case,priority:"+priority+","+url);
	                 q.put(priority+id+"|"+reqtime+"|"+url); 
		    	 
		             }
		            else {
		    	    
		    	    System.out.println("second recent update/delete case:"+url);
		    	
	        	    // String mdate =  formatter_utc.format(m);
	        	
	        	    deletejobs.setInt(1,id);
		          
		            int a = deletejobs.executeUpdate();
		            // System.out.println("update status:"+a);
		            //conn.commit();
		            /*  if (poclient!=null) {
                  	 String resdate = MementoUtils.timeTravelJsFormatter.format(now);
                  	//no need to send results 
                  	  System.out.println("batch send repeat:"+url);
                      poclient.resultsurl_put(url, resdate);
                      }
		             else {
		            	 System.out.println("poclient is null");
		             }
				  */
		          // count=count-1;
		           }
		     
		        } //url
		     }
             rs0.close();
		    }
		    //conn.commit();
	  } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
		
	  }
	
	  /*
	   * (non-Javadoc)
	   * @see gov.lanl.agg.batch.RunMeBatch#selectJobs(java.util.concurrent.BlockingQueue)
	   * 
	   *  //jobs table structure
		   // id auto_increment not null primary key,
		   // url varchar(1024),
		    //reqtime DATETIME,
		    //process_id INT null default null
		    //compltime DATETIME
	   */
	  
	  public boolean do_eviction(String url){
		  url= MementoUtils.RemoveProtocol (url);
		  String sql="select distinct hostname from links a, archive_register b where a.id=md5(?) and b.id=a.archive_id";
		  		           
		  try (   Connection conn = cPool.getConnection();
				  
				  PreparedStatement s = conn.prepareStatement(sql);
					){
			        conn.setAutoCommit(true);
			        
			        s.setString(1,url);
					ResultSet rs = s.executeQuery();
					 int count = 0;
					 String name="";
					 while(rs.next()) {
						  name = rs.getString(1);
						  count = count+1;
						}
						rs.close();	
			        
			         if (count==1 && name.equals("ia")){
			        	 deleteLinkmaster(url);
			        	 deleteAllLinks(url);
			        	 return true;
			         }
			  
		   }
			  catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		  
		  
		  return false;
	  }
	
	  public void updateStats(CacheStats cs) {
		  Date d = new Date();
		  String dd = MementoUtils.formatter_sdb.format(d)+"-00 00:00:00";
		  String service = cs.getService();
		           int a = cs.resetHits();
		           int b = cs.resetStale();
		           int c = cs.resetMiss();
		           
		         //  System.out.println("service:"+service);
		           
		           //System.out.println("stats:"+a+","+b+","+c);
		           //System.out.println(dd);
		  String sql = 
				"insert into cache_stats (pdate,service,hits,stale,miss) "+
		         " values ('"+dd+"', '"+service+"',"+a+","+b+","+c+") ON DUPLICATE KEY UPDATE "
		         		+ "  hits=hits+"+a+", stale=stale+"+b+", miss=miss+"+c+";";
		  System.out.println(sql);
		
		  try (   Connection conn = cPool.getConnection();
				  Statement s = conn.createStatement();
					){
			        conn.setAutoCommit(true);
			  s.executeUpdate(sql);  
			  
		   }
			  catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 
			 
		  
		  
	  }
	  
	  public void updateJobs (String process_id,String ctime) {
		  //I am going to substitute it to delete in future
		 // Statement s = null;
		  // Connection conn = null;
		  //Statement s = null;
		  //String sql2 = "update jobs set compltime ='"+ctime+"' where process_id = "+process_id+";";
		  String sql2 = "delete quick from jobs  where process_id = "+process_id+";";
		  // PreparedStatement pst = null;
		try (   Connection conn = cPool.getConnection();
				 Statement s = conn.createStatement();
				
				){
			// conn = startConnection();
			// conn = cPool.getConnection();
			 conn.setAutoCommit(true);
			// s = conn.createStatement();
			 s.executeUpdate(sql2);
			 //conn.commit();
			//pst = conn.prepareStatement(sql2);
		
		   //pst.setString(2,process_id);
		   //pst.setString(1,ctime );
		   //pst.execute();
		   
	  } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	  }
	  
	  public void deleteJobs (String process_id,String ctime) {
		  //I am going to substitute it to delete in future
		 // Statement s = null;
		  // Connection conn = null;
		  //Statement s = null;
		  String sql2 = "delete quick from jobs  where process_id = "+process_id+";";
		 // PreparedStatement pst = null;
		try (   Connection conn = cPool.getConnection();
				 Statement s = conn.createStatement();
				
				){
			// conn = startConnection();
			// conn = cPool.getConnection();
			 conn.setAutoCommit(false);
			// s = conn.createStatement();
			 s.executeUpdate(sql2);
			 conn.commit();
			//pst = conn.prepareStatement(sql2);
		
		   //pst.setString(2,process_id);
		   //pst.setString(1,ctime );
		   //pst.execute();
		   
	  } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	  }
	  
	  public void updateLinkmaster (String url,String ctime,String status) {
		  
		  //I am going to substitute it to delete in future
		 // Statement s = null;
		 // Connection conn = null;
		  //? if archive makes available records retroactivly not accurate logic. 
		  //String sql2 = "update linkmaster set updtime ='"+ctime+"' where id = md5('"+url+"');";
		  
		  url= MementoUtils.RemoveProtocol (url);
		  String sqlp = "update linkmaster set updtime =?, status=? where id = md5(?);";
		 // PreparedStatement pst = null;
		try ( Connection conn = cPool.getConnection();
				// Statement s = conn.createStatement();
				PreparedStatement pst = conn.prepareStatement(sqlp);
				){
			//conn = startConnection();
			//conn = cPool.getConnection();
			 //s = conn.createStatement();
			// conn.setAutoCommit(false);
			 conn.setAutoCommit(true);
			//  s.executeUpdate(sql2);
			//pst = conn.prepareStatement(sql2);
		     //conn.commit();
			 pst.setString(1,ctime ); 
			 if (status==null) {
			     pst.setNull(2,java.sql.Types.CHAR);
			 }
			 else {
				 pst.setString(2,status);
			 }
		     pst.setString(3,url);
		     pst.execute();
		   //conn.commit();
	  } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
	  }
	
  public void deleteLinkmaster (String url) {
		  		  
		  url= MementoUtils.RemoveProtocol (url);
		  String sqlp = "delete from linkmaster  where id = md5(?);";
		 
		try ( Connection conn = cPool.getConnection();	
				PreparedStatement pst = conn.prepareStatement(sqlp);
				){
		
			 conn.setAutoCommit(true);
			
		     pst.setString(1,url);
		     pst.execute();
		   
	  } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
	  }
	
	  
	  
	  public void updateLinkSummary(String url,String hostname,String ctime,String code,String total){
		       url = MementoUtils.RemoveProtocol (url);
		   
		   String  psql = " insert into linksummary (url,hostname,id,updtime,code,total) " +
                   " values(  ?, ? , md5(?), ?,?,?) " +
                  "  ON DUPLICATE KEY UPDATE updtime=?,code=?,total=?; "; 
     
		   try (   Connection conn = cPool.getConnection();
				   PreparedStatement slinks = conn.prepareStatement(psql);)
				   {
			         conn.setAutoCommit(true); 
			          slinks.setString(1,url);
			          slinks.setString(2,hostname);
			          slinks.setString(3,url);
			          slinks.setString(4,ctime);
			          slinks.setString(5, code);
			          slinks.setString(6, total);
			          slinks.setString(7,ctime);
			          slinks.setString(8, code);
			          slinks.setString(9, total);
			          int a = slinks.executeUpdate();
			          
				   }
		   catch (Exception e) {
				  
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   
	  }
	  
	  public void deleteAllLinks(String url){
		  url = MementoUtils.RemoveProtocol (url);
		  String dsql= "delete from links where id=md5(?) ";
		  try (  
				   Connection conn = cPool.getConnection();
				  // PreparedStatement marklinks = conn.prepareStatement(csql);
				   PreparedStatement updatelinks = conn.prepareStatement(dsql); 
				  // PreparedStatement deletelinks = conn.prepareStatement(dsql); 
				   ){
			  conn.setAutoCommit(true);  
			  updatelinks.setString(1,url);
			  updatelinks.executeUpdate();
		   }
		   catch (Exception e) {
						  
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	  
		  
	  }
	  
	  public List updateAllLinks(String url,Map <String,List>mresult) {
		  url= MementoUtils.RemoveProtocol (url);
		  List archives = new ArrayList();
		   Statement s = null;
		   //Connection conn = null;
		   //PreparedStatement updatelinks=null;
		   TimeZone tzo = TimeZone.getTimeZone("GMT");
		   ThreadSafeSimpleDateFormat formatter_utc = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		   formatter_utc.setTimeZone(tzo);
		   ThreadSafeSimpleDateFormat  httpformatter = new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");	
		   httpformatter.setTimeZone(tzo);
		       // insert ignore can be changed to replace ignore
		   // String psql =  "INSERT IGNORE  into links (id,mdate,archive_id,href,type,rel,part) " + 	        
		     //       	"select md5(?), ?, a.id , ?,?,?, 0 " +
			   //         		" from archive_register a where hostname = ?;";
				  
		        //String psql=  "insert  into links (id,mdate,archive_id,href,type,rel,part) " + 	        
		          //  	"select md5(?), ?, a.id , ?,?,?, 0 " +
		            //	" from archive_register a where hostname = ?  and (select count(*) from links where id=md5(?) and mdate=? and archive_id=a.id)=0;";
		        
		    //insert update
		     String csql = "update links set status='D' where id=md5(?);";
		      
		    // String  psql = " insert into links (id,mdate,archive_id,href,type,rel,part,status) " +
		      //             "select md5(?), ?, a.id , ?,?,?, 0, NULL" +
	            //           " from archive_register a where hostname = ? ON DUPLICATE KEY UPDATE ";
		     String  psql = " insert ignore into links (id,mdate,archive_id,href,type,rel,part,status)   " +
	             "select md5(?), ?, a.id , ?,?,?, 0, NULL " +
                      " from archive_register a where hostname = ? limit 1 ON DUPLICATE KEY UPDATE href=values(href),status=NULL "; 
                
		     String dsql= "delete from links where id=md5(?) and status='D'";
		    //1 url
		    //2 mysqldate
		    //3href
		    //4type
		    //5 rel
	       //6 hostname
		    
		   try (  
				   Connection conn = cPool.getConnection();
				   PreparedStatement marklinks = conn.prepareStatement(csql);
				   PreparedStatement updatelinks = conn.prepareStatement(psql); 
				   PreparedStatement deletelinks = conn.prepareStatement(dsql); 
				   ) {
			
				//conn = cPool.getConnection();
				   conn.setAutoCommit(false);
			   // conn.setAutoCommit(true);
			   //List<Link> links = header.getLinks();
				// updatelinks = conn.prepareStatement(psql); 
			    System.out.println("resultset from archives:"+mresult.size());
			    marklinks.setString(1,url);
			    marklinks.executeUpdate();
			  //  conn.commit();
			   int count=0;
			   int vcount = 0;
			   for ( String host : mresult.keySet()) {
				   
			               List<Link> links = mresult.get(host);
			               
				          // System.out.println("for"+ host + "got links" +links.size()+"for url:"+url); 
				              
			                  for (Link f : links) {
			                	  vcount = vcount+1;
				 //timemap paging need to be implemented 
				                         if (f.getDatetime()!=null) {
				                        	 count=count+1;
				    	                           String type="";
				    	                           if (f.getType() != null) {
				    		                       type = f.getType();
				    	                           }
				                            String datestr = f.getDatetime();
				                           
				                            // System.out.println("datestr:"+datestr+","+f.getHref());
				                            Date d = httpformatter.parse(datestr);
				                            
				                            String mysqldate = formatter_utc.format(d);
				                            //SimpleDateFormat df = new SimpleDateFormat("yyyy");
				                            //String year = df.format(d);
				                                 //  if ( d.before(new Date("2006-09-19"))){
				                        		  // System.out.println("datestr:"+datestr+","+f.getHref());
				                            
				                                    //}
				                            //int st = s.executeUpdate(sql);
				   	                         updatelinks.setString(1,url);
					                         updatelinks.setString(2,mysqldate);
					                         updatelinks.setString(3,f.getHref());
					                         //System.out.println("from update links:" +f.getHref());
					                         updatelinks.setString(4,type);
					                         updatelinks.setString(5,f.getRelationship());
					                         updatelinks.setString(6,host);
					                         //updatelinks.setString(7,f.getHref());
					                         // updatelinks.setString(7, url);
					                         // updatelinks.setString(8, mysqldate);
					                         //System.out.println(updatelinks);
					                         //temporary close it
					                         //int a = updatelinks.executeUpdate();
					                         // System.out.println("After : " +  updatelinks.toString());
					                          updatelinks.addBatch();
					                        
					                        // updatelinks.clearParameters();
	            	                       //return if falue to update; 
				                          }//if
				                        
	            	 
	                          }//loop links
			                  System.out.println("for host: "+host+","+count+"for url:"+url);
			                  if (count!=0) { archives.add(host);}
			                 count = 0;
				  if (vcount==10000) { 
					  updatelinks.executeBatch();
				      //conn.commit();
				      vcount=0;
				      updatelinks.clearBatch();
				 }
			   } //host loop
			   
			   //test without delete
			  // System.out.println(updatelinks.toString());
			   updatelinks.executeBatch();
			   conn.commit();
			   deletelinks.setString(1,url);
			   deletelinks.executeUpdate();
			   //conn.commit();
			   conn.setAutoCommit(true); 
		   }
		   catch (Exception e) {
			  
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return archives;
		   
		  
	  }
	 
	
	/**
	 * @param args
	 */
	
		// TODO Auto-generated method stub
		public void printMe() {
			System.out.println("Run Me ~");
		}
	

		
}
