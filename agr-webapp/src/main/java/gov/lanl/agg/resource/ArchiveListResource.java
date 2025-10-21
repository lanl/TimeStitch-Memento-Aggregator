package gov.lanl.agg.resource;

import gov.lanl.agg.ArchiveDescription;

import java.net.URI;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

/*
@author Lyudmila Balakireva
*/

@Path("/archives/*")

public class ArchiveListResource {
	static URI baseUri;
	static String proxybaseuri;
	 
	 // static StorageImpl storage;
	    //static CacheStorage storage;
	  //static String defaulttimemap;
	  //static int pagesize;
	  //static List tmlist;
	  static List archivedesc;
	  static {
	
	        
	        MyInitServlet cl = MyInitServlet.getInstance();
	        //Map  params = (Map) cl.getAttribute("params");
	        //tmlist = (List) cl.getAttribute("timemaplist");
	        archivedesc= (List) cl.getAttribute("archivedesc");
	      //  storage =  (CacheStorage) MyInitServlet.getInstance().getAttribute("storage");
	        //storage = new StorageImpl(params);
	       
	     
	  }
	  
	  
	public	ArchiveListResource( @Context UriInfo uriInfo )
    {
        this.baseUri = uriInfo.getBaseUri();      
    }
  	
	@GET
	//@Path("timegates/*")
	@Produces("application/xml" )
	public  Response getMyLinks( @Context UriInfo ui) throws ParseException {		
		StringBuffer sb= new StringBuffer();
		sb.append("<?xml version=\"1.0\"?>\n<archives>\n");
		Iterator it = archivedesc.iterator();		
		while ( it.hasNext()) {
		ArchiveDescription ar =	(ArchiveDescription) it.next();

			sb.append("<archive id=\""+ar.getName()+"\" >\n");
			sb.append("<timegate uri=\""+ ar.getTimegate()  +"\" />\n");
			sb.append("<memento uri=\""+ ar.getMementotemplate()  +"\" />\n");
			sb.append("<policy type=\""+ ar.getAccesstatus()  +"\" rewritestatus=\""+ ar.getRewritestatus() +  "\" mementostatus=\""+ ar.getMementostatus()+ "\"/>\n");
			sb.append("</archive>\n");
		}
		sb.append("</archives>");
       	
		 ResponseBuilder r = Response.ok(sb.toString());
		  
		   return  r.build(); 
		
		
}
	
	
	
	
}
