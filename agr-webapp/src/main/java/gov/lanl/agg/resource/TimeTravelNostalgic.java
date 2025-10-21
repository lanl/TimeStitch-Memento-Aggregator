package gov.lanl.agg.resource;

import gov.lanl.agg.cache.CacheStorage;
import gov.lanl.agg.utils.Tokens;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;

/**
 * @author: Harihar Shankar, 10/29/14 2:52 PM
 */


@Path(Tokens.NOSTALGIC_BASE_PATH)

public class TimeTravelNostalgic {

    static List<String> nostalgicUrls = new ArrayList<>();
    static {
        java.nio.file.Path path = Paths.get("nostalgic_urls.txt");
        ClassLoader cl = TimeTravelNostalgic.class.getClassLoader();
        InputStream inputStream;
        if (cl != null) {
            inputStream = cl.getResourceAsStream("nostalgic_urls.txt");
        }
        else {
            inputStream = ClassLoader.getSystemResourceAsStream("nostalgic_urls.txt");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("http://")) {
                    line = "http://" + line;
                }
                nostalgicUrls.add(line);
            }
        }
        catch (IOException ignore) {
            System.out.println(ignore.getMessage());
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getMemento(@Context HttpHeaders httpHeaders,
                             @Context UriInfo uriInfo)
            throws ParseException, URISyntaxException {

        String mementoUrl;

        /*
        Map<String, String> memento;
        memento = ((CacheStorage) MyInitServlet.getInstance().getAttribute("storage")).getNostalgicInfo();
        mementoUrl = memento.get("url");
        */
        //reqDate = memento.get("date");

        int idx = new Random().nextInt(nostalgicUrls.size());
        mementoUrl = nostalgicUrls.get(idx);

        Response.ResponseBuilder responseBuilder = Response.status(302);
        String location =  Tokens.LIST_BASE_PATH + getRandomDate() + "/" + mementoUrl;
        responseBuilder.header("Location", location);
        return responseBuilder.build();
    }

    public String getRandomDate() {
        long start = java.sql.Timestamp.valueOf("1997-01-01 00:00:00").getTime();
        long end = new Date().getTime();

        long diff = end - start + 1;
        Timestamp rand = new Timestamp(start + (long)(Math.random() * diff));

        java.text.SimpleDateFormat  lformatter_utc = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        return lformatter_utc.format(rand);
    }
}
