package gov.lanl.agg.webapp;



import gov.lanl.agg.helpers.ArchiveListConfig;
//import gov.lanl.agg.resource.*;
import gov.lanl.agg.resource.MyInitServlet;


import java.net.URI;
import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Logger;

//import javax.servlet.DispatcherType;
import javax.ws.rs.core.UriBuilder;

//import org.glassfish.grizzly.http.server.HttpServer;
//import org.glassfish.grizzly.http.server.NetworkListener;
//import org.glassfish.grizzly.servlet.FilterRegistration;
//import org.glassfish.grizzly.servlet.WebappContext;
//import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
//import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
//import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
//import org.glassfish.jersey.server.ResourceConfig;

import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
//import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.jersey.spi.container.servlet.ServletContainer;
//import org.glassfish.jersey.servlet.ServletContainer;

//import com.sun.jersey.api.core.PackagesResourceConfig;
public enum AggServer {
	INSTANCE;
	
	public static final int DEFAULT_PORT = 80;
	public static final String DEFAULT_DIR="/var/www";
    private GrizzlyWebServer server;
    private int port = DEFAULT_PORT;
    public static final int DEFAULT_MAX_THREADS = 500;
    public static final int DEFAULT_MIN_THREADS = 5;

    public static final String CONFIG_MIN_THREADS = "rest.min.grizzly.threads";
    public static final String CONFIG_MAX_THREADS = "rest.max.grizzly.threads";
    private int minThreads = DEFAULT_MIN_THREADS;
    private int maxThreads = DEFAULT_MAX_THREADS;
    String webResourcesPath="/";
    private static final Logger logger = Logger.getLogger(AggServer.class.getName());
    public void startServer() {
        startServer( DEFAULT_PORT,DEFAULT_DIR);
        
    }
   String JERSEY_SERVLET_CONTEXT_PATH="";

  

	//private HttpServer httpServer;
	public Map<String, String> prop;

	

	public void startnewServer(int port, String sdir) {
		this.port = port;

		try {
			System.out.println("Starting grizzly...");
/*
			URI serverUri = UriBuilder.fromUri("http://localhost/").port(port).build();
			Map<String, String> prop = ArchiveListConfig.loadConfigFile();
        	
			ResourceConfig rc = new ResourceConfig();
			//rc.packages("gov.lanl.agg.resource");
			
			rc.register(ArchiveListResource.class).register(ArchiveResource.class);
			rc.register(TimeTravelAPResource.class);
			
			httpServer = GrizzlyHttpServerFactory.createHttpServer(serverUri, rc);
			
			
			//setThreadLimits(prop);
			// Initialize and register Jersey Servlet
			WebappContext context = new WebappContext("WebappContext", JERSEY_SERVLET_CONTEXT_PATH);
			context.addListener(MyInitServlet.class);
			FilterRegistration registration = context.addFilter("ServletContainer", ServletContainer.class);
			registration.setInitParameter("javax.ws.rs.Application", ResourceConfig.class.getName());
			EnumSet<DispatcherType> dispatcherTypes = EnumSet.allOf(DispatcherType.class);
			//registration.
			registration.addMappingForUrlPatterns(dispatcherTypes, "/*");
			//registration.addMappingForUrlPatterns(dispatcherTypes, "/*");
			context.deploy(httpServer);
			

			httpServer.start();

			setThreadLimits(prop);
*/
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
   
   public void startServer( int port,String sdir  ) {
        this.port = port;
        //final HashMap<String, String> initParams = new HashMap<String, String>();
        //initParams.put("com.sun.jersey.config.property.packages", "gov.lanl.archive.resource");

        try {
        	
        	server = new GrizzlyWebServer(port);
        	//need to add config file; //mysql params/threads/iips url/proxy 
        	Map<String, String> prop = ArchiveListConfig.loadConfigFile();
        	
        	   // Set max/min threads
             setThreadLimits(prop);
        	// server.setMaxThreads(  DEFAULT_MAX_THREADS );
            // server.setCoreThreads( DEFAULT_MIN_THREADS );
             
            

             
             
             
            // Create our REST service
            ServletAdapter jersey = new ServletAdapter(sdir);
            jersey.setHandleStaticResources(true);
            jersey.setServletInstance( new ServletContainer() );

            // Tell jersey where to find REST resources
            jersey.addInitParameter( "com.sun.jersey.config.property.packages",
                    "gov.lanl.agg.resource" );
            //jersey.setServletPath( "/" );
            jersey.setContextPath("/");
            //jersey.setServletPath( "/aggr" );
           // jersey.addServletContextListener(new MyServletContextListener());
            jersey.addServletListener(MyInitServlet.class.getName());
            server.addGrizzlyAdapter( jersey,new String[]{"/"});
            
           
            //MyInitServlet cl = MyInitServlet.getInstance();
            
          
            server.start();
            
            
          
        	
        	/*
        	 System.out.println("Starting grizzly...");
        	 logger.info("Starting Loggin...");
			 threadSelector = GrizzlyWebContainerFactory.create(getLocalhostBaseUri( port ), initParams);
			 GrizzlyAdapter adapter  = (GrizzlyAdapter) threadSelector.getAdapter();
			 adapter.setHandleStaticResources(true); 
			 */
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    public String getBaseUri()
    {
        return getLocalhostBaseUri( port );
    }
  /*  
    public void stopServer() {
        threadSelector.stopEndpoint();
    }
    */
    public void stopServer()
    { //httpServer.stop();
        server.stop();
    }
    public static String getLocalhostBaseUri()
    {
        return getLocalhostBaseUri( DEFAULT_PORT );
    }
    
    public static String getLocalhostBaseUri( int port )
    {
        return "http://localhost:" + port + "/";
    }
    
   /* Check if there are thread limits set in the config file, use them if they
    * are integers. Otherwise fall back to defaults.
    */
    
    
 
   private void setThreadLimits(Map prop)
   {
	   
       if ( prop.containsKey(CONFIG_MIN_THREADS ) )
       {
           try
           {
               minThreads = Integer.valueOf( (String) prop.get(CONFIG_MIN_THREADS ) );
           }
           catch ( Exception e )
           {
           }
       }

       if ( prop.containsKey( CONFIG_MAX_THREADS ) )
       {
           try
           {
               maxThreads = Integer.valueOf( (String) prop.get(CONFIG_MAX_THREADS ) ); 
               System.out.println("test" +  maxThreads);
           }
           catch ( Exception e )
           {
           }
       }

       // Ensure min threads is never larger than max threads
       minThreads = maxThreads >= minThreads ? minThreads : maxThreads;
       System.out.println("maxthreads"+ maxThreads);
       System.out.println("maxthreads"+ minThreads);
     /*  ThreadPoolConfig config = ThreadPoolConfig.defaultConfig().setPoolName("mypool").setCorePoolSize(minThreads)
				.setMaxPoolSize(maxThreads);

		NetworkListener listener = httpServer.getListeners().iterator().next();
		GrizzlyExecutorService threadPool = (GrizzlyExecutorService) listener.getTransport().getWorkerThreadPool();
		threadPool.reconfigure(config);
		
       */
       
       //httpServer.
       server.setMaxThreads( maxThreads );
       server.setCoreThreads( minThreads );
   }
    
    
}
