package gov.lanl.agg.batch;

import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.LinkParser;


import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

public class RunMeTask {

	 private Connection persistentConnection;
	 String connectionstr = null;  
	  String user=null;
	  String pass=null;
	  static DefaultApacheHttpClientConfig cc;
	 // static ApacheHttpClient client;
	  static {
		  cc= new DefaultApacheHttpClientConfig();
          cc.getProperties().put(DefaultApacheHttpClientConfig.PROPERTY_PROXY_URI,"http://proxyout.lanl.gov:8080/"); 
       //   client = ApacheHttpClient.create(cc);
	  }
	  public RunMeTask() {
			try {
			    persistentConnection=getNewConnection();
			} catch (SQLException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			}
		    }
		    public RunMeTask(Map map) {
			try {
			    this.connectionstr = (String) map.get("db.connectionstr");
			    this.user = (String) map.get("db.user");
			    this.pass = (String) map.get("db.pass");
			   
			    
			    persistentConnection = getNewConnection();
			} catch (SQLException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			}
		    }
	  
		    
		    public void fupdateArchives(List tmlist,List tglist,Map<String,Integer> map) {
		    	   Statement s = null;
				   Connection conn = null;
				   ResultSet rs0 =null;
				   try {
			    		 conn = startConnection();
			    		 conn.setAutoCommit(false);
			    		     s = conn.createStatement();
				   for (String key: map.keySet()) {
						 Integer in = map.get(key);
						
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
			    	finally {
			    		
			    		if (rs0 != null) {
		                    try {
								rs0.close();
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                }
			    		
				    	 if (s != null) {
			                    try {
									s.close();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			                }
				    	
				   }			
			    }
					 
		    
		    public void updateArchives(List tmlist,List tglist,Map<String,Integer> map) {
		    	  Statement s = null;
				   Connection conn = null;
				   ResultSet rs0 =null;
				  
				   int count = 0;
		    	String sql ="select count(*) from  archive_register; ";
		    	try {
		    		 conn = startConnection();
		    		 conn.setAutoCommit(false);
		    		     s = conn.createStatement();
					     rs0 = s.executeQuery(sql);
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
		    	finally {
		    		
		    		if (rs0 != null) {
	                    try {
							rs0.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
		    		
			    	 if (s != null) {
		                    try {
								s.close();
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                }
			    	
			   }			
		    }
		    
	  public Map selectJob () {
		  Map m = new HashMap();
		   Statement s = null;
		   Connection conn = null;
		   ResultSet rs0 =null;
		   String url="";
		   PreparedStatement pst = null;
		   try {
			conn = startConnection();
		    conn.setAutoCommit(false);
		    s = conn.createStatement();
		   // id auto_increment not null primary key,
		   // url varchar(255),
		    //reqtime DATETIME,
		    //process_id INT null default null
		    //compltime DATETIME
		    String sql = "select id,url from jobs where process_id is null order by id asc limit 1 FOR UPDATE;";
		    rs0 = s.executeQuery(sql);
		
		   Integer id=0;
			 while (rs0.next()) {
				    id = rs0.getInt(1); 
				    url = rs0.getString(2);
				
				 m.put("url", url);
				 m.put("id", id);
			 }
		    rs0.close();
		    //all urls in the table mark as  
		    if (id!=0) {
		    String sql2 = "update jobs set process_id = " +id + " where  process_id is null and url = '"+url+ "';";
			int st = s.executeUpdate(sql2);
		    }
		          //pst = conn.prepareStatement(sql2);
		   //pst.setInt(1, id);
		   //pst.setString(2, url);
		   //pst.execute();
		  //update table after job is done to flag "F"?
		   //update of updated fild
		 conn.commit();
		  
	  } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   finally {
		    	 if (s != null) {
	                    try {
							s.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
		    	 if (pst != null) {
	                    try {
							pst.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
		   }			 
		 //return process_id;  
		 return m;  
	  }
	
	  public void selectJobs (BlockingQueue q) {
		
		 
		   Statement s = null;
		   Connection conn = null;
		   ResultSet rs0 =null;
		   PreparedStatement updatejobs=null;
		   String url="";
		  // PreparedStatement pst = null;
		  // if ( inoutq.size()>1000)  return;
		   try {
			conn = startConnection();
		    conn.setAutoCommit(false);
		    s = conn.createStatement();
		   // id auto_increment not null primary key,
		   // url varchar(255),
		    //reqtime DATETIME,
		    //process_id INT null default null
		    //compltime DATETIME
		    String psql="update jobs set process_id = ? where  process_id is null and url = ?;";
		    updatejobs = conn.prepareStatement(psql);
		    String sql = "select id,url from jobs where process_id is null order by id asc limit 1 for update;";	    
			int count = 1200-q.size();
			
		    for (int i=1;i<count;i++){
	        rs0 = s.executeQuery(sql);
		    Integer id=0;
			       while (rs0.next()) {
				         id = rs0.getInt(1); 
				         url = rs0.getString(2);
				         System.out.println("from select jobs;"+id+"|"+url);
				   // q.add(id+"|"+url);									
			             }
		    rs0.close();
		 	          updatejobs.setInt(1,id);
			          updatejobs.setString(2,url);
			
		              int a = updatejobs.executeUpdate();
		              System.out.println("status"+a);
		              conn.commit();
		              q.put(id+"|"+url);
		     }
		  
	  } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   finally {
			   try {
			  // conn.setAutoCommit(true);
			   if (updatejobs!=null) {
				   updatejobs.close(); 
			   }
			   } catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	 if (s != null) {
	                    try {
	                    
							s.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
		    	
		   }			 
		 //return process_id;  
		// return m;  
	  }
	
	  
	  
	  public void updateJobs (String process_id,String ctime) {
		  //I am going to substitute it to delete in future
		 // Statement s = null;
		   Connection conn = null;
		  Statement s = null;
		  String sql2 = "update jobs set compltime ='"+ctime+"' where process_id = "+process_id+";";
		  PreparedStatement pst = null;
		try {
			 conn = startConnection();
			 conn.setAutoCommit(false);
			 s = conn.createStatement();
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
		  finally {
			 
		    	 if (s != null) {
	                    try {
							s.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
		    			    	
	        }
	  }
	  
	  public void updateLinkmaster (String url,String ctime) {
		  //I am going to substitute it to delete in future
		  Statement s = null;
		  Connection conn = null;
		  //? if archive makes available records retroactivly not accurate logic. 
		  String sql2 = "update linkmaster set updtime ='"+ctime+"' where id = md5('"+url+"');";
		  PreparedStatement pst = null;
		try {
			conn = startConnection();
			 s = conn.createStatement();
			 conn.setAutoCommit(false);
			 s.executeUpdate(sql2);
			//pst = conn.prepareStatement(sql2);
		     conn.commit();
		 //  pst.setString(2,id);
		  // pst.setString(1,ctime );
		   //pst.execute();
		   
	  } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  finally {
		    	 if (s != null) {
	                    try {
							s.close();
							//con.setAutoCommit(true);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
		    			    	
	        }
	  }
	  
	  public void updateLinks(String url,String timemap,String host) {
		 
		   Statement s = null;
		   Connection conn = null;
		   PreparedStatement updatelinks=null;
		   java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		   java.text.SimpleDateFormat  httpformatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");	
			
		        TimeZone tzo = TimeZone.getTimeZone("GMT");
		        httpformatter.setTimeZone(tzo);
		   Map m = new HashMap();
		   try {
				conn = startConnection();
				conn.setAutoCommit(false);
				// s = conn.createStatement();
				
				// DefaultApacheHttpClientConfig cc = new DefaultApacheHttpClientConfig();		
		         //cc.getProperties().put(DefaultApacheHttpClientConfig.PROPERTY_PROXY_URI,"http://proxyout.lanl.gov:8080/"); 
		         ApacheHttpClient client = ApacheHttpClient.create(cc);
		         //client.getProperties();
		   //Client client = Client.create();
		  
			// System.out.println("timegate url from "+url);
		   String atmurl = timemap + url;
		   WebResource webResource = client.resource(atmurl);
		   URI u = webResource.getURI()	;
		  // String hostname = u.getHost();
		   //System.out.println(hostname);
		   ClientResponse response = webResource.get(ClientResponse.class);
		   int status = response.getStatus();
		   // return if status !=200
		   if (response.getStatus()==200) {
			   String output = response.getEntity(String.class);
			  // System.out.println(output);
			   LinkParser parser = new LinkParser(output);
			   parser.parse();
			   LinkHeader header=parser.getHeader();
			   List<Link> links = header.getLinks();
			   System.out.println("array size:"+links.size());
			   String psql =  "insert IGNORE into links (id,mdate,archive_id,href,type,rel,part) " + 	        
		            	"select md5(?), ?, id , ?,?,?, 0 " +
			            		" from archive_register where hostname = ?;";
			   updatelinks = conn.prepareStatement(psql);         						   
			   for (Link f : links) {
				  // System.out.println("href:"+f.getHref());
				   //System.out.println("rel:"+f.getRelationship());
				 //timemap paging need to be implemented 
				    if (f.getDatetime()!=null) {
				    	String type="";
				    	if (f.getType() != null) {
				    		type = f.getType();
				    	}
				    String datestr = f.getDatetime();
				    Date d = httpformatter.parse(datestr);
				    String mysqldate =formatter_utc.format(d);
	            	//String sql= "insert IGNORE into links (id,mdate,archive_id,href,type,rel,part) " + 	        
	            	//"select md5('"+url+"'), '" +mysqldate+"', id , '"  +f.getHref() +"','"+ type + "','" 
	            //+f.getRelationship()+"', 0 from archive_register where hostname ='" +host +"';";
	            	//System.out.println("sql"+sql);
	            	  
				//	int st = s.executeUpdate(sql);
					updatelinks.setString(1,url);
					updatelinks.setString(2,mysqldate);
					updatelinks.setString(3,f.getHref());
					updatelinks.setString(4,type);
					updatelinks.setString(5,f.getRelationship());
					updatelinks.setString(6,host);
					updatelinks.executeUpdate();
	            	 //return if falue to update; 
				    }
	            	 
	             }
				conn.commit();
		   }
		   }
		   catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   finally {
		    	 if (updatelinks != null) {
	                    try {
	                    	updatelinks.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
		   }
	  }
	  
	  
	
	/**
	 * @param args
	 */
	
		// TODO Auto-generated method stub
		public void printMe() {
			System.out.println("Run Me ~");
		}
	

		 Connection getNewConnection () throws SQLException {
			    DriverManager.registerDriver (new com.mysql.jdbc.Driver());
			    Connection conn=null;
			    
		                if (connectionstr!=null) {
		            	 conn = DriverManager.getConnection(connectionstr,user,pass);
		                }
		                else {
			     conn = DriverManager.getConnection
			         ("jdbc:mysql://localhost:3306/aggregator", "cache", "plenty");
			                         // @machineName:port:SID,   userid,  password
		                }
			    return conn;
			    
			   }
		    
		    private Connection startConnection() throws SQLException {
		        if (persistentConnection != null) {
		            return checkConnection(persistentConnection);
		        } else {
		            return getNewConnection();
		        }
		    }
		    private Connection checkConnection(Connection connection) throws SQLException {
		        Statement stmt;
		        try {
		            stmt = connection.createStatement();
		            stmt.executeQuery("SELECT 0");
		        } catch (SQLException ex) {
		            connection = getNewConnection();
		        }
		        return connection;
		    }
		    
				
		
		
}
