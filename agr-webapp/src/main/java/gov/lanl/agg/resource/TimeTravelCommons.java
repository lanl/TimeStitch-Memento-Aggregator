package gov.lanl.agg.resource;


import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.CacheStats;
import gov.lanl.agg.Link;
import gov.lanl.agg.RulesDescription;
import gov.lanl.agg.TimeTravel;
//import gov.lanl.agg.*;
import gov.lanl.agg.batch.RunMeBatchTask;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.helpers.TimeTravelAggQuick;
import gov.lanl.agg.utils.CommonRuleMatcher;
import gov.lanl.agg.utils.LiveResourceClient;
import gov.lanl.agg.utils.MLClient;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.OriginalResource;
//import gov.lanl.agg.utils.*;
//import gov.lanl.agg.helpers.*;
import gov.lanl.agg.utils.RemoteCacheClient;
import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;

import java.lang.System;
import java.net.URI;
import java.net.URL;
//import java.net.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
//import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;


/**
 * @author: Harihar Shankar, 6/30/14 2:05 PM
 */

public class TimeTravelCommons {


    static List<ArchiveDescription> archiveDescription;
    static ThreadSafeSimpleDateFormat httpformatter;
    static  ThreadSafeSimpleDateFormat machineFormatter;
    static  ThreadSafeSimpleDateFormat displayFormatter;
    static  ThreadSafeSimpleDateFormat displayDateFormatter;
    static  ThreadSafeSimpleDateFormat dayFormatter;
    static  ThreadSafeSimpleDateFormat monthFormatter;
    static  ThreadSafeSimpleDateFormat yearFormatter;
    //static MementoCommons mc;
    static String proxybaseuri;
    NavigableMap<Long, Link> linkmap;
    static Map<String, ArchiveDescription> archiveShortName = new HashMap<String, ArchiveDescription>();

    static List<String> timetravelDynamicArchiveNames;
    static List<String> timetravelCacheArchiveNames;
    static RulesDescription timetravelDynamicRules;
    static RulesDescription timetravelCacheRules;
    static RemoteCacheClient remoteCacheClient;
    static String mlBaseUrl = "";
    static MLClient mlClient;

    // ttLogger: url, req_date, service, hit, miss, stale, bypass, accept_datetime
    static Logger ttLogger;
    // mlLogger: url, req_date, listofpredictedarchives, listofaddededarchivesviarules, accept_datetime
    static Logger mlLogger;

    static String dateNow;
    static Date dateNow_;

    static {
    	try {
        MyInitServlet cl = MyInitServlet.getInstance();
        archiveDescription = (List<ArchiveDescription>) cl.getAttribute("archivedesc");
        //defaultArchives = new ArrayList<ArchiveDescription>();
        //mementoCompliantArchiveNames = new ArrayList <String> ();

        Map params = (Map) cl.getAttribute("params");
        remoteCacheClient = new RemoteCacheClient((String) params.get("config.cache.registry"),
                (String) params.get("config.cache.self"));
        if (params.containsKey("config.service.rules")) {
            String rules = (String) params.get("config.service.rules");

            timetravelDynamicRules =  CommonRuleMatcher.load_rules(rules, "timetravel_dynamic");
            
            timetravelCacheRules =  CommonRuleMatcher.load_rules(rules, "timetravel_cache");
            timetravelDynamicArchiveNames = timetravelDynamicRules.getDefaultArchives();
            timetravelCacheArchiveNames = timetravelCacheRules.getDefaultArchives();
        }

        if (params.containsKey("baseuri.proxy")) {
            proxybaseuri = (String) params.get("baseuri.proxy");
        } else {
            proxybaseuri = null;
        }
        // mc = new MementoCommons(null);
        httpformatter =  MementoCommons.httpformatter;
        machineFormatter = MementoCommons.timeTravelMachineFormatter;
        displayFormatter = MementoCommons.timeTravelDisplayFormatter;
        displayDateFormatter = MementoCommons.timeTravelDisplayDateFormatter;
        dayFormatter = MementoCommons.timeTravelDayFormatter;
        monthFormatter = MementoCommons.timeTravelMonthFormatter;
        yearFormatter = MementoCommons.timeTravelYearFormatter;

        mlClient = new MLClient();
        mlBaseUrl = (String) cl.getAttribute("mlbaseurl");

        ttLogger = Logger.getLogger("stats");
        mlLogger = Logger.getLogger("mlservice");
        dateNow_ = new Date();
        dateNow = MementoUtils.timeTravelMachineFormatter.format( dateNow_ );
    	}
    	
    	catch (Exception ignore) {
    		System.out.println("from timetravelcommons constructor");
    		ignore.printStackTrace();
            
        }
    }

   // public TimeTravelCommons(URI uri) {
       // mc = new MementoCommons(uri);
   // }

