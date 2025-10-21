package gov.lanl.agg.batch;


import gov.lanl.agg.CacheStats;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.leansoft.bigqueue.IBigQueue;
/*
@author Lyudmila Balakireva

*/ 
public class StatsUpdater extends Thread {
		    
		    
		     private boolean running = true;
		     RunMeBatch task;
		     List stats;
		     CacheStats tm;
		     CacheStats tg;
		     CacheStats tt;
		     CacheStats batch;
		     public StatsUpdater(CacheStats tg,CacheStats tm,CacheStats tt,CacheStats batch,RunMeBatch task){
		    	 this.task = task;
		    	 this.tg = tg;
		    	 this.tm = tm;
		    	 this.tt = tt;
		    	 this.batch = batch;
				}
		     
		    @Override
		    public void run() {
		        // Keeps running indefinitely, until the termination flag is set to false
		        while (running) {
		        	   try {
		        		   // Iterator it = stats.iterator();   
		        		   // while (it.hasNext()) {
		        		    //	 CacheStats cs = (CacheStats) it.next();
		        		        if (tm!=null) {
		        		         task.updateStats(tm);
		        		         }
		        		        if (tt!=null){
		        		         task.updateStats(tt);
		        		        }
		        		        if ( tg!=null) {
		        		         task.updateStats(tg);
		        		        }
		        		        if (batch!=null){
		        		        	task.updateStats(batch);
		        		        }
		        		    //}
		        		    //task.selectJobs(bigQueue);
		        		    
		        		    //System.out.println("q size after select jobs:"+bigQueue.size());
		        		    //check size of the q every 10 sec, if it less 100 
							//Thread.sleep(1 * 30 * 1000);		// Sleep for 1/2 minutes			    	
							 TimeUnit.MINUTES.sleep(15);
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

	   

