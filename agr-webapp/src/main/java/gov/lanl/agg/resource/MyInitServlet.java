package gov.lanl.agg.resource;

import gov.lanl.agg.CacheStats;
import gov.lanl.agg.batch.MyExtraQProducer;
import gov.lanl.agg.batch.MyQConsumer;
import gov.lanl.agg.batch.MyQProducer;
import gov.lanl.agg.batch.QueStats;
import gov.lanl.agg.batch.RunMeBatch;
import gov.lanl.agg.batch.StatsUpdater;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.helpers.ArchiveListConfig;
import gov.lanl.agg.utils.AbbrSQLUtils;
import gov.lanl.agg.utils.HttpClientTest;
import gov.lanl.agg.utils.HttpClient_IP;
import gov.lanl.batchsync.PostProducer;
import gov.lanl.batchsync.URLPostClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.LogManager;
//import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;

//import gov.lanl.aggregatorml.main.ClassifierPool;
/*
@author Lyudmila Balakireva

*/
 
public class MyInitServlet implements ServletContextListener {
	// private static final String String = null;
	ServletContext context;
	// Scheduler scheduler = null;
	private ExecutorService executor;
	// private ExecutorService exec;
	private ExecutorService bexec;
	private ExecutorService epool;
	BoneCP connectionPool = null;
	BoneCP abbrconnectionPool = null;
	IBigQueue ResSyncQueue = null;
	IBigQueue LiveDownloadQue = null;
	RunMeBatch task = null;
	ExecutorService statsexec=null;
	private static MyInitServlet _instance;
	List stats = new ArrayList();
	// BlockingQueue inputq=null;
	PriorityBlockingQueue inputq = null;
	CacheStats tgstats = new CacheStats("tg");
	CacheStats tmstats = new CacheStats("tm");
	CacheStats trstats = new CacheStats("tt");
	CacheStats batchstats = new CacheStats("batch");
	StatsUpdater sttg = null;
	QueStats qupdater = null;
	HttpClient_IP clientip;
	CacheStorage storage = null;

