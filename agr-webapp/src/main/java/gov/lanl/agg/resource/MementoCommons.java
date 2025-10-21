package gov.lanl.agg.resource;

import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;

import java.net.URI;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

/*
@author Lyudmila Balakireva
*/

public class MementoCommons {
    public  static ThreadSafeSimpleDateFormat  httpformatter;
    public  static ThreadSafeSimpleDateFormat timeTravelMachineFormatter;
    public  static ThreadSafeSimpleDateFormat timeTravelJsFormatter;
    public  static ThreadSafeSimpleDateFormat timeTravelDisplayFormatter;
    public  static ThreadSafeSimpleDateFormat timeTravelDisplayDateFormatter;
    public  static ThreadSafeSimpleDateFormat timeTravelYearFormatter;
    public  static ThreadSafeSimpleDateFormat timeTravelMonthFormatter;
    public  static ThreadSafeSimpleDateFormat timeTravelDayFormatter;
    public  static ThreadSafeSimpleDateFormat formatter_utc;
    //public  static ThreadSafeSimpleDateFormat timeTravelHttpFormatter;
    //public  static ThreadSafeSimpleDateFormat timeTravelMachineFormatter;
    ThreadSafeSimpleDateFormat dtformatter;
    // private static Index idx;
    static final List mementoresourcesupportedformats = new ArrayList();
    static final List  dtsupportedformatsv = new ArrayList();
    static final public List  srvformats = new ArrayList();
    //   static List dtsupportedformats = new ArrayList();

    static  URI baseUri;
    static {

        //TimeZone tz = TimeZone.getTimeZone("UTC");
        //timeTravelHttpFormatter = new ThreadSafeSimpleDateFormat("dd MMMM YYYY kk:mm:ss");
        TimeZone tz = TimeZone.getTimeZone("GMT");
        //timeTravelMachineFormatter = new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss");
        //timeTravelMachineFormatter.setTimeZone(tz);
        httpformatter = new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
        httpformatter.setTimeZone(tz);

        // TimeTravel formats
        timeTravelMachineFormatter = new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss");
        timeTravelMachineFormatter.setTimeZone(tz);
        timeTravelJsFormatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        timeTravelJsFormatter.setTimeZone(tz);
        //timeTravelDisplayFormatter = new ThreadSafeSimpleDateFormat("dd MMMM YYYY HH:mm:ss");
        //timeTravelDisplayFormatter.setTimeZone(tz);
        timeTravelDisplayDateFormatter = new ThreadSafeSimpleDateFormat("YYYY-MM-dd");
        timeTravelDisplayDateFormatter.setTimeZone(tz);
        timeTravelDisplayFormatter = new ThreadSafeSimpleDateFormat("dd MMM yyyy HH:mm:ss z");
        timeTravelDisplayFormatter.setTimeZone(tz);
        timeTravelDayFormatter = new ThreadSafeSimpleDateFormat("dd");
        timeTravelDayFormatter.setTimeZone(tz);
        timeTravelMonthFormatter = new ThreadSafeSimpleDateFormat("MMMM");
        timeTravelMonthFormatter.setTimeZone(tz);
        timeTravelYearFormatter = new ThreadSafeSimpleDateFormat("YYYY");
        timeTravelYearFormatter.setTimeZone(tz);

        formatter_utc = new ThreadSafeSimpleDateFormat("yyyyMMdd HH:mm:ss");
        formatter_utc.setTimeZone(tz);
        ThreadSafeSimpleDateFormat dtformatter = new ThreadSafeSimpleDateFormat("E, dd MMM yyyy");
        //  idx = ArchiveConfig.getMetadataIndex();
        mementoresourcesupportedformats.add(timeTravelMachineFormatter);

        dtsupportedformatsv.add(new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z"));
        dtsupportedformatsv.add( new ThreadSafeSimpleDateFormat("E, dd MMM yyyy z"));
        dtsupportedformatsv.add( new ThreadSafeSimpleDateFormat("E, dd MMM yyyy"));


        srvformats.add(timeTravelMachineFormatter);
        srvformats.add(new ThreadSafeSimpleDateFormat("yyyyMMdd"));

        // MyServletContextListener cl= MyServletContextListener.getInstance();
        //    cl.setAttribute("idx", idx);
    }

    public	MementoCommons( URI baseUri )
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
        List<ThreadSafeSimpleDateFormat> dateFormats = new ArrayList<ThreadSafeSimpleDateFormat>();

        dateFormats.add(new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss"));
        dateFormats.add(new ThreadSafeSimpleDateFormat("yyyyMMddHHmm"));
        dateFormats.add(new ThreadSafeSimpleDateFormat("yyyyMMddHH"));
        dateFormats.add(new ThreadSafeSimpleDateFormat("yyyyMMdd"));
        dateFormats.add(new ThreadSafeSimpleDateFormat("yyyyMM"));
        dateFormats.add(new ThreadSafeSimpleDateFormat("yyyy"));
        dateFormats.add(new ThreadSafeSimpleDateFormat("E, dd MMM yyyy HH:mm:ss z"));

        return checkDateValidity(reqDate, dateFormats);
    }

    public  static Date checkDateValidity(String httpdate , List list) {
        System.out.println("validity check"+httpdate);
       // Date d = new Date();
        Date d = null;
        Iterator it  = list.iterator();
        int count=0;
        while (it.hasNext()) {
            ThreadSafeSimpleDateFormat formatter =  (ThreadSafeSimpleDateFormat) it.next();
            try {
                TimeZone tzo = TimeZone.getTimeZone("GMT");
                formatter.setTimeZone(tzo);
                count = count+1;
                d = formatter.parse(httpdate);
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


}
