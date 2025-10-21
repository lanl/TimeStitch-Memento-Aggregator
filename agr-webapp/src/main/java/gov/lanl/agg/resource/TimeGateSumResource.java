package gov.lanl.agg.resource;

import gov.lanl.agg.utils.SummaryUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path("/sumsrv/{date}/{id:.*}")

public class TimeGateSumResource {

	@GET
	// I may need to copy all  logic to @HEAD
	public Response  getTimegate( @Context HttpHeaders hh, @Context UriInfo ui, @PathParam("id") String idp,@PathParam("date") String rdate ) throws ParseException, URISyntaxException {
		  URI baseurl = ui.getBaseUri();
		  URI ur = ui.getRequestUri(); 
		 //System.out.println("request url:"+ur.toString());
		  String id = ur.toString().replaceFirst(baseurl.toString()+"sumsrv/"+rdate +"/", "");
		  TimeGateResource tg = new TimeGateResource(ui); 
		  //Date dtdate = MementoCommons.timeTravelMachineFormatter.parse(rdate);
		   Date srdate = MementoCommons.checkSrDateValidity(rdate);
		   String cdate = MementoCommons.httpformatter.format(srdate);
		   //if url does not have public suffix skip
		     String sumurl = null;
		     boolean skip = false;
		     try {
		     sumurl = SummaryUtils.shortin(id,"default");
			         if (sumurl!=null) {
				     System.out.println("id"+id+",sumurl"+sumurl);
			         }
		     }
		     catch ( Exception ignore) {
					System.out.println("bad tm:"+id);
					skip=true;
					}
		  
		   List al = tg.Summary_check(sumurl, srdate);
		   StringBuffer sb = new StringBuffer();
		   sb.append("<html><p>" + id +"</p><br></br><p>" + sumurl +"</p>");
		   Iterator it = al.iterator();
		   sb.append("<ul>");
		   while (it.hasNext()) {
			  String p = (String) it.next();
			  sb.append("<li>"+p);
		   }
		   sb.append("</ul>");
		   sb.append("</html>");
		   
		   ResponseBuilder r = Response.ok(sb.toString());
		   return  r.build(); 
		  	}	
	
	 @HEAD
	 public Response  getHTimegate( @Context HttpHeaders hh, @Context UriInfo ui, @PathParam("id") String id,@PathParam("date") String rdate ) throws ParseException, URISyntaxException {
		return getTimegate( hh, ui, id,rdate );
	 }
	
	 @POST
		
	 public Response replytoPOST() {
		 ResponseBuilder r = Response.status(405);
		  r.header("Allow", "GET,HEAD"); 
		  r.header("Vary","Accept-Datetime");
		 return r.build();
	 }
	 
	 @PUT
	 public Response replytoPUT() {
		 ResponseBuilder r = Response.status(405);
		  r.header("Allow", "GET,HEAD"); 
		  r.header("Vary","Accept-Datetime");
		  
		 return r.build();
	 }
	 
	 @DELETE
	 public Response replytoDELETE() {
		 ResponseBuilder r = Response.status(405);
		  r.header("Allow", "GET,HEAD"); 
		  r.header("Vary","Accept-Datetime");
		 return r.build();
	 }
	 
	 
	 
	 
}