    public  String getOriginalUrl(String requestUrl) {

        //begin live client check
        LiveResourceClient liveclient = new LiveResourceClient();
        HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
        liveclient.checkFeedUrl(client, requestUrl);
        OriginalResource ores = liveclient.getOriginalResource();
        String orig = ores.getOriginaURI();
        if (orig == null) {
            orig = requestUrl;
        }
        return orig;
    }

    public  String getRequestHost(String requestUrl) {

        /*
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (!urlValidator.isValid(requestUrl)) {
            System.out.println("Invalid URL: " + requestUrl);
            return null;
        }
        */
        try {

            URL rUrl = new URL(requestUrl);
            String host = rUrl.getHost();
            if (host.startsWith("www.")) {
                host = host.replace("www.", "");
            }
            return host;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static String getFormattedDate(String date, String format) {

        if (date == null) {
            return null;
        }

        gov.lanl.agg.utils.ThreadSafeSimpleDateFormat dateFormat;
        if (format.equals("month")) {
            dateFormat = monthFormatter;
        } else if (format.equals("day")) {
            dateFormat = dayFormatter;
        } else if (format.equals("year")) {
            dateFormat = yearFormatter;
        } else if (format.equals("display")) {
            dateFormat = displayFormatter;
            //} else if (format.equals("tt")) {
            //    dateFormat = machineFormatter;
        } else {
            dateFormat = machineFormatter;
            //dateFormat = httpformatter;
        }

        try {
            Date fMemento = machineFormatter.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fMemento);
            return dateFormat.format(cal.getTime());
        }
        catch (ParseException ignore) {
            //System.out.println(ignore.getMessage());
        }
        // may be the date is in http format
        try {
            Date fMemento = httpformatter.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fMemento);
            return dateFormat.format(cal.getTime());
        }
        catch (ParseException ignore) {
            //System.out.println(ignore.getMessage());
        }
        return null;
    }

    public static String getFormattedTimeDelta(Date requestDate, Date mementoDate, String formatType) {

        if (formatType.isEmpty()) {
            formatType = "info";
        }
        Long delta = 0L;
        String direction = "";
        if (formatType.equals("info")) {
            direction = "&#8722;";
        }
        else if (formatType.equals("title")) {
            direction = "before";
        }
        if (requestDate.getTime() >= mementoDate.getTime()) {
            delta = requestDate.getTime() - mementoDate.getTime();
        }
        else {
            delta = mementoDate.getTime() - requestDate.getTime();
            if (formatType.equals("info")) {
                direction = "+";
            }
            else if (formatType.equals("title")) {
                direction = "after";
            }
        }
        Long deltaHours = delta / (60 * 60 * 1000) % 24;
        Long deltaDays = delta / (24 * 60 * 60 * 1000);
        Long deltaYears = deltaDays / 365;
        Long deltaDaysAfterYears = deltaDays % 365;

        if (Math.floor(deltaYears) > 0) {
            String deltaText = "";
            if (deltaYears == 1) {
                deltaText = deltaYears + " year ";
            }
            else {
                deltaText = deltaYears + " years ";
            }
            if (deltaDaysAfterYears > 0) {
                if (deltaDaysAfterYears == 1) {
                    deltaText += deltaDaysAfterYears + " day ";
                }
                else {
                    deltaText += deltaDaysAfterYears + " days ";
                }
            }
            if (formatType.equals("info")) {
                return direction + deltaText;
            }
            else {
                return deltaText + direction;
            }
        }
        else if (deltaDays != 0) {
            if (formatType.equals("info")) {
                if (deltaDays == 1) {
                    return direction + deltaDays + " day";
                }
                else {
                    return direction + deltaDays + " days";
                }
            }
            else {
                if (deltaDays == 1) {
                    return deltaDays + " day " + direction;
                }
                else {
                    return deltaDays + " days " + direction;
                }
            }
        }
        else {
            if (deltaHours == 0 || deltaHours == 1) {
                deltaHours = 1L;
                if (formatType.equals("info")) {
                    direction = "<";
                    return direction + deltaHours + " hour";
                }
                else {
                    return deltaHours + " hour " + direction;
                }
            }
            if (formatType.equals("info")) {
                return direction + deltaHours + " hours";
            }
            else {
                return deltaHours + " hours " + direction;
            }
        }
    }

