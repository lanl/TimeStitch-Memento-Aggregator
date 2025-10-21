package gov.lanl.agg.utils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;

public class HttpClientTest  {
	 Integer readTimeout=15000; //10sec  The timeout says how long to wait for the other end to send a SYN-ACK in response to the initial SYN packet(s).
	 private static final int TOTAL_MAX_CONNECTIONS = 4000;
	 Integer connectTimeout = readTimeout; //30sec tcp connection handshake
	 int maxConnectionsPerHost=200;
	 HttpClient httpClient;
	 
	 public HttpClientTest(Integer readTimeout, Integer connectTimeout) {
		 readTimeout = readTimeout;
		 connectTimeout = connectTimeout;
	 }
	 
	 public ApacheHttpClient initclient(String hproxy,int port) {
	 MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
     connectionManager.getParams().setConnectionTimeout(connectTimeout);
	 connectionManager.getParams().setSoTimeout(readTimeout);
	 connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
      httpClient = new HttpClient(connectionManager);
      httpClient.getParams().setParameter("http.useragent", "TimeTravelAggregator-lanl;Browser");
     if (hproxy!=null){
     httpClient.getHostConfiguration().setProxy(hproxy,port);
     }
     ApacheHttpClientHandler httpClientHandler = new ApacheHttpClientHandler(httpClient);
   
     ApacheHttpClient contentServerClient = new ApacheHttpClient(httpClientHandler);
     contentServerClient.setConnectTimeout(connectTimeout);
     contentServerClient.setReadTimeout(readTimeout);
    
      return contentServerClient;
	}
	 
	 public HttpClient getHttpClient(){
		 return httpClient;
	 }
}
