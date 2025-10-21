package gov.lanl.agg.resource;

import gov.lanl.agg.TimeTravel;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.Tokens;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.*;

import gov.lanl.agg.Link;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * @author: Harihar Shankar, 6/11/14 2:42 PM
 */

//@Path(Tokens.JSON_API_BASE_PATH)
@Path  ("/api/json/")

public class TimeTravelAPResource {

   //  TimeTravelCommons timeTravelCommons;
    String protocol;
    URI baseUri ;
    

    public TimeTravelAPResource(@Context UriInfo uriInfo) {
       // timeTravelCommons = new TimeTravelCommons(uriInfo.getBaseUri());
    	 this.baseUri = uriInfo.getBaseUri();
    }

    @Path("{date}/{id:.*}")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTimegate(@Context HttpHeaders httpHeaders,
                                @Context UriInfo uriInfo,
                                @PathParam("id") String idUrl,
                                @PathParam("date") String reqDate)
            throws ParseException, URISyntaxException {


        List <String> hscheme = httpHeaders.getRequestHeader("X-Forwarded-Proto");
        if (hscheme == null) {
            protocol ="http://";
        }
        else {
            protocol ="https://";
        }

        Date requestDate = MementoCommons.getTimeTravelDate(reqDate);
        String requestUrl = uriInfo.getRequestUri().getPath().replace(Tokens.JSON_API_BASE_PATH + reqDate +"/", "");

        if (!requestUrl.startsWith("http://") && !requestUrl.startsWith("https://")) {
            requestUrl = "http://" + requestUrl;
        }
        else if (requestUrl.startsWith("https://")) {
            requestUrl = requestUrl.replaceFirst("https:", "http:");
        }

        if (uriInfo.getRequestUri().getQuery() != null) {
            requestUrl += "?" + uriInfo.getRequestUri().getQuery();
        }

        String encodedRequestUrl = gov.lanl.agg.utils.MementoUtils.validateAndEncodeUrl(requestUrl);

        if (encodedRequestUrl == null || requestDate.getTime() > new Date().getTime()) {
            String reqUrl = uriInfo.getRequestUri().toString();
            System.out.println("No Memento found for invalid url:" + requestUrl);
            String location = reqUrl.replace(Tokens.JSON_API_BASE_PATH, Tokens.LIST_BASE_PATH);

            Response.ResponseBuilder responseBuilder = Response.status(302);
            System.out.println("ERROR: " + location);
            responseBuilder.header("Location", location);
            return responseBuilder.build();
        }

        // find the original url of the req. follows redirect, and rel=orig
        TimeTravelCommons timeTravelCommons = new TimeTravelCommons();
        String orig = timeTravelCommons.getOriginalUrl(encodedRequestUrl);
        String requestHost = timeTravelCommons.getRequestHost(orig);

        /*
        try {
            Date now = new Date();
            IBigQueue ressyncque = (IBigQueue) MyInitServlet.getInstance().getAttribute("ResSyncQue");
            if  (ressyncque!=null) {
                String pload = now.getTime()+"|"+ orig;
                ressyncque.enqueue(pload.getBytes());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        */
       HashMap<String, String> bm = (HashMap<String,String>) MyInitServlet.getInstance().getAttribute("blacklistDomains");
        System.out.println("request host:"+requestHost);
        if (requestHost == null
                || MementoUtils.isDomainBlacklisted(requestHost, bm)) {
            System.out.println("No Memento found for:" + requestUrl);
            String reqUrl = uriInfo.getRequestUri().toString();
            String location = reqUrl.replace(Tokens.JSON_API_BASE_PATH, Tokens.LIST_BASE_PATH);

            Response.ResponseBuilder responseBuilder = Response.status(302);
            System.out.println("ERROR: " + location);
            responseBuilder.header("Location", location);
            return responseBuilder.build();
        }

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
        //TimeTravelCommons timeTravelCommons = new TimeTravelCommons(uriInfo.getBaseUri());
        TimeTravel timeTravel =timeTravelCommons.getMementos(orig, requestHost, requestDate, cacheControl, "api");
        //TimeTravel timeTravel = timeTravelCommons.prepareForDisplay(tt, requestDate, true);

        timeTravel.setRequestUrl(encodedRequestUrl);
        Map<String, List<Link>> summary = timeTravel.getSummary();

        Map<String, Object> api = new HashMap<String, Object>();

        api.put("original_uri", timeTravel.getOriginalUrl());
        api.put("timegate_uri", protocol + TimeTravelCommons.proxybaseuri + "timegate/" + timeTravel.getOriginalUrl());

        Map<String, String> tmUri = new HashMap<String, String>();
        tmUri.put("link_format", protocol + TimeTravelCommons.proxybaseuri + "timemap/link/" + timeTravel.getOriginalUrl());
        tmUri.put("json_format", protocol + TimeTravelCommons.proxybaseuri + "timemap/json/" + timeTravel.getOriginalUrl());
        api.put("timemap_uri", tmUri);

        Map<String, Map<String, Object>> mementos = new HashMap<>();
        List<Link> mems = summary.get("summary");
        if (mems == null && cacheControl.equals("only-if-cached")) {
            Response.ResponseBuilder responseBuilder = Response.status(504);
            return responseBuilder.build();
        }
        else if (mems == null) {

            String reqUrl = uriInfo.getRequestUri().toString();
            String location = reqUrl.replace(Tokens.JSON_API_BASE_PATH, Tokens.LIST_BASE_PATH);

            Response.ResponseBuilder responseBuilder = Response.status(302);
            System.out.println("ERROR: " + location);
            responseBuilder.header("Location", location);
            return responseBuilder.build();
            //return "No mementos were found for the request.";
        }

        Date firstMemento = null;
        String firstMemUrl = null;
        Date prevMemento = null;
        String prevMemUrl = null;
        Date nextMemento = null;
        String nextMemUrl = null;
        Date lastMemento = null;
        String lastMemUrl = null;
        Date closestMemento = null;
        String closestMemUrl = null;

        for (Link link : mems) {
            Date linkDate = MementoCommons.httpformatter.parse(link.getDatetime());
            switch (link.getRelationship()) {
                case "memento": {
                    closestMemento = linkDate;
                    closestMemUrl = link.getHref();
                    break;
                }
                case "prev memento": {
                    prevMemento = linkDate;
                    prevMemUrl = link.getHref();
                    break;
                }
                case "next memento": {
                    nextMemento = linkDate;
                    nextMemUrl = link.getHref();
                    break;
                }
                case "last memento": {
                    lastMemento = linkDate;
                    lastMemUrl = link.getHref();
                    break;
                }
                case "first memento": {
                    firstMemento = linkDate;
                    firstMemUrl = link.getHref();
                    break;
                }
            }
        }

        Map<String, List<Link>> duplicates = ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage")).getDuplicateMementos(encodedRequestUrl, firstMemento, prevMemento, closestMemento, nextMemento, lastMemento);



        if (duplicates.get("first").size() > 0) {
            mementos.put("first", makeMementoJson(duplicates, "first"));
        }
        else if (duplicates.get("first").size() == 0 && firstMemento != null) {
            mementos.put("first", makeDynMementoJson(firstMemento, firstMemUrl));
        }

        if (duplicates.get("prev").size() > 0) {
            mementos.put("prev", makeMementoJson(duplicates, "prev"));
        }
        else if (duplicates.get("prev").size() == 0 && prevMemento != null) {
            mementos.put("prev", makeDynMementoJson(prevMemento, prevMemUrl));
        }

        if (duplicates.get("memento").size() > 0) {
            mementos.put("closest", makeMementoJson(duplicates, "memento"));
        }
        else if (duplicates.get("memento").size() == 0 && closestMemento != null) {
            mementos.put("closest", makeDynMementoJson(closestMemento, closestMemUrl));
        }

        if (duplicates.get("next").size() > 0) {
            mementos.put("next", makeMementoJson(duplicates, "next"));
        }
        else if (duplicates.get("next").size() == 0 && nextMemento != null) {
            mementos.put("next", makeDynMementoJson(nextMemento, nextMemUrl));
        }

        if (duplicates.get("last").size() > 0) {
            mementos.put("last", makeMementoJson(duplicates, "last"));
        }
        else if (duplicates.get("last").size() == 0 && lastMemento != null) {
            mementos.put("last", makeDynMementoJson(lastMemento, lastMemUrl));
        }

        /*
        for (Link link : mems) {
            Date linkDate = MementoCommons.httpformatter.parse(link.getDatetime());
            Map<String, String> rels = new HashMap<String, String>();

            if (link.getRelationship().equals("memento")) {
                rels.put("datetime", MementoCommons.timeTravelJsFormatter.format(linkDate));
                rels.put("uri", link.getHref());
                mementos.put("closest", rels);
            }
            else if (link.getRelationship().startsWith("prev")) {
                rels.put("datetime", MementoCommons.timeTravelJsFormatter.format(linkDate));
                rels.put("uri", link.getHref());
                mementos.put("prev", rels);
            }
            else if (link.getRelationship().startsWith("next")) {
                rels.put("datetime", MementoCommons.timeTravelJsFormatter.format(linkDate));
                rels.put("uri", link.getHref());
                mementos.put("next", rels);
            }

            else if (link.getRelationship().startsWith("last")) {
                rels.put("datetime", MementoCommons.timeTravelJsFormatter.format(linkDate));
                rels.put("uri", link.getHref());
                mementos.put("last", rels);
            }
            else if (link.getRelationship().startsWith("first")) {
                rels.put("datetime", MementoCommons.timeTravelJsFormatter.format(linkDate));
                rels.put("uri", link.getHref());
                mementos.put("first", rels);
            }
        }
        */


        /*
        LinkHeader linkHeader = ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage")).getTimegateInfo(orig, requestDate,null);

        if (linkHeader != null) {
            NavigableMap<Long, Link> tgMementos = linkHeader.getOrderedLinksByDate();
            if (tgMementos.size() > 0) {
                Link firstLink = tgMementos.get(tgMementos.firstKey());
                Link lastLink = tgMementos.get(tgMementos.lastKey());

                Map<String, String> first = new HashMap<String, String>();
                Date firstDate = MementoCommons.formatter_utc.parse(firstLink.getDatetime());
                first.put("datetime", MementoCommons.timeTravelJsFormatter.format(firstDate));
                first.put("uri", firstLink.getHref());
                mementos.put("first", first);

                Map<String, String> last = new HashMap<String, String>();
                Date lastDate = MementoCommons.formatter_utc.parse(lastLink.getDatetime());
                last.put("datetime", MementoCommons.timeTravelJsFormatter.format(lastDate));
                last.put("uri", lastLink.getHref());
                mementos.put("last", last);
            }
        }
        */

        api.put("mementos", mementos);

        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        //return gson.toJson(api, Map.class);
        Response.ResponseBuilder responseBuilder = Response.ok(gson.toJson(api, Map.class));
        responseBuilder.header("Last-Modified", timeTravelCommons.httpformatter.format(timeTravel.getLastModified()));
        return responseBuilder.build();

        //  return serializeToHtml(timeTravel);

        //return serializeToJson(timeTravel);
        //TimeTravel tt = new TimeTravel();
        //tt.setAcceptDatetime(requestDate);
        //tt.setOriginalUrl(requestUrl);
        //return serializeToHtml(tt);
    }

