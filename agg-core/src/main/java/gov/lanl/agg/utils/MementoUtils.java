package gov.lanl.agg.utils;

//import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;


import org.apache.commons.lang3.time.FastDateFormat;

import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
//import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.validator.routines.UrlValidator;
/*
@author Lyudmila Balakireva
*/

public class MementoUtils {
    public  static   FastDateFormat httpformatter;
    public  static FastDateFormat timeTravelMachineFormatter;
    public  static FastDateFormat timeTravelJsFormatter;
    public  static FastDateFormat timeTravelDisplayFormatter;
    public  static FastDateFormat timeTravelDisplayDateFormatter;
    public  static FastDateFormat timeTravelYearFormatter;
    public  static FastDateFormat timeTravelMonthFormatter;
    public  static FastDateFormat timeTravelDayFormatter;
    public  static FastDateFormat formatter_utc;
    public  static FastDateFormat formatter_db;
    public  static FastDateFormat formatter_sdb;
    public  static FastDateFormat dtformatter;
    static final List mementoresourcesupportedformats = new ArrayList();
    static final List  dtsupportedformatsv = new ArrayList();
    static final public List  srvformats = new ArrayList();
 
    static  URI baseUri;
    static {

          TimeZone tz = TimeZone.getTimeZone("GMT");
          httpformatter = FastDateFormat.getInstance("E, dd MMM yyyy HH:mm:ss z",tz,Locale.US);
            dtformatter = FastDateFormat.getInstance("E, dd MMM yyyy",tz,Locale.US);
          

        // TimeTravel formats
        timeTravelMachineFormatter = FastDateFormat.getInstance("yyyyMMddHHmmss",tz,Locale.US);
        timeTravelJsFormatter =  FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'",tz,Locale.US);
        System.out.println("formating");
        timeTravelDisplayDateFormatter = FastDateFormat.getInstance("yyyy-MM-dd",tz,Locale.US);
        timeTravelDisplayFormatter = FastDateFormat.getInstance("dd MMM yyyy HH:mm:ss z",tz,Locale.US);
        timeTravelDayFormatter = FastDateFormat.getInstance("dd",tz,Locale.US);   
        timeTravelMonthFormatter = FastDateFormat.getInstance("MMMM",tz,Locale.US);
        timeTravelYearFormatter = FastDateFormat.getInstance("yyyy",tz,Locale.US);
     

        formatter_utc = FastDateFormat.getInstance("yyyyMMdd HH:mm:ss",tz,Locale.US);
        formatter_db  = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss",tz,Locale.US);
        formatter_sdb  = FastDateFormat.getInstance("yyyy-MM",tz,Locale.US);  
       
        mementoresourcesupportedformats.add(timeTravelMachineFormatter);

        dtsupportedformatsv.add(FastDateFormat.getInstance("E, dd MMM yyyy HH:mm:ss z",tz,Locale.US));
        dtsupportedformatsv.add(FastDateFormat.getInstance("E, dd MMM yyyy z",tz,Locale.US));
        dtsupportedformatsv.add(FastDateFormat.getInstance("E, dd MMM yyyy",tz,Locale.US));


        srvformats.add(timeTravelMachineFormatter);
        srvformats.add(FastDateFormat.getInstance("yyyyMMdd",tz,Locale.US));

      
    }

    public	MementoUtils( URI baseUri )
    {
        this.baseUri = baseUri;
    }


    public static  String  composeLink(Date date,String id,String type) {
        String str = ", <"+ baseUri.toString() +"memento/"+ timeTravelMachineFormatter.format(date)+"/"+id+">;rel=\""+type+"\"; datetime=\"" +httpformatter.format(date)+ "\"";
        return str;
    }
    public static  String  composeMemUrl(Date date,String id,String type) {
        String str = baseUri.toString() +"memento/"+ timeTravelMachineFormatter.format(date)+"/"+id;
        return str;
    }

    public String composeLinkHeader(Date memento, Date l,Date f,String id) {
        StringBuffer sb = new StringBuffer();
        String mem = composeLink(memento,id,"memento");

        String mfl = null;
        if ( (memento.equals(f)) && memento.equals(l)) {
            mfl = composeLink(memento,id,"memento first last");
        }
        else if (memento.equals(f)){

            mfl = composeLink(memento,id,"memento first");
            mfl = mfl + composeLink(l,id,"memento last");


        }
        else if (memento.equals(l)) {
            mfl = composeLink(memento,id,"memento last");
            mfl = mfl + composeLink(f,id,"memento first");

        }
        else  {

            mfl = mem ;
            mfl = mfl +composeLink(l,id,"memento last");
            mfl = mfl + composeLink(f,id,"memento first");


        }


        return mfl;
    }




    public  Date checkMementoDateValidity(String httpdate){
        //System.out.println("mementoformat");
        Date d=checkDateValidity( httpdate ,mementoresourcesupportedformats );
        return d;
    }


    public  Date checkDtDateValidity(String httpdate){
        //System.out.println("dtformat");
        Date d=checkDateValidity( httpdate ,  dtsupportedformatsv );
        return d;
    }

    public  static Date checkSrDateValidity(String httpdate){
        //System.out.println("srvformat");
        Date d = checkDateValidity( httpdate ,  srvformats );
        return d;
    }

