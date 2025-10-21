package gov.lanl.agg.utils;

import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.LinkParser;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

public class LiveResourceClient {

    OriginalResource oresource;
    public  Integer redirectCount = 0;

    public LiveResourceClient() {
        oresource  = new  OriginalResource();
    }

    public  OriginalResource getOriginalResource() {
        return oresource;
    }

    //this class looking for timegate,timemap info
    public String checkFeedUrl(HttpClient client, String feedUrl) {
        String response = feedUrl;
        try {
            URI feedUri = new URI(feedUrl);
            feedUrl = feedUri.toASCIIString();
        }
        catch (Exception ignore) {
            return null;
        }
        HttpMethod method = new HeadMethod(feedUrl);
        //HttpMethod method = new GetMethod(feedUrl);
        DefaultHttpMethodRetryHandler retryHandler = new DefaultHttpMethodRetryHandler(0, false);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
        method.setFollowRedirects(false);
        method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

        String feedHost = null;
        String feedProtocol = null;
        try {
            URL feedurl = new URL(feedUrl);
            feedHost = feedurl.getHost();
            feedProtocol = feedurl.getProtocol();
        }
        catch (Exception ignore) {
            return null;
        }

        try {
            // request feed
            client.getParams().setConnectionManagerTimeout(10000);
            int statusCode = client.executeMethod(method);
            //System.out.println("status" +statusCode);
           // System.out.println("update live:"+statusCode +"url:"+feedUrl );

            /** @Harish
             * As of 01/23/2015, the live client need not follow redirects.
             * Check if the request/feed url has rel=original,
             * if yes, return original,
             * if no, return feed url.
             */


            Header link_msg = method.getResponseHeader("Link");
            if (link_msg != null) {
                String add_msg = link_msg.getValue();
                LinkParser parser = new LinkParser(add_msg);
                parser.parse();
                LinkHeader linkheadertmp = parser.getHeader();
                Link ltimegate = linkheadertmp.getLinkByRelationship("original");
                if (ltimegate != null) {
                    //String timegate = ltimegate.getHref();
                    oresource.setOriginalURI(ltimegate.getHref());
                    //response = checkFeedUrl(client,timegate,date);
                }
                else {
                    oresource.setOriginalURI(feedUrl);
                }
            }
            else {
                oresource.setOriginalURI(feedUrl);
            }

            response = feedUrl;
            /*
            if ((statusCode == 301) | (statusCode == 302)|(statusCode == 303)) {

                // boolean addredirect = false;
                Header location = method.getResponseHeader("Location");
                if ( !location.getValue().equals("")) {
                    // recursively check URL until it's not redirected any more
                    System.out.println("redirect: " + location.getValue());
                    String nexturl = location.getValue();

                    // @harish: fixing relative urls in location header
                    String host = null;
                    String protocol = null;
                    try {
                        URL url = new URL(nexturl);
                        host = url.getHost();
                        protocol = url.getProtocol();
                    }
                    catch (Exception ignore) {}

                    if (host == null) {
                        nexturl = feedHost + nexturl;
                    }
                    if (protocol == null) {
                        nexturl = feedProtocol + "://" + nexturl;
                    }

                    // @Harish: commenting the https replace to avoid
                    // infinite redirects as some sites force https only.

                    //if (nexturl.startsWith("https:")) {
                    //    nexturl = nexturl.replaceFirst("https:", "http:");
                    //}
                    redirectCount++;
                    if (redirectCount <= 50) {
                        response = checkFeedUrl(client, nexturl);
                    }
                }
            } else {
                //other codes original resource as input parameter
                if (statusCode==200) {
                    System.out.println("orig resource:"+feedUrl);
                    Header link_msg = method.getResponseHeader("Link");
                    if (link_msg!=null){
                        String add_msg = link_msg.getValue();
                        LinkParser parser = new LinkParser(add_msg);
                        parser.parse();
                        LinkHeader linkheadertmp = parser.getHeader();
                        Link ltimegate = linkheadertmp.getLinkByRelationship("original");
                        if (ltimegate!=null) {
                            //String timegate = ltimegate.getHref();
                            oresource.setOriginalURI(ltimegate.getHref());
                            //response = checkFeedUrl(client,timegate,date);
                        }
                        else {
                            oresource.setOriginalURI(feedUrl);
                        }
                    }
                    else {
                        oresource.setOriginalURI(feedUrl);
                    }
                }
                response = feedUrl;
            }
            */

        } catch (Exception ioe) {
            response = feedUrl;
        }
        finally { method.releaseConnection(); }
        return response;
    }

}
