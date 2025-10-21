package gov.lanl.agg;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class TimeTravel {
	@Expose
    protected String originalUrl;
	@Expose
    protected String requestUrl;
	@Expose
    protected Date acceptDatetime;

    protected Date lastModified;
	
    protected Map<String, NavigableMap<Long, Link>> mementos=null;
    protected Map<String, List<Map<String, String>>> distribution;
    @Expose
    //not that is good name .....
    @SerializedName("mementos")
    protected Map<String, List<Link>> summary = new HashMap<>();
    protected Map<Long, String> sortedArchives = new HashMap<>();
    protected Boolean isDynamic = false;
    protected String cmsArchive = "";
    protected String nativeArchive = "";

    @XmlJavaTypeAdapter(JsonMapAdapter.class)
    protected Map<String, String> byArchiveCount;

    public TimeTravel()
    {
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String href) {
        this.requestUrl = href;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String href) {
        this.originalUrl = href;
    }

    public Date getAcceptDatetime() {
        return acceptDatetime;
    }

    public void setAcceptDatetime(Date aDatetime) {
        this.acceptDatetime = aDatetime;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Map<String, NavigableMap<Long, Link>> getMementos() {
        return mementos;
    }

    public void setMementos(Map<String, NavigableMap<Long, Link>> map) {
        this.mementos = map;
    }

    public Map<String, List<Map<String, String>>> getDistribution() {
        return distribution;
    }

    public void setDistribution(Map<String, List<Map<String, String>>> map) {
        this.distribution = map;
    }

    public Map<String, String> getByArchiveCount() {
        return byArchiveCount;
    }

    public void setByArchiveCount(Map<String, String> map) {
        this.byArchiveCount = map;
    }

    public void setSummary(Map<String, List<Link>> summary) {
        Iterator<String> summaryKeys = summary.keySet().iterator();
        while (summaryKeys.hasNext()) {
            String key = summaryKeys.next();
            if (key != null && summary.get(key) != null) {
                this.summary.put(key, summary.get(key));
            }
        }
    }

    public Map<Long, String> getSortedArchives() {
        return this.sortedArchives;
    }

    public void setSortedArchives(Map<Long, String> sortedArchives, Boolean isNative) {
        Iterator<Long> archiveKeys = sortedArchives.keySet().iterator();
        while (archiveKeys.hasNext()) {
            Long key = archiveKeys.next();
            if (key != null && isNative) {
            //if (key != null && sortedArchives.get(key).toLowerCase().equals("native")) {
                // a native archive always gets precedence in the sorted display.
                this.sortedArchives.put(0L, sortedArchives.get(key));
            }
            else if (key != null && sortedArchives.get(key) != null) {
                Long reqDt = this.acceptDatetime.getTime();
                Long delta = 0L;
                if (reqDt > key) {
                    delta = reqDt - key;
                }
                else {
                    delta = key - reqDt;
                }
                if (this.sortedArchives.containsKey(delta)) {
                    while (this.sortedArchives.containsKey(delta)) {
                        delta++;
                    }
                }
                this.sortedArchives.put(delta, sortedArchives.get(key));
            }
        }
        //System.out.println("Sorted archives:: ");
        //System.out.println(this.sortedArchives);
    }

    public Map<String, List<Link>> getSummary() {
        return summary;
    }

    public String getCmsArchive() {
        return this.cmsArchive;
    }
    public void setCmsArchive(String cmsArchive) {
        this.cmsArchive = cmsArchive;
    }

    public String getNativeArchive() {
        return this.nativeArchive;
    }
    public void setNativeArchive (String nativeArchive) {
        this.nativeArchive = nativeArchive;
    }

    public Boolean getIsDynamic() {
        return this.isDynamic;
    }
    public void setIsDynamic(Boolean isDynamic) {

        this.isDynamic = isDynamic;
    }
}