	// AppConfig config;
	public void contextDestroyed(ServletContextEvent contextEvent) {
		// ArchiveConfig.shutdown();
		System.out.println("Context Destroyed");

		int largestPoolSize = ((ThreadPoolExecutor) executor).getLargestPoolSize();
		int activecount = ((ThreadPoolExecutor) executor).getActiveCount();
		int poolcount = ((ThreadPoolExecutor) executor).getPoolSize();
		System.out.printf("fixed thread pool largest size was %d threadsn", largestPoolSize);
		System.out.printf("active  size was %d threadsn", activecount);
		System.out.printf("active  size was %d threadsn", poolcount);

		task.rollbackJobs(inputq);
		// Iterator it = stats.iterator();
		// while (it.hasNext()) {
		// CacheStats cs = (CacheStats) it.next();
		if (tgstats != null) {
			task.updateStats(tgstats);
		}
		if (tmstats != null) {
			task.updateStats(tmstats);
		}
		if (trstats != null) {
			task.updateStats(trstats);
		}
		if (batchstats != null) {
			task.updateStats(batchstats);
		}
		
		context = null;
		executor.shutdown();
		epool.shutdown();
		statsexec.shutdown();
		// exec.shutdown();
		bexec.shutdown();
		LogManager.shutdown();
		
		try {
			clientip.shutdown();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		connectionPool.close();
		if (abbrconnectionPool != null) {
			abbrconnectionPool.close();
		}
		connectionPool.shutdown();
		if (abbrconnectionPool != null) {
			abbrconnectionPool.shutdown();
		}
		try {
			if (ResSyncQueue != null) {
				ResSyncQueue.flush();
				ResSyncQueue.close();
			}
			if (LiveDownloadQue != null) {
				LiveDownloadQue.flush();
				LiveDownloadQue.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void loadBlacklisted(Map params) {
		HashMap<String, String> blacklistedDomains = new HashMap<String, String>();

		if (params.containsKey("domains.blacklist.filepath")) {
			String blacklistDomainsFile = (String) params.get("domains.blacklist.filepath");
			try {
				BufferedReader br = new BufferedReader(new FileReader(blacklistDomainsFile));
				String line = null;
				while ((line = br.readLine()) != null) {
					blacklistedDomains.put(line, null);
				}
				br.close();
			} catch (IOException ignore) {
				System.out.println("Domain blacklist file not found at " + blacklistDomainsFile);

			}
		}
		System.out.println(blacklistedDomains.size() + " domains were included in the blacklist.");
		context.setAttribute("blacklistDomains", blacklistedDomains);
	}

	private static void initializeLogger()
    {
      Properties logProperties = new Properties();

      try
      {
        // load our log4j properties / configuration file
    	  //ClassLoader c_l = TimeGateResource.class.getClassLoader();

          //java.io.InputStream in;

          //if (c_l != null) {
            //  in = c_l.getResourceAsStream("log4j_agg.xml");
          //} else {
            //  in = ClassLoader.getSystemResourceAsStream("log4j_agg.xml");

          //}
        //logProperties.load(in);
        //System.out.println(logProperties.toString());
        //PropertyConfigurator.configure(logProperties);
        DOMConfigurator.configure("log4j_agg.xml");
        System.out.println("log4j initialized");
       
      }
      catch(Exception e)
      {
        System.out.println("log4j properties not found");
      }
    } 
	
	public void initHTTPClientForWebServices(Map params) {
		int numofbatchprocess = 9;
		String tnum = (String) params.get("batch.timemap.thread.number");
		if (tnum != null) {
			numofbatchprocess = Integer.parseInt(tnum);
		}

		String to = (String) params.get("socket.timeout.millisecond");
		Integer socktimeout = 15000;
		if (to != null) {
			socktimeout = Integer.parseInt(to);
		}
		String toc = (String) params.get("socket.conntimeout.millisecond");
		Integer sockconntimeout = 15000;
		if (toc != null) {
			sockconntimeout = Integer.parseInt(toc);
		}

		HttpClientTest httptest = new HttpClientTest(socktimeout, sockconntimeout);

		ApacheHttpClient client;

		if (params.containsKey("web.proxyout.host")) {
			String proxyurl = (String) params.get("web.proxyout.host");
			String proxyport = (String) params.get("web.proxyout.port");
			int pport = Integer.parseInt(proxyport);
			client = httptest.initclient(proxyurl, pport);
		} else {
			client = httptest.initclient(null, 0);
		}

		HttpClient cli_u = httptest.getHttpClient();
		context.setAttribute("httpcli", cli_u);
		context.setAttribute("httpclient", client); // phasing out this client

	}

	public void initHTTPClientForBatchServices(Map params) {
		// Aug 24 2015
		String bto = (String) params.get("batch.socket.timeout.millisecond");
		Integer bsocktimeout = 15000;
		if (bto != null) {
			bsocktimeout = Integer.parseInt(bto);
		}
		String btoc = (String) params.get("batch.socket.conntimeout.millisecond");
		Integer bsockconntimeout = 15000;
		if (btoc != null) {
			bsockconntimeout = Integer.parseInt(btoc);
		}
		HttpClientTest httptestb = new HttpClientTest(bsocktimeout, bsockconntimeout);
		ApacheHttpClient clientb;

		if (params.containsKey("web.proxyout.host")) {
			String proxyurl = (String) params.get("web.proxyout.host");
			String proxyport = (String) params.get("web.proxyout.port");
			int pport = Integer.parseInt(proxyport);
			clientb = httptestb.initclient(proxyurl, pport);
		} else {
			clientb = httptestb.initclient(null, 0);
		}

		HttpClient cli_b = httptestb.getHttpClient();
		context.setAttribute("httpclib", cli_b);
	}

	public void initThreadPoolForWebServices() {
		// cached
		executor = new ThreadPoolExecutor(0, 1500, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		// 60 sec default?
		((ThreadPoolExecutor) executor).setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		context.setAttribute("MY_EXECUTOR", executor);
	}

	public void initThreadPoolForBatchServices(Map params, int numofdefaultarchives) {
		int numofbatchprocess = 9;

		String tnum = (String) params.get("batch.timemap.thread.number");
		if (tnum != null) {
			numofbatchprocess = Integer.parseInt(tnum);
		}
		int archthreads = (numofdefaultarchives + 1) * numofbatchprocess * 2; // randomly
																				// multilplied
																				// by
																				// 2
		epool = new ThreadPoolExecutor(0, archthreads, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		// 60 sec default?
		((ThreadPoolExecutor) epool).setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

	}

	public void initbatch_update(){
				
		ArchiveListConfig config = ArchiveListConfig.getInstance();
		if (connectionPool==null) {
			createDbLinksPOOL(config.getParams());
		}
		if (task==null) {
			task = InitBatchTask(config.getParams());
			task.fupdateArchives( config.getTMList(), config.getTGList(), config.getMap());
		}
		int numofbatchprocess = 9;
		int numofdefaultarchives = config.getTMList().size();
		String tnum = (String) config.getParams().get("batch.timemap.thread.number");
		if (tnum != null) {
			numofbatchprocess = Integer.parseInt(tnum);
		}
		//? do /i need blacklisted for batch?
		//if (context.getAttribute("blacklistDomains")!=null) {
			//loadBlacklisted(config.getParams());
		//}
		initHTTPClientForBatchServices(config.getParams());
		initThreadPoolForBatchServices(config.getParams(), numofdefaultarchives);
		if( inputq==null) {
			 inputq = new PriorityBlockingQueue(1200, idComparator);
		}
		if (sttg==null) {
		 sttg = new StatsUpdater(tgstats, tmstats, trstats, batchstats, task);
		}
			

			HttpClient cli_b = (HttpClient) context.getAttribute("httpclib");
			long timebudget = 250L;
			String tbudget = (String) config.getParams().get("batch.timemap.thread.timebudget");

			if (tbudget != null) {
				timebudget = Long.parseLong(tbudget);
			}
			// setting producers
			System.out.println("Init Q process and daily update");
			bexec = Executors.newFixedThreadPool(numofbatchprocess + 2  + 1);
			bexec.execute(new MyQProducer(inputq, task));
			MyExtraQProducer myextrap = InitDailyCacheUpdate(task, config.getParams());
			bexec.submit(myextrap);
			bexec.execute(sttg);
			//if (qupdater!=null) {
				//bexec.execute(qupdater);
			//}
			if (storage==null) {
			 storage = InitStorage( config.getParams());
			}
			for (int i = 0; i < numofbatchprocess; i++) {
				System.out.println("setting out tasks");
				MyQConsumer batchTimemapTask = new MyQConsumer(inputq, (HashMap) reverse(config.getMap()), 
						config.getArchiveList(), config.getTMList(), task, epool, storage, cli_b,
						batchstats);
			
				batchTimemapTask.setTimeBudget(timebudget);
				batchTimemapTask.setRules((String) config.getParams().get("config.service.rules"));
				batchTimemapTask.setCacheRegistry((String) config.getParams().get("config.cache.registry"),
						(String) config.getParams().get("config.cache.self"));
				
				bexec.execute(batchTimemapTask);

				
			}
				
		
	}
	
	
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {

		this.context = sce.getServletContext();
		_instance = this;
		// path not used currently
		String path = context.getRealPath(context.getContextPath());
		System.out.println("listener init:" + path);
		context.setAttribute("path", path);
		initializeLogger();			
		ArchiveListConfig config = ArchiveListConfig.getInstance();
		System.out.println("starting db pool");
		createDbLinksPOOL(config.getParams());
		System.out.println("starting cache storage");
		CacheStorage storage = InitStorage(config.getParams());
		System.out.println("starting batch task ");
		task = InitBatchTask(config.getParams());
		System.out.println("starting archive update in db ");
		task.fupdateArchives(config.getTMList(), config.getTGList(), config.getMap());
    	System.out.println("loading blacklisted");
		loadBlacklisted(config.getParams());
    	
    	
		initHTTPClientForWebServices(config.getParams());
		initThreadPoolForWebServices();

		inputq = new PriorityBlockingQueue(1200, idComparator);		
		context.setAttribute("Myqueque", inputq);
    	String livesync = (String) config.getParams().get("resourcesync.live");
    	qupdater = new QueStats(inputq,task);
		//int postnum = 0;

		// sttg = new StatsUpdater(tgstats, tmstats, trstats, batchstats, task);

		String tsmflag = (String) config.getParams().get("timemap.static.style");
		if (tsmflag != null) {
			context.setAttribute("timemapstyle", tsmflag);
		} else {
			context.setAttribute("timemapstyle", "false");
		}

		String batchsync = (String) config.getParams().get("resourcesync.batch");
		if (batchsync.equals("true") ){
			initbatch_update();
		}
		statsexec = Executors.newFixedThreadPool(2);
		
		if (qupdater!=null) {
			statsexec.execute(qupdater);
			//statsexec.execute(sttg);
		}
		
		//this needto be cleaned
		//if (livesync.equals("true")) {
			//storage.setApplicationMode("true");
			//bexec = Executors.newFixedThreadPool(numofbatchprocess + postnum + 1);
			//bexec.execute(sttg);
			//context.setAttribute("liveclient", "true");

		//} else {
			/*
			 * if (batchsync.equals("true")) { System.out.println(
			 * "Init Q process and daily update") ; bexec =
			 * Executors.newFixedThreadPool(numofbatchprocess+2+postnum +1);
			 * bexec.execute(new MyQProducer(inputq,task)); MyExtraQProducer
			 * myextrap = InitDailyCacheUpdate(task,params);
			 * bexec.submit(myextrap); bexec.execute(sttg);
			 * //task.setURLPostClient(poclient); }
			 */
			//context.setAttribute("liveclient", "false");

		//}
	       

		// bexec.execute(new MyQProducer(bQueue,task));
		context.setAttribute("liveclient", "false");
		HashMap hmap = reverse(config.getMap());
		context.setAttribute("hmap", hmap);
		String mlbaseurl = (String) config.getParams().get("mlbaseurl");
		context.setAttribute("mlbaseurl", mlbaseurl);
		context.setAttribute("task", task);
		context.setAttribute("storage", storage);
		context.setAttribute("timemaplist", config.getTMList());
		context.setAttribute("timegatelist", config.getTGList());
		context.setAttribute("hostmap", config.getMap());
		context.setAttribute("params", config.getParams());
		context.setAttribute("timappaging", config.getPagingList());
		context.setAttribute("archivedesc", config.getArchiveList());

		context.setAttribute("timegatestats", tgstats);
		context.setAttribute("timemapstats", tmstats);
		context.setAttribute("timetravelstats", trstats);

		// context.setAttribute( "LiveDownloadQue", LiveDownloadQue);

	}

	public static <K, V> HashMap<V, K> reverse(Map<K, V> map) {
		HashMap<V, K> rev = new HashMap<V, K>();
		for (Map.Entry<K, V> entry : map.entrySet())
			rev.put(entry.getValue(), entry.getKey());
		return rev;
	}

	public static MyInitServlet getInstance() {
		return _instance;
	}

	public void setAttribute(String key, Object value) {
		context.setAttribute(key, value);
	}

	public Object getAttribute(String key) {
		Object value = context.getAttribute(key);
		return value;
	}

	public CacheStorage InitStorage(Map params) {
		String cs = (String) params.get("db.connectionstr");
		String class_impl = Load_storage(cs);
		CacheStorage storage = null;
		try {
			Class<?> myUsingClassClass = Class.forName(class_impl);
			Constructor myUsingClassConstr = myUsingClassClass.getConstructor(BoneCP.class);
			storage = (CacheStorage) myUsingClassConstr.newInstance(connectionPool);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return storage;
	}

	public RunMeBatch InitBatchTask(Map params) {
		RunMeBatch task = null;
		String cs = (String) params.get("db.connectionstr");
		String batch_impl = Load_batch(cs);
		try {
			Class<?> mybClassClass = Class.forName(batch_impl);
			Constructor mybClassConstr = mybClassClass.getConstructor(BoneCP.class);
			task = (RunMeBatch) mybClassConstr.newInstance(connectionPool);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return task;
	}

	public void InitSummaryModule(Map params) {

		String sum_module = (String) params.get("summary.module");
		if (sum_module.equals("true")) {
			createABRPOOL(params);
		}

		// context.setAttribute("abbrconnPool", abbrconnectionPool);

		AbbrSQLUtils autils = new AbbrSQLUtils(abbrconnectionPool);
		context.setAttribute("autils", autils);

	}

	public MyExtraQProducer InitDailyCacheUpdate(RunMeBatch task, Map params) {
		String refresh = (String) params.get("batch.timemap.daily.refresh");
		String purge = (String) params.get("batch.timemap.daily.purge");

		int interval = 1 * 30 * 1000 * 60 * 24;
		MyExtraQProducer myextrap = new MyExtraQProducer(task);
		if (refresh != null) {
			myextrap.setLocalDailyRefresh(Boolean.parseBoolean(refresh));
		}
		if (purge != null) {
			myextrap.setLocalDailyDelete(Boolean.parseBoolean(purge));
		}
		myextrap.setSleepInterval(interval);
		return myextrap;
	}

	public PostProducer InitLiveResourceSync(Map params, IBigQueue bigQueue, HttpClient cli_u) {
		String resourcesync = (String) params.get("resourcesync.live");
		PostProducer pp = null;
		try {
			// live aggregator mode

			if (resourcesync.equals("true")) {
				String resourcesyncdir = (String) params.get("resourcesync.live.dir");
				String postpoint = (String) params.get("resourcesync.live.postpoint");
				bigQueue = new BigQueueImpl(resourcesyncdir, "outqueue");
				URLPostClient postclient = new URLPostClient(postpoint);

				// need to find it in properties
				String tm = (String) params.get("baseuri.proxy") + "timemap/link/";
				postclient.setTimeMapUrl(tm);

				pp = new PostProducer(postclient, bigQueue);
				pp.setNotify(true);

			}

			// can be null
			context.setAttribute("ResSyncQue", bigQueue);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return pp;
	}

	public URLPostClient InitBatchResourceSync(Map params, HttpClient cli_u) {
		String resourcesyncbatch = (String) params.get("resourcesync.batch");
		URLPostClient poclient = null;
		if (resourcesyncbatch.equals("true")) {
			String postpoint = (String) params.get("resourcesync.batch.postpoint");
			poclient = new URLPostClient(postpoint);
			poclient.setBatchDownloadUrl((String) params.get("resourcesync.batch.download"));

		}
		return poclient;
	}

	public String Load_storage(String cs) {
		try {
			// load the database driver (make sure this is in your classpath!)
			// String cs=(String) params.get("db.connectionstr");
			String class_impl;
			// String batch_impl;
			if (cs.contains("cassandra")) {
				class_impl = "gov.lanl.agg.cache.cassandra.StorageImpl";
				// batch_impl="gov.lanl.agg.batch.cassandra.RunMeCassBatchTask";
				Class.forName("org.apache.cassandra.cql.jdbc.CassandraDriver");
				return class_impl;
			} else {

				class_impl = "gov.lanl.agg.cache.mysql.StorageImpl";
				// batch_impl="gov.lanl.agg.batch.RunMeBatchTask";
				Class.forName("com.mysql.jdbc.Driver");
				// abbrev db
				Class.forName("org.apache.cassandra.cql.jdbc.CassandraDriver");
				return class_impl;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public String Load_batch(String cs) {
		try {
			// load the database driver (make sure this is in your classpath!)
			// String cs=(String) params.get("db.connectionstr");
			// String class_impl;
			String batch_impl;
			if (cs.contains("cassandra")) {
				// class_impl="gov.lanl.agg.cache.cassandra.StorageImpl";
				batch_impl = "gov.lanl.agg.batch.cassandra.RunMeCassBatchTask";
				Class.forName("org.apache.cassandra.cql.jdbc.CassandraDriver");
				return batch_impl;
			} else {

				// class_impl="gov.lanl.agg.cache.mysql.StorageImpl";
				batch_impl = "gov.lanl.agg.batch.RunMeBatchTask";
				Class.forName("com.mysql.jdbc.Driver");
				// abbrev db
				Class.forName("org.apache.cassandra.cql.jdbc.CassandraDriver");
				return batch_impl;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public void createABRPOOL(Map params) {
		BoneCPConfig abbrdsconf = new BoneCPConfig();
		abbrdsconf.setJdbcUrl((String) params.get("abbrdb.connectionstr")); // jdbc
																			// url
																			// specific
																			// to
																			// your
																			// database,
																			// eg
																			// jdbc:mysql://127.0.0.1/yourdb
		// if (!cs.contains("cassandra")) {
		// dsconf.setUsername((String) params.get("db.user"));
		// dsconf.setPassword((String) params.get("db.pass"));
		// }
		abbrdsconf.setMinConnectionsPerPartition(15);
		abbrdsconf.setMaxConnectionsPerPartition(60);
		abbrdsconf.setPartitionCount(6);
		try {
			abbrconnectionPool = new BoneCP(abbrdsconf);

			// conn = abbrconnectionPool.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // setup the connection pool

	}

	public void createDbLinksPOOL(Map params) {
		String connectionString = "";

		// adding support for linked docker containers
		String envHostIp = (String) params.get("db.env.host_ip");
		String envHostPort = (String) params.get("db.env.host_port");
		String envHostDb = (String) params.get("db.env.host_db_name");

		if (envHostIp != null && envHostPort != null && envHostDb != null) {
			try {
				if (System.getenv(envHostPort) != null && System.getenv(envHostIp) != null) {
					connectionString = "jdbc:mysql://" + System.getenv(envHostIp) + ":" + System.getenv(envHostPort)
							+ "/" + envHostDb;
				}
			} catch (Exception ignore) {
			}
		}

		if (connectionString.isEmpty()) {
			connectionString = (String) params.get("db.connectionstr"); // +"?useLegacyDatetimeCode=false";
			System.out.println("connected:" + connectionString);
		}
		BoneCPConfig dsconf = new BoneCPConfig();
		dsconf.setJdbcUrl(connectionString); // jdbc url specific to your
												// database, eg
												// jdbc:mysql://127.0.0.1/yourdb
		if (!connectionString.contains("cassandra")) {
			dsconf.setUsername((String) params.get("db.user"));
			dsconf.setPassword((String) params.get("db.pass"));
		}
		dsconf.setMinConnectionsPerPartition(15);
		dsconf.setMaxConnectionsPerPartition(60);
		dsconf.setPartitionCount(9);
		dsconf.setIdleConnectionTestPeriodInMinutes(10);
		dsconf.setConnectionTestStatement("/* ping */ SELECT 1 ;");
		try {
			connectionPool = new BoneCP(dsconf);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // setup the connection pool

	}

	public static Comparator<String> idComparator = new Comparator<String>() {
		@Override
		public int compare(String c1, String c2) {
			return (int) (Integer.parseInt(c1.substring(0, 1)) - Integer.parseInt(c2.substring(0, 1)));
		}
	};
}
