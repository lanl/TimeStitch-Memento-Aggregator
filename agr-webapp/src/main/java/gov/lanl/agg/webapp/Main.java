package gov.lanl.agg.webapp;





public class Main {
	 public static void main(String[] argsArray) throws Exception {
	        Args args = new Args( argsArray );
	        //need to add path to config
	        //System.setProperty( "ta.storage.basedir", args.get( "path", "target/db" ) );
	        //System.out.println("dir to"+System.getProperty("ta.storage.basedir"));
	      //  System.setProperty( "index", args.get("indexClass",  "gov.lanl.archive.index.cassandra.Index"));
	       // System.setProperty( "warcdir", args.get( "warcpath", "wa-db" ) );
	        
	        int port = args.getNumber( "port", AggServer.DEFAULT_PORT ).intValue();
	        String sdir = args.get("sdir", "/var/www");
	         System.out.println("sdir" + sdir);
	        final String baseUri = AggServer.getLocalhostBaseUri( port );
	        System.out.println(String.format("Running server at [%s]", baseUri));
	      //  ArchiveConfig.loadConfigFile();
	        AggServer.INSTANCE.startServer( port,sdir );
	        //System.out.println("Press Ctrl-C to kill the server");
	        
	        // TODO We couldn't have a System.in.read() here since Java Service Wrapper
	        // couldn't keep the service running if we had :)
//	        System.in.read();
	       
	        Runtime.getRuntime().addShutdownHook( new Thread()
	        {
	            @Override public void run()
	            {
	                try
	                {
	                    System.out.println( "Shutting down the server" );
	                    AggServer.INSTANCE.stopServer();
	                   // ArchiveConfig.shutdown();
	                   // TripleStoreLocator.shutdownSailDatabase();
	                    //DatabaseLocator.shutdownGraphDatabase(new URI(baseUri));
	                }
	                catch ( Exception e )
	                {
	                    throw new RuntimeException( e );
	                }
	            }
	        } );
	    }
}
