package gov.lanl.agg.resource;

import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.cache.CacheStorage;

import java.net.URI;
import java.util.Date;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.leansoft.bigqueue.IBigQueue;

//this is endpoint to post url, date for batch cache processing


@Path("/batchcacheout/{date}/{id:.*}")

public class BatchAcceptURLResource {
	
	
	
	 @POST
	 public Response notifyPOST(@Context UriInfo ui, @PathParam("id") String idp,@PathParam("date") String rdate) {
		   URI baseurl = ui.getBaseUri();
		   URI ur = ui.getRequestUri(); 
		   String id = ur.toString().replaceFirst(baseurl.toString()+"batchcacheout/"+rdate +"/", "");
		  
		   Date srdate = MementoCommons.checkSrDateValidity(rdate);
		    /*
		    try {
		    IBigQueue resque = (IBigQueue) MyInitServlet.getInstance().getAttribute("ProcessQue");
		   //String cdate = MementoCommons.httpformatter.format(srdate);
		    if  (resque!=null) {
	             String pload = rdate+"|"+ id;
	             System.out.println("added to process queue:"+pload);
	             resque.enqueue(pload.getBytes());
	             }
		    }
		    catch (Exception e) {
			      // TODO Auto-generated catch block
			        e.printStackTrace();
		            }
		    */
		   // keep db based Q for batch
		   //load to db first
		   if (srdate!=null) {
		   ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage")).checkCacheRelax(id);
		   }
		   ResponseBuilder r = Response.status(204);
		 
		 return r.build();
	 }
	
	
	
	
}
