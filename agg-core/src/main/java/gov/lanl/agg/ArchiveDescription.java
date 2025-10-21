package gov.lanl.agg;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
@author Lyudmila Balakireva

*/

public class ArchiveDescription {
public String name;
public  List <DomainSuffDesc> domainsuffixes;
public Date originated;
public Integer ordernumber;
public String timegate;
public String timemap;
public String pagingstatus;
public String includestatus;
public Date  updatestatus; //freshly added
public DelayPeriod delayperiod; //let not update from wayback for example every two week delay per archive??
public String rewritestatus;
public String accesstatus;
public String mementostatus; //native memento or proxy
public String mementotemplate; //rule to construct memento or n/a
public String startwithfilter="**";
public String openwayback;
public String longname="**";
public String icon="";
public String calendarurl="";
public String archiveType;
public Boolean bestSelect;
public String timegateRedirect;
public String timemapRedirect;
public String timemapGlobal;
public String timegateGlobal;

 public void setName(String name) {
	 this.name=name;
 }

 public String getName(){
	 return name;
 }
 
 public void setLongname(String name) {
	 this.longname=name;
 }

 public String getLongname(){
	 return longname;
 }
 
 public void setIcon(String name) {
	 this.icon=name;
 }

 public String getIcon(){
	 return icon;
 }
 
 public void setCalendarUrl(String name) {
	 this.calendarurl=name;
 }

 public String getCalendarUrl(){
	 return calendarurl;
 }
 
 
public String getArchiveType() {
    return archiveType;
}

public void setArchiveType(String archiveType) {
    this.archiveType = archiveType;
}

public Boolean getBestSelect() {
    return bestSelect;
}

public void setBestSelect(Boolean bestSelect1) {
    bestSelect = bestSelect1;
}

public String getTimegateRedirect() {
    return timegateRedirect;
}
public void setTimegateRedirect(String str) {
    timegateRedirect = str;
} 
public String getTimemapRedirect() {
    return timemapRedirect;
}
public void setTimemapRedirect(String str) {
    timemapRedirect = str;
} 
 

 public void setOrdernumber(int i){
	  ordernumber=i;
 }
 
 public Integer getOrdernumber() {
	 return ordernumber;
 }
 
 public String getTimemap(){
	 return timemap;
 }
 
 public void setTimemap(String timemap){
	 this.timemap=timemap;
 }
 public String getTimemapGlobal(){
	 return timemapGlobal;
 }
 
 public void setTimemapGlobal(String timemap){
	 this.timemapGlobal=timemap;
 }
 
 public String getTimegateGlobal(){
	 return timegateGlobal;
 }
 
 public void setTimegateGlobal(String timegate){
	 this.timegateGlobal = timegate;
 }
 
 
 public String getTimegate(){
	 return timegate;
 }
 
 public void setTimegate(String timegate){
	 this.timegate = timegate;
 } 
 
 public void  setIncludestatus(String sinclude) {
	 includestatus=sinclude; 
 }
 
 public String getIncludestatus() {
	 return includestatus;
 }
 
 public void  setPagingstatus(String pstatus) {
	 pagingstatus=pstatus; 
 }
 
 public String getPagingstatus() {
	 return pagingstatus;
 }
 
 public List getDomainSuffixes() {
	 return domainsuffixes;  
 }
 
 public void setDomainSuffixes( List <DomainSuffDesc> sf){
	 domainsuffixes = sf;
	 
 }
 
  public void setOriginated(Date odate){
	 originated=odate;
 }
 
  public Date getOriginated(){
	  return  originated;
  }
  
 public  void  setUpdatestatus(Date pstatus) {
		 updatestatus=pstatus; 
	 }
 public Date getUpdateStatus() {
		 return updatestatus;
	 }
	 
public 	 void  setDelaystatus(DelayPeriod pstatus) {
		 delayperiod=pstatus; 
	 }
	 
public	 DelayPeriod getDelaystatus() {
		 return delayperiod;
	 }
	 
public 	 void  setRewritestatus(String pstatus) {
	rewritestatus=pstatus; 
}

public	 String getRewritestatus() {
	 return rewritestatus;
}
public 	 void  setOpenwaybackstatus(String pstatus) {
	openwayback=pstatus; 
}

public	 String getOpenwaybackstatus() {
	 return openwayback;
}

public 	 void  setAccesstatus(String pstatus) {
	accesstatus=pstatus; 
}

public	 String getAccesstatus() {
	 return accesstatus;
}
public 	 void setMementostatus(String pstatus) {
	mementostatus=pstatus; 
}

public	 String getMementostatus() {
	 return mementostatus;
}

public 	 void  setMementotemplate(String pstatus) {
	mementotemplate=pstatus; 
}

public	 String getMementotemplate() {
	 return mementotemplate;
}

public 	 void  setStartwith(String pstatus) {
	startwithfilter=pstatus; 
}

public	 String getStartwith() {
	 return startwithfilter;
}

}
