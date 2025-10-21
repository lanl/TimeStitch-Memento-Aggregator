package gov.lanl.agg.batch;




import java.util.Date;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import gov.lanl.agg.utils.MementoUtils;


/*
@author Lyudmila Balakireva

*/ 
public class QueStats extends Thread {
		    
	         Logger qlogger = Logger.getLogger("qstats");
		     private boolean running = true;
		     RunMeBatch task;
		     PriorityBlockingQueue q;
		    
		     public QueStats(PriorityBlockingQueue q,RunMeBatch task){
		    	 this.task = task;
		    	 this.q = q;
				}
		     
		    @Override
		    public void run() {
		        // Keeps running indefinitely, until the termination flag is set to false
		        while (running) {
		        	   try {
		        		   
		        		      long qcount =  task.check_Que()+q.size();
		        		      Date updtime = new Date();
		      				  String updtimelog = MementoUtils.timeTravelMachineFormatter.format(updtime);
		      			      qlogger.info(qcount +" "+updtimelog); 	  
		        		      
		        		      TimeUnit.HOURS.sleep(6);
							
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

	   

