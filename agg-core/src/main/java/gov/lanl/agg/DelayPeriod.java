package gov.lanl.agg;

public class DelayPeriod {
	String type;
	Integer period;
	
	public 	 void  setPeriod(Integer pstatus) {
		 period=pstatus; 
	 }
	 
    public	 Integer getPeriod() {
		 return period;
	 }
    
	public 	 void  setType(String type) {
		 this.type=type; 
	 }
	 
    public	 String getType() {
		 return type;
	 }
}
