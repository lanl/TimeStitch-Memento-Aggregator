package gov.lanl.agg;

import java.util.concurrent.atomic.AtomicInteger;

public class CacheStats {

	String service;
	//String pdate;
	 private AtomicInteger miss = new AtomicInteger(0);
	 private AtomicInteger hit = new AtomicInteger(0);
	 private AtomicInteger stale = new AtomicInteger(0);
	
	
	      public CacheStats(String service){
	    	  this.service = service;
	      }
	
	      public String getService()
		   {
		      return service;
		   }
				 	      
		      public int getMiss()
		   {
		      return miss.get();
		   }
				 
		 
		   public void incrementMiss()
		   {
			   miss.incrementAndGet();
		   }
		   
		   public int getStale()
		   {
		      return stale.get();
		   }
				 
		 
		   public void increamentStale()
		   {
		      stale.incrementAndGet();
		   }
		   
		   public int getHit()
		   {
		      return hit.get();
		   }
				  
		 
		   public void incrementHit()
		   {
		       hit.incrementAndGet();
		   }
		   
		   public int resetHits() {
			   return hit.getAndSet(0);
			  
		   }
		   public int resetStale() {
			   return stale.getAndSet(0);
			  
		   }
		   
		   public int resetMiss() {
			   return miss.getAndSet(0);
			  
		   }
		   
}
