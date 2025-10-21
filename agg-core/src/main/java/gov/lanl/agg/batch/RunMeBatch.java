package gov.lanl.agg.batch;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.CacheStats;
import gov.lanl.batchsync.URLPostClient;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import com.leansoft.bigqueue.IBigQueue;
/*
@author Lyudmila Balakireva

*/
public interface RunMeBatch {
	  public void fupdateArchives(List tmlist,List tglist,Map<String,Integer> map) ;
	  public Map selectJob () ;
	  public Date checkLastUpdate(String url);
	  public void selectJobs (PriorityBlockingQueue q);
	 // public void selectJobs (IBigQueue q);
	  public void updateJobs (String process_id,String ctime);
	  public void updateLinkmaster (String url,String ctime,String status);
	  public void deleteLinkmaster (String url);
	  public List updateAllLinks(String url,Map <String,List>result);
	  public Map getLastMementos(String url);
	  public void populateJobs();
	  public void cleanCache();
	  public void addArchive( String hostname,String timegate, String timemap, String name,String cal_page); 
	  public ArchiveDescription  getArchiveInfo(String archive_id) ;
	  public void rollbackJobs(PriorityBlockingQueue q);
	  public void   cleanQue() ;
	  public void updateStats(CacheStats cs);
	  public void setURLPostClient(URLPostClient client);
	  public void  updateLinkSummary(String url,String hostname,String ctime,String code,String total);
	  public long check_Que();
}
