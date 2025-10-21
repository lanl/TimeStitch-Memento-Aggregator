package gov.lanl.agg.cache.mysql;

import java.text.ParseException;
import java.util.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.jolbox.bonecp.BoneCP;

import gov.lanl.agg.ArchiveDescription;
import gov.lanl.agg.BatchMap;
import gov.lanl.agg.Link;
import gov.lanl.agg.LinkHeader;
import gov.lanl.agg.TimeMapLinkDesc;
import gov.lanl.agg.TimeTravel;
import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.utils.MementoUtils;


public class StorageImpl implements CacheStorage{
    //private Connection persistentConnection;
    //String connectionstr = null;
    //String user=null;
    // String pass=null;
    BoneCP cPool= null;
    String livesync = "false";

/*    String Get_Timegate_info = "select mdate,href,type,rel from links where id = md5(?) " +
            " and mdate=(select max(mdate) from links where id= md5(?) and mdate <= ?)"+
    		" union all select mdate,href,type,rel from links where id = md5(?) " +
    		" and mdate=(select min(mdate) from links where id= md5(?) and mdate > ?)"+
            " union all select mdate,href,type,rel from links where id =md5(?) " +
            " and mdate =(select min(mdate) from links where id = md5(?)) " +
            " union all select mdate,href,type,rel from links where id =md5(?) " +
            "and mdate =(select max(mdate) from links where id = md5(?)); "; */


    //String getNostalgicSql = "select mdate, href from links cross join (select count(id) as cnt from links) const where rand() <= 100/cnt order by rand() limit 1;";
    //String getNostalgicSql = "select linkmaster.url from linkmaster where linkmaster.id in (select linkmaster.id from linkmaster union select distinct(links.id) from links) order by rand() limit 1;";

    String getNostalgicRandomURL = "select url, id from linkmaster order by rand(now()) limit 1;";
    String checkRandUrlForMemento = "select count(id) from links where id=?";

    String Check_cache ="select updtime, numreq,status from linkmaster where id = md5(?)  ;";

    // String Timemap_info1 =	"select mdate,href,type,rel from links where id = md5(?) " +
    //        "  order by mdate  limit ?,?;";

    //String Timemap_info =	"select mdate,href,type,rel from links where id = md5(?) " +
    //        " and mdate> ? order by mdate limit ? , ? ;";

    public StorageImpl(BoneCP cPool) {
        this.cPool = cPool;

    }

    public void setApplicationMode(String livesync){
        this.livesync = livesync;

    }

    public String getApplicationMode(){
        return livesync;

    }
    public String formatDate(String date) {
        date = date.replace('T', ' ');
        date = date.replace('Z', ' ');
        date = date.trim();
        return date;
    }

    public Map<String, String> getNostalgicInfo() {
        Map<String, String> memento = new HashMap<>();

        java.text.SimpleDateFormat  lformatter_utc = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        try (Connection conn = cPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getNostalgicRandomURL)
        ) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                //Date mdate = rs.getTimestamp(1);
                String href = rs.getString(1);
                String id = rs.getString(2);
                //memento.put("date", lformatter_utc.format(mdate));
                //System.out.println("got rand url: " + href);

