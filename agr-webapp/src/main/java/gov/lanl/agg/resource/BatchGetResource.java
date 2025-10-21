package gov.lanl.agg.resource;

import gov.lanl.agg.BatchMap;
import gov.lanl.agg.Link;
import gov.lanl.agg.batch.RunMeBatchTask;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.utils.MementoUtils;

import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

//this is for batch cache processing
//download timemap per archive from this endpoint end point

@Path("/batch/{id:.*}")

public class BatchGetResource {
	
	
	 @HEAD
	 @Produces({MediaType.APPLICATION_JSON})
	 public Response getHMapbyArchive(@Context UriInfo ui, @PathParam("id") String idp) {
		    URI baseurl = ui.getBaseUri();
		    URI ur = ui.getRequestUri(); 
		    String id = ur.toString().replaceFirst(baseurl.toString()+"batch/", "");
		  
		     RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
	        //lastmodified header
		     Date lastupdate = rmtask.checkLastUpdate(id);
		     if (lastupdate!=null){
		    	  ResponseBuilder r = Response.ok();
		          r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
		          return r.build();
		     }else{
		    	 ResponseBuilder r = Response.status(404);
		    	 return r.build();
	  			  
		     }
  			  
		 
	}
	
	 @GET
	 @Produces({MediaType.APPLICATION_JSON})
	 public Response getTimeMapbyArchive(@Context UriInfo ui, @PathParam("id") String idp) {
		   URI baseurl = ui.getBaseUri();
		   URI ur = ui.getRequestUri(); 
		   String id = ur.toString().replaceFirst(baseurl.toString()+"batch/", "");
		   CacheStorage cache=((CacheStorage) MyInitServlet.getInstance().getAttribute("storage"));
		   BatchMap bmap = cache.getBatchInfo(id);
		   Map <String,List <Link>>mementos =  bmap.getMementos();
		   
		     RunMeBatchTask rmtask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
	        //lastmodified header
		     Date lastupdate = rmtask.checkLastUpdate(id);
		    
		   if ( bmap.getMementos().size()==0) {
			  ResponseBuilder r = Response.status(404);
			  if (lastupdate!=null){
				  r.header("Last-Modified", MementoUtils.httpformatter.format(lastupdate));
			  }
			  return  r.build();
		   }
		   StringBuffer sb = new StringBuffer();
		   sb.append("{");
		   sb.append("\"original_uri\":\""+bmap.getOriginalUrl()+"\",\n");
		  if (bmap.getRequestDatetime()!=null) {
		   sb.append("\"request_date\":\""+  MementoUtils.timeTravelJsFormatter.format(bmap.getRequestDatetime())+"\",\n");
		   }
	       if (bmap.getUpdateDatetime()!=null) {
		   sb.append("\"update_date\":\""+  MementoUtils.timeTravelJsFormatter.format(bmap.getUpdateDatetime())+"\",\n");
	       } 
		   sb.append("\"mementos\": {\n");
		   sb.append("\"list\": [ \n");
		   int total = mementos.size();
		   System.out.println("total"+total);
		   Iterator it = mementos.keySet().iterator();
		   int count = 0;
		     while (it.hasNext()) {
		    	String ar = (String) it.next();	
		    	count = count+1;
		        List<Link> links = (List<Link>) mementos.get(ar);
		             for (int i=0;i<links.size();i++) {
		            	  sb.append(" {\n"); 
		            	  sb.append("\"archive_id\":\""+ar+"\",\n");	 
		            	  Link link = links.get(i);
		            	  
		            	   //reformat date
		            	   String mdate = link.getDatetime();
		            	     try {
						 	  Date d = MementoUtils. formatter_utc.parse(mdate);
							  sb.append("\"datetime\":\""+MementoUtils.timeTravelJsFormatter.format(d)+"\",\n");
						      } catch (ParseException e) {
							// TODO Auto-generated catch block
							  e.printStackTrace();
						      }
		             
		              sb.append("\"uri\":\""+link.getHref()+"\"\n");
		              if ((i==links.size()-1) && (count==total)){
		            	  sb.append("}\n");
		              }else {
		              sb.append("},\n");
		              }
		             }
		            
	         }
		     
		     sb.append("]\n");
		     sb.append("}}\n");
		     
		 
		   
		        ResponseBuilder r = Response.ok(sb.toString());
		        r.header("Last-Modified", MementoUtils.httpformatter.format(bmap.getUpdateDatetime()));
			    return  r.build(); 
		 
	 }
	
	 
	
	
}
