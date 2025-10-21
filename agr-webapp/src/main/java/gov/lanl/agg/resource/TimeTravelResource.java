package gov.lanl.agg.resource;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.TimeTravel;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.Tokens;
import gov.lanl.agg.resource.template.TimeTravelResult;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.*;

import gov.lanl.agg.Link;

/**
 * @author: Harihar Shankar, 6/11/14 2:42 PM
 */

@Path(Tokens.LIST_BASE_PATH)

public class TimeTravelResource {

   // static TimeTravelCommons timeTravelCommons;
    public TimeTravelResource(@Context UriInfo uriInfo) {
       // timeTravelCommons = new TimeTravelCommons(uriInfo.getBaseUri());
    }

    @Path("{date}/{id:.*}")
    @GET
    @Produces({MediaType.TEXT_HTML})
    public Response getTimegate(@Context HttpHeaders httpHeaders,
                              @Context UriInfo uriInfo,
                              @PathParam("id") String idUrl,
                              @PathParam("date") String reqDate)
            throws ParseException, URISyntaxException {

        //some different validity routine
        Date requestDate = MementoCommons.getTimeTravelDate(reqDate);

        String requestUrl = uriInfo.getRequestUri().getPath().replace(Tokens.LIST_BASE_PATH + reqDate +"/", "");

        requestUrl = requestUrl.trim();
        if (!requestUrl.startsWith("http://") && !requestUrl.startsWith("https://")) {
            requestUrl = "http://" + requestUrl;
        }
        else if (requestUrl.startsWith("https://")) {
            requestUrl = requestUrl.replaceFirst("https:", "http:");
        }

        if (requestUrl.startsWith("http://http://")) {
            requestUrl = requestUrl.replaceFirst("http://", "");
        }

        if (requestUrl.startsWith("http://https://")) {
            requestUrl = requestUrl.replaceFirst("http://", "");
        }

        if (uriInfo.getRequestUri().getQuery() != null) {
            requestUrl += "?" + uriInfo.getRequestUri().getQuery();
        }

        String encodedRequestUrl = gov.lanl.agg.utils.MementoUtils.validateAndEncodeUrl(requestUrl);
        // invalid url -- 404
        if (encodedRequestUrl == null
                || requestDate.getTime() > new Date().getTime()) {
            //throw new NotFoundException("No memento found for: " + requestUrl);
            System.out.println("No Memento found for:" + requestUrl);
            TimeTravel tt = new TimeTravel();
            tt.setAcceptDatetime(requestDate);
            tt.setOriginalUrl(requestUrl);
            tt.setRequestUrl(requestUrl);
            Response.ResponseBuilder responseBuilder = Response.status(404);
            responseBuilder.entity(createErrorPage(tt, requestDate));
            return responseBuilder.build();
        }
        TimeTravelCommons timeTravelCommons = new TimeTravelCommons();
        // find the original url of the req. follows redirect, and rel=orig
        String orig = timeTravelCommons.getOriginalUrl(encodedRequestUrl);
        String requestHost = timeTravelCommons.getRequestHost(orig);

        // invalid url -- 404
        if (requestHost == null) {
            //throw new NotFoundException("No memento found for: " + requestUrl);
            System.out.println("No Memento found for:" + requestUrl);
            TimeTravel tt = new TimeTravel();
            tt.setAcceptDatetime(requestDate);
            tt.setOriginalUrl(requestUrl);
            tt.setRequestUrl(requestUrl);
            Response.ResponseBuilder responseBuilder = Response.status(404);
            responseBuilder.entity(createErrorPage(tt, requestDate));
            return responseBuilder.build();
        }

        if (MementoUtils.isDomainBlacklisted(requestHost, (HashMap<String, String>) MyInitServlet.getInstance().getAttribute("blacklistDomains"))) {
            // Blacklisted Domain -- 403
            System.out.println("Blacklisted Domain: " + requestUrl);
            TimeTravel tt = new TimeTravel();
            tt.setAcceptDatetime(requestDate);
            tt.setOriginalUrl(requestUrl);
            tt.setRequestUrl(requestUrl);
            Response.ResponseBuilder responseBuilder = Response.status(403);
            responseBuilder.entity(createUnauthorizedPage(tt, requestDate));
            return responseBuilder.build();
        }

        /*
        try {
            IBigQueue ressyncque = (IBigQueue) MyInitServlet.getInstance().getAttribute("ResSyncQue");
            Date now = new Date();
            if  (ressyncque!=null) {
                String pload = now.getTime()+"|"+ orig;
                ressyncque.enqueue(pload.getBytes());
            }
        }
        catch (Exception e) {

            e.printStackTrace();
        }
        */

        // checking cache-control headers
        String cacheControl = "none";
        List<String> cacheControlHeader = httpHeaders.getRequestHeader("Cache-Control");
        if (cacheControlHeader != null) {
            String cacheControlValue = cacheControlHeader.get(0);
            if (cacheControlValue.toLowerCase().contains("no-cache")) {
                cacheControl = "no-cache";
            }
            else if (cacheControlValue.toLowerCase().contains("only-if-cached")) {
                cacheControl = "only-if-cached";
            }
        }

        // get mementos if from cache or dyn. always gets native mementos dyn.
       // TimeTravelCommons timeTravelCommons = new TimeTravelCommons();
        TimeTravel tt = timeTravelCommons.getMementos(orig, requestHost, requestDate, cacheControl, "list");
        TimeTravel timeTravel = timeTravelCommons.prepareForDisplay(tt, requestDate);
        timeTravel.setRequestUrl(encodedRequestUrl);

        if ((timeTravel.getSummary().size() == 0 || timeTravel.getSortedArchives().size() == 0) && cacheControl.equals("only-if-cached")) {
            Response.ResponseBuilder responseBuilder = Response.status(504);
            return responseBuilder.build();
        }
        if (timeTravel.getSummary().size() == 0 || timeTravel.getSortedArchives().size() == 0) {
            Response.ResponseBuilder responseBuilder = Response.status(404);
            responseBuilder.entity(createErrorPage(timeTravel, requestDate));
            return responseBuilder.build();
        }
        Response.ResponseBuilder responseBuilder = Response.ok(serializeToHtml(timeTravel, requestDate));
        responseBuilder.header("Last-Modified", TimeTravelCommons.httpformatter.format(timeTravel.getLastModified()));
        return responseBuilder.build();
    }

