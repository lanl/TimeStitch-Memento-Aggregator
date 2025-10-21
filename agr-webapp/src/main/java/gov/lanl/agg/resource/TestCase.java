package gov.lanl.agg.resource;

import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;

import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path("/test/")
public class TestCase {
static	ThreadSafeSimpleDateFormat httpformatter;
static	ThreadSafeSimpleDateFormat formatterout;
static	{
	 httpformatter = new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");	     
     TimeZone tzo = TimeZone.getTimeZone("GMT");
     httpformatter.setTimeZone(tzo);
     formatterout = new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss");
     formatterout.setTimeZone(tzo);
	}
//Pattern	Original Resource	TimeGate	Memento	Negotiation Style	
//Pattern 1.1  	URI-R  	URI-R  	URI-M  	302 
//original 200
	@GET
	@Path("1_1_1/{id:.*}")
	public  Response getPage_1 (@PathParam("id") String idp, @Context UriInfo ui, @Context HttpHeaders hh)  {
		 List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
		 URI baseurl = ui.getBaseUri();
		 if (hdatetime==null) {
			  String u=baseurl.toString() +"test/1_1_1/"+idp;
			  String timegate="<"+u+">; rel=\"timegate\"";
			          String s = getSimplePage (idp);
			          ResponseBuilder r = Response.ok(s);
			          r.header("Link", timegate );
			          return r.build();
		 }
		 else {
			
			  String u=baseurl.toString() +"test/1_1_1/"+idp;
			  String origlink="<"+u+">; rel=\"original\"";
			  String timegate=",<"+u+">; rel=\"timegate\"";
			 ResponseBuilder r = Response.status(302);
			 String dstr = hdatetime.get(0);
			 String dout="";
			 try {
				Date mdate = httpformatter.parse(dstr);
				dout = formatterout.format(mdate);
			 } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			 }
			
			 String location=baseurl.toString()+"test/memento/"+dout+"/"+idp;
             r.header("Location",location);
             //r.header("Vary","negotiate,accept-datetime");
             r.header("Vary","Accept-Datetime");
             r.header("Link",origlink+ timegate );
           return  r.build();
		 }
	}
	
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style	
	//Pattern 1.1  	URI-R  	URI-R  	URI-M  	302 
	//original 200 returns vary
		@GET
		@Path("1_1_bis/{id:.*}")
		public  Response getPage_0 (@PathParam("id") String idp, @Context UriInfo ui, @Context HttpHeaders hh)  {
			 List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
			 URI baseurl = ui.getBaseUri();
			 if (hdatetime==null) {
				  String u=baseurl.toString() +"test/1_1_bis/"+idp;
				  String timegate="<"+u+">; rel=\"timegate\"";
				          String s = getSimplePage (idp);
				          ResponseBuilder r = Response.ok(s);
				          r.header("Link", timegate );
				          r.header("Vary","Accept-Datetime");
				          return r.build();
			 }
			 else {
				
				  String u=baseurl.toString() +"test/1_1_bis/"+idp;
				  String origlink="<"+u+">; rel=\"original\"";
				  String timegate=",<"+u+">; rel=\"timegate\"";
				 ResponseBuilder r = Response.status(302);
				 String dstr = hdatetime.get(0);
				 String dout="";
				 try {
					Date mdate = httpformatter.parse(dstr);
					dout = formatterout.format(mdate);
				 } catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				 }
				
				 String location=baseurl.toString()+"test/memento_bis/"+dout+"/"+idp;
	             r.header("Location",location);
	             //r.header("Vary","negotiate,accept-datetime");
	             r.header("Vary","Accept-Datetime");
	             r.header("Link",origlink+ timegate );
	           return  r.build();
			 }
		}
		
	
	
	
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style	
	//Pattern 1.1  	URI-R  	URI-R  	URI-M  	302 
	//original 302
	@GET
	@Path("1_1_2/{id:.*}")
	public  Response getPage_2 (@PathParam("id") String idp, @Context UriInfo ui, @Context HttpHeaders hh)  {
		 List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
		 URI baseurl = ui.getBaseUri();
		 if (hdatetime==null) {
			  String u=baseurl.toString() +"test/1_1_2/"+idp;
			  String location = baseurl.toString() +"test/5/"+idp;
			  String timegate=",<"+u+">; rel=\"timegate\"";
			  ResponseBuilder r = Response.status(302);
			 
			  r.header("Location",location);
			          //String s = getStaticPage (idp);
			          //ResponseBuilder r = Response.ok(s);
			          return r.build();
		 }
		 else {
			
			  String u=baseurl.toString() +"test/1_1_2/"+idp;
			  String origlink="<"+u+">; rel=\"original\"";
			  String timegate=",<"+u+">; rel=\"timegate\"";
			 ResponseBuilder r = Response.status(302);
			 String dstr = hdatetime.get(0);
			 String dout="";
			 try {
				Date mdate = httpformatter.parse(dstr);
				dout = formatterout.format(mdate);
			 } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			 }
			
			 String location=baseurl.toString()+"test/memento/"+dout+"/"+idp;
             r.header("Location",location);
             //r.header("Vary","negotiate,accept-datetime");
             r.header("Vary","Accept-Datetime");
             r.header("Link",origlink+ timegate );
           return  r.build();
		 }
	}	
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style
	//Pattern 1.2  	URI-R  	URI-R  	URI-M  	200  
	@GET
	@Path("1_2/{id:.*}")
	public  Response getPage_1_2 (@PathParam("id") String idp, @Context UriInfo ui, @Context HttpHeaders hh)  {
		 List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
		 URI baseurl = ui.getBaseUri();
		 if (hdatetime==null) {
			          String s = getSimplePage (idp);
			          String u=baseurl.toString() +"test/1_2/"+idp;
			          String timegate="<"+u+">; rel=\"timegate\"";
			          
			          ResponseBuilder r = Response.ok(s);
			          r.header("Link", timegate );
			          return r.build();
		 }
		 else {
			 
			 String dstr = hdatetime.get(0);
				String s = getSimplePage ("version:"+dstr+","+idp);		         
					          ResponseBuilder r = Response.ok(s);
					         // URI baseurl = ui.getBaseUri();
					          
					          String dout="";
								 try {
									Date mdate = httpformatter.parse(dstr);
									dout = formatterout.format(mdate);
								 } catch (ParseException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								 }
								//need to fix link original or provide different method
							  String location=baseurl.toString()+"test/memento_1_2/"+dout+"/"+idp;      
					          String u=baseurl.toString() +"test/1_2/"+idp;
					          String origlink="<"+u+">; rel=\"original\"";
					          String timegate=",<"+u+">; rel=\"timegate\"";
					          r.header("Content-Location",location);
					          r.header("Memento-Datetime", dstr);
					          r.header("Link",origlink+ timegate );
					          r.header("Vary","Accept-Datetime");
			 
			 
			 
           return  r.build();
		 }
	}
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style
	//Pattern 1.3  	URI-R  	URI-R  	URI-R  	200 
	//no location header
	@GET
	@Path("1_3/{id:.*}")
	public  Response getPage_1_3 (@PathParam("id") String idp, @Context UriInfo ui, @Context HttpHeaders hh)  {
		 List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
		  URI baseurl = ui.getBaseUri();
		 if (hdatetime==null) {
			          String s = getSimplePage (idp);
			          String u=baseurl.toString() +"test/1_3/"+idp;
			          String timegate="<"+u+">; rel=\"timegate\"";
			          ResponseBuilder r = Response.ok(s);
			          r.header("Link", timegate );
			          return r.build();
		 }
		 else {
			 
			 String dstr = hdatetime.get(0);
				String s = getSimplePage ("version:"+dstr+","+idp);		         
					          ResponseBuilder r = Response.ok(s);
					          //URI baseurl = ui.getBaseUri();
					          
					          String dout="";
								 try {
									Date mdate = httpformatter.parse(dstr);
									dout = formatterout.format(mdate);
								 } catch (ParseException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								 }
							  String u=baseurl.toString() +"test/1_3/"+idp;
					          String origlink="<"+u+">; rel=\"original\"";
					          String timegate=",<"+u+">; rel=\"timegate\"";
					         // r.header("Content-Location",location);
					          r.header("Memento-Datetime", dstr);
					          r.header("Link",origlink+ timegate );
					          r.header("Vary","Accept-Datetime");
			 
			 
			 
           return  r.build();
		 }
	}
	
	
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style
	//Pattern 2.1  	URI-R  	URI-G  	URI-M  	302
	
	@GET
	@Path("2_1/{id:.*}")
	@Produces("text/html;charset=UTF-8" )
	public  Response getPage_10( @Context UriInfo ui,@PathParam("id") String idp)  {
		  ResponseBuilder r = Response.ok(getSimplePage("2_1 origresource"));
		  //String d ="Fri, 20 Mar 2009 11:00:00 GMT";
		///  r.header("Memento-Datetime", "Fri, 20 Mar 2009 11:00:00 GMT");
		  URI baseurl = ui.getBaseUri();
		  String u=baseurl.toString() +"test/2_1/"+idp;
		  String tg=baseurl.toString() +"test/2_1/timegate/"+u;
		  String link="<"+tg+">; rel=\"timegate\"";
		  r.header("Link",link);
		  //r.lastModified( somedate);
		  return r.build() ;
		
	}
	
	@GET
	@Path("2_1/timegate/{id:.*}")
	@Produces("text/html;charset=UTF-8" )
	
	public  Response getPage_11( @Context UriInfo ui,@PathParam("id") String idp,@Context HttpHeaders hh)  {
		 List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
		 URI baseurl = ui.getBaseUri();
		 String dstr;
		 if (hdatetime==null) {
			  dstr = "Tue, 12 Feb 2013 17:43:22 GMT";
		 }
		 else {
			  dstr = hdatetime.get(0);
		 }
			
			 //String u=baseurl.toString() +"test/2_1/"+idp;
			 String ut=baseurl.toString() +"test/2_1/timegate/"+idp;
			 String origlink="<"+idp+">; rel=\"original\"";
			 String timegate=",<"+ut+">; rel=\"timegate\"";
			 ResponseBuilder r = Response.status(302);
			// String s = getSimplePage ("2_2 test version:"+dstr+","+idp);		         
	          //ResponseBuilder r = Response.ok(s);
			 //String dstr = hdatetime.get(0);
			 String dout="";
			 try {
				Date mdate = httpformatter.parse(dstr);
				dout = formatterout.format(mdate);
			 } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			 }			
			 String location=baseurl.toString()+"test/memento_2_1/"+dout+"/"+idp;
             r.header("Location",location);
             //r.header("Vary","negotiate,accept-datetime");
             r.header("Vary","Accept-Datetime");
            // r.header("Memento-Datetime", dstr);
             r.header("Link",origlink+ timegate );
           return  r.build();
		 
	
	}
	
	
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style
	//Pattern 2.2  	URI-R  	URI-G  	URI-M  	200  
	//concluded of  two-3 methods
	
	@GET
	@Path("2_2/{id:.*}")
	@Produces("text/html;charset=UTF-8" )
	public  Response getPage_6( @Context UriInfo ui,@PathParam("id") String idp)  {
		  ResponseBuilder r = Response.ok(getSimplePage("2_2 origresource"));
		  //String d ="Fri, 20 Mar 2009 11:00:00 GMT";
		///  r.header("Memento-Datetime", "Fri, 20 Mar 2009 11:00:00 GMT");
		  URI baseurl = ui.getBaseUri();
		  String u=baseurl.toString() +"test/2_2/"+idp;
		  String tg=baseurl.toString() +"test/2_2/timegate/"+u;
		  String link="<"+tg+">; rel=\"timegate\"";
		  r.header("Link",link);
		  //r.lastModified( somedate);
		  return r.build() ;
		
	}
	
	@GET
	@Path("2_2/timegate/{id:.*}")
	@Produces("text/html;charset=UTF-8" )
	
	public  Response getPage_7( @Context UriInfo ui,@PathParam("id") String idp,@Context HttpHeaders hh)  {
		 List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
		 URI baseurl = ui.getBaseUri();
		 String dstr;
		 if (hdatetime==null) {
			  dstr = "Tue, 12 Feb 2013 17:43:22 GMT";
		 }
		 else {
			  dstr = hdatetime.get(0);
		 }
			
			// String u=baseurl.toString() +"test/2_2/"+idp;
			 String ut=baseurl.toString() +"test/2_2/timegate/"+idp;
			 String origlink="<"+idp+">; rel=\"original\"";
			 String timegate=",<"+ut+">; rel=\"timegate\"";
			// ResponseBuilder r = Response.status(302);
			 String s = getSimplePage ("2_2 test version:"+dstr+","+idp);		         
	          ResponseBuilder r = Response.ok(s);
			 //String dstr = hdatetime.get(0);
			 String dout="";
			 try {
				Date mdate = httpformatter.parse(dstr);
				dout = formatterout.format(mdate);
			 } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			 }			
			 String location=baseurl.toString()+"test/memento_2_2/"+dout+"/"+idp;
             r.header("Content-Location",location);
             //r.header("Vary","negotiate,accept-datetime");
             r.header("Vary","Accept-Datetime");
             r.header("Memento-Datetime", dstr);
             r.header("Link",origlink+ timegate );
           return  r.build();
		 
	
	}
	
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style
	//Pattern 2.3  	URI-R  	URI-G  	URI-G  	200  
	@GET
	@Path("2_3/{id:.*}")
	@Produces("text/html;charset=UTF-8" )
	public  Response getPage_9( @Context UriInfo ui,@PathParam("id") String idp)  {
		  ResponseBuilder r = Response.ok(getSimplePage("2_3 origresource"));
		  //String d ="Fri, 20 Mar 2009 11:00:00 GMT";
		///  r.header("Memento-Datetime", "Fri, 20 Mar 2009 11:00:00 GMT");
		  URI baseurl = ui.getBaseUri();
		  String u=baseurl.toString() +"test/2_3/"+idp;
		  String tg=baseurl.toString() +"test/2_3/timegate/"+u;
		  String link="<"+tg+">; rel=\"timegate\"";
		  r.header("Link",link);
		  //r.lastModified( somedate);
		  return r.build() ;
		
	}
	
	
	@GET
	@Path("2_3/timegate/{id:.*}")
	@Produces("text/html;charset=UTF-8" )
	
	public  Response getPage_8( @Context UriInfo ui,@PathParam("id") String idp,@Context HttpHeaders hh)  {
		 List <String> hdatetime = hh.getRequestHeader("Accept-Datetime");
		 URI baseurl = ui.getBaseUri();
		 String dstr;
		 if (hdatetime==null) {
			  dstr = "Tue, 12 Feb 2013 17:43:22 GMT";
		 }
		 else {
			  dstr = hdatetime.get(0);
		 }
			
			 String u=baseurl.toString() +"test/2_3/"+idp;
			 String ut=baseurl.toString() +"test/2_3/timegate/"+idp;
			 String origlink="<"+idp+">; rel=\"original\"";
			 String timegate=",<"+ut+">; rel=\"timegate\"";
			// ResponseBuilder r = Response.status(302);
			 String s = getSimplePage ("2_3 test version:"+dstr+","+idp);		         
	          ResponseBuilder r = Response.ok(s);
			 //String dstr = hdatetime.get(0);
			 String dout="";
			 try {
				Date mdate = httpformatter.parse(dstr);
				dout = formatterout.format(mdate);
			 } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			 }			
			 //String location=baseurl.toString()+"test/memento_2_2/"+dout+"/"+idp;
            // r.header("Content-Location",location);
             //r.header("Vary","negotiate,accept-datetime");
             r.header("Vary","Accept-Datetime");
             r.header("Memento-Datetime", dstr);
             r.header("Link",origlink+ timegate );
           return  r.build();
		 
	
	}
	
	
	
	
	@GET
	@Path("memento/{date}/{id:.*}")
	
	public  Response getMemento (@PathParam("id") String idp,@PathParam("date") String date, @Context UriInfo ui, @Context HttpHeaders hh)  {
		Date mdate=null;
		try {
			mdate = formatterout.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		 String dstr = httpformatter.format(mdate);
		String s = getSimplePage ("version:"+date+","+idp);		         
			          ResponseBuilder r = Response.ok(s);
			          URI baseurl = ui.getBaseUri();
			          String u=baseurl.toString() +"test/1_1_1/"+idp;
			          String origlink="<"+u+">; rel=\"original\"";
			          String timegate=",<"+u+">; rel=\"timegate\"";
			          r.header("Memento-Datetime", dstr);
			          r.header("Link",origlink+ timegate );
             return  r.build();
		 
	}
	
	@GET
	@Path("memento_bis/{date}/{id:.*}")
	
	public  Response getbMemento (@PathParam("id") String idp,@PathParam("date") String date, @Context UriInfo ui, @Context HttpHeaders hh)  {
		Date mdate=null;
		try {
			mdate = formatterout.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		 String dstr = httpformatter.format(mdate);
		String s = getSimplePage ("version:"+date+","+idp);		         
			          ResponseBuilder r = Response.ok(s);
			          URI baseurl = ui.getBaseUri();
			          String u=baseurl.toString() +"test/1_1_bis/"+idp;
			          String origlink="<"+u+">; rel=\"original\"";
			          String timegate=",<"+u+">; rel=\"timegate\"";
			          r.header("Memento-Datetime", dstr);
			          r.header("Link",origlink+ timegate );
             return  r.build();
		 
	}
	
	
	@GET
	@Path("memento_1_2/{date}/{id:.*}")
	
	public  Response get2Memento (@PathParam("id") String idp,@PathParam("date") String date, @Context UriInfo ui, @Context HttpHeaders hh)  {
		Date mdate=null;
		try {
			mdate = formatterout.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		 String dstr = httpformatter.format(mdate);
		String s = getSimplePage ("version:"+dstr+","+idp);		         
			          ResponseBuilder r = Response.ok(s);
			          URI baseurl = ui.getBaseUri();
			          String u=baseurl.toString() +"test/1_2/"+idp;
			          String origlink="<"+u+">; rel=\"original\"";
			          String timegate=",<"+u+">; rel=\"timegate\"";
			          r.header("Memento-Datetime", dstr);
			          r.header("Link",origlink+ timegate );
             return  r.build();
		 
	}
	
	@GET
	@Path("memento_2_1/{date}/{id:.*}")
	
	public  Response get4Memento (@PathParam("id") String idp,@PathParam("date") String date, @Context UriInfo ui, @Context HttpHeaders hh)  {
		Date mdate=null;
		try {
			mdate = formatterout.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		 String dstr = httpformatter.format(mdate);
		//String s = getSimplePage ("version:"+date+","+idp);
		String s = getSimplePage ("2_1 test version:"+dstr+","+idp);
			          ResponseBuilder r = Response.ok(s);
			          URI baseurl = ui.getBaseUri();
			          //String u=baseurl.toString() +"test/2_1/"+idp;
			          String origlink="<"+idp+">; rel=\"original\"";
			          String ut=baseurl.toString() +"test/2_1/timegate/"+idp;
			          String timegate=",<"+ut+">; rel=\"timegate\"";
			          r.header("Memento-Datetime", dstr);
			          r.header("Link",origlink+ timegate );
             return  r.build();
		 
	}
	
	
	
	
	@GET
	@Path("memento_2_2/{date}/{id:.*}")
	
	public  Response get3Memento (@PathParam("id") String idp,@PathParam("date") String date, @Context UriInfo ui, @Context HttpHeaders hh)  {
		Date mdate=null;
		try {
			mdate = formatterout.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		 String dstr = httpformatter.format(mdate);
		//String s = getSimplePage ("version:"+date+","+idp);
		String s = getSimplePage ("2_2 test version:"+dstr+","+idp);
			          ResponseBuilder r = Response.ok(s);
			          URI baseurl = ui.getBaseUri();
			        //  String u=baseurl.toString() +"test/2_2/"+idp;
			          String origlink="<"+idp+">; rel=\"original\"";
			          String ut=baseurl.toString() +"test/2_2/timegate/"+idp;
			          String timegate=",<"+ut+">; rel=\"timegate\"";
			          r.header("Memento-Datetime", dstr);
			          r.header("Link",origlink+ timegate );
             return  r.build();
		 
	}
	
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style
	//Pattern 3  	URI-R  	-  	URI-R  	-  
	//fixed resource
	@GET
	@Path("3/")
	@Produces("text/html;charset=UTF-8" )
	public  Response getPage( @Context UriInfo ui)  {
		  ResponseBuilder r = Response.ok(getSimplePage(null));
		  //String d ="Fri, 20 Mar 2009 11:00:00 GMT";
		  r.header("Memento-Datetime", "Fri, 20 Mar 2009 11:00:00 GMT");
		  URI baseurl = ui.getBaseUri();
		  String u=baseurl.toString() +"test/3/";
		  String link="<"+u+">; rel=\"original\"";
		  r.header("Link",link);
		  //r.lastModified( somedate);
		  return r.build() ;
		
	}
	
	@GET
	@Path("5/{id:.*}")
	@Produces("text/html;charset=UTF-8" )
	public  Response getPage_5( @Context UriInfo ui,@PathParam("id") String idp)  {
		  ResponseBuilder r = Response.ok(getSimplePage(idp));
		  //String d ="Fri, 20 Mar 2009 11:00:00 GMT";
		///  r.header("Memento-Datetime", "Fri, 20 Mar 2009 11:00:00 GMT");
		  URI baseurl = ui.getBaseUri();
		  String u=baseurl.toString() +"test/1_1_2/"+idp;
		 // String u=baseurl.toString() +"test/3/";
		  String link="<"+u+">; rel=\"timegate\"";
		  r.header("Link",link);
		  //r.lastModified( somedate);
		  return r.build() ;
		
	}
	
	//Pattern 4  	URI-R  	-  	URI-M  	-  
	//Pattern	Original Resource	TimeGate	Memento	Negotiation Style
	//memento without timegate
	@GET
	@Path("4/")
	public  Response get4Page( @Context UriInfo ui)  {
		  //get page from hello file
		  ResponseBuilder r = Response.ok(getSimplePage(null));
		  //String d ="Fri, 20 Mar 2009 11:00:00 GMT";
		  r.header("Memento-Datetime", "Tue, 22 May 2012 17:43:22 GMT");
		  URI baseurl = ui.getBaseUri();
		  String u=baseurl.toString() +"test/4/";
		  String link ="<http://lanlsource.lanl.gov/hello>;rel=\"original\"";
		  link = link + ",<"+u+">;rel=\"memento first\"; datetime=\"Tue, 22 May 2012 17:43:22 GMT\"";
		  r.header("Link", link);
		 
		 // r.header("Link",link);
		  //r.lastModified( somedate);
		  return r.build() ;
		
	}
	
	
	
	
	public static String getSimplePage (String text){
		String test ="<html>"+
		"<head>"+
		"<title>       </title>"+
		"<style type=\"text/css\">"+
		"<!-- "+
		"h1	{text-align:center; "+
			"font-family:Arial, Helvetica, Sans-Serif;" +
			"} "+
		"p	{text-indent:20px;"+
			"}"+
		"-->" +
		"</style>"+
		"</head>"+
		"<body bgcolor = \"#ffffcc\" text = \"#000000\">" +
		"<h1>Hello, World!</h1>" ;
		if (text!=null) {test=test+"<p>"+text+"</p>"; }
		test = test+"</body>" +
		"</html>";
		return test;
	}
}
