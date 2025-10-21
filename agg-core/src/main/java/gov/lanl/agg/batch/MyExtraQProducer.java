package gov.lanl.agg.batch;

import java.util.concurrent.TimeUnit;
	 
public class MyExtraQProducer extends Thread {
		     //BlockingQueue q = null;
		     private boolean running = true;
		     RunMeBatch task;
		     private boolean dailyrefresh = false;
		     private boolean dailydelete = false;
		     int interval = 1 * 30 * 1000*60*24;
		     
		     public MyExtraQProducer(RunMeBatch task){
		    	 this.task = task;
				// this.q = q;
			 }
		     
		     public void setLocalDailyRefresh(boolean refresh) {
		    	 dailyrefresh = refresh;
		     }
		     public void setLocalDailyDelete(boolean refresh) {
		    	 dailydelete = refresh;
		     }
		     
		     public void setSleepInterval(int interval) {
		    	 interval = interval;
		     }
		    @Override
		    public void run() {
		        // Keeps running indefinitely, until the termination flag is set to false
		        while (running) {
		        	   try {
		        		   
		        		    
		        		     if (dailyrefresh) {   
		        		    	 System.out.println("once a day refresh:");	 
		        		         task.populateJobs();
		        		     // task.cleanQue();
		        		     }
		        		     if (dailydelete) {
		        		    	 System.out.println("once a day delete:");	 	 
		        		      task.cleanCache();
		        		     }
		        		    //int minuta = 1 * 30 * 1000;
		        		    // int interval = minuta*60*24;
		        		   // System.out.println("q size after select jobs:"+q.size());
							// Thread.sleep(interval);		// Sleep for 1 minutes	
		        		   //  TimeUnit.SECONDS.sleep(5);
							   TimeUnit.HOURS.sleep(12);
						} catch (Exception e) {
							 //TODO Auto-generated catch block
							e.printStackTrace();
							  System.out.println("Counter Thread in run() - interrupted while sleeping");
						}
					   
		        }
		    }
		 
		    // Terminates thread execution
		    public void halt() {
		        this.running = false;
		    }
		}

	   