    public  TimeTravel getMementos(String requestUrl, String requestHost, Date requestDate, String cacheControl, String serviceName) {

        TimeTravel timetravel = new TimeTravel();
        timetravel.setOriginalUrl(requestUrl);
        timetravel.setAcceptDatetime(requestDate);
        long requestTime = requestDate.getTime();
        Boolean nativeCmsSummarySet = false;

        List<String> cacheArchivesFromRules = CommonRuleMatcher.getArchives(requestUrl, timetravelCacheRules);
        List<String> dynamicArchivesFromRules = CommonRuleMatcher.getArchives(requestUrl, timetravelDynamicRules);

        List<String> specialDynamicArchiveNames = new ArrayList<>();
        List<String> defaultDynamicArchiveNames = new ArrayList<>();
        List<String> specialCacheArchiveNames = new ArrayList<>();
        List<String> defaultCacheArchiveNames = new ArrayList<>();


        for (String archiveName : cacheArchivesFromRules) {
            if (timetravelCacheArchiveNames.contains(archiveName)) {
                defaultCacheArchiveNames.add(archiveName);
            }
            else {
                specialCacheArchiveNames.add(archiveName);
            }
        }
        HttpClient client = (HttpClient) MyInitServlet.getInstance().getAttribute("httpcli");
        CacheStats stats = (CacheStats) MyInitServlet.getInstance().getAttribute("timetravelstats");
        CacheStorage storage = ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage"));
        RunMeBatchTask rmTask = (RunMeBatchTask) MyInitServlet.getInstance().getAttribute("task");
        Date lastUpdate = rmTask.checkLastUpdate(requestUrl);
        TimeZone tz = TimeZone.getTimeZone("GMT");
        Calendar calendar = new GregorianCalendar(tz);
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Date cacheStaleCutOff = calendar.getTime();
        Boolean statsIncremented = false;
        Boolean checkCache = true;
        Boolean bypassCache = false;
        Boolean staleCache = false;

        Map<String, NavigableMap<Long, Link>> mementosForTimeTravel;
        int mcount = 0;
        TimeTravel timeTravelFromCache;

        if (lastUpdate != null) {
            if (lastUpdate.before(cacheStaleCutOff)) {
                stats.increamentStale();
                statsIncremented = true;
                staleCache = true;
                checkCache = false;
            }

            if (cacheControl.equals("no-cache")) {
                // 2015-07-21
                // commenting out check for 30 min cache grace period.

                Calendar noCacheCal = new GregorianCalendar(tz);
                noCacheCal.setTime(new Date());
                noCacheCal.add(Calendar.MINUTE, -10);
                Date noCacheCutOff = noCacheCal.getTime();
                if (lastUpdate.before(noCacheCutOff)) {
                    checkCache = false;
                    bypassCache = true;
                }
                else {
                    checkCache = true;
                }
            }

            if (cacheControl.equals("only-if-cached")) {
                checkCache = true;
            }
        }
        else if (cacheControl.equals("no-cache")) {
            Map m = (Map) remoteCacheClient.CheckCaches(client, requestUrl, requestHost, rmTask, storage);
            if (m != null) {
                lastUpdate = rmTask.checkLastUpdate(requestUrl);
            }
        }
        else if (cacheControl.equals("only-if-cached")) {
            return timetravel;
        }

        // getting dynamic archive list from ml

        List<String> recommended = new ArrayList<>();
        List<String> mlExcludedArchives = new ArrayList<>();
        if (!bypassCache) {
            mlExcludedArchives = mlClient.checkUrl(client, mlBaseUrl+requestUrl, recommended);
        }

        String recString = String.join(":", recommended);

        //System.out.println("ml list: " + mlExcludedArchives);

        for (String archiveName : dynamicArchivesFromRules) {
            //System.out.println("dyn: " + archiveName);
            if (timetravelDynamicArchiveNames.contains(archiveName) &&
                    !mlExcludedArchives.contains(archiveName)) {
                defaultDynamicArchiveNames.add(archiveName);
            }
            else if (!mlExcludedArchives.contains(archiveName)){
                specialDynamicArchiveNames.add(archiveName);
            }
        }
        //System.out.println("dyn: " + defaultDynamicArchiveNames);

        // getting archive desc class from archive names
        List<ArchiveDescription> defaultCacheArchives = new ArrayList<>();
        List<ArchiveDescription> specialCacheArchives = new ArrayList<>();
        List<ArchiveDescription> defaultDynamicArchives = new ArrayList<>();
        List<ArchiveDescription> specialDynamicArchives = new ArrayList<>();
        for (ArchiveDescription ad : archiveDescription) {
            String adName = ad.getName();
            if (defaultCacheArchiveNames.contains(adName)) {
                defaultCacheArchives.add(ad);
            }
            else if (specialCacheArchiveNames.contains(adName)) {
                specialCacheArchives.add(ad);
            }
            if (defaultDynamicArchiveNames.contains(adName)) {
                defaultDynamicArchives.add(ad);
            }
            else if (specialDynamicArchiveNames.contains(adName)) {
                specialDynamicArchives.add(ad);
            }
            archiveShortName.put(adName, ad);
        }

        String dynArchiveList = String.join(":", defaultDynamicArchiveNames);

        // adding native to the cache list
        defaultCacheArchiveNames.add(requestHost);
        defaultCacheArchiveNames.addAll(specialCacheArchiveNames);

        timeTravelFromCache = ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage")).getInfo(requestUrl, requestDate, defaultCacheArchiveNames, !checkCache,dateNow_);

        mementosForTimeTravel = timeTravelFromCache.getMementos();
        if (mementosForTimeTravel == null && cacheControl.equals("only-if-cached")) {
            return timetravel;
        }
        else if (mementosForTimeTravel == null) {
            Calendar noCacheCal = new GregorianCalendar(tz);
            noCacheCal.setTime(new Date());
            noCacheCal.add(Calendar.MINUTE, -10);
            Date noCacheCutOff = noCacheCal.getTime();
            if (lastUpdate != null && lastUpdate.before(noCacheCutOff)) {
                checkCache = false;
            }
            else {
                checkCache = true;
            }
        }

        //System.out.println("DEFAULT: " + defaultCacheArchiveNames);
        //System.out.println("SPECIAL: " + specialCacheArchiveNames);

        //System.out.println("DEFAULT Dyn: " + defaultDynamicArchiveNames);
        //System.out.println("SPECIAL Dyn: " + specialDynamicArchiveNames);


        // checking cache first

        if (mementosForTimeTravel != null && checkCache) {

            stats.incrementHit();
            statsIncremented = true;

            // ttLogger: url, req_date, service, hit, miss, stale, bypass, accept_datetime
            List<String> logMsg = Arrays.asList(requestUrl,
                    dateNow, serviceName,
                    "1", "0", "0", "0",
                    machineFormatter.format(requestDate));
            ttLogger.info(String.join(" ", logMsg));

            mcount = mementosForTimeTravel.size();

            Map<String, List<Link>> mementosByArchive = new HashMap<>();
            Iterator archives = mementosForTimeTravel.keySet().iterator();
            while (archives.hasNext()) {
                String archiveName = (String) archives.next();
                if (archiveName.equals("summary")) {
                    /*
                    for (Long mem : mementosForTimeTravel.get(archiveName).keySet()) {
                        Link link = mementosForTimeTravel.get(archiveName).get(mem);
                        System.out.println("summary: " + link.getRelationship() + " : " + link.getHref());
                    }
                    */
                    continue;
                }

                //System.out.println("cache: " + archiveName);
                NavigableMap<Long, Link> archiveMementos = mementosForTimeTravel.get(archiveName);
                if (archiveMementos == null) {
                    continue;
                }

                //System.out.println("isnative: " + requestHost + " - " + archiveName);
                Boolean isNative = timetravel.getNativeArchive().equals(archiveName)
                        || timetravel.getCmsArchive().equals(archiveName)
                        || requestHost.equals(archiveName);
                Map<Long, List<Link>> sortedArchiveMementos = summarizeMementos(archiveMementos, requestTime, isNative);
                Long mementoDt = (Long) sortedArchiveMementos.keySet().toArray()[0];

                mementosByArchive.put(archiveName, sortedArchiveMementos.get(mementoDt));
                if (isNative) {
                    timetravel.setNativeArchive(archiveName);
                }
            }
            timetravel.setSummary(mementosByArchive);
            timetravel.setLastModified(lastUpdate);
        }

        if (!statsIncremented) {
            stats.incrementMiss();
        }

        // if cache miss, doing dynamic search
        Map<Long, List<Link>> summary = new HashMap<>();
        String formattedRequestDate = httpformatter.format(requestDate);
        if (mcount == 0) {

            // ttLogger: url, req_date, service, hit, miss, stale, bypass, accept_datetime

            List<String> logMsg = Arrays.asList(requestUrl,
                    dateNow,
                    serviceName,
                    "0", "1",
                    staleCache ? "0" : "1",
                    bypassCache ? "1" : "0",
                    machineFormatter.format(requestDate)
                    );
            ttLogger.info(String.join(" ", logMsg));

            // mlLogger: url, req_date, listofpredictedarchives, listofaddededarchivesviarules, accept_datetime
            List<String> mlmsg = Arrays.asList(requestUrl,  dateNow,
                    //machineFormatter.format(requestDate),
                    recString, dynArchiveList, machineFormatter.format(requestDate)
            );

            mlLogger.info(String.join(" ", mlmsg));


            TimeTravel timeTravelDynamic = doDynamic(requestUrl, formattedRequestDate, requestTime, defaultDynamicArchives, false);
            mementosForTimeTravel = timeTravelDynamic.getMementos();
            Map<String, List<Link>> archiveSummary = new HashMap<>();
            //System.out.println("dyn arc: " + mementosForTimeTravel.keySet());
            for (String archiveName : mementosForTimeTravel.keySet()) {
                Long mementoDt = getMementoDatetime(mementosForTimeTravel.get(archiveName), requestTime, false);
                List<Link> mementos = castToSummaryObject(mementosForTimeTravel.get(archiveName), mementoDt);
                archiveSummary.put(archiveName, mementos);
            }
            TimeTravel nativeTt = doDynamic(requestUrl, formattedRequestDate, requestTime, specialDynamicArchives, true);
            for (String archiveName : nativeTt.getMementos().keySet()) {
                //ArchiveDescription ad = archiveShortName.get(archiveName);
                if (archiveName.equals("summary")) {
                    continue;
                }
                //System.out.println("native: " + archiveName);
                // native
                if (archiveName.equals("Native")
                        && nativeTt.getMementos().get("Native") != null) {

                    Boolean redundantNativeArchive = false;
                    String nativeMementoUrl = nativeTt.getMementos().get(archiveName).firstEntry().getValue().getHref();
                    for (ArchiveDescription ad : defaultCacheArchives) {
                        String tgBaseUrl = ad.getTimegate();
                        tgBaseUrl = tgBaseUrl.replace("http://", "");
                        tgBaseUrl = tgBaseUrl.replace("https://", "");
                        tgBaseUrl = tgBaseUrl.split("/")[0];
                        tgBaseUrl = tgBaseUrl.split(":")[0];

                        nativeMementoUrl = nativeMementoUrl.replace("http://", "");
                        nativeMementoUrl = nativeMementoUrl.replace("https://", "");
                        if (nativeMementoUrl.startsWith(tgBaseUrl)) {
                            redundantNativeArchive = true;
                            break;
                        }
                    }
                    if (redundantNativeArchive) {
                        //System.out.println("Redundant: " + nativeMementoUrl);
                        continue;
                    }
                    mementosForTimeTravel.put(requestHost, nativeTt.getMementos().get("Native"));
                    Long mementoDt = getMementoDatetime(nativeTt.getMementos().get(archiveName), requestTime, false);
                    List<Link> mementos = castToSummaryObject(nativeTt.getMementos().get("Native"), mementoDt);
                    archiveSummary.put(requestHost, mementos);
                    //summary = summarizeMementos(rulesTt.getMementos().get("Native"), requestTime, true);

                    summary.put(mementoDt, mementos);
                    timetravel.setNativeArchive(requestHost);
                    if (!nativeCmsSummarySet) {
                        Map<String, List<Link>> summaryMap = new HashMap<>();
                        summaryMap.put("summary", mementos);
                        timetravel.setSummary(summaryMap);
                        nativeCmsSummarySet = true;
                    }
                }
            }
            timetravel.setSummary(archiveSummary);
            timetravel.setIsDynamic(true);
            timetravel.setLastModified(new Date());
        }

        // Additional archives in rules
        TimeTravel rulesTt = doDynamic(requestUrl, formattedRequestDate, requestTime, specialDynamicArchives, false);

        Map<String, List<Link>> archiveSummary = new HashMap<>();
        for (String archiveName : rulesTt.getMementos().keySet()) {

            ArchiveDescription ad = archiveShortName.get(archiveName);
            if (archiveName.equals("summary") || ad == null || rulesTt.getMementos().get(archiveName) == null) {
                continue;
            }
            /*
            // native
            if (archiveName.equals("Native")
                    && rulesTt.getMementos().get("Native") != null) {

                mementosForTimeTravel.put(requestHost, rulesTt.getMementos().get("Native"));
                Long mementoDt = getMementoDatetime(rulesTt.getMementos().get(archiveName), requestTime, false);
                List<Link> mementos = castToSummaryObject(rulesTt.getMementos().get("Native"), mementoDt);
                archiveSummary.put(requestHost, mementos);
                //summary = summarizeMementos(rulesTt.getMementos().get("Native"), requestTime, true);

                summary.put(mementoDt, mementos);
                timetravel.setNativeArchive(requestHost);
            }
            */
            // cms
            if (ad.archiveType.toLowerCase().equals("cms")
                    && rulesTt.getMementos().get(archiveName) != null) {

                /*
                for (Long t : rulesTt.getMementos().get(archiveName).keySet()) {
                    Link l = rulesTt.getMementos().get(archiveName).get(t);
                    System.out.println(l.getRelationship() + " - " + l.getDatetime());
                }
                */
                mementosForTimeTravel.put(archiveName, rulesTt.getMementos().get(archiveName));
                Long mementoDt = getMementoDatetime(rulesTt.getMementos().get(archiveName), requestTime, true);
                List<Link> mementos = castToSummaryObject(rulesTt.getMementos().get(archiveName), mementoDt);
                archiveSummary.put(archiveName, mementos);
                //summary = summarizeMementos(rulesTt.getMementos().get(archiveName), requestTime, true);
                summary.put(mementoDt, mementos);
                timetravel.setCmsArchive(archiveName);
                if (!nativeCmsSummarySet) {
                    Map<String, List<Link>> summaryMap = new HashMap<>();
                    summaryMap.put("summary", mementos);
                    timetravel.setSummary(summaryMap);
                    nativeCmsSummarySet = true;
                }
            }
            else {
                mementosForTimeTravel.put(archiveName, rulesTt.getMementos().get(archiveName));
                Long mementoDt = getMementoDatetime(rulesTt.getMementos().get(archiveName), requestTime, false);
                List<Link> mementos = castToSummaryObject(rulesTt.getMementos().get(archiveName), mementoDt);
                archiveSummary.put(archiveName, mementos);
            }
        }
        timetravel.setSummary(archiveSummary);

        NavigableMap<Long, Link> allMementos = new ConcurrentSkipListMap<>();
        for (String archiveName : mementosForTimeTravel.keySet()) {
            //System.out.println("memtn: " + archiveName);
            if (mementosForTimeTravel.get(archiveName) != null) {
                allMementos.putAll(mementosForTimeTravel.get(archiveName));
            }
        }
        summary = summarizeMementos(allMementos, requestTime, false);
        /*
        if (summary.isEmpty() && timetravel.getIsDynamic()) {
            NavigableMap<Long, Link> allMementos = new ConcurrentSkipListMap<>();
            for (String archiveName : mementosForTimeTravel.keySet()) {
                if (mementosForTimeTravel.get(archiveName) != null) {
                    allMementos.putAll(mementosForTimeTravel.get(archiveName));
                }
            }
            summary = summarizeMementos(allMementos, requestTime, false);
        }
        else if (summary.isEmpty()) {
            NavigableMap<Long, Link> allMementos = new ConcurrentSkipListMap<>();
            if (mementosForTimeTravel.get("summary") != null) {
                allMementos.putAll(mementosForTimeTravel.get("summary"));
            }
            else {
                for (String archiveName : mementosForTimeTravel.keySet()) {
                    if (mementosForTimeTravel.get(archiveName) != null) {
                        allMementos.putAll(mementosForTimeTravel.get(archiveName));
                    }
                }
            }
            //System.out.println("summary-dd " +  summary.isEmpty());
            summary = summarizeMementos(allMementos, requestTime, false);
        }
        */

        Map<String, List<Link>> summaryMap = new HashMap<>();
        if (summary != null && !nativeCmsSummarySet) {
            List<Long> summaryKeys = new ArrayList<>(summary.keySet());
            summaryMap.put("summary", summary.get(summaryKeys.get(0)));
        }
        timetravel.setSummary(summaryMap);

        timetravel.setMementos(mementosForTimeTravel);

        return timetravel;
    }

