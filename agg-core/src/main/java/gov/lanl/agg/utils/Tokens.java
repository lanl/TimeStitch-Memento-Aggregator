package gov.lanl.agg.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Harihar Shankar, 6/11/14 3:20 PM
 */
public class Tokens {
    public static final String DOMAIN_NAME = "http://mementoweb.org";
    public static final String MEMENTO_BASE_PATH = "/memento/";
    public static final String LIST_BASE_PATH = "/list/";
    public static final String MAP_BASE_PATH = "/map/";
    public static final String REDIRECT_BASE_PATH = "/redirect/";
    public static final String NOSTALGIC_BASE_PATH = "/nostalgic/";
    public static final String JSON_API_BASE_PATH = "/api/json/";
    public static final List<String> WIKIA_DOMAINS = new ArrayList<String>();

    static {
        WIKIA_DOMAINS.add("wikia.com");
        WIKIA_DOMAINS.add("wowwiki.com");
        WIKIA_DOMAINS.add("en.memory-alpha.org");
        WIKIA_DOMAINS.add("wiki.ffxiclopedia.org");
        WIKIA_DOMAINS.add("jedipedia.de");
    }
}
