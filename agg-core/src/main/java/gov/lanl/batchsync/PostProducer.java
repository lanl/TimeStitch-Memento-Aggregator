package gov.lanl.batchsync;
import com.leansoft.bigqueue.IBigQueue;

public class PostProducer extends Thread {
		     //BlockingQueue q = null;
		     private boolean running = true;
		     URLPostClient task;
		     private boolean notify = false;
		     IBigQueue bigQueue  = null;
		     int interval = 1 * 30 * 1000*60*24;
		     
		     public PostProducer(URLPostClient task,IBigQueue bigQueue){
		    	 this.task = task;
		    	 //this.bigQueue = task.getQue();
		    	 this.bigQueue = bigQueue;
				// this.q = q;
			 }
		     
		     public void setNotify(boolean refresh) {
		    	 notify = refresh;
		     }
		    // public void setLocalDailyDelete(boolean refresh) {
		    	// dailydelete = refresh;
		     //}
		     
		     public void setSleepInterval(int interval) {
		    	 interval = interval;
		     }
		    @Override
		    public void run() {
		        // Keeps running indefinitely, until the termination flag is set to false
		        while (running) {
		        	   try {
		        		    //System.out.println("q size before select jobs:"+q.size());
		        		    
		        		     if (notify) { 
		        		    	 if (bigQueue!=null) {
		        		    	 if (!bigQueue.isEmpty()){
		        		    		 String item = new String(bigQueue.dequeue());
		        		    		 System.out.println("from postproducer:"+item);
		        		             task.seedurl_put(item);
		        		    	 }    
		        		    	 }
		        		     }
		        		     
		        		    //int minuta = 1 * 30 * 1000;
		        		    // int interval = minuta*60*24;
		        		   // System.out.println("q size after select jobs:"+q.size());
							// Thread.sleep(interval);		// Sleep for 1 minutes			    	
							
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

	   

