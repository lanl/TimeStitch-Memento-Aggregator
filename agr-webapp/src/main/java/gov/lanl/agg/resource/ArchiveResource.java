package gov.lanl.agg.resource;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.batch.RunMeBatch;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

/*
@author Lyudmila Balakireva
*/

@Path("/archivedesc/{id:.*}")

public class ArchiveResource {
	static List <ArchiveDescription> adesc;
	static {	
	         MyInitServlet cl = MyInitServlet.getInstance();
             adesc = (List<ArchiveDescription>) cl.getAttribute("archivedesc");
      }
	
	@GET
	
	public Response  getDescription( @Context HttpHeaders hh, @Context UriInfo ui, @PathParam("id") String idp ) throws ParseException, URISyntaxException {
		 URI baseurl = ui.getBaseUri();
		 URI ur = ui.getRequestUri(); 
		 //System.out.println("request url:"+ur.toString());
		
		 String id = ur.toString().replaceFirst(baseurl.toString()+"archivedesc/", "");
		 System.out.println("archivedesc into get:"+id);
	     //first check if archive is in config
		 ArchiveDescription  fa = null; 
		  Iterator<ArchiveDescription> ait = adesc.iterator();
	         while (ait.hasNext()) {
	             ArchiveDescription ad = ait.next();
	             //String aname = ad.getLongname();
	             String shortName = ad.getName();
	             if (shortName.equals(id)) {
	            	 fa = ad; break;
	             }
	         }
		 
	         if (fa==null) {
	        	 
	        	   RunMeBatch btask = (RunMeBatch) MyInitServlet.getInstance().getAttribute("task");
	        	   ArchiveDescription ad = btask.getArchiveInfo(id);
	        	   if (ad!=null) {
	        		   fa=ad;
	        	   }
	        		 
	        	 
	         }
	         
	         if (fa==null) {
	 	    	//no archive 
	 	    	   ResponseBuilder r = Response.status(404);				          
	               return r.build();
	 	     }
	         else {
	        	 String m = compose_message(fa);
	        	 ResponseBuilder r = Response.ok(m);
				  
				   return  r.build(); 
	         }
		 
		 
	}
	
	public String compose_message(ArchiveDescription ad) {
		StringBuffer sb = new StringBuffer();
		
		sb.append("{");
		    sb.append("archive_id \":\"" + ad.getName()+"\",");
		    String name=ad.getLongname();
		    if (ad.getLongname()==null) {
		    	name=ad.getName();
		    }
		    sb.append("archive_name\":\""+name+"\",");
		     //"public_status":"open",
		    sb.append("timegate\":\""+ ad.getTimegate()+"\",");
		    sb.append("timemap\":"+ ad.getTimemap() + "\",");
		    if (ad.getCalendarUrl()!=null){
		    sb.append("calendar_page\":\""+ ad.getCalendarUrl() + "\",");
		    }
			sb.append("}");


		return sb.toString();
	}
	
}
