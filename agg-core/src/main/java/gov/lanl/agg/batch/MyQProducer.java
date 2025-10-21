package gov.lanl.agg.batch;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.leansoft.bigqueue.IBigQueue;
/*
@author Lyudmila Balakireva

*/ 
public class MyQProducer extends Thread {
		     PriorityBlockingQueue q = null;
		     IBigQueue bigQueue = null;
		     private boolean running = true;
		     RunMeBatch task;
		     public MyQProducer(PriorityBlockingQueue q,RunMeBatch task){
		    	 this.task = task;
				 this.q = q;
			 }
		     
		     public MyQProducer( IBigQueue q,RunMeBatch task){
		    	 this.task = task;
				 this.bigQueue = q;
			 }
		    @Override
		    public void run() {
		        // Keeps running indefinitely, until the termination flag is set to false
		        while (running) {
		        	   try {
		        		    //System.out.println("q size before select jobs:"+q.size());
		        		    task.selectJobs(q);
		        		    //task.selectJobs(bigQueue);
		        		    
		        		    //System.out.println("q size after select jobs:"+bigQueue.size());
		        		    //check size of the q every 10 sec, if it less 100 
							//Thread.sleep(1 * 30 * 1000);		// Sleep for 1/2 minutes			    	
							 TimeUnit.SECONDS.sleep(5);
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

	   

