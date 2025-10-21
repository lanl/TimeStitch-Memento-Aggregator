package gov.lanl.agg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class RulesDescription {
Map<Pattern, List<String>> urlRules= null;
List<String> defaultArchives = new ArrayList<>();

public Map<Pattern, List<String>> getUrlRules() {
    return urlRules;
}

public void setUrlRules(Map<Pattern, List<String>> m){
	urlRules = m;
}
public List<String> getDefaultArchives() {
    return defaultArchives;
}

public void setDefaultArchives(List<String> defaulta) {
     this.defaultArchives=defaulta;
}
}
