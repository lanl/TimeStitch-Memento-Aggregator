package gov.lanl.agg;

import java.util.Date;

public class TimeMapLinkDesc {

	Date fromdate;
	Date untildate;
	int  total;
	       public Date getFromdate()
	       {
		      return fromdate;
		   }
		
		   public void setFromdate(Date datetime)
		   {
		      this.fromdate = datetime;
		   }
		   public Date getUntildate()
	       {
		      return untildate;
		   }
		
		   public void setUntildate(Date datetime)
		   {
		      this.untildate = datetime;
		   }
		   public void setTotal(int tot){
			   total=tot;
		   }
		   public int getTotal() {
			   return total;
		   }
}
