package gov.lanl.agg.resource;

import gov.lanl.agg.Link;
import gov.lanl.agg.TimeTravel;
import gov.lanl.agg.utils.MementoUtils;
import gov.lanl.agg.utils.Tokens;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

/**
 * @author: Harihar Shankar, 6/30/14 2:03 PM
 */

@Path(Tokens.MEMENTO_BASE_PATH)

public class TimeTravelGetResource {

   // static TimeTravelCommons timeTravelCommons;

    static {
    }

    public TimeTravelGetResource(@Context UriInfo uriInfo) {
        //timeTravelCommons = new TimeTravelCommons(uriInfo.getBaseUri());
    }

    public TimeTravelGetResource() {
    }

    @Path("{date}/{id:.*}")
    @GET
    public Response getTimegate(@Context HttpHeaders httpHeaders,
                                @Context UriInfo uriInfo,
                                @PathParam("id") String idp,
                                @PathParam("date") String reqDate)
            throws ParseException, URISyntaxException {
    	TimeTravelCommons timeTravelCommons = new TimeTravelCommons();
        String requestPath = uriInfo.getPath();
        if (!requestPath.startsWith("/")) {
            requestPath = "/" + requestPath;
        }

        String requestUrl = requestPath.replaceFirst(
                Tokens.MEMENTO_BASE_PATH + reqDate + "/", "");

        requestUrl = requestUrl.trim();
        if (!requestUrl.startsWith("http://") && !requestUrl.startsWith("https://")) {
            requestUrl = "http://" + requestUrl;
        }
        else if (requestUrl.startsWith("https://")) {
            requestUrl = requestUrl.replaceFirst("https:", "http:");
        }

        if (uriInfo.getRequestUri().getQuery() != null) {
            requestUrl += "?" + uriInfo.getRequestUri().getQuery();
        }

        //some different validity routine
        Date requestDate = MementoCommons.getTimeTravelDate(reqDate);

        String encodedRequestUrl = gov.lanl.agg.utils.MementoUtils.validateAndEncodeUrl(requestUrl);

        if (encodedRequestUrl == null || requestDate.getTime() > new Date().getTime()) {
            String location = requestPath.replace(Tokens.MEMENTO_BASE_PATH, Tokens.LIST_BASE_PATH);

            Response.ResponseBuilder responseBuilder = Response.status(302);
            System.out.println("ERROR: " + location);
            responseBuilder.header("Location", location);
            return responseBuilder.build();
        }

        String orig = timeTravelCommons.getOriginalUrl(encodedRequestUrl);
        String requestHost = timeTravelCommons.getRequestHost(orig);

        if (requestHost == null
                || requestDate.getTime() > new Date().getTime()
                || MementoUtils.isDomainBlacklisted(requestHost, (HashMap<String, String>) MyInitServlet.getInstance().getAttribute("blacklistDomains"))
                ) {
            String location = requestPath.replace(Tokens.MEMENTO_BASE_PATH, Tokens.LIST_BASE_PATH);

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

        TimeTravel tt = timeTravelCommons.getMementos(orig, requestHost, requestDate, cacheControl, "memento");
        //TimeTravel timeTravel = timeTravelCommons.prepareForDisplay(tt, requestDate);
        //System.out.println(tt.getByArchiveCount().toString());

        //NavigableMap<Long, Link> summary = timeTravel.getMementos().get("summary");
        //NavigableMap<Long, Link> summary = timeTravel.getSummary();
        Map<String, List<Link>> summary = tt.getSummary();

        String location = "";
        List<Link> summaryLinks = summary.get("summary");
        if (summaryLinks != null) {
            for (Link summaryLink : summaryLinks) {
                if (summaryLink.getRelationship().equals("memento")) {
                    location = summaryLink.getHref();
                    break;
                }
            }
        }

        // memento found!
        if (!location.equals("")) {
            Response.ResponseBuilder responseBuilder = Response.status(302);
            //responseBuilder.header("Location", Tokens.REDIRECT_BASE_PATH + reqDate + "/" + location);
            responseBuilder.header("Location", location);
            responseBuilder.header("Link", "<"+orig+">;rel=\"original\"");
            responseBuilder.header("Last-Modified", TimeTravelCommons.httpformatter.format(tt.getLastModified()));
            return responseBuilder.build();
        }
        else if (cacheControl.equals("only-if-cached")) {
            Response.ResponseBuilder responseBuilder = Response.status(504);
            return responseBuilder.build();
        }
        else {
            location = requestPath.replace(Tokens.MEMENTO_BASE_PATH, Tokens.LIST_BASE_PATH);

            Response.ResponseBuilder responseBuilder = Response.status(302);
            System.out.println("ERROR: " + location);
            responseBuilder.header("Location", location);
            return responseBuilder.build();
        }
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