    public TimeTravel prepareForDisplay(TimeTravel timetravel, Date requestDate) {

        long requestTime = requestDate.getTime();

        Map<String, NavigableMap<Long, Link>> mementosForTimeTravel = timetravel.getMementos();
        if (mementosForTimeTravel == null) {
            return timetravel;
        }

        //Map<String, List<Link>> mementosByArchive = new HashMap<>();
        Iterator archives = mementosForTimeTravel.keySet().iterator();
        while (archives.hasNext()) {
            String archiveName = (String) archives.next();
            if (archiveName.equals("summary")) {
                continue;
            }

            NavigableMap<Long, Link> archiveMementos = mementosForTimeTravel.get(archiveName);
            if (archiveMementos == null) {
                continue;
            }
            Boolean isNative = timetravel.getNativeArchive().equals(archiveName)
                    || timetravel.getCmsArchive().equals(archiveName);
            //Map<Long, List<Link>> sortedArchiveMementos = summarizeMementos(archiveMementos, requestTime, isNative);

            List<Link> sortedArchiveMementos = timetravel.getSummary().get(archiveName);
            if (sortedArchiveMementos == null) {
                System.out.println("NULL: " + archiveName);
                continue;
            }

            //Long mementoDt = (Long) sortedArchiveMementos.keySet().toArray()[0];
            Long mementoDt = 0L;
            for (Link link : sortedArchiveMementos) {
                if (link.getRelationship().equals("memento")) {
                    try{
                        mementoDt = httpformatter.parse(link.getDatetime()).getTime();
                    }
                    catch (ParseException ignore) {}
                }
            }

            Map<Long, String> archiveOrder = new HashMap<>();
            archiveOrder.put(mementoDt, archiveName);
            timetravel.setSortedArchives(archiveOrder, isNative);
            //mementosByArchive.put(archiveName, sortedArchiveMementos.get(mementoDt));
        }
        //timetravel.setSummary(mementosByArchive);

        return timetravel;
    }

