package gov.lanl.agg;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author: Harihar Shankar, 6/11/14 9:40 AM
 */
public final class JsonMapAdapter extends
        XmlAdapter<JsonMapType, Map<String, String>> {

    @Override
    public JsonMapType marshal(Map<String, String> arg0) throws Exception {
        JsonMapType jsonMapType = new JsonMapType();
        for (Entry<String, String> entry : arg0.entrySet()) {
            JsonMapEntryType jsonMapEntryType = new JsonMapEntryType();
            jsonMapEntryType.key = entry.getKey();
            jsonMapEntryType.value = entry.getValue();
            jsonMapType.entry.add(jsonMapEntryType);
        }
        return jsonMapType;
    }

    @Override
    public Map<String, String> unmarshal(JsonMapType arg0) throws Exception {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        for (JsonMapEntryType jsonMapEntryType : arg0.entry) {
            hashMap.put(jsonMapEntryType.key, jsonMapEntryType.value);
        }
        return hashMap;
    }
}
