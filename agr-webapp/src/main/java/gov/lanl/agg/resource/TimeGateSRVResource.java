package gov.lanl.agg.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
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

@Path("/tgsrv/{date}/{id:.*}")

public class TimeGateSRVResource {

	@GET
	// I may need to copy all  logic to @HEAD
	public Response  getTimegate( @Context HttpHeaders hh, @Context UriInfo ui, @PathParam("id") String idp,@PathParam("date") String rdate ) throws ParseException, URISyntaxException {
		  URI baseurl = ui.getBaseUri();
		  URI ur = ui.getRequestUri(); 
		 //System.out.println("request url:"+ur.toString());
		  String id = ur.toString().replaceFirst(baseurl.toString()+"tgsrv/"+rdate +"/", "");
		  TimeGateResource tg = new TimeGateResource(ui); 
		  //Date dtdate = MementoCommons.timeTravelMachineFormatter.parse(rdate);
		   Date srdate = MementoCommons.checkSrDateValidity(rdate);
		   String cdate = MementoCommons.httpformatter.format(srdate);
		   
		   boolean nocache = false;
		    boolean onlycached = false;
		    
	        List <String> cachecontrollst = hh.getRequestHeader("Cache-Control");
			 if (cachecontrollst!=null){
			 String cacheconstrolstr = cachecontrollst.get(0);
			 if (cacheconstrolstr.toLowerCase().indexOf("no-cache")>0){
				 System.out.println("no-cache");
				 nocache = true;
			 }
			 if (cacheconstrolstr.toLowerCase().indexOf("only-if-cached")>0){
				 System.out.println("only-if-cached");
				 onlycached = true;
			 }
			}
		   
		  return tg.make_timegate_res(id,  cdate, nocache,  onlycached);
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
