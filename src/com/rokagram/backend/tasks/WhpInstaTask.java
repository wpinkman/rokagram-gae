package com.rokagram.backend.tasks;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.rokagram.backend.Instagram;
import com.rokagram.backend.UriBuilder;
import com.rokagram.backend.Utils;
import com.rokagram.backend.dao.DAO;
import com.rokagram.entities.IgPostEntity;
import com.rokagram.entities.WhpEntity;

@SuppressWarnings("serial")
public class WhpInstaTask implements DeferredTask {

    public static final Logger log = Logger.getLogger(WhpInstaTask.class.getName());
    private static URLFetchService fetcherService = com.google.appengine.api.urlfetch.URLFetchServiceFactory
            .getURLFetchService();

    private static ObjectMapper mapper = new ObjectMapper();
    private Calendar calPST = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));

    @Override
    public void run() {
        try {

            Long start = null;
            String apiUrl = buildIgFeedUrl(start);

            int depth = 0;
            processOnePage(apiUrl, depth);

        } catch (MalformedURLException e) {
        }
    }

    private String buildIgFeedUrl(Long maxTs) {
        UriBuilder b = UriBuilder.fromUri(Instagram.INSTAGRAM_API_BASE);

        b.path("users");
        b.path(Instagram.INSTAGRAM_USERID);
        b.path("media");
        b.path("recent");
        b.queryParam("client_id", Instagram.getClientId());
        if (maxTs != null) {
            b.queryParam("max_timestamp", maxTs.toString());
        }

        String apiUrl = b.toString();
        return apiUrl;
    }

    private void processOnePage(String apiUrl, int depth) throws MalformedURLException {

        log.info("apiUrl:" + apiUrl + ", depth:" + depth);

        if (depth++ > 1000) {
            log.severe("apiUrl:" + apiUrl + ", depth:" + depth);
            return;
        }

        HTTPRequest request = new HTTPRequest(new URL(apiUrl));
        try {
            HTTPResponse response = fetcherService.fetch(request);
            int responseCode = response.getResponseCode();
            if (responseCode == 200) {

                JsonNode rootNode = mapper.readTree(response.getContent());
                JsonNode dataNode = rootNode.path("data");

                int code = rootNode.path("meta").path("code").asInt();

                if (code == 200) {

                    String nextUrl = rootNode.path("pagination").path("next_url").asText();
                    Long maxTs = null;
                    boolean abort = false;

                    for (JsonNode node : dataNode) {

                        IgPostEntity postEntity = new IgPostEntity();

                        postEntity.fillFromNode(node);

                        long created_time = node.path("created_time").asLong();
                        maxTs = created_time;

                        // look for existance of WPH tag...

                        String whpTag = null;
                        int numWhpTags = 0;
                        JsonNode tagsNode = node.path("tags");
                        for (JsonNode tagNode : tagsNode) {
                            String tag = tagNode.textValue().toLowerCase();
                            if (tag.startsWith("whp")) {
                                whpTag = tag;
                                numWhpTags++;
                            }
                            if (tag.equals("lowdownground")) {
                                abort = true;
                            }
                        }

                        if (numWhpTags > 1) {
                            log.warning("Too many WPH tags:" + mapper.writeValueAsString(tagsNode));
                        }

                        Date created = postEntity.getCreated();
                        calPST.setTime(created);
                        int dow = calPST.get(Calendar.DAY_OF_WEEK);

                        if (whpTag != null) {

                            DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
                            String formattedDate = df.format(postEntity.getCreated());

                            log.info(formattedDate + " :: " + postEntity.getCaption());

                            WhpEntity whp = DAO.ofy().load().type(WhpEntity.class).id(whpTag).now();
                            if (whp == null) {
                                whp = new WhpEntity();
                                whp.setTag(whpTag);
                            }

                            if (dow < Calendar.WEDNESDAY) {
                                if (whp.getFeatureInsta() == null) {
                                    whp.setFeatureInsta(postEntity);
                                    whp.setFeatureDate(created);
                                } else {
                                    abort = true;
                                }
                            } else {
                                if (whp.getAnnounceInsta() == null) {
                                    whp.setAnnounceInsta(postEntity);
                                    whp.setAnnounceDate(created);
                                } else {
                                    abort = true;
                                }
                            }

                            if (!abort) {
                                DAO.ofy().save().entities(whp, postEntity).now();
                                if (Utils.isDevServer()) {
                                    System.out.println("Updating: " + whp);
                                    System.out.println("Writing: " + postEntity);
                                } else {

                                    Utils.sendAdminEmai("Found new Insta: #" + whp.getCapitalizedTag(),
                                            postEntity.getCaption());

                                }

                            }
                        }

                        if (abort) {
                            log.info("Aborting Instagram feed recurse");
                            break;
                        }
                    }

                    if (maxTs != null && !abort) {
                        nextUrl = buildIgFeedUrl(maxTs);
                        processOnePage(nextUrl, depth);
                    } else {
                        log.info("No posts found or abort. Either way.. all done");
                    }

                }
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }

}
