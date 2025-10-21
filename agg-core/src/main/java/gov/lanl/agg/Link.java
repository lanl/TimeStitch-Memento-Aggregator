package gov.lanl.agg;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
public class Link {

	       //protected String title;
	       @Expose
	       @SerializedName("rel")
	       protected String relationship;
	       @Expose
	       @SerializedName("uri")
	       protected String href;
	 	   protected String type;
	 	   @Expose
	 	   @SerializedName("datetime")
	 	   protected String datetime;
	 	   //what format?
	 	   protected MultivaluedMap<String, String> extensions = new MultivaluedMapImpl();
	
	 	  public Link()
	 	 	   {
	 	 	   }
	 	 	
	 	 	   public Link(String datetime, String relationship, String href, String type, MultivaluedMap<String, String> extensions)
	 	 	   {
	 	 	      this.relationship = relationship;
	 	 	      this.href = href;
	 	 	      this.type = type;
	 	 	      this.datetime = datetime;
	 	 	      if (extensions != null) this.extensions = extensions;
	 	 	   } 
	 	   
	 	 	 public String getRelationship()
	 	 	   {
	 	 		      return relationship;
	 	 		   }
	 	 		
	 	 		   public void setRelationship(String relationship)
	 	 		   {
	 	 		      this.relationship = relationship;
	 	 		   }
	 	 		
	 	 		   public String getHref()
	 	 		   {
	 	 		      return href;
	 	 		   }
	 	 		
	 	 		   public void setHref(String href)
	 	 		   {
	 	     	      this.href = href;
	 	 		   }
	 	 		
	 	 		   public String getType()
	 	 		   {
	 	 		      return type;
	 	 		   }
	 	 		
	 	 		   public void setType(String type)
	 	 		   {
	 	 		      this.type = type;
	 	 		   }
	 	 		   
	 	 		// public String getTitle()
	 	 		  // {
	 	 			//      return title;
	 	 			  // }
	 	 			
	 	 			   //public void setTitle(String title)
	 	 			   //{
	 	 			     // this.title = title;
	 	 			   //}
	 	 		
	 	 			 public String getDatetime()
		 	 		   {
		 	 			      return datetime;
		 	 			   }
		 	 			
		 	 			   public void setDatetime(String datetime)
		 	 			   {
		 	 			      this.datetime = datetime;
		 	 			   }
	 	 			   
	 	 			 public MultivaluedMap<String, String> getExtensions()
	 	 				   {
	 	 				      return extensions;
	 	 				   }
}
