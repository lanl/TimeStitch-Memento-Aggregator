package gov.lanl.agg;

import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

public class LinkHeader {

	private Map<String, Link> linksByRelationship = new HashMap<String, Link>();
	private Map<String, Link> linksByDate = new HashMap<String, Link>();
	private List<Link> links = new ArrayList<Link>();
	private List<Link> speciallinks = new ArrayList<Link>();
	ThreadSafeSimpleDateFormat formatter_utc = new ThreadSafeSimpleDateFormat("yyyyMMdd HH:mm:ss");
	TimeZone tz = TimeZone.getTimeZone("GMT");

	int host_id;
	int http_code;
	private NavigableMap<Long, Link> orderedlinks = (NavigableMap<Long, Link>) new TreeMap<Long, Link>();

	public void addLinks(List<Link> links) {
		links = links;

	}

	public LinkHeader addLink(final Link link) {
		links.add(link);

		return this;
	}

	public LinkHeader addOderedLink(final Link link) {
		String d = link.getDatetime();
		formatter_utc.setTimeZone(tz);

		if (d != null) {
			Date dd;
			try {
				dd = formatter_utc.parse(d);

				Long ld = new Long(dd.getTime());

				orderedlinks.put(ld, link);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			speciallinks.add(link);
		}
		return this;
	}

	public LinkHeader addLink(final String date, final String rel, final String href, final String type) {
		final Link link = new Link(date, rel, href, type, null);
		return addLink(link);
	}

	public Link getLinkByRelationship(String rel) {
		return linksByRelationship.get(rel);
	}

	public Map<String, Link> getLinksByDate() {
		return linksByDate;
	}

	public NavigableMap<Long, Link> getOrderedLinksByDate() {
		return orderedlinks;
	}

	public Map<String, Link> getLinksByRelationship() {
		return linksByRelationship;
	}

	public List<Link> getLinks() {
		return links;
	}

	public List<Link> getSpecialLinks() {
		return speciallinks;
	}

	public void setHostId(int host_id) {
		this.host_id = host_id;
	}

	public int getHostId() {
		return this.host_id;
	}

	public void setStatus(int http_code) {
		this.http_code = http_code;
	}

	public int getStatus() {
		return this.http_code;
	}
}