    public String createUnauthorizedPage(TimeTravel timeTravel, Date requestDate) {

        TimeTravelResult timeTravelResult = new TimeTravelResult();

        String htmlTemplate = timeTravelResult.getTemplate();
        String script = "";
        script += "var acceptDatetime = \"" + MementoCommons.timeTravelJsFormatter.format(timeTravel.getAcceptDatetime()) + "\";\n";
        script += "var requestUrl = \"" + timeTravel.getRequestUrl() + "\";\n";
        htmlTemplate = htmlTemplate.replace("##script##", script);

        if (requestDate.getTime() > new Date().getTime()) {
            htmlTemplate = htmlTemplate.replace("##archive_result##", timeTravelResult.getFutureTimeErrorTemplate());
            htmlTemplate = htmlTemplate.replace("##page_title##", "Memento Time Travel");

            htmlTemplate = htmlTemplate.replaceAll("(##)(.*?)(##)", "");
        }
        if (timeTravel.getSummary().size() == 0 || timeTravel.getSortedArchives().size() == 0) {
            htmlTemplate = htmlTemplate.replace("##archive_result##", timeTravelResult.getUnauthorizedErrorTemplate());
            htmlTemplate = htmlTemplate.replace("##page_title##", "Memento Time Travel");

            htmlTemplate = htmlTemplate.replaceAll("(##)(.*?)(##)", "");
        }
        return htmlTemplate;
    }

    public String createErrorPage(TimeTravel timeTravel, Date requestDate) {

        TimeTravelResult timeTravelResult = new TimeTravelResult();

        String htmlTemplate = timeTravelResult.getTemplate();
        String script = "";
        script += "var acceptDatetime = \"" + MementoCommons.timeTravelJsFormatter.format(timeTravel.getAcceptDatetime()) + "\";\n";
        script += "var requestUrl = \"" + timeTravel.getRequestUrl() + "\";\n";
        htmlTemplate = htmlTemplate.replace("##script##", script);

        if (requestDate.getTime() > new Date().getTime()) {
            htmlTemplate = htmlTemplate.replace("##archive_result##", timeTravelResult.getFutureTimeErrorTemplate());
            htmlTemplate = htmlTemplate.replace("##page_title##", "Memento Time Travel");

            htmlTemplate = htmlTemplate.replaceAll("(##)(.*?)(##)", "");
        }
        if (timeTravel.getSummary().size() == 0 || timeTravel.getSortedArchives().size() == 0) {
            htmlTemplate = htmlTemplate.replace("##archive_result##", timeTravelResult.getErrorTemplate());
            htmlTemplate = htmlTemplate.replace("##page_title##", "Memento Time Travel");

            htmlTemplate = htmlTemplate.replaceAll("(##)(.*?)(##)", "");
        }
        return htmlTemplate;
    }

