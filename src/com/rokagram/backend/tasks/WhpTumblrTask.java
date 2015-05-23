package com.rokagram.backend.tasks;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.datastore.Link;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.rokagram.backend.UriBuilder;
import com.rokagram.backend.Utils;
import com.rokagram.backend.dao.DAO;
import com.rokagram.entities.SystemSettingsEnum;
import com.rokagram.entities.TumblrPostEntity;
import com.rokagram.entities.WhpEntity;

@SuppressWarnings("serial")
public class WhpTumblrTask implements DeferredTask {

    public static final Logger log = Logger.getLogger(WhpTumblrTask.class.getName());
    private static URLFetchService fetcherService = com.google.appengine.api.urlfetch.URLFetchServiceFactory
            .getURLFetchService();

    private static ObjectMapper mapper = new ObjectMapper();

    private String tag;
    private String postId;

    public WhpTumblrTask() {
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public void run() {

        if (StringUtils.isEmpty(this.postId)) {
            checkTumblr();
        } else {
            checkTumblr(this.postId);
        }
        // pick up some randos
        // checkTumblr("31733087090");
        //
        // // 85247667385 and 85537029737- whpmamatoldme
        // checkTumblr("85247667385");
        // checkTumblr("85537029737");

    }

    private void checkTumblr() {
        checkTumblr(null);

    }

    private void checkTumblr(String postId) {

        int offset = 0;

        tumble(offset, postId);

        log.info("ALL DONE");
    }

    private void tumble(int offset, String postId) {

        UriBuilder b = UriBuilder.fromUri("http://api.tumblr.com");
        b.path("v2");
        b.path("blog");
        b.path("blog.instagram.com");
        b.path("posts");
        b.queryParam("api_key", DAO.getSystemSetting(SystemSettingsEnum.TUMBLR_API_KEY));
        b.queryParam("type", "photo");
        b.queryParam("offset", offset);
        if (postId == null) {
            b.queryParam("tag", "weekend hashtag project");
        } else {
            b.queryParam("id", postId);
        }

        HTTPRequest request = new HTTPRequest(b.toURL());
        try {
            HTTPResponse response = fetcherService.fetch(request);
            int responseCode = response.getResponseCode();

            if (responseCode == 200) {
                JsonNode rootNode = mapper.readTree(response.getContent());

                int count = 0;
                boolean abort = false;

                for (JsonNode postNode : rootNode.path("response").path("posts")) {
                    long timestamp = postNode.path("timestamp").asLong();
                    long secPst = timestamp - (8 * 60 * 60);
                    Date tsDate = new Date(secPst * 1000L);

                    StringBuilder sb = new StringBuilder();
                    String whpTag = null;

                    for (JsonNode tagNode : postNode.path("tags")) {
                        String tagText = tagNode.asText();
                        sb.append("\"" + tagText + "\" ");

                        // bail here.. WHP's before this one are not uniform
                        // enougth to bot-scrape
                        if (tagText.equalsIgnoreCase("lowdownground") || tagText.equalsIgnoreCase("inthewindow")) {
                            abort = true;
                        }

                        String lcTag = tagText.toLowerCase();
                        if (lcTag.startsWith("whp") && lcTag.length() > 3) {
                            whpTag = lcTag;
                            break;
                        }
                    }

                    if (whpTag == null && !abort) {
                        for (JsonNode tagNode : postNode.path("tags")) {
                            String tagText = tagNode.asText();
                            sb.append("\"" + tagText + "\" ");

                            // hacks to fix up sloppy tumblr posts.. come on
                            // Instagram Community Team.. get yo act togetha!
                            if (tagText.equalsIgnoreCase("mother's day")) {
                                whpTag = "whpmotherlylove";
                            }
                            if (tagText.equalsIgnoreCase("neonsigns")) {
                                whpTag = "whpneonsigns";
                            }
                            if (tagText.equalsIgnoreCase("candid")) {
                                whpTag = "whpcandid";
                            }
                        }
                    }
                    DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
                    String formattedDate = df.format(tsDate);

                    boolean flagAbnomal = false;
                    if (!(formattedDate.startsWith("Monday") || formattedDate.startsWith("Friday"))) {
                        flagAbnomal = true;
                    }
                    if (whpTag == null) {
                        flagAbnomal = true;
                    }

                    String postUrl = postNode.path("post_url").asText();

                    if (Utils.isDevServer()) {
                        System.out.println((flagAbnomal ? "***" : "") + offset + " " + formattedDate + " :: "
                                + sb.toString() + ", postUrl:" + postUrl);
                    }

                    if (whpTag != null) {

                        log.info(formattedDate + " :: " + postUrl);

                        TumblrPostEntity tpe = new TumblrPostEntity();
                        tpe.setId(postNode.path("id").asText());
                        tpe.setTimestamp(new Date(timestamp * 1000L));
                        tpe.setCaptionHtml(postNode.path("caption").asText());
                        tpe.setPost_url(new Link(postUrl));
                        for (JsonNode photoNode : postNode.path("photos")) {
                            String json = mapper.writeValueAsString(photoNode);
                            tpe.getPhotosJson().add(new Text(json));
                        }

                        WhpEntity whp = DAO.ofy().load().type(WhpEntity.class).id(whpTag).now();
                        if (whp == null) {
                            whp = new WhpEntity();
                            whp.setTag(whpTag);
                        }

                        if (formattedDate.startsWith("Monday")) {
                            if (whp.getFeatureTumblr() == null) {
                                whp.setFeatureTumblr(tpe);
                                whp.setFeatureDate(tpe.getTimestamp());
                            } else {
                                abort = true;
                            }
                        } else {
                            if (whp.getAnnounceTumblr() == null) {
                                whp.setAnnounceTumblr(tpe);
                                whp.setAnnounceDate(tpe.getTimestamp());
                            } else {
                                abort = true;
                            }
                        }

                        if (!abort) {
                            DAO.ofy().save().entities(tpe, whp).now();
                            if (Utils.isDevServer()) {
                                System.out.println("Updating: " + whp);
                                System.out.println("Writing: " + tpe);
                            } else {

                                Utils.sendAdminEmai("Found new Tumblr: #" + whp.getCapitalizedTag(),
                                        tpe.getCaptionHtml());

                            }
                        }
                    }

                    if (abort)
                        break;

                    count++;
                    offset++;

                }

                if (count > 0 && !abort) {
                    if (postId == null) {
                        tumble(offset, postId);
                    }
                }

            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }
}
