package gov.lanl.agg.utils;

public class OriginalResource {
	 String originalUrl=null;
	 String timegate=null;
	 String timemap=null;
	 String timemapindex=null;
	 
	 boolean timegateflag = false;
	 
	 public String getTimeGateURI()
	 { return this.timegate;}
	 
	
	 public void setTimeGateURI(String timegate)
	 {  this.timegate=timegate;}
	 
	 public void setTimeMapURI(String itimemap ){
		this.timemap=itimemap; 
	 }
	 public String getTimeMapURI(){
			return timemap; 
		 }
	 //special case of our transactional archive
	 public void setTimeMapIndexURI(String itimemap ){
			 this.timemapindex = itimemap; 
		 }
	 public String getTimeMapIndexURI(){
			return timemapindex; 
		 }
	 public String getOriginaURI()
	 { return this.originalUrl;
	 }
	 
	 public void setOriginalURI(String url){
		this.originalUrl = url; 
	 }
	 
	 
	  public boolean isTimeGate(){
		 
		return  timegateflag;
	 }
	  
	 public void setTimegateFlag(){
		 timegateflag = true;
	 }
	 
	  // public void TimeGate(boolean timegateflag)
	    // {
			//timegateflag=timegateflag;
		 //}
		 	 
}