    public  TimeTravel doDynamic(String requestUrl,
                                String formattedRequestDate,
                                long requestTime,
                                List<ArchiveDescription> dynamicArchives,
                                Boolean checkNative
    ) {

        TimeTravel timetravel = new TimeTravel();
        TimeTravelAggQuick tgq = new TimeTravelAggQuick();
        linkmap = tgq.getTimegateInfo( dynamicArchives, requestUrl, formattedRequestDate, checkNative); //

        // reduce linkmap to the "summary", Links and add it to archiveLinks
        Map<ArchiveDescription, List<Link>> archivesLinks =  tgq.getPerArchive();
        //test the values
        Iterator<ArchiveDescription> it = archivesLinks.keySet().iterator();

        Map<String, NavigableMap<Long, Link>> mementos = new HashMap<String, NavigableMap<Long, Link>>();

        while (it.hasNext()) {

            ArchiveDescription ad = it.next();
            String archiveShortName = ad.getName();
            List<Link> alinks = archivesLinks.get(ad);
            NavigableMap<Long, Link> memento = new ConcurrentSkipListMap<Long, Link>();

            Iterator lit = alinks.iterator();
            while (lit.hasNext()) {
                Link l = (Link) lit.next();
                String rel = l.getRelationship();
                if (rel.contains("memento")) {
                    //so far all links timemap, timegate as well
                    Long mementoDt = 0L;

                    try {
                        Date memDt = httpformatter.parse(l.getDatetime());
                        mementoDt = memDt.getTime();
                    } catch (ParseException e) {
                        System.out.println("ERROR: " + e.getMessage());
                        continue;
                    }
                    memento.put(mementoDt, l);
                }
            }
            mementos.put(archiveShortName, memento);
        }
        //NavigableMap<Long, Link> summary = createSummary(linkmap, requestTime);

        //mementos.put("summary", summary);
        timetravel.setMementos(mementos);
        return timetravel;
    }

