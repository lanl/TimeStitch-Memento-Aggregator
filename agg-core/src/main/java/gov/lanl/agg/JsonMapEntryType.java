package gov.lanl.agg;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * @author: Harihar Shankar, 6/11/14 9:51 AM
 */

public class JsonMapEntryType {

    @XmlElement(name="key")
    public String key;

    @XmlElement(name="value")
    public String value;
}
