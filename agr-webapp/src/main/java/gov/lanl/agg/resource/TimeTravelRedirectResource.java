package gov.lanl.agg.resource;

import gov.lanl.agg.utils.Tokens;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

/**
 * @author: Harihar Shankar, 6/23/14 9:55 AM
 */

@Path(Tokens.REDIRECT_BASE_PATH)

public class TimeTravelRedirectResource {

    @Path("{url:.*}")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getMemento(@Context HttpHeaders httpHeaders,
                             @Context UriInfo uriInfo)
        throws ParseException, URISyntaxException {

        String reqPath;
        if (uriInfo.getRequestUri().getQuery() != null) {
            reqPath = uriInfo.getPath() + "?" + uriInfo.getRequestUri().getQuery();
        }
        else {
            reqPath = uriInfo.getPath();
        }

        if (!reqPath.startsWith("/")) {
            reqPath = "/" + reqPath;
        }
        reqPath = reqPath.replace(Tokens.REDIRECT_BASE_PATH, "");
        String reqDate = reqPath.split("/")[0];
        String mementoUrl = reqPath.replaceFirst(reqDate + "/", "");
        Date requestDate = MementoCommons.getTimeTravelDate(reqDate);

        String displayDate = MementoCommons.timeTravelDisplayDateFormatter.format(requestDate);

        if (!mementoUrl.startsWith("http://") && !mementoUrl.startsWith("https://")) {
            return "ERROR: Bad URL!";
        }
        try {
            new URL(mementoUrl);
        }
        catch (MalformedURLException ignore) {
            return "ERROR: Bad URL!";
        }

        System.out.println("MEM: " + mementoUrl);
        String html =
                "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">" +
                "  <title>Memento</title>" +
                "</head>" +
                "<body>" +
                "<div style='text-align: center'>\n" +
                        "    <img id='memento_img' src='/css/images/memento_logo.svg' style=\"width: 350px; height: 350px;\"/>\n" +
                        "    <h1>mementoweb.org</h1>\n" +
                        "    <h2>Redirecting to best Memento near "+displayDate+"</h2>\n" +
                        "</div>\n" +
                "  <script>" +
                        "  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){\n" +
                        "  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),\n" +
                        "  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)\n" +
                        "  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');\n" +
                        "\n" +
                        "  ga('create', 'UA-10627462-1', 'mementoweb.org');\n" +
                        "  ga('send', 'pageview');\n" +
                        "\n" +
                        "setTimeout(function() {window.location.href = \"" + mementoUrl + "\";}, 150);" +
                "  </script>" +
                "</body>" +
                "</html>";

        return html;
    }
}