    public List<Link> castToSummaryObject(NavigableMap<Long, Link> allMementos, Long mementoDt) {
        List<Link> mementos = new ArrayList<>();
        Boolean mementoFound = false;
        for (Long mementoTime : allMementos.keySet()) {
            mementos.add(allMementos.get(mementoTime));
            if (allMementos.get(mementoTime).getRelationship().equals("memento")) {
                mementoFound = true;
            }
        }

        if (!mementoFound) {
            //Long mementoDt = getMementoDatetime(allMementos, requestTime, isNative);
            Link link = new Link(
                    httpformatter.format(new Date(mementoDt)),
                    "memento",
                    allMementos.get(mementoDt).getHref(),
                    null, null);
            mementos.add(link);
        }
        return mementos;
    }

    public static Map<Long, List<Link>> summarizeMementos(NavigableMap<Long, Link> mementos, Long requestTime, Boolean isNative) {
        List<Link> sortedMementos = new ArrayList<Link>();

        if (mementos == null || mementos.size() == 0 ) {
            return null;
        }

        Long key = getMementoDatetime(mementos, requestTime, isNative);

        String location = mementos.get(key).getHref();

        sortedMementos.add(new Link(
                httpformatter.format(new Date(key)),
                "memento",
                location,
                null, null)
        );

        long firstKey = mementos.firstKey();
        sortedMementos.add(new Link(httpformatter.format(new Date(mementos.firstKey())),
                "first memento",
                mementos.get(firstKey).getHref(),
                null, null)
        );
        long prevKey;
        try {
            prevKey = mementos.lowerKey(key);
        }
        catch (NullPointerException ignore) {
            prevKey = 0;
        }

        if (prevKey != 0 && prevKey != firstKey) {
            sortedMementos.add(new Link(httpformatter.format(new Date(prevKey)),
                    "prev memento",
                    mementos.get(prevKey).getHref(),
                    null, null)
            );
        }


        long lastKey = mementos.lastKey();
        sortedMementos.add(new Link(httpformatter.format(new Date(mementos.lastKey())),
                "last memento",
                mementos.get(mementos.lastKey()).getHref(),
                null, null)
        );
        long nextKey;
        try {
            nextKey = mementos.higherKey(key);
        }
        catch (NullPointerException ignore) {
            nextKey = 0;
        }

        if (nextKey != 0 && nextKey != lastKey) {
            sortedMementos.add(new Link(httpformatter.format(new Date(nextKey)),
                    "next memento",
                    mementos.get(nextKey).getHref(),
                    null, null)
            );
            if (mementos.higherEntry(nextKey) != null) {
                long nextNextKey = mementos.higherKey(nextKey);
                sortedMementos.add(new Link(httpformatter.format(new Date(nextNextKey)),
                        "afternext  memento",
                        mementos.get(nextNextKey).getHref(),
                        null, null));
            }
        }
        Map<Long, List<Link>> returnMementos = new HashMap<Long, List<Link>>();
        returnMementos.put(key, sortedMementos);
        return returnMementos;
    }

