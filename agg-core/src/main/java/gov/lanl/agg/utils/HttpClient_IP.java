package gov.lanl.agg.utils;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

public class HttpClient_IP  {
	 Integer readTimeout=15000; //10sec  The timeout says how long to wait for the other end to send a SYN-ACK in response to the initial SYN packet(s).
	 private static final int TOTAL_MAX_CONNECTIONS = 4000;
	 Integer connectTimeout = readTimeout; //30sec tcp connection handshake
	 int maxConnectionsPerHost=200;
	 
	 private static CloseableHttpClient httpclient;
	 private static IdleConnectionMonitorThread monitor;
	 public HttpClient_IP(Integer readTimeout, Integer connectTimeout) {
		 readTimeout = readTimeout;
		 connectTimeout = connectTimeout;
	 }
	 
	 	 
	 
	 public CloseableHttpClient initclient(String hproxy,int port) {
		
		 ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
	            @Override
	            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
	                HeaderElementIterator it = new BasicHeaderElementIterator
	                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
	                while (it.hasNext()) {
	                    HeaderElement he = it.nextElement();
	                    String param = he.getName();
	                    String value = he.getValue();
	                    if (value != null && param.equalsIgnoreCase
	                       ("timeout")) {
	                        return Long.parseLong(value) * 1000;
	                    }
	                }
	                return 10 * 1000;
	            }
	        };
		 
		 
				  Builder builder = RequestConfig.custom()
				    .setSocketTimeout(readTimeout)
				    .setConnectTimeout(connectTimeout)
				    .setConnectionRequestTimeout(5000);
				  
				   if(hproxy!=null){
				    HttpHost proxy = new HttpHost(hproxy, port);
				    builder.setProxy(proxy);
				   }
				 RequestConfig defaultRequestConfig = builder.build();   
		 
		 PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
	        cm.setMaxTotal(TOTAL_MAX_CONNECTIONS);
	        cm.setDefaultMaxPerRoute(maxConnectionsPerHost);
	        
	         httpclient = HttpClients.custom()
	                .setConnectionManager(cm)
	                .setDefaultRequestConfig(defaultRequestConfig)
	                .disableCookieManagement()
	                 .setKeepAliveStrategy(myStrategy)
	                .setUserAgent("Mozilla/5.0 xx-dev-web-common httpclient/4.x")
	               // .disableContentCompression()?
	                .build();
	 
	        // Start up an eviction thread.
	        monitor = new IdleConnectionMonitorThread(cm);
	        // Don't stop quitting.
	        monitor.setDaemon(true);
	        monitor.start();
    
      return httpclient;
	}
	 public CloseableHttpClient get() {
	      return httpclient;
	    }

	 
	// Watches for stale connections and evicts them.
	    private class IdleConnectionMonitorThread extends Thread {
	      // The manager to watch.
	      private final PoolingHttpClientConnectionManager cm;
	      // Use a BlockingQueue to stop everything.
	      private final BlockingQueue<Stop> stopSignal = new ArrayBlockingQueue<Stop>(1);

	      // Pushed up the queue.
	      private class Stop {
	        // The return queue.
	        private final BlockingQueue<Stop> stop = new ArrayBlockingQueue<Stop>(1);

	        // Called by the process that is being told to stop.
	        public void stopped() {
	          // Push me back up the queue to indicate we are now stopped.
	          stop.add(this);
	        }

	        // Called by the process requesting the stop.
	        public void waitForStopped() throws InterruptedException {
	          // Wait until the callee acknowledges that it has stopped.
	          stop.poll(30, TimeUnit.SECONDS);
	        }

	      }
	 
	 
	      IdleConnectionMonitorThread(PoolingHttpClientConnectionManager cm) {
	          super();
	          this.cm = cm;
	        }

	        @Override
	        public void run() {
	          try {
	            // Holds the stop request that stopped the process.
	            Stop stopRequest;
	            // Every 5 seconds.
	            while ((stopRequest = stopSignal.poll(185, TimeUnit.SECONDS)) == null) {
	              // Close expired connections
	              cm.closeExpiredConnections();
	              // Optionally, close connections that have been idle too long.
	              cm.closeIdleConnections(60, TimeUnit.SECONDS);
	              // Look at pool stats.
	              //logger.debug("Stats: {}", cm.getTotalStats());
	              System.out.println("Stats: {}"+cm.getTotalStats());
	            }
	            // Acknowledge the stop request.
	            stopRequest.stopped();
	          } catch (InterruptedException ex) {
	            // terminate
	          }
	        }

	        public void shutdown() throws InterruptedException {
	          //logger.trace("Shutting down client pool");
	          // Signal the stop to the thread.
	          Stop stop = new Stop();
	          stopSignal.add(stop);
	          // Wait for the stop to complete.
	          stop.waitForStopped();
	          // Close the pool - Added
	          try {
	        	  httpclient.close();
	          } catch (IOException ioe) {
	              //logger.info("IO Exception while closing HttpClient connecntions.");
	          }
	          // Close the connection manager.
	          cm.close();
	         // logger.trace("Client pool shut down");
	        }

	      }

	    

	  public static void shutdown() throws InterruptedException {
	    // Shutdown the monitor.
	    monitor.shutdown();
	  }

	}

