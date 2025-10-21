package gov.lanl.agg.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.Link;
import gov.lanl.agg.TimeTravel;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.resource.template.TimeTravelResult;
import gov.lanl.agg.utils.Tokens;

import javax.websocket.server.PathParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.net.IDN;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

/**
 * @author: Harihar Shankar, 7/11/14 11:04 AM
 */

@Path(Tokens.MAP_BASE_PATH)

public class TimeTravelMapResource {

   // static TimeTravelCommons timeTravelCommons;

    public TimeTravelMapResource(@Context UriInfo uriInfo) {
     //   timeTravelCommons = new TimeTravelCommons(uriInfo.getBaseUri());
    }

    @Path("{id:.*}")
    @GET
    @Produces({MediaType.TEXT_HTML})
    public String getMap(@Context HttpHeaders httpHeaders,
                         @Context UriInfo uriInfo,
                         @PathParam("id") String idb
                         )
        throws ParseException, URISyntaxException {

        String requestPath = uriInfo.getPath();
        if (!requestPath.startsWith("/")) {
            requestPath = "/" + requestPath;
        }

        String requestUrl = requestPath.replaceFirst(
                Tokens.MAP_BASE_PATH, "");

        if (!requestUrl.startsWith("http://") && !requestUrl.startsWith("https://")) {
            requestUrl = "http://" + requestUrl;
        }

        if (uriInfo.getRequestUri().getQuery() != null) {
            requestUrl += "?" + uriInfo.getRequestUri().getQuery();
        }
        TimeTravelCommons timeTravelCommons = new TimeTravelCommons();
        String orig = timeTravelCommons.getOriginalUrl(requestUrl);

        String requestHost = timeTravelCommons.getRequestHost(requestUrl);
        if (requestHost == null) {
            //throw new NotFoundException("No memento found for: " + requestUrl);
            System.out.println("No Memento found for:" + requestUrl);
            TimeTravel tt = new TimeTravel();
            Date requestDate = new Date();
            tt.setAcceptDatetime(requestDate);
            tt.setOriginalUrl(requestUrl);
            return "ERROR! Bad URL!";
            //return serializeToHtml(tt, requestDate);
        }
        Date requestDate = new Date();
        /*
        List<String> mementoArchiveNames = new ArrayList<String>(TimeTravelCommons.mementoCompliantArchiveNames);
        TimeTravel timeTravelFromCache = ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage")).getInfo(requestUrl, requestDate, mementoArchiveNames);

        Map<String, List<Map<String, String>>> distribution = timeTravelFromCache.getDistribution();
        if (distribution == null) {
            return "No mementos found";
        }
        TimeTravelResult timeTravelResult = new TimeTravelResult();
        String timelines = "";
        for (String archiveId : distribution.keySet()) {
            ArchiveDescription archiveDescription = TimeTravelCommons.archiveShortName.get(archiveId);

            String archiveFullName = archiveId;
            if (archiveDescription != null) {
                archiveFullName = archiveDescription.getLongname();
            }

            String timeline = timeTravelResult.getMapResultTemplate();
            timeline = timeline.replaceAll("##archive_id##", archiveId);
            timeline = timeline.replaceAll("##archive_full_name##", archiveFullName);

            timelines += timeline;

        }

        String mapTemplate = timeTravelResult.getMapTemplate();

        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        String jsonDistribution = gson.toJson(distribution, Map.class);
        String script = "var mementoDistribution = " + jsonDistribution;
        mapTemplate = mapTemplate.replace("##script##", script);

        mapTemplate = mapTemplate.replace("##archive_result##", timelines);

        mapTemplate = mapTemplate.replace("##page_title##", "Mementos for " + orig);
        mapTemplate = mapTemplate.replace("##original_url##", orig);

        mapTemplate = mapTemplate.replaceAll("(##)(.*?)(##)", "");

        return mapTemplate;
        */
        return null;
    }

    public Map<ArchiveDescription, Map<String, String>> getDistibution(Map<ArchiveDescription, List<Link>> timemapsPerArchive) {

        Map<ArchiveDescription, Map<String, String>> distribution = new HashMap<ArchiveDescription, Map<String, String>>();

        ArchiveDescription summary = new ArchiveDescription();
        summary.setLongname("Summary");
        summary.setName("summary");

        Map<String, String> summaryDistribution = new HashMap<String, String>();

        for (ArchiveDescription archive : timemapsPerArchive.keySet()) {
            Map<String, String> dist = new HashMap<String, String>();
            for (Link link : timemapsPerArchive.get(archive)) {
                String datetime = link.getDatetime();
                if (datetime == null) {
                    // must be an original or a timegate/timemap url
                    //System.out.println("NULL: " + link.getHref());
                    continue;
                }
                String year = TimeTravelCommons.getFormattedDate(datetime, "year");
                String month = TimeTravelCommons.getFormattedDate(datetime, "month");
                String date = year + "-" + month + "-14";
                Integer count = 0;
                Integer summaryCount = 0;
                if (dist.containsKey(date)) {
                    count = Integer.parseInt(dist.get(date));
                    summaryCount = Integer.parseInt(summaryDistribution.get(date));
                }
                count++;
                summaryCount++;
                dist.put(date, count.toString());
                summaryDistribution.put(date, summaryCount.toString());
            }
            distribution.put(archive, dist);
            System.out.println(archive.getLongname());
            System.out.println(dist);
        }
        distribution.put(summary, summaryDistribution);
        System.out.println(summary.getLongname());
        System.out.println(summaryDistribution);

        return distribution;
    }
}