    public Map<Long, List<Link>> summarizeMementosWithDuplicates(NavigableMap<Long, Link> mementos, Long requestTime, Boolean isNative) {
        List<Link> sortedMementos = new ArrayList<Link>();

        if (mementos == null || mementos.size() == 0 ) {
            return null;
        }

        Long key = getMementoDatetime(mementos, requestTime, isNative);

        String location = mementos.get(key).getHref();

        sortedMementos.add(new Link(
                httpformatter.format(new Date(key)),
                "memento",
                location,
                null, null)
        );

        long firstKey = mementos.firstKey();
        sortedMementos.add(new Link(httpformatter.format(new Date(mementos.firstKey())),
                "first memento",
                mementos.get(firstKey).getHref(),
                null, null)
        );
        long prevKey;
        try {
            prevKey = mementos.lowerKey(key);
        }
        catch (NullPointerException ignore) {
            prevKey = 0;
        }

        if (prevKey != 0 && prevKey != firstKey) {
            sortedMementos.add(new Link(httpformatter.format(new Date(prevKey)),
                    "prev memento",
                    mementos.get(prevKey).getHref(),
                    null, null)
            );
        }


        long lastKey = mementos.lastKey();
        sortedMementos.add(new Link(httpformatter.format(new Date(mementos.lastKey())),
                "last memento",
                mementos.get(mementos.lastKey()).getHref(),
                null, null)
        );
        long nextKey;
        try {
            nextKey = mementos.higherKey(key);
        }
        catch (NullPointerException ignore) {
            nextKey = 0;
        }

        if (nextKey != 0 && nextKey != lastKey) {
            sortedMementos.add(new Link(httpformatter.format(new Date(nextKey)),
                    "next memento",
                    mementos.get(nextKey).getHref(),
                    null, null)
            );
            if (mementos.higherEntry(nextKey) != null) {
                long nextNextKey = mementos.higherKey(nextKey);
                sortedMementos.add(new Link(httpformatter.format(new Date(nextNextKey)),
                        "afternext  memento",
                        mementos.get(nextNextKey).getHref(),
                        null, null));
            }
        }
        Map<Long, List<Link>> returnMementos = new HashMap<Long, List<Link>>();
        returnMementos.put(key, sortedMementos);
        return returnMementos;
    }

