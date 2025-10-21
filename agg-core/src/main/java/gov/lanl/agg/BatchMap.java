package gov.lanl.agg;

import gov.lanl.agg.Link;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;



public class BatchMap {
	    @Expose
        @SerializedName("original_uri")
        protected String originalUrl;
	    @Expose
        @SerializedName("request_date")
       
	  
	    protected Date requestDatetime;
	    @Expose
        @SerializedName("update_date")
        
		protected Date updateDatetime;
		protected String http_statuses;
		    
		protected Map<String, List<Link>> mementos = new HashMap<>();
		
	    public String getOriginalUrl() {
	        return originalUrl;
	    }

	    public void setOriginalUrl(String href) {
	        this.originalUrl = href;
	    }

	    public Date getRequestDatetime() {
	        return requestDatetime;
	    }

	    public void setRequestDatetime(Date aDatetime) {
	        this.requestDatetime = aDatetime;
	    }
	    
	    public Date getUpdateDatetime() {
	        return updateDatetime;
	    }

	    public void setUpdateDatetime(Date aDatetime) {
	        this.updateDatetime = aDatetime;
	    }
	    
	    public Map<String, List<Link>> getMementos() {
	        return mementos;
	    }

	    public void addMementos( String archive,List m) {
	    	 mementos.put(archive,m);
	       
	    }

}
