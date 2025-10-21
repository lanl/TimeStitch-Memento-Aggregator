package gov.lanl.agg.utils;

import gov.lanl.agg.Link;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.core.MultivaluedMap;

public class PagingUtils {
	  
	static ThreadSafeSimpleDateFormat httpformatter;
	static {
		
		  TimeZone tz = TimeZone.getTimeZone("GMT");
		  httpformatter = new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");	
		  httpformatter.setTimeZone(tz);
	    
    }
	Map paginglist;
	public PagingUtils(Map pagingList) {
		 this.paginglist = paginglist;
	}
    // status 1
    public List timemap_paging_index (List<Link> ttlist) {
    	//download from every timemap reference 
    	String timemap=null;
    	List timemaps = new ArrayList();
    	 for (Link temp : ttlist) {
    		   if (temp.getRelationship().equals("timemap")) {
    			   System.out.println("util paging:"+temp.getHref());
    			   //timemap=temp.getHref();
    			   timemaps.add(temp.getHref());
    		   }
    		   }
    	 return timemaps;
    }
    
    
    
    //status 3
    public String timemap_paging_backward (List<Link> ttlist) {
    	String selffrom = null;
    	//String selfuntil = null;
    	//fetch self
    	String timemap=null;
    	if (ttlist!=null) {
    		for (Link temp : ttlist) {
        		     if (temp.getRelationship().equals("self")) {
        			     MultivaluedMap<String, String> ext = temp.getExtensions();
        			     if (ext.containsKey("from")) {
        				  List<String> lfrom = ext.get("from");
        				   selffrom = lfrom.get(0);
        				 System.out.println("self from:" +selffrom);
        			       }
        		         //	if (ext.containsKey("until")) {
        			     //	List<String> luntil = ext.get("until");
        				//selfuntil= luntil.get(0);
        				//System.out.println("self until:" +selfuntil);
        			    //}
        		       }//if
    		      }//for
    		    
    	if (selffrom!=null){
			//if no self, then no paging considered.
	       for (Link temp : ttlist) {
		   if (temp.getRelationship().equals("timemap")) {
			   MultivaluedMap<String, String> ext = temp.getExtensions();
    			if (ext.containsKey("until")) {
    				List<String> lfrom = ext.get("until");
    				String until = lfrom.get(0);
    				if (until.equals("")){until=null;}
    				//System.out.println("timemap until:" +until);
    				//if (until<from) wikipeda 
    				if (selffrom!=null&&until!=null){
    				 try {
						Date untilldate = httpformatter.parse(until);
						     if (untilldate.before(httpformatter.parse(selffrom))){
							      timemap=temp.getHref();
						          //tasks.add(new TimeMapTask(hostid, temp.getHref(), client));
						       }
					     } catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					     }
    				}
    			
    			  }//if
		         }//if
	              }//for
    	        }//if
		   }
	       