    public static Long getMementoDatetime(NavigableMap<Long, Link> mementos, Long requestTime, Boolean isNative) {

        long key;
        long fKey = 0L;
        long cKey = 0L;
        try {
            fKey = mementos.floorKey(requestTime);
        }
        catch (NullPointerException ignore) {}
        try {
            cKey = mementos.ceilingKey(requestTime);
        }
        catch (NullPointerException ignore) {}

        if (cKey == 0 && fKey == 0) {
            return null;
        }
        else if (isNative) {
            if (fKey > 0) {
                key = fKey;
            }
            else {
                key = mementos.firstKey();
            }
        }
        /*
        else if (fKey > 0) {
            key = fKey;
        }
        else {
            key = cKey;
        }
        */

        else if (cKey == 0) {
            key = fKey;
        }
        else if (fKey == 0) {
            key = cKey;
        }
        else {
            Long cDelta = cKey - requestTime;
            Long fDelta = requestTime - fKey;
            if (cDelta <= fDelta) {
                key = cKey;
            }
            else {
                key = fKey;
            }
        }

        return key;
    }

    /*
    public NavigableMap<Long, Link> createSummary(NavigableMap<Long, Link> m, Long requestTime){

        NavigableMap<Long, Link> links = new ConcurrentSkipListMap<Long, Link>();
        if (m.size() == 0 ) {
            return links;
        }
        long key;
        if ( m.size() == 0 ) {
            return null;
        }

        try {
            key = m.floorKey(requestTime);
        }
        catch (NullPointerException e ) {
            // can't find a memento before the dt,
            // choosing closest memento...
            key = m.ceilingKey(requestTime);
        }
        String location = m.get(key).getHref() ;
        links.put(key, new Link(machineFormatter.format(new Date(key)), "memento", location, null, null));

        long prevKey = key;
        try {
            prevKey = m.lowerKey(key);
        }
        catch (NullPointerException ignore) {}
        links.put(prevKey, new Link(machineFormatter.format(new Date(prevKey)), "prev memento", m.get(prevKey).getHref(), null, null));

        long nextKey = key;
        try {
            nextKey = m.higherKey(key);
        }
        catch (NullPointerException ignore) {}
        links.put(nextKey, new Link(machineFormatter.format(new Date(nextKey)), "next memento", m.get(nextKey).getHref(), null, null));

        if (nextKey != m.lastKey()) {
            long nextNextKey = m.higherKey(nextKey);
            links.put(nextNextKey, new Link(machineFormatter.format(new Date(nextNextKey)), "afternext  memento", m.get(nextNextKey).getHref(), null, null));
        }

        //Map<String, List<Link>> summary = new HashMap<String, List<Link>>();
        //summary.put("summary", links);
        //t.setSummary(summary);
        return links;
    }
    */

}
