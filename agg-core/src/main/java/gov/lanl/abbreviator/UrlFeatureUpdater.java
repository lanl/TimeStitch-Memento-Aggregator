package gov.lanl.abbreviator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.Statement;

import gov.lanl.agg.utils.URLFeature;

public class UrlFeatureUpdater {
	 static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	public static void main(String[] args) {
		//try {
		     Args margs = new Args( args );
		     String table =  margs.get("table", "features");
		     String database = margs.get("database", "batch_aggregator");
		     String connectionstr = margs.get("connstr", "");
		     String urlstr= margs.get("url", "");
		 
		    
		    
		 //   System.out.println("_path"+c1 + "\n="+c2+"\n=path "+c3+"\n-"+c4+"\n-host"+c5+"\n."+c6+"\n .host "+c7+
		   // 		"\nhostlen"+c8+ "\npathlen"+c9+"\nquerylen"+c10+"\nslashnum"+c11+"\nurl len"+c12);
		     
		     try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		     Connection conn = null;
			  try {
				conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+database+"?useUnicode=yes&characterEncoding=UTF-8","cache","plenty");
			   } catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			 }
		     
		    
			     int limit = 1000;
			     int start = 0;
		         
		         String sqlc = "select count(*) from "+table +";"; 
		        // System.out.println("sql"+sql);
		        try (      PreparedStatement st = conn.prepareStatement(sqlc);)
		              
		         {
		            int count = 0;
		        	  ResultSet  rs = st.executeQuery();
			            while (rs.next()) {
			                count = rs.getInt(1);
			            }
		        	rs.close();
		        	int i=0;
		        	while ( i<count){
		        		 String sql =	"select url from " +table+ " limit "+start+","+limit+ ";";	
		        		 System.out.println(sql);
		        	Statement st1 =  (Statement) conn.createStatement();
		            ResultSet  rs1 = st1.executeQuery(sql);
		                   while (rs1.next()) {
		                          String url = rs1.getString(1);
		                          String sql2 = make_sql( url, table);
		                          if (sql2!=null) {
		                        	  try {
		                          Statement st2 =  (Statement) conn.createStatement();
		   		                            st2.executeUpdate(sql2);
		   		                            st2.close();
		                        	  }
		                        	  catch (SQLException e) {
		                        		  //ignore unescaped url for now
		              		            // TODO Auto-generated catch block
		              		            e.printStackTrace();
		              		        }
		                          }
		   		    		 }
		                   rs1.close();
		                   st1.close();
		            limit = limit+1000;
		            start = start+1000;
		            i=i+count/1000;
		        	}//while count
		        }
		        catch (SQLException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        }
		        finally{
		        	try {
						conn.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        }

		     
	}
	
	static String make_sql(String url,String table){
		  URLFeature uf= new URLFeature(url);
             String suffix = uf.getSuffix();
             if (suffix.equals("")) return null;
  //System.out.println("suffix"+suffix);
  String mw=uf.getMainWord();
  String[] words =uf.getPathWords();
  String fw="";
  if (words!=null) {
  if (words.length>0) {
	  fw=words[0];
	//  for (int i=0;i<words.length;i++){
      // System.out.println("word" +words[i] );
	  //}
  }
  }
  int c1 = uf.get__num_path();
  int c2 = uf.getAmp_num();
  int c3 = uf.getAmp_num_path();
  int c4 = uf.getDash_num();
  int c5 = uf.getDash_num_host();
  int c6 = uf.getDot_num();
  int c7 = uf.getDot_num_host();
  int c8 = uf.getHost_len();
  int c9 = uf.getPath_len();
  int c10 = uf.getQuery_len();
  int c11 = uf.getSlash_num();
  int c12 = uf.getUrl_len();
  String c13 =uf.getHostname();
  String sql2= "update "+ table + " set hostname='" +c13 +
 		 "'," + "url_len="+c12+",host_len="+c8+",path_len="+c9+
 		 ",q_len="+c10+",slash_num="+c11+",dot_num="+c6+",dot_num_host="+c7+
 		 ",amp_num="+c2+",amp_num_path="+c3+",dash_num="+c4+",dash_num_host="+c5+
 		 ",num__path="+c1+", suffix='"+suffix+"', main_word='"+mw+"',path_word_1='"+fw+ "'  where url='"+url+"'";
  return sql2;
	}
	
}
