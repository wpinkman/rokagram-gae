package com.rokagram.backend.tasks;

import java.io.IOException;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.rokagram.backend.Instagram;
import com.rokagram.backend.UriBuilder;
import com.rokagram.backend.dao.DAO;
import com.rokagram.entities.IgPostEntity;

@SuppressWarnings("serial")
public class ResolveFeaturedPostTask implements DeferredTask {
    public static final Logger log = Logger.getLogger(ResolveFeaturedPostTask.class.getName());
    private static URLFetchService fetcherService = com.google.appengine.api.urlfetch.URLFetchServiceFactory
            .getURLFetchService();
    private static ObjectMapper mapper = new ObjectMapper();

    private String tag;
    private String shortcode;

    public ResolveFeaturedPostTask(String tag, String shortcode) {
        super();
        this.tag = tag;
        this.shortcode = shortcode;
    }

    @Override
    public void run() {

        UriBuilder b = UriBuilder.fromUri(Instagram.INSTAGRAM_API_BASE);
        b.path("media");
        b.path("shortcode");
        b.path(shortcode);
        b.queryParam("client_id", Instagram.getClientId());

        try {
            HTTPResponse apiResponse = fetcherService.fetch(b.toURL());
            if (apiResponse.getResponseCode() == 200) {
                JsonNode rtnode = mapper.readTree(apiResponse.getContent());

                int code = rtnode.path("meta").path("code").asInt();

                if (code == 200) {
                    JsonNode dataNode = rtnode.path("data");

                    final IgPostEntity ipe = new IgPostEntity();
                    ipe.fillFromNode(dataNode);

                    ipe.setWhpfeatured(this.tag);

                    DAO.ofy().save().entity(ipe);

                    String found = "Found featured post " + shortcode + " for #" + tag;
                    log.info(found);
                    System.out.println(found);

                } else {
                    log.warning("code: " + code + ". Didn't find featured post " + shortcode + " for #" + tag);
                }
            } else {
                log.warning("resp code: " + apiResponse.getResponseCode() + ". Didn't find featured post " + shortcode
                        + " for #" + tag);
            }

        } catch (IOException e) {
            log.warning(e.getMessage());
        }

        // }

    }

}