    public String serializeToHtml(TimeTravel timeTravel, Date requestDate) {

        TimeTravelResult timeTravelResult = new TimeTravelResult();

        String htmlTemplate = timeTravelResult.getTemplate();
        String script = "";
        script += "var acceptDatetime = \"" + MementoCommons.timeTravelJsFormatter.format(timeTravel.getAcceptDatetime()) + "\";\n";
        script += "var requestUrl = \"" + timeTravel.getRequestUrl() + "\";\n";
        htmlTemplate = htmlTemplate.replace("##script##", script);

        /*
        if (requestDate.getTime() > new Date().getTime()) {
            htmlTemplate = htmlTemplate.replace("##archive_result##", timeTravelResult.getFutureTimeErrorTemplate());
            htmlTemplate = htmlTemplate.replace("##page_title##", "Memento Time Travel");

            htmlTemplate = htmlTemplate.replaceAll("(##)(.*?)(##)", "");
            return htmlTemplate;
        }
        if (timeTravel.getSummary().size() == 0 || timeTravel.getSortedArchives().size() == 0) {
            htmlTemplate = htmlTemplate.replace("##archive_result##", timeTravelResult.getErrorTemplate());
            htmlTemplate = htmlTemplate.replace("##page_title##", "Memento Time Travel");

            htmlTemplate = htmlTemplate.replaceAll("(##)(.*?)(##)", "");
            return htmlTemplate;
        }
        */

        //String mementoMessages = "Best Mementos for the requested date " + MementoCommons.timeTravelDisplayFormatter.format(timeTravel.getAcceptDatetime()) + "<br/>";
        String mementoMessages = "Mementos closest to the requested date " + MementoCommons.timeTravelDisplayFormatter.format(timeTravel.getAcceptDatetime()) + "<br/>";
        if (!timeTravel.getRequestUrl().equals(timeTravel.getOriginalUrl())) {
            String requestUrlChanges = timeTravelResult.getRequestURLChangesTemplate();
            mementoMessages += requestUrlChanges.replace("##original_url##", timeTravel.getOriginalUrl());
        }

        Map<Long, String> orderedArchive = timeTravel.getSortedArchives();
        SortedSet<Long> mementoDts = new TreeSet<Long>(orderedArchive.keySet());

        /*
        ObjectMapper jsonMapper = new ObjectMapper();
        List<Map<String, String>> distribution = timeTravel.getDistribution();
        String jsonDistribution = "var mementoDistribution = ";
        try {
            jsonDistribution += jsonMapper.writeValueAsString(distribution);
        }
        catch (Exception ignore) {
            System.out.println("ERROR: " + ignore.getMessage());
        }
        */

        Map<String, List<Link>> summary = timeTravel.getSummary();

        // memento-tt navigation details
        List<Link> summaryLinks = summary.get("summary");
        if (summaryLinks != null) {
            String summaryTemplate = timeTravelResult.getMementoSummary();
            for (Link summaryLink : summaryLinks) {

                if (summaryLink.getRelationship().equals("prev memento")) {
                    String ttPrevUrl = Tokens.LIST_BASE_PATH + TimeTravelCommons.getFormattedDate(summaryLink.getDatetime(), "tt") + "/" + timeTravel.getOriginalUrl();
                    summaryTemplate = summaryTemplate.replace("##prev_timetravel_memento_url##", ttPrevUrl);
                    //htmlTemplate = htmlTemplate.replace("##prev_timetravel_memento_url##", summaryLink.getHref());
                    summaryTemplate = summaryTemplate.replace("##prev_timetravel_memento_dt##", TimeTravelCommons.getFormattedDate(summaryLink.getDatetime(), "display"));

                } else if (summaryLink.getRelationship().equals("next memento")) {
                    String ttNextUrl = Tokens.LIST_BASE_PATH + TimeTravelCommons.getFormattedDate(summaryLink.getDatetime(), "tt") + "/" + timeTravel.getOriginalUrl();
                    summaryTemplate = summaryTemplate.replace("##next_timetravel_memento_url##", ttNextUrl);
                    //htmlTemplate = htmlTemplate.replace("##next_timetravel_memento_url##", summaryLink.getHref());
                    summaryTemplate = summaryTemplate.replace("##next_timetravel_memento_dt##", TimeTravelCommons.getFormattedDate(summaryLink.getDatetime(), "display"));

                } else if (summaryLink.getRelationship().equals("memento")) {
                    summaryTemplate = summaryTemplate.replace("##curr_timetravel_memento_dt##", TimeTravelCommons.getFormattedDate(summaryLink.getDatetime(), "display"));

                    // page title
                    String ttMementoUrl = Tokens.DOMAIN_NAME + Tokens.LIST_BASE_PATH + TimeTravelCommons.getFormattedDate(summaryLink.getDatetime(), "tt") + "/" + timeTravel.getOriginalUrl();
                    String pageTitleTemplate = timeTravelResult.getPageTitleTemplate();
                    pageTitleTemplate = pageTitleTemplate.replace("##title_original_url##", timeTravel.getOriginalUrl());
                    pageTitleTemplate = pageTitleTemplate.replace("##title_timetravel_url##", ttMementoUrl);
                    pageTitleTemplate = pageTitleTemplate.replace("##title_req_datetime##", TimeTravelCommons.displayDateFormatter.format(timeTravel.getAcceptDatetime()));
                    htmlTemplate = htmlTemplate.replace("##page_title##", pageTitleTemplate);

                }
            }
            htmlTemplate = htmlTemplate.replace("##memento_summary##", summaryTemplate);
        }

        //Integer totalMementos = 0;
        // Per archive details
        String content = "";
        for (Long mementoDt : mementoDts) {

            String archiveName = orderedArchive.get(mementoDt);
            ArchiveDescription archive = TimeTravelCommons.archiveShortName.get(archiveName);

            String archiveCalendarPage = timeTravelResult.getArchiveCalendarPageTemplate();
            if (archiveName.equals("summary")) {
                continue;
            }
            String archiveResult = timeTravelResult.getArchiveResultTemplate();
            List<Link> links = summary.get(archiveName);

            if (!TimeTravelCommons.archiveShortName.keySet().contains(archiveName.toLowerCase())) {
                archive = new ArchiveDescription();
                archive.setCalendarUrl("");
                String mementoUrl = links.get(0).getHref();
                mementoUrl = mementoUrl.replace("http://", "");
                mementoUrl = mementoUrl.replace("https://", "");
                String baseUrl = mementoUrl.split("/")[0];
                archive.setLongname(baseUrl);
            }

            String archiveFullName;
            if (archive.getLongname() == null) {
                archiveFullName = archiveName;
            }
            else {
                archiveFullName = archive.getLongname();
            }
            String firstMementoYear = "";
            String lastMementoYear = "";

            //String mementoAdjective = "Best";
            if (!TimeTravelCommons.archiveShortName.keySet().contains(archiveName.toLowerCase())) {
                //mementoAdjective = "Exact";
                String nativeArchiveMessage = timeTravelResult.getNativeArchiveMessageTemplate();
                String nextMementoDt = "";
                String lastMementoDt = "";
                String memDt = "";
                for (Link link : links) {
                    if (link.getRelationship().equals("memento")) {
                        memDt = TimeTravelCommons.getFormattedDate(link.getDatetime(), "display");
                    }
                    if (link.getRelationship().equals("next memento")) {
                        nextMementoDt = TimeTravelCommons.getFormattedDate(link.getDatetime(), "display");
                    }
                    else if (link.getRelationship().equals("last memento")) {
                        lastMementoDt = TimeTravelCommons.getFormattedDate(link.getDatetime(), "display");
                    }
                }
                if (nextMementoDt.equals("") && !lastMementoDt.equals("")) {
                    nativeArchiveMessage = nativeArchiveMessage.replace("##native_memento_from##", memDt);
                    nativeArchiveMessage = nativeArchiveMessage.replace("##native_memento_to##", lastMementoDt);
                }
                else if (!nextMementoDt.equals("")) {
                    nativeArchiveMessage = nativeArchiveMessage.replace("##native_memento_from##", memDt);
                    nativeArchiveMessage = nativeArchiveMessage.replace("##native_memento_to##", nextMementoDt);
                }
                else {
                    nativeArchiveMessage = nativeArchiveMessage.replace("##native_memento_from##", memDt);
                    nativeArchiveMessage = nativeArchiveMessage.replace("##native_memento_to##", memDt);
                }
                archiveResult = archiveResult.replace("##native_archive_message##", nativeArchiveMessage);
            }
            Long firstMementoDate = 0L;
            for (Link link : links) {
                //String subResultMementoUrl = Tokens.REDIRECT_BASE_PATH + link.getHref();
                String subResultMementoUrl = link.getHref();
                String subResultMementoDt = TimeTravelCommons.getFormattedDate(link.getDatetime(), "display");
                String subResultType = "";
                String subResultTypeTitle = "";

                if (link.getRelationship().equals("prev memento")
                        || link.getRelationship().equals("memento prev")) {
                    subResultType = "##prev_memento##";
                    subResultTypeTitle = "Previous Memento";
                }
                else if (link.getRelationship().equals("next memento")
                        || link.getRelationship().equals("memento next")) {
                    subResultType = "##next_memento##";
                    subResultTypeTitle = "Next Memento";
                }
                else if (link.getRelationship().equals("first memento")
                        || link.getRelationship().equals("memento first")) {
                    subResultType = "##first_memento##";
                    subResultTypeTitle = "First Memento";
                    if (!archive.getCalendarUrl().equals("")) {
                        firstMementoYear = TimeTravelCommons.getFormattedDate(link.getDatetime(), "year");
                    }
                    try {
                        firstMementoDate = TimeTravelCommons.httpformatter.parse(link.getDatetime()).getTime();
                    } catch (ParseException ignore) {}

                }
                else if (link.getRelationship().equals("last memento")
                        || link.getRelationship().equals("memento last")) {
                    subResultType = "##last_memento##";
                    subResultTypeTitle = "Last Memento";
                    if (!archive.getCalendarUrl().equals("")) {
                        lastMementoYear = TimeTravelCommons.getFormattedDate(link.getDatetime(), "year");
                    }
                }
                else if (link.getRelationship().equals("memento")) {
                    archiveResult = archiveResult.replace("##memento_redirect_url##", subResultMementoUrl);
                    archiveResult = archiveResult.replace("##memento_url##", link.getHref());
                    archiveResult = archiveResult.replace("##memento_dt##", subResultMementoDt);
                    archiveResult = archiveResult.replace("##archive_title##", archiveFullName);
                    //archiveResult = archiveResult.replace("##memento_adjective##", mementoAdjective);
                    Date memDt = null;
                    try {
                        memDt = TimeTravelCommons.httpformatter.parse(link.getDatetime());
                    }
                    catch (ParseException ignore) {}

                    if ((!TimeTravelCommons.archiveShortName.keySet().contains(archiveName.toLowerCase())
                            || archive.getArchiveType().toLowerCase().equals("cms"))
                            ) {

                        if (firstMementoDate == 0) {
                            for (Link l : links) {
                                if (l.getRelationship().equals("first memento")
                                        || l.getRelationship().equals("memento first")) {
                                    try {
                                        firstMementoDate = TimeTravelCommons.httpformatter.parse(l.getDatetime()).getTime();
                                    } catch (ParseException ignore) {}
                                    break;
                                }
                            }
                        }

                        if (firstMementoDate > 0 && firstMementoDate <= requestDate.getTime()) {
                            //archiveResult = archiveResult.replace("##memento_delta##", "0 min");
                            archiveResult = archiveResult.replace("##memento_delta##", TimeTravelCommons.getFormattedTimeDelta(requestDate, memDt, "info"));
                            archiveResult = archiveResult.replace("##memento_delta_title##", "Active at requested date");
                        }
                        else {
                            archiveResult = archiveResult.replace("##memento_delta##", TimeTravelCommons.getFormattedTimeDelta(requestDate, memDt, "info"));
                            archiveResult = archiveResult.replace("##memento_delta_title##", TimeTravelCommons.getFormattedTimeDelta(requestDate, memDt, "title"));
                        }
                    }
                    else {
                        archiveResult = archiveResult.replace("##memento_delta##", TimeTravelCommons.getFormattedTimeDelta(requestDate, memDt, "info"));
                        archiveResult = archiveResult.replace("##memento_delta_title##", TimeTravelCommons.getFormattedTimeDelta(requestDate, memDt, "title"));
                    }

                }
                if (!subResultType.equals("")) {
                    String subResultTemplate = timeTravelResult.getSubResultTemplate();
                    subResultTemplate = subResultTemplate.replace("##sub_result_memento_redirect_url##", subResultMementoUrl);
                    subResultTemplate = subResultTemplate.replace("##sub_result_type_title##", subResultTypeTitle);
                    subResultTemplate = subResultTemplate.replace("##sub_result_memento_dt##", subResultMementoDt);
                    Date memDt = null;
                    try {
                        memDt = TimeTravelCommons.httpformatter.parse(link.getDatetime());
                    }
                    catch (ParseException ignore) {
                        System.out.println("ERROR: " + ignore.getMessage());
                    }
                    subResultTemplate = subResultTemplate.replace("##sub_result_memento_delta##", TimeTravelCommons.getFormattedTimeDelta(requestDate, memDt, "info"));

                    archiveResult = archiveResult.replace(subResultType, subResultTemplate);
                }
            }
            if (!archive.getCalendarUrl().equals("")
                    && !lastMementoYear.equals("")
                    && !firstMementoYear.equals("")) {

                //archiveCalendarPage = archiveCalendarPage.replace("##mementos_per_archive##", timeTravel.getByArchiveCount().get(archiveName));
                archiveCalendarPage = archiveCalendarPage.replace("##archive_img_url##", archive.getIcon());
                archiveCalendarPage = archiveCalendarPage.replace("##archive_calendar_url##", archive.getCalendarUrl() + timeTravel.getOriginalUrl());
                archiveCalendarPage = archiveCalendarPage.replace("##first_memento_year##", firstMementoYear);
                archiveCalendarPage = archiveCalendarPage.replace("##last_memento_year##", lastMementoYear);

                archiveResult = archiveResult.replace("##archive_calendar_page##", archiveCalendarPage);
            }
            else {
                archiveResult = archiveResult.replace("##archive_calendar_page##", "");
            }
            archiveResult = archiveResult.replace("##archive_name##", archiveFullName);
            //totalMementos += Integer.parseInt(timeTravel.getByArchiveCount().get(archiveName));
            content += archiveResult;
        }
        /*
        String totalMementosMessage;
        if (timeTravel.getIsDynamic()) {
            totalMementosMessage = "<div><span id='total_number_of_mementos' class='ui-state-error ui-corner-all'>We are processing all the mementos for this resource. Check back later for complete results.</span></div>";
        }
        else {
            totalMementosMessage = "<div><span id='total_number_of_mementos'>Total number of mementos: " + totalMementos.toString() + "</span></div>";
        }
        */

        htmlTemplate = htmlTemplate.replace("##archive_result##", content);
        htmlTemplate = htmlTemplate.replace("##memento_messages##", mementoMessages);

        htmlTemplate = htmlTemplate.replace("##prev_memento##", "<a>Previous Memento</a><br/>" +
                "<p class='sub_sub_result'>data not provided<br/>&nbsp;</p>");
        htmlTemplate = htmlTemplate.replace("##next_memento##", "<a>Next Memento</a><br/>" +
                "<p class='sub_sub_result'>data not provided<br/>&nbsp;</p>");
        htmlTemplate = htmlTemplate.replace("##last_memento##", "<a>Last Memento</a><br/>" +
                "<p class='sub_sub_result'>data not provided<br/>&nbsp;</p>");
        htmlTemplate = htmlTemplate.replace("##first_memento##", "<a>First Memento</a><br/>" +
                "<p class='sub_sub_result'>data not provided<br/>&nbsp;</p>");


        htmlTemplate = htmlTemplate.replaceAll("(##)(.*?)(##)", "");

        return htmlTemplate;
    }

    @POST
    public Response replytoPOST() {
        Response.ResponseBuilder r = Response.status(405);
        r.header("Allow", "GET,HEAD");
        r.header("Vary","Accept-Datetime");
        return r.build();
    }

    @PUT
    public Response replytoPUT() {
        Response.ResponseBuilder r = Response.status(405);
        r.header("Allow", "GET,HEAD");
        r.header("Vary","Accept-Datetime");

        return r.build();
    }

    @DELETE
    public Response replytoDELETE() {
        Response.ResponseBuilder r = Response.status(405);
        r.header("Allow", "GET,HEAD");
        r.header("Vary","Accept-Datetime");
        return r.build();
    }
}