    private static Map<String, Object> makeMementoJson(Map<String, List<Link>> mementos, String rel) {

        Map<String, Object> rels = new HashMap<>();
        List<Link> dupMementos = mementos.get(rel);

        List<String> dupUris = new ArrayList<>();
        for (Link dupLink : dupMementos) {
            dupUris.add(dupLink.getHref());
        }
        rels.put("datetime", dupMementos.get(0).getDatetime());
        rels.put("uri", dupUris);
        return rels;
    }

    private static Map<String, Object> makeDynMementoJson(Date mementoDatetime, String mementoUrl) {
        Map<String, Object> rel = new HashMap<>();
        rel.put("datetime", MementoCommons.timeTravelJsFormatter.format(mementoDatetime));
        List<String> mem = new ArrayList<>();
        mem.add(mementoUrl);
        rel.put("uri", mem);
        return rel;
    }

    public String serializeToJson(TimeTravel timeTravel){
        //Gson gson = new Gson();
        //.serializeNulls() do we want it or not?
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        String json = gson.toJson(timeTravel,TimeTravel.class);
        return json;
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
    /*
    public class MyExclusionStrategy implements ExclusionStrategy {
        private final Class<?> typeToSkip;

        private MyExclusionStrategy(Class<?> typeToSkip) {
          this.typeToSkip = typeToSkip;
        }

        public boolean shouldSkipClass(Class<?> clazz) {
          return (clazz == typeToSkip);
        }

        public boolean shouldSkipField(FieldAttributes f) {
          return f.getAnnotation(Foo.class) != null;
        }
      }

    */
}
