package gov.lanl.agg.cache;

import gov.lanl.agg.BatchMap;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.TimeTravel;

import java.util.Date;
import java.util.List;
import java.util.Map;

/*
@author Lyudmila Balakireva

*/
public interface CacheStorage {
	 LinkHeader getTimegateInfo(String url,  Date reqtime, List <String> names,boolean refresh);
	 LinkHeader getTimegateInfo(String url,  Date reqtime, List <String> names,boolean refresh,Date serv);
	 LinkHeader getTimeMapInfo(String url,Date start,Date end,int istart,int limit, List <String> names);
            int getMatchingCount(String url);	
     TimeTravel getInfo(String url, Date reqtime, List <String> names,boolean refresh);
     TimeTravel getInfo(String url, Date reqtime, List <String> names,boolean refresh,Date serv);
     Map<String, List<Link>> getDuplicateMementos(String url, Date firstMemento, Date prevMemento,
              Date closestMemento, Date nextMemento, Date lastMemento);
     Map<String, String> getNostalgicInfo();
     BatchMap getBatchInfo(String url); 
     boolean checkCacheRelax(String url);
     boolean checkCache(String url, Date memdate, boolean force_refresh,Date service_date );
     Map getTimeMapIndexInfo(String url, int limit,List <String> names);
     void setApplicationMode(String livesync);
     
}
