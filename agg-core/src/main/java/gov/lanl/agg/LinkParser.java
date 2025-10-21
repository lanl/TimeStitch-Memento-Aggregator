package gov.lanl.agg;

import gov.lanl.agg.utils.ThreadSafeSimpleDateFormat;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.ws.rs.core.MultivaluedMap;

//import com.sun.jersey.core.util.MultivaluedMapImpl;
//import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

public class LinkParser {
	private int curr;
 	private String value;
 	private LinkHeader header = new LinkHeader();
 	
 	
 	
 	public LinkParser(String value)
 	 	{
 	 	this.value = value;
 	 	this.curr = 0;
 	 	}
 	
 	public LinkHeader getHeader()
 	 	{
 	 	return header;
 	 	}
 	 	
public void parse()
{
 	String href = null;
 	MultivaluedMap<String, String> attributes = new MultivaluedMapImpl();
 	while (curr < value.length())
 	{
 	
 	char c = value.charAt(curr);
 	    
 	    if (c == '<')
 	    {
 	     if (href != null) throw new IllegalArgumentException("Uanble to parse Link header. Too many links in declaration: " + value);
 	      href = parseLink();
 	      // System.out.println("href"+ href);
 	      }
 	     else if (c == ';' || c == ' '|| c== '\n' ||c=='\r')
 	     {
 	       curr++;
 	       continue;
 	     }
 	     else if (c == ',')
 	     {
 	      populateLink(href, attributes);
 	      href = null;
 	      attributes = new MultivaluedMapImpl();
 	      curr++;
 	      
 	      }
 	     else
 	     {
 	     parseAttribute(attributes);
 	     }
 	}
 	// System .out.println("last link:");
 	populateLink(href, attributes);
 	
 	
 	} 

	
public String parseLink()
	{
	int end = value.indexOf('>', curr);
	if (end == -1) throw new IllegalArgumentException("Unable to parse Link header. No end to link: " + value);
	String href = value.substring(curr + 1, end);
	curr = end + 1;
	return href;
	}
	
public void parseAttribute(MultivaluedMap<String, String> attributes)
{
	//System.out.println("from parse attribute:"+value);
	int end = value.indexOf('=', curr);
	if (end == -1 || end + 1 >= value.length())
	throw new IllegalArgumentException("Unable to parse Link header. No end to parameter: " + value);
	String name = value.substring(curr, end);
	name = name.trim();
	//System.out.println("name0"+name);
	curr = end + 1;
	String val = null;
	if (curr >= value.length())
	{
	val = "";
	}
	else
	{
	
	if (value.charAt(curr) == '"')
	{
	if (curr + 1 >= value.length())
	throw new IllegalArgumentException("Unable to parse Link header. No end to parameter: " + value);
	curr++;
	end = value.indexOf('"', curr);
	if (end == -1)
	throw new IllegalArgumentException("Unable to parse Link header. No end to parameter: " + value);
	val = value.substring(curr, end);
	curr = end + 1;
	}
	else
	{
	StringBuffer buf = new StringBuffer();
	while (curr < value.length())
	{
	char c = value.charAt(curr);
	if (c == ',' || c == ';') break;
	buf.append(value.charAt(curr));
	curr++;
	}
	val = buf.toString();
	}
	}
	
	attributes.add(name, val);
	
	}
	
	

public  LinkHeader fromString(String value) throws IllegalArgumentException
{
	return from(value);
	}
	
	public  LinkHeader from(String value) throws IllegalArgumentException
	{
	LinkParser parser = new LinkParser(value);
	parser.parse();
	return parser.getHeader();
	
	}

	public static void main(String [ ] args)
	{ String test = "<https://doi.org/10.17026/dans-zm3-nkk4> ; rel=\"cite-as\",<https://doi.org/10.17026/dans-zm3-nkk4> ; rel=\"describedby\" ; type=\"application/vnd.datacite.datacite+xml\",<https://doi.org/10.17026/dans-zm3-nkk4> ; rel=\"describedby\" ; type=\"application/vnd.citationstyles.csl+json\",<http://www.persistent-identifier.nl?identifier=urn%3Anbn%3Anl%3Aui%3A13-z7og-v8> ; rel=\"cite-as\",<https://easy.dans.knaw.nl/ui/resources/easy/export?sid=easy-dataset%3A61891&format=XML> ; rel=\"describedby\" ; type=\"application/xml\" ; profile=\"https://easy.dans.knaw.nl/easy/easymetadata/\",<https://easy.dans.knaw.nl/ui/resources/easy/export?sid=easy-dataset%3A61891&format=CSV> ; rel=\"describedby\" ; type=\"txt/csv\";";

		LinkParser parser = new LinkParser(test);
        parser.parse();
        LinkHeader  linkheader = parser.getHeader();
        List<Link> a = linkheader.getLinks();
        Iterator<Link> it = a.iterator();
        while (it.hasNext()) {
        Link l = (Link) it.next();
        System.out.println (l.getHref());
        System.out.println (l.getRelationship());
        System.out.println (l.getType());
        }
        
        
        
	}
	
protected void populateLink(String href, MultivaluedMap<String, String> attributes)
{
	 List<String> rels = attributes.get("rel");
	// List<String> revs = attributes.get("rev");
	 String datetime = attributes.getFirst("datetime");
	
	//System.out.println("datetime" +datetime + ","+ href );
	if (datetime != null) attributes.remove("datetime");
	String type = attributes.getFirst("type");
	//System.out.println("type" +type);
	if (type != null) attributes.remove("type");	
	Set<String> relationships = new HashSet<String>();
	if (rels != null)
	{
	relationships.addAll(rels);
	attributes.remove("rel");
	}
	//if (revs != null)
	//{
	//relationships.addAll(revs);
	//attributes.remove("rev");
	//}
	
	for (String relationship : relationships)
	{
		//System.out.println("relationship"+relationship);
		
		Link link = new Link(datetime, relationship, href, type, attributes);
		   if (datetime==null) {
			header.getSpecialLinks().add(link);	
		    }
		    header.getLinks().add(link);
	    	header.getLinksByDate().put(datetime, link);
	        StringTokenizer tokenizer = new StringTokenizer(relationship);
	        while (tokenizer.hasMoreTokens())
	       {
	       String rel = tokenizer.nextToken();
	      //System.out.println("rel"+rel);
	      //do not want dublicates 
	       //Link link = new Link(datetime, rel, href, type, attributes);
	       //header.getSpecialLinks().add(link);
	        header.getLinksByRelationship().put(rel, link);
	      //header.getLinksByDate().put(datetime, link);
	      //header.getLinks().add(link);
	       }//while
	
	}//for
	}
}
	
	