    	return timemap;
    }
    
  //status 2
    public String timemap_paging_forward (List<Link> ttlist) {
    	//String selffrom = null;
    	String selfuntil = null;
    	//fetch self
    	String timemap=null;
    	if (ttlist!=null) {
    		for (Link temp : ttlist) {
        		     if (temp.getRelationship().equals("self")) {
        			     MultivaluedMap<String, String> ext = temp.getExtensions();
        		         	if (ext.containsKey("until")) {
        			     	List<String> luntil = ext.get("until");
        				    selfuntil= luntil.get(0);
        				    System.out.println("self until:" +selfuntil);
        			        }
        		       }//if
    		      }//for
    		    System.out.println("in timemap paging forward");
    		if (selfuntil==null) return null;
    	if (selfuntil!=null){
			//if no self, then no paging considered.
	       for (Link temp : ttlist) {
		        if (temp.getRelationship().equals("timemap")) {
			      MultivaluedMap<String, String> ext = temp.getExtensions();
    			    if (ext.containsKey("from")) {
    				List<String> lfrom = ext.get("from");
    				String from = lfrom.get(0);
    				System.out.println("timemap from:" +from);        				
    				 try {
						Date fromdate = httpformatter.parse(from);
						     if (fromdate.after(httpformatter.parse(selfuntil))||fromdate.equals(httpformatter.parse(selfuntil))){
							      timemap=temp.getHref();
						      }
					     } catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					     }
    			
    			  }//if
		         }//if
	              }//for
    	        }//if
		   }
	       
    	return timemap;
    }
    
    
    //paging -1?
	public String timemap_paging_unknown (List <Link> ttlist,int ihost) {
		// String pagestatus="0"; //no paging
		 System.out.println("ihost"+ihost);
		 
		 paginglist.put(ihost,"0");
		 String selfuntil = null;
		 String selffrom = null;
		 String timemap=null;
    	 if (ttlist!=null) {
    		for (Link temp : ttlist) {
        		     if (temp.getRelationship().equals("self")) {
        			     MultivaluedMap<String, String> ext = temp.getExtensions();
        			        if (ext!=null) {
        		         	if (ext.containsKey("until")) {
        			     	List<String> luntil = ext.get("until");
        				    selfuntil= luntil.get(0);
        				    System.out.println("self until:" +selfuntil);
        			         }
        		         	if (ext.containsKey("from")) {
            			     	List<String> luntil = ext.get("from");
            				    selffrom= luntil.get(0);
            				    System.out.println("self from:" +selffrom);
            			         }
        			        }
        		         	
        		       }//if
    		      }//for
    		
    		//now look into timemaps
    		
    		
    		if (selfuntil!=null){
    			//if no self, then no paging considered.
    	       for (Link temp : ttlist) {
    		   if (temp.getRelationship().equals("timemap")) {
    			   MultivaluedMap<String, String> ext = temp.getExtensions();
    			    String from = null;
    			    String until = null;
        			if (ext.containsKey("from")) {
        				List<String> lfrom = ext.get("from");
        				 from = lfrom.get(0);
        				 if (from.equals("")){from=null;}
        				//System.out.println("timemap from:" +from);
        			}
        			
        			if (ext.containsKey("until")) {
        				List<String> lfrom = ext.get("until");
        				 until = lfrom.get(0);
        				 if (until.equals("")){until=null;}
        				//System.out.println("timemap until:" +until);
        			}
        			
        			if (selfuntil!=null && from!=null) {
        				try {
    						Date fromdate = httpformatter.parse(from);
    						     if (fromdate.after(httpformatter.parse(selfuntil)) || fromdate.equals(httpformatter.parse(selfuntil))){
    							      timemap = temp.getHref();
    						        
    						       }
    					     } catch (ParseException e) {
    						// TODO Auto-generated catch block
    						 e.printStackTrace();
    					     }
        				//pagestatus="2";
        				paginglist.put(ihost,"2");
        				
        			}
        			
        			
        			if (selffrom!=null&&until!=null){
        				//if no self, then no paging considered.
        		      // for (Link temp : ttlist) {
        			  // if (temp.getRelationship().equals("timemap")) {
        				//   MultivaluedMap<String, String> ext = temp.getExtensions();
        	    			//if (ext.containsKey("until")) {
        	    				//List<String> lfrom = ext.get("until");
        	    				//String until = lfrom.get(0);
        	    				//System.out.println("timemap until:" +until);
        	    				//if (until<from) wikipeda 
        	    				 try {
        							Date untilldate = httpformatter.parse(until);
        							     if (untilldate.before(httpformatter.parse(selffrom))){
        								      timemap = temp.getHref();
        							          //tasks.add(new TimeMapTask(hostid, temp.getHref(), client));
        							       }
        						     } catch (ParseException e) {
        							// TODO Auto-generated catch block
        							e.printStackTrace();
        						     }
        	    				 paginglist.put(ihost,"3");
                                 	    			
        	    			  //}//if
        			       //  }//if
        		          //    }//for
        	    	        }//if
        			
        			
        			
        			
        			
        			
        			
        			
    		   }
    	       }
    		}
    		
    		
    		
    		
    	}
		return timemap;
	}
    
    
	public  String composeStatus(int http_status)
	   {
		   String hstatus;
        if (http_status>=500) {
      	  hstatus="5XX";
        }
        else if (http_status>=400&&http_status<500){
      	  hstatus="4XX";
      	  
        }
        else if (http_status>=300&&http_status<400) {
      	  hstatus="3XX";
        }
        else {
      	  hstatus="2XX";
        }
        return hstatus;
	    }
    
	
}