    public static Date getTimeTravelDate(String reqDate) {
        List<FastDateFormat> dateFormats = new ArrayList<FastDateFormat>();
        TimeZone tz = TimeZone.getTimeZone("GMT");
        dateFormats.add( FastDateFormat.getInstance("yyyyMMddHHmmss",tz));
        dateFormats.add( FastDateFormat.getInstance("yyyyMMddHHmm",tz));
        dateFormats.add(FastDateFormat.getInstance("yyyyMMddHH",tz));
        dateFormats.add(FastDateFormat.getInstance("yyyyMMdd",tz));
        dateFormats.add(FastDateFormat.getInstance("yyyyMM",tz));
        dateFormats.add(FastDateFormat.getInstance("yyyy",tz));
        dateFormats.add(FastDateFormat.getInstance("E, dd MMM yyyy HH:mm:ss z",tz));

        return checkDateValidity(reqDate, dateFormats);
    }

    public  static Date checkDateValidity(String httpdate , List list) {
        System.out.println("validity check"+httpdate);
       // Date d = new Date();
        Date d = null;
        Iterator it  = list.iterator();
        int count=0;
        while (it.hasNext()) {
            FastDateFormat formatter =  (FastDateFormat) it.next();
            try {
               // TimeZone tzo = TimeZone.getTimeZone("GMT");
               // formatter.setTimeZone(tzo);
                count = count+1;
                d =  formatter.parse(httpdate);
                //System.out.println("Req Date: " +timeTravelMachineFormatter.format(d));
                break;
            }
            catch (Exception e) {
                System.out.println("attempt to parse"+ count );
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
        }
        return d;
    }

    public static Boolean isDomainBlacklisted(String requestHost, HashMap<String, String> blacklistDomains) {
    boolean b = blacklistDomains.containsKey(requestHost);
    	//return false;
        return (Boolean) b;
    }

    /**
     * create a URI that may be a URL with unicode in the hostname.  If so, first convert the hostname to A-Labels via RFC 3490. Works around an issue with constructing URIs with urls  containing unicode hostnames.
     */
    public static String validateAndEncodeUrl(String requestUrl) {

        String validUrl;
        String requestHost;
        try {
            final URL url=new URL(requestUrl);
            requestHost = url.getHost();
            final String safeHost=IDN.toASCII(requestHost);
            if (safeHost.equals(url.getHost())) {
                validUrl = new URI(requestUrl).toASCIIString();
            }
            else {
                final StringBuilder newURL = new StringBuilder();
                newURL.append(url.getProtocol());
                newURL.append("://");
                if (url.getUserInfo() != null) {
                    newURL.append(url.getUserInfo());
                    newURL.append("@");
                }
                newURL.append(safeHost);
                if (url.getPort() != -1) {
                    newURL.append(":" + url.getPort());
                }
                newURL.append(url.getFile());
                if (url.getRef() != null) {
                    newURL.append("#");
                    newURL.append(url.getRef());
                }
                validUrl = new URI(newURL.toString()).toASCIIString();
            }
        }
        catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
            return null;
        }

        String newUrl = validUrl;
        int secondHttpIndex = validUrl.lastIndexOf("http://");
        int secondHttpsIndex = validUrl.lastIndexOf("https://");
        if (secondHttpIndex > 0) {
            newUrl = newUrl.substring(0, secondHttpIndex) + newUrl.substring(secondHttpIndex + 7);
        }
        else if (secondHttpsIndex > 0) {
            newUrl = newUrl.substring(0, secondHttpsIndex) + newUrl.substring(secondHttpsIndex + 8);
        }

        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (!urlValidator.isValid(newUrl)) {
            System.out.println("Invalid URL: " + validUrl);
            return null;
        }

        /*
        if (!isDomainBlacklisted(requestHost, blacklistDomains)) {
            return validUrl;
        }
        else {
            System.out.println("This domain is blacklisted.");
            return null;
        }
        */
        return validUrl;
    }
    
    public static String RemoveProtocol (String id)
    {
        try {
            URL url = new URL(id);
            String protocol = url.getProtocol();
            String result = id.replaceFirst(protocol + ":", "");
            if (result.startsWith("//"))
            {
                result = result.substring(2);
            }
            
            return result;
        } catch (Exception e) {
            //System.out.println(e);
        }
        return id;
    }

    public static String composeErrorPage403(String url) {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>").append("<head><title>The requested URI is blacklisted</title><head>\n");
        sb.append("<body>");
        sb.append("<div align=\"center\" style=\"margin-top:20ex;margin-left:20ex;margin-right:20ex;border:1px double gray;font-weight:bold;font-family:monospace;font-size:120%;padding:2em;background-color:#eeeeee\">\n");
        sb.append("<table bgcolor=\"#eeeeee\" border=\"0\" width=\"100%\">\n");
        sb.append("<tr style=\"font-weight:bold;font-family:monospace;font-size:120%\">\n");
        sb.append("<td width=\"10%\" align=\"left\">\n");
        sb.append("<a href=\"http://www.mementoweb.org\"><img src=\"http://www.mementoweb.org/mementologo.png\" alt=\"memento logo\" width=\"100\" height=\"100\" style=\"border-style: none\"/></a>\n");
        sb.append("</td><td width=\"90%\" align=\"center\">");
        sb.append("Error, the requested URI:<br><br><a href=\"").append(url).append("\">").append(url).append("</a><br><br>is blacklisted.\n");
        sb.append("</td></tr></table>\n");
        sb.append("</div></body></html>");
        return sb.toString();
    }
    
    
}