                try (Connection connection = cPool.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement(checkRandUrlForMemento)
                ) {
                    preparedStatement.setString(1, id);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        //System.out.println("Confirmed rand url");
                        memento.put("url", href);
                    }
                    else {
                        System.out.println("Rand URL not found: " + href);
                        getNostalgicInfo();
                    }
                } catch (SQLException ignore) {}
            }
        } catch (SQLException ignore) {}
        return memento;
    }

    public TimeTravel getInfo(String url, Date reqtime,List <String> names,boolean refresh) {
    	return getInfo( url,  reqtime, names,refresh,null);
    }
    public TimeTravel getInfo(String url, Date reqtime,List <String> names,boolean refresh,Date reqdate) {
    	url = MementoUtils.RemoveProtocol (url);
        System.out.println("TimeTravelInfo from Cache");
        java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone tz = TimeZone.getTimeZone("GMT");
        formatter_utc.setTimeZone(tz);
        java.text.SimpleDateFormat  lformatter_utc = new java.text.SimpleDateFormat("yyyyMMdd HH:mm:ss");
        java.text.SimpleDateFormat  testformatter_utc = new java.text.SimpleDateFormat("yyyyMMdd HH:mm:ss Z");
        testformatter_utc.setTimeZone(tz);
        //TimeZone tz = TimeZone.getTimeZone("GMT");
        //  lformatter_utc.setTimeZone(tz);
        String mysqlreqdate = formatter_utc.format(reqtime);
        //System.out.println("mysqltime "+mysqlreqdate);
        //check that thing in cache
        Statement s = null;
        // PreparedStatement st = null;
        StringBuilder sb = new StringBuilder();
        Iterator<String> itn = names.iterator();
        int sz = names.size();
        int count=0;

        Map<String, List<Map<String, String>>> dist = new HashMap<>();
        List<Map<String, String>> summaryDist = new ArrayList<>();
        dist.put("summary", summaryDist);

        while (itn.hasNext()) {
            String name = itn.next();
            count =count + 1;
            if (count<sz) {
                sb.append("'"+name+"',");
            }
            else {
                sb.append("'"+name+"'");
            }
            dist.put(name, new ArrayList<Map<String, String>>());

        }
        System.out.println("archivelist:"+sb.toString());
        String sqla = "select hostname,mdate,href from links a,archive_register b"
                + " where a.id=md5(?) and a.archive_id=b.id and b.hostname in ("+sb.toString()+") order by archive_id, mdate";
        String sqlb = "select YEAR(mdate), MONTH(mdate),  count(*) from links a, archive_register b where a.id=md5(?) and a.archive_id=b.id and b.hostname in ("+sb.toString()+") group by  YEAR(mdate),MONTH(mdate); ";
        //summary
        String sqlc ="select * from ( "+
                " select distinct mdate mindate, href from links a,  archive_register b where a.id=md5(?) and mdate <=? and a.archive_id=b.id and b.hostname in ("+ sb.toString() +") order by mdate desc limit 3 ) dummy1 "+
                " union all "+
                " select * from ( " +
                " select distinct mdate mindate, href from links a, archive_register b where a.id=md5(?) and mdate > ? and a.archive_id=b.id and b.hostname in ("+sb.toString()+") order by mdate asc limit 3) dummy2 "+
                " order by mindate ;";

        String sqlDistributionByArchive = "SELECT YEAR(mdate), MONTH(mdate), count(*), ar.hostname " +
                "FROM links l, archive_register ar " +
                "WHERE l.id=md5(?) " +
                "AND l.archive_id=ar.id " +
                "AND ar.hostname IN (" + sb.toString() + ") " +
                "GROUP BY YEAR(mdate), MONTH(mdate), ar.hostname";

        Map map = new HashMap();
        TimeTravel tr = new TimeTravel();

        // Connection conn = null;
        //ResultSet rs0 =null;
        //LinkHeader lh = new LinkHeader();
        //List links;
        if (checkCache(url, reqtime,refresh,reqdate)) {

            System.out.println("in cache");
            try (  Connection conn = cPool.getConnection();
                   PreparedStatement st = conn.prepareStatement(sqla);
                   PreparedStatement stb = conn.prepareStatement(sqlb);
                   PreparedStatement prepDistributionByArchive = conn.prepareStatement(sqlDistributionByArchive);
                   PreparedStatement stc = conn.prepareStatement(sqlc);
            ){

                st.setString(1,url);
                stb.setString(1,url);
                prepDistributionByArchive.setString(1, url);
                stc.setString(1,url);
                stc.setString(2,mysqlreqdate);
                stc.setString(3,url);
                stc.setString(4,mysqlreqdate);

                NavigableMap<Long, Link> m = null;
                //List<Map<String, String>> dist = new ArrayList<>();

                String prev_hostname="";
                ResultSet rs0 = st.executeQuery();
                while (rs0.next()) {

                    String mdate = null;
                    String hostname = rs0.getString(1);
                    //System.out.println("hostname: "+hostname);
                    if (!hostname.equals(prev_hostname)) {
                        if (m!=null) {
                            map.put(prev_hostname,m);
                        }
                        m = new TreeMap<Long, Link>();
                    }
                    Date _date = rs0.getTimestamp(2);
                    //System.out.println("date="+_date);
                    String href = rs0.getString(3);
                    // System.out.println("href="+href);

                    Link link = new Link();

                    // need to format date
                    if (_date!=null) {
                        mdate = lformatter_utc.format(_date);
                        //System.out.println("mdate:"+mdate +href );
                        //System.out.println("_date="+_date);
                        link.setDatetime(mdate);
                    }
                    link.setHref(href);
                    //links.add(link);
                    //needto check if date is GMT one
                    mdate = mdate.trim();
                    // mdate = mdate.replace(" ", "T");
                    mdate = mdate+" GMT";

                    // System.out.println("mdate"+mdate);
                    java.util.Date wdate = testformatter_utc.parse(mdate);
                    //System.out.println("wdate"+wdate);
                    m.put(wdate.getTime(),link);
                    prev_hostname = hostname;

                }
                //last archive

                map.put(prev_hostname,m);

                rs0.close();

                // distribution full collection summary
                ResultSet rs = stb.executeQuery();
                while (rs.next()) {
                    String year = rs.getString(1);
                    String month = rs.getString(2);
                    Integer tcount = rs.getInt(3);

                    Map<String, String> dd = new HashMap<>();
                    if (Integer.parseInt(month) < 10) {
                        month = "0" + month;
                    }

                    dd.put("date", year + "-" + month + "-14");
                    dd.put("total", tcount.toString());
                    dist.get("summary").add(dd);
                }
                // distribution per archive
                ResultSet resultDistributionByArchive = prepDistributionByArchive.executeQuery();
                while(resultDistributionByArchive.next()) {
                    String year = resultDistributionByArchive.getString(1);
                    String month = resultDistributionByArchive.getString(2);
                    Integer tcount = resultDistributionByArchive.getInt(3);
                    String archiveId = resultDistributionByArchive.getString(4);
                    Map<String, String> dd = new HashMap<>();
                    if (Integer.parseInt(month) < 10) {
                        month = "0" + month;
                    }
                    dd.put("date", year + "-" + month + "-14");
                    dd.put("total", tcount.toString());

                    dist.get(archiveId).add(dd);
                }
                rs.close();
                resultDistributionByArchive.close();

                //summary
                ResultSet rsc = stc.executeQuery();
                m = new TreeMap<Long, Link>();
                String udate = null;
                while (rsc.next()) {
                    Link link = new Link();
                    Date _date = rsc.getTimestamp(1);
                    String memUrl = rsc.getString(2);
                    if (_date!=null) {
                        udate = lformatter_utc.format(_date);
                        link.setDatetime(udate);
                        link.setHref(memUrl);
                    }
                    udate = udate+" GMT";
                    java.util.Date sdate = testformatter_utc.parse(udate);
                    m.put(sdate.getTime(),link);
                }
                rs.close();

                map.put("summary", m);
                tr.setDistribution(dist);
                tr.setMementos(map);

                return tr;

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }



        }
        return tr;

    }

    public Map<String, List<Link>> getDuplicateMementos(String url, Date firstMemento, Date prevMemento,
                                                        Date closestMemento, Date nextMemento, Date lastMemento) {
    	url = MementoUtils.RemoveProtocol (url);

        if (closestMemento == null) {
            return null;
        }

        Map<String, List<Link>> duplicates = new HashMap<>();
        duplicates.put("memento", new ArrayList<Link>());
        duplicates.put("first", new ArrayList<Link>());
        duplicates.put("prev", new ArrayList<Link>());
        duplicates.put("next", new ArrayList<Link>());
        duplicates.put("last", new ArrayList<Link>());

        String sql = "select mdate, href from links where id = md5(?)" +
                " and (mdate=(?)";

        if (firstMemento != null) {
            sql += " or mdate=(?)";
        }
        if (prevMemento != null) {
            sql += " or mdate=(?)";
        }
        if (nextMemento != null) {
            sql += " or mdate=(?)";
        }
        if (lastMemento != null) {
            sql += " or mdate=(?)";
        }
        sql += ");";

        java.text.SimpleDateFormat output_formatter_default = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        java.text.SimpleDateFormat output_formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        java.text.SimpleDateFormat formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone tz = TimeZone.getTimeZone("GMT");
        formatter_utc.setTimeZone(tz);
        output_formatter_utc.setTimeZone(tz);

        try ( Connection conn = cPool.getConnection();
              PreparedStatement  st = conn.prepareStatement(sql)
        ) {
            st.setString(1, url);
            st.setString(2, formatter_utc.format(closestMemento));
            int index = 2;
            if (firstMemento != null) {
                st.setString(++index, formatter_utc.format(firstMemento));
            }
            if (prevMemento != null) {
                st.setString(++index, formatter_utc.format(prevMemento));
            }
            if (nextMemento != null) {
                st.setString(++index, formatter_utc.format(nextMemento));
            }
            if (lastMemento != null) {
                st.setString(++index, formatter_utc.format(lastMemento));
            }
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Link link = new Link();
                Date memDate = rs.getTimestamp(1);
                String memUrl = rs.getString(2);
                if (memDate != null) {
                    link.setDatetime(output_formatter_default.format(memDate));
                    link.setHref(memUrl);
                    if (link.getDatetime().equals(output_formatter_utc.format(closestMemento))) {
                        duplicates.get("memento").add(link);
                    }
                    else if (firstMemento != null && link.getDatetime().equals(output_formatter_utc.format(firstMemento))) {
                        duplicates.get("first").add(link);
                    }
                    else if (prevMemento != null && link.getDatetime().equals(output_formatter_utc.format(prevMemento))) {
                        duplicates.get("prev").add(link);
                    }
                    else if (nextMemento != null && link.getDatetime().equals(output_formatter_utc.format(nextMemento))) {
                        duplicates.get("next").add(link);
                    }
                    else if (lastMemento != null && link.getDatetime().equals(output_formatter_utc.format(lastMemento))) {
                        duplicates.get("last").add(link);
                    }
                }
            }
            rs.close();
            return duplicates;
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return duplicates;
    }



    public BatchMap getBatchInfo(String url) {
    	url = MementoUtils.RemoveProtocol (url);
        Statement s = null;
        // PreparedStatement st = null;
        TimeZone tz = TimeZone.getTimeZone("GMT");
        java.text.SimpleDateFormat  lformatter_utc = new java.text.SimpleDateFormat("yyyyMMdd HH:mm:ss");

        Map<String, List<Map<String, String>>> dist = new HashMap<>();
        List<Map<String, String>> summaryDist = new ArrayList<>();


        String sqla = "select hostname,mdate,href from links a,archive_register b"
                + " where a.id=md5(?) and a.archive_id=b.id  order by archive_id, mdate";
        String sqlb = "select reqtime, updtime, status from linkmaster where id=md5(?);";


        BatchMap bmap = new BatchMap();
        bmap.setOriginalUrl(url);
        List links = null;

        //  if (checkCacheRelax(url)) {

        // System.out.println("in get batch info");
        try (  Connection conn = cPool.getConnection();
               PreparedStatement sta = conn.prepareStatement(sqla);
               PreparedStatement stb = conn.prepareStatement(sqlb);

        ){
            sta.setString(1,url);
            stb.setString(1,url);

            ResultSet rs = stb.executeQuery();
            if (rs.next()) {
                Date _date1 = rs.getTimestamp(1);
                if (_date1!=null) {
                    bmap.setRequestDatetime(_date1);
                }
                Date _date2 = rs.getTimestamp(2);
                if (_date2!=null) {
                    bmap.setUpdateDatetime(_date2);
                }

                rs.close();

                String prev_hostname="";
                ResultSet rs0 = sta.executeQuery();
                while (rs0.next()) {
                    String mdate = null;
                    String hostname = rs0.getString(1);
                    // System.out.println("hostname"+hostname);
                    if (!hostname.equals(prev_hostname)) {
                        // System.out.println("hostname"+hostname);

                        if (links!=null) {
                            // System.out.println("prev_hostname"+prev_hostname);
                            bmap.addMementos(prev_hostname,links);
                        }
                        links = new ArrayList<Link>();
                    }
                    Date _date = rs0.getTimestamp(2);
                    //System.out.println("date="+_date);
                    String href = rs0.getString(3);
                    //System.out.println("href="+href);

                    Link link = new Link();
                    if (_date!=null) {
                        mdate = lformatter_utc.format(_date);
                        //System.out.println("mdate:"+mdate +href );
                        //System.out.println("_date="+_date);
                        link.setDatetime(mdate);
                    }
                    link.setHref(href);
                    links.add(link);
                    prev_hostname = hostname;
                }
                //last archive
                if( !prev_hostname.equals("")){
                    bmap.addMementos(prev_hostname,links);
                }
                rs0.close();
            }




        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



        //  }
        return bmap;

    }


   public LinkHeader getMementos(String url, Date starttime,Date endtime,List <String> names) {
		url = MementoUtils.RemoveProtocol (url);
		 java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	     TimeZone tz = TimeZone.getTimeZone("GMT");
	     formatter_utc.setTimeZone(tz);
	     String mysqlstartdate = formatter_utc.format(starttime);
	     String mysqlenddate = formatter_utc.format(endtime);
	     Statement s = null;

	        StringBuilder sb = new StringBuilder();
	        Iterator<String> itn = names.iterator();
	        int sz = names.size();
	        int count = 0;
	        while (itn.hasNext()) {
	            String name = itn.next();
	            count =count + 1;
	            if (count<sz) {
	                sb.append("'"+name+"',");
	            }
	            else {
	                sb.append("'"+name+"'");
	            }
	        }
	        
	        String sql = "select mdate,href,type,rel from links a,archive_register b where a.id = md5(?) and a.archive_id=b.id and b.hostname in ("+sb.toString()+")" +
	                " and mdate=(select max(mdate) from links a,archive_register b where a.id= md5(?) and "
	                + "  (mdate BETWEEN ? AND ? )   and a.archive_id=b.id and b.hostname in ("+sb.toString()+"))";
	        
	                
	        
	     
	return null;   
   }
    public LinkHeader getTimegateInfo(String url, Date reqtime,List <String> names,boolean refresh) {
    	return  getTimegateInfo(url, reqtime, names, refresh,null);
    }

    public LinkHeader getTimegateInfo(String url, Date reqtime,List <String> names,boolean refresh,Date service_date) {
    	url = MementoUtils.RemoveProtocol (url);
        System.out.println("TimegateInfo from Cache");
        java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone tz = TimeZone.getTimeZone("GMT");
        formatter_utc.setTimeZone(tz);
        java.text.SimpleDateFormat  lformatter_utc = new java.text.SimpleDateFormat("yyyyMMdd HH:mm:ss:0");
        //TimeZone tz = TimeZone.getTimeZone("GMT");
        //  lformatter_utc.setTimeZone(tz);
        String mysqlreqdate = formatter_utc.format(reqtime);
        //System.out.println("mysqltime"+mysqlreqdate);
        //check that thing in cache
        Statement s = null;

        StringBuilder sb = new StringBuilder();
        Iterator<String> itn = names.iterator();
        int sz = names.size();
        int count = 0;
        while (itn.hasNext()) {
            String name = itn.next();
            count =count + 1;
            if (count<sz) {
                sb.append("'"+name+"',");
            }
            else {
                sb.append("'"+name+"'");
            }


        }
        String Get_Timegate_info = "select mdate,href,type,rel from links a,archive_register b where a.id = md5(?) and a.archive_id=b.id and b.hostname in ("+sb.toString()+")" +
                " and mdate=(select max(mdate) from links a,archive_register b where a.id= md5(?) and mdate <= ? and a.archive_id=b.id and b.hostname in ("+sb.toString()+"))"+
                " union all select mdate,href,type,rel from links a, archive_register b where a.id = md5(?) " +
                " and a.archive_id=b.id and b.hostname in ("+sb.toString()+")"  +
                " and mdate=(select min(mdate) from links a, archive_register b where a.id= md5(?) and mdate > ? and a.archive_id=b.id and b.hostname in ("+sb.toString()+") )"+
                " union all select mdate,href,type,rel from links a, archive_register b where a.id =md5(?) " +
                " and a.archive_id=b.id and b.hostname in ("+sb.toString()+")"  +
                " and mdate =(select min(mdate) from links a, archive_register b where a.id = md5(?) and a.archive_id=b.id and b.hostname in ("+sb.toString()+")) " +
                " union all select mdate,href,type,rel from links a,  archive_register b where a.id =md5(?) " +
                " and a.archive_id=b.id and b.hostname in ("+sb.toString()+")"  +
                " and mdate =(select max(mdate) from links a, archive_register b where a.id = md5(?) and a.archive_id=b.id and b.hostname in ("+sb.toString()+")); ";
        // System.out.println(Get_Timegate_info);
        LinkHeader lh = new LinkHeader();
        if (checkCache(url, reqtime,refresh,service_date)) {

            System.out.println("in cache");
            try (  Connection conn = cPool.getConnection();
                   PreparedStatement st = conn.prepareStatement(Get_Timegate_info);
            ){
                //conn = startConnection();
                //conn = cPool.getConnection();
                // s = conn.createStatement();
                //st = conn.prepareStatement(sqlp);
                st.setString(1,url);
                st.setString(2,url);
                st.setString(3,mysqlreqdate);
                st.setString(4,url);
                st.setString(5,url);
                st.setString(6, mysqlreqdate);
                st.setString(7,url);
                st.setString(8,url);
                st.setString(9,url);
                st.setString(10,url);


                ResultSet rs0 = st.executeQuery();
                while (rs0.next()) {
                    String mdate = null;
                    Date _date = rs0.getTimestamp(1);
                    // System.out.println("date="+_date);
                    String href = rs0.getString(2);
                    //System.out.println("href="+href);
                    String type = rs0.getString(3);
                    String rel = rs0.getString(4);
                    Link link = new Link();
                    // need to format date
                    if (_date!=null) {
                        mdate = lformatter_utc.format(_date);
                        // System.out.println("mdate:"+mdate);
                        link.setDatetime(mdate);
                    }
                    link.setHref(href);
                    if (type!=null) {
                        link.setType(type);
                    }
                    link.setRelationship(rel);
                    // lh.addOderedLink(link);
                    //lh.addLink(link);
                    // why I commented out ordered link?
                    //  System.out.println("from looop:"+link.getHref());
                    lh = lh.addOderedLink(link);
                }
                rs0.close();
                // System.out.println("size from storage implementation:"+lh.getOrderedLinksByDate().size());
                return lh;

            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }



        }
        return null;

    }



    public boolean  checkCache(String url, Date memdate, boolean force_refresh){
    	return   checkCache( url,  memdate, force_refresh,null );
    }

    public boolean  checkCache(String url, Date memdate, boolean force_refresh,Date service_date ) {
    	url = MementoUtils.RemoveProtocol (url);
        //reqdate means memento date time
    	 TimeZone tz = TimeZone.getTimeZone("GMT");
    	 Date reqtime ;
    			if (service_date==null) {
    			reqtime = new Date();
    			}
    			else {
    			reqtime = service_date;	
    			}
    			
        java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
       
    			
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());       
        cal.add(Calendar.DAY_OF_MONTH,-30);
        Date cuttof = cal.getTime();

        formatter_utc.setTimeZone(tz);
        boolean check = false;
        boolean refresh = false;

        boolean refreshalways = true;
        if (livesync.equals("true")){ refreshalways = false;}
        String rdate =  formatter_utc.format(reqtime);
        Statement s = null;
        //Connection conn = null;
        ResultSet rs=null;
        // PreparedStatement st = null;
        PreparedStatement st0 = null;
        PreparedStatement st00 = null;
        PreparedStatement st1 = null;
        PreparedStatement st2 = null;
        try (  Connection conn = cPool.getConnection();
               PreparedStatement  st = conn.prepareStatement(Check_cache);
        ) {

            int count=0;
            st.setString(1,url);
            // rs = s.executeQuery(sql0);
            rs = st.executeQuery();
            String sqlu1="";
            if (rs.next()) {
                Date d_update = rs.getTimestamp(1);
                String status = rs.getString(3);
                if (rs.wasNull()) {
                    //System.out.println("was NULL");
                    status = "";
                }

                count = rs.getInt(2)+1;
                
                if (status.equals('F')||status.equals('N')){
                    //status 'F' no records due to network problem
                	//status 'N' no records 
                    check = false;
                    refresh = true;
                    sqlu1 = "update linkmaster set reqtime=?,numreq=?  where id=md5(?);";
                    st00 = conn.prepareStatement(sqlu1);
                    st00.setString(1,rdate);
                    st00.setInt(2,count);
                    st00.setString(3,url);
                    st00.executeUpdate();
                    
                    force_refresh = true;
                } else {
                    if (d_update!=null) {
                        // System.out.println("memdate"+ memdate);
                        //System.out.println("d_update" +d_update);
                        //add grace period 10 min

                       // aug 19 change 
                    	//Calendar cal = Calendar.getInstance();
                        //cal.setTime(d_update);
                        //cal.add(Calendar.MINUTE, 10);
                        
                        //Date d_updatea = cal.getTime();

                         if (d_update.after(cuttof)){
                       // if ( memdate.before(d_updatea)) {
                    	    
                            check = true;

                            sqlu1 = "update linkmaster set reqtime=?,numreq=? where id=md5(?);";
                            st0 = conn.prepareStatement(sqlu1);
                            st0.setString(1,rdate);
                            st0.setInt(2,count);
                            st0.setString(3,url);
                            st0.executeUpdate();
                            //jan 12 2015 added refresh = true; rely on job processing itself to discard.
                            //refresh = true;
                        }
                        else {
                            //set updatetime to  old magic date? to trigger refresh as new
                            check = true;
                            refresh = true;
                            //  sqlu1 = "update linkmaster set reqtime=?,numreq=?, updtime = '1996-12-31'  where id=md5(?);";
                            sqlu1 = "update linkmaster set reqtime=?,numreq=?  where id=md5(?);";
                            st00 = conn.prepareStatement(sqlu1);
                            st00.setString(1,rdate);
                            st00.setInt(2,count);
                            st00.setString(3,url);
                            st00.executeUpdate();
                            force_refresh = true;
                        }
                    }//d_update
                    //sqlu1 = "update linkmaster set reqtime='" + rdate + "',numreq="+ count+" where id=md5('"+url+"');";
                    //Sept 18 changed logic to serve from cache
                    //sqlu1 = "update linkmaster set reqtime=?,numreq=? where id=md5(?);";

                    //  st0 = conn.prepareStatement(sqlu1);
                    // st0.setString(1,rdate);
                    // st0.setInt(2,count);
                    // st0.setString(3,url);
                    // st0.executeUpdate();
                }}
            else {
                //  sqlu1 =  "insert into linkmaster (url,id,reqtime,numreq) values  ('"+url+"',md5('"+url+"'),'"+rdate+"',1) ;" ;
            	//no records
                sqlu1 =  "insert into linkmaster (url,id,reqtime,numreq) values  (?,md5(?),?,1) ;" ;

                st1 = conn.prepareStatement(sqlu1);
                st1.setString(1,url);
                st1.setString(2,url);
                st1.setString(3,rdate);
                st1.executeUpdate();
                check= false;
                refresh = true;
                force_refresh = true;
            }

            count=count+1;

            // System.out.println("storage impl:"+sqlu1);
            //s.executeUpdate(sqlu1);
            //st1.executeUpdate();
            //if (refresh) {
            if (refreshalways){
            	if (force_refresh){
                // String  sqli1 = "insert into  jobs (url,reqtime) values ('"+url+"','"+rdate+"');";
                // System.out.println("add to jobs");
                String  sqli1 = "insert ignore into  jobs (url,reqtime,hashkey) values (?,?,md5(?));";// ON DUPLICATE KEY UPDATE  priority = NULL;";
                //System.out.println("storage impl:"+sqli1);
                st2 = conn.prepareStatement(sqli1);
                st2.setString(1,url);
                st2.setString(2,rdate);
                st2.setString(3,url);
                // s.executeUpdate(sqli1);
                st2.executeUpdate();
            	}
            }


        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (st1 != null)
                try {
                    st1.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            if (st2 != null)
                try {
                    st2.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            if (st0 != null)
                try {
                    st0.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }





        }



        return check;
    }

    public boolean  checkCacheRelax(String url) {
    	url = MementoUtils.RemoveProtocol (url);
        // we are not checking cache freshness here, just presence of url
        //reqdate means memento date time
        Date reqtime = new Date();
        java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean check = false;
        //change it to false dec 20
        boolean refresh = false;
        if (livesync.equals("true")){
            refresh = false;}
        // Statement s = null;
        // Connection conn = null;
        ResultSet rs=null;
        // PreparedStatement st = null;
        PreparedStatement st0 = null;
        PreparedStatement st1 = null;
        PreparedStatement st2 = null;
        // String sql0 ="select  numreq, updtime from linkmaster where id = md5(?);";

        String rdate =  formatter_utc.format(reqtime);
        try (  Connection conn = cPool.getConnection();
               PreparedStatement  st = conn.prepareStatement(Check_cache);
        ){
            int count=0;
            st.setString(1,url);
            rs = st.executeQuery();

            String sqlu1="";
            if (rs.next()) {
                //count = rs.getInt(1)+1;
                Date d_update = rs.getTimestamp(1);
                if (d_update!=null) {
                    check = true;
                }
                count = rs.getInt(2)+1;
                //sqlu1 = "update linkmaster set reqtime='" + rdate + "',numreq="+ count+" where id = md5('"+url+"');";
                sqlu1 = "update linkmaster set reqtime=?,numreq=? where id = md5(?);";
                st0 = conn.prepareStatement(sqlu1);
                st0.setString(1,rdate);
                st0.setInt(2,count);
                st0.setString(3,url);
                st0.executeUpdate();

            }
            else {
                //  sqlu1 = "insert into linkmaster (url,id,reqtime,numreq) values  ('"+url+"',md5('"+url+"'),'"+rdate+"',1) ;" ;
                sqlu1 = "insert into linkmaster (url,id,reqtime,numreq) values  (?,md5(?),?,1) ;" ;
                st1 = conn.prepareStatement(sqlu1);
                st1.setString(1,url);
                st1.setString(2,url);
                st1.setString(3,rdate);
                st1.executeUpdate();
                check = false;

            }

            // if (!check) {
            if (refresh){
                //String  sqli1 = "insert into  jobs (url,reqtime) values ('"+url+"','"+rdate+"');";
                //  System.out.println("add to jobs:" + url);
                String  sqli1 = "insert ignore into  jobs (url,reqtime,hashkey) values (?,?,md5(?));" ;
                // ON DUPLICATE KEY UPDATE  priority = NULL;";

                st2 = conn.prepareStatement(sqli1);
                st2.setString(1,url);
                st2.setString(2,rdate);
                st2.setString(3,url);
                st2.executeUpdate();
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (st1 != null)
                try {
                    st1.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            if (st2 != null)
                try {
                    st2.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            if (st0 != null)
                try {
                    st0.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }



        }
        return check;
    }

    //public List <ArchiveDescription> getArchiveInfo(){
    // Connection conn = null;
    // ResultSet rs=null;
    //PreparedStatement st = null;
    //String sql = "select hostname,timegate,timemap from archive_register "

    //}
    public int getMatchingCount(String url) {
    	url = MementoUtils.RemoveProtocol (url);

        int count = 0;
        // Statement s = null;
        //Connection conn = null;
        //  ResultSet rs=null;
        // PreparedStatement st = null;
        //String sql =	"select count(*)  from links where id = md5('"+url+"');" ;
        String sql =	"select count(*)  from links where id = md5(?);" ;
        // System.out.println("sql"+sql);
        try ( Connection conn = cPool.getConnection();
              PreparedStatement  st = conn.prepareStatement(sql);
        ) {
            //conn = startConnection();
            // conn = cPool.getConnection();
            //st = conn.prepareStatement(sql);
            st.setString(1,url);
            // s = conn.createStatement();
            //rs = s.executeQuery(sql);
            ResultSet  rs = st.executeQuery();
            while (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            return count;
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



        return  count;
    }

    @Override

    public   Map getTimeMapIndexInfo(String url, int limit,List <String> names) {
    	url = MementoUtils.RemoveProtocol (url);
    	 StringBuilder sb = new StringBuilder();
    	 Map tm = new LinkedHashMap(); 
         Iterator<String> itn = names.iterator();
         int sz = names.size();
         int count = 0;
         while (itn.hasNext()) {
         	String name = itn.next();
         	count =count + 1;
         	if (count<sz) {
         	sb.append("'"+name+"',");
         	}
         	else {
         	sb.append("'"+name+"'");	
         	}
         }
    	String sql = "SELECT *  FROM  ( SELECT @row := @row +1 AS rownum, cast(mdate as CHAR) from links a ,"+
       "  archive_register b, (select @row :=0) r  where a.id=md5(?)" +
       " and a.archive_id=b.id and b.hostname in ("+sb.toString()+")" +
        " order by mdate) as DT where  MOD(rownum,"+limit+")=0 ;";
    	String sql2 = "SELECT * FROM  ( SELECT @row := @row +1 AS rownum, cast(mdate as CHAR) from links a, archive_register b," +
    			 " (select @row :=-1) r  where a.id=md5(?)" +
    			 " and a.archive_id=b.id and b.hostname in ("+sb.toString()+")" +
    			 " order by mdate) as DT where  MOD(rownum,"+limit+")=0 ;";
    	String sql1 = "";
    	ResultSet rs = null;
    	
    	  try (  Connection conn = cPool.getConnection();
                  PreparedStatement  st = conn.prepareStatement(sql);
                  PreparedStatement st2 = conn.prepareStatement(sql2);
           ) {
    		    
    		     if (!checkCacheRelax(url)) {
    	            //or null?
    	            System.out.println("not in cache relax");
    	            return tm;
    	        }
    		    
    		     st2.setString(1,url);
    		    // st2.setInt(2, limit);
    		     rs = st2.executeQuery();
    		     int rcount=0;
    		     java.util.Calendar cal = Calendar.getInstance(); 
		    	 cal.setTimeZone(TimeZone.getTimeZone("GMT")); 

    		     while (rs.next()) {
    		    	 Integer rowcount = rs.getInt(1);    		    	
    		    	 //java.util.Date _date = rs.getTimestamp(2,cal);
    		    	 String  _datestr=rs.getString(2)+" GMT";
    		    	// System.out.println("second qstr:"+rowcount+","+_datestr);
    		    	 //System.out.println("first q"+rowcount+","+_datestr);
    		    	 Date _date= MementoUtils.formatter_db.parse(_datestr);
    		    	 TimeMapLinkDesc ld = new  TimeMapLinkDesc();
    		    	 ld.setFromdate(_date);
    		    	// ld.setTotal(limit);
    		    	 rcount = rowcount;
    		    	      //  if (rowcount==0) rowcount=1;
    		    	 
    		    	 tm.put(String.valueOf(rowcount), ld);
    		    	 }
    		      rs.close();
    		      
    		      st.setString(1,url);
    		     // st.setInt(2, limit);
    		      rs = st.executeQuery();
    		      
    		      while (rs.next()) {
     		    	  Integer rowcount = rs.getInt(1);
     		    	  //java.util.Date _date = rs.getTimestamp(2,cal);
     		    	  String  _datestr=rs.getString(2)+" GMT";
     		    	//  System.out.println("second qstr:"+rowcount+","+_datestr);
     		    	  Date _date= MementoUtils.formatter_db.parse(_datestr);
     		    	 // System.out.println("second q:"+rowcount+","+_date);
     		    	  int key = rowcount-limit;
     		    	  if (tm.containsKey(String.valueOf(key))) {
     		    	  TimeMapLinkDesc ld = (TimeMapLinkDesc) tm.get(String.valueOf(key));
     		    	  //if (ld!=null) {
     		    	  ld.setUntildate(_date);
     		    	 // System.out.println(ld.getFromdate()+","+ld.getUntildate());
     		    	  tm.put(String.valueOf(key), ld);
     		    	  
     		    	  }
     		    	  //}
    		      }
    		     
    		    rs.close();
    		     
    	  }
    	  catch (SQLException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          } catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	  finally {
                   if (rs != null) {
                       try {
                          rs.close();
                        } catch (SQLException e) {
                      // TODO Auto-generated catch block
                      e.printStackTrace();
                       }
                   }
    	  }
    	return tm;
    }
    public LinkHeader getTimeMapInfo(String url, Date start, Date end, int istart,int limit,List <String> names) {
    	url = MementoUtils.RemoveProtocol (url);
        //direction?
        // start from date ?
        //select SQL_CALC_FOUND_ROWS * FROM ( select href from links where archive_id=3 ) res, (select found_rows() as total) tot limit 1,10;
        //String sql;
        //Statement s = null;
        // Connection conn = null;
        //PreparedStatement st = null;
        // PreparedStatement st1 = null;
    	java.text.SimpleDateFormat  formatter_utc_ = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	     TimeZone tz = TimeZone.getTimeZone("GMT");
	     formatter_utc_.setTimeZone(tz);
	     String mysqlstartdate = null;
	     String  mysqlenddate = null;
	     if (start!=null) {
	      mysqlstartdate = formatter_utc_.format(start);
	     }
	     if (end!=null){
	      mysqlenddate = formatter_utc_.format(end);

	     }
    	
        ResultSet rs=null;
        LinkHeader lh = new LinkHeader();
        String mysqlreqdate = null;
        Date reqtime = new Date();
        StringBuilder sb = new StringBuilder();
        Iterator<String> itn = names.iterator();
        int sz = names.size();
        int count = 0;
        while (itn.hasNext()) {
            String name = itn.next();
            count =count + 1;
            if (count<sz) {
                sb.append("'"+name+"',");
            }
            else {
                sb.append("'"+name+"'");
            }
        }


        if (!checkCacheRelax(url)) {
            //or null?
            // System.out.println("not in cache relax");
            return null;
        } else {
            String sql1 =	"select mdate,href,type,rel from   links a,  archive_register b where a.id = md5(?) " +
                    " and a.archive_id=b.id and b.hostname in ("+sb.toString()+")" +
                    "  order by mdate  limit ?,?;";
            String sql =	"select mdate,href,type,rel from links  a,  archive_register b where a.id = md5(?) " +
                    " and a.archive_id=b.id and b.hostname in ("+sb.toString()+")" +
                    " and mdate between ? and  ? order by mdate limit ? , ? ;";
           
            try (  Connection conn = cPool.getConnection();
                   PreparedStatement  st = conn.prepareStatement(sql1);
                   PreparedStatement st1 = conn.prepareStatement(sql);
            ) {
                // conn = cPool.getConnection();
                if (start==null) {
                	System.out.println("timemap_info:"+sql1);
                    //   sql =	"select mdate,href,type,rel from links where id = md5('"+url+"') " +
                    //		"  order by mdate " +
                    //"limit "+istart+ ","+limit;

                    // sql =	"select mdate,href,type,rel from links where id = md5(?) " +
                    //	"  order by mdate  limit ?,?;";

                    // st = conn.prepareStatement(sql);
                    st.setString(1,url);
                    st.setInt(2, istart);
                    st.setInt(3, limit);
                    // s = conn.createStatement();
                    //rs = s.executeQuery(sql);
                    rs = st.executeQuery();
                }
                else {
                	System.out.println("timemap_info:"+sql);
                    //some other systems like cassandra cases not going to  implement here
                    //sql =	"select mdate,href,type,rel from links where id = md5('"+url+"') " +
                    //	" and mdate>'"+mysqlreqdate+"' order by mdate " +
                    //"limit "+istart+ ","+limit;

                    // sql =	"select mdate,href,type,rel from links where id = md5(?) " +
                    //" and mdate> ? order by mdate limit ? , ? ;";
                    //st1 = conn.prepareStatement(sql);
                    st1.setString(1,url);
                    st1.setString(2,mysqlstartdate);
                    st1.setString(3,mysqlenddate);
                    st1.setInt(4, istart);
                    st1.setInt(5, limit);
                    // s = conn.createStatement();
                    //rs = s.executeQuery(sql);
                    rs = st1.executeQuery();

                    // System.out.println("storage impl"+ sql);
                }
                //conn = startConnection();
                // conn = cPool.getConnection();
                //s = conn.createStatement();

                // rs = s.executeQuery(sql);
                while (rs.next()) {
                    String mdate = null;
                    Date _date = rs.getTimestamp(1);
                    String href = rs.getString(2);
                    String type = rs.getString(3);
                    String rel = rs.getString(4);
                    Link link = new Link();
                    java.text.SimpleDateFormat  formatter_utc = new java.text.SimpleDateFormat("yyyyMMdd HH:mm:ss");
                    if (_date!=null) {
                        mdate = formatter_utc.format(_date);
                    }
                    // need to format date
                    link.setDatetime(mdate);
                    link.setHref(href);
                    link.setType(type);
                    link.setRelationship(rel);
                    lh.addOderedLink(link);
                    lh.addLink(link);
                }


                return lh;
                //   }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }



            } //try
        } //if
        // TODO Auto-generated method stub
        return null;
    }

}
