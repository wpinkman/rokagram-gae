package com.rokagram.backend;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.images.Composite;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ImagesServiceFailureException;
import com.google.appengine.api.urlfetch.FetchOptions;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.rokagram.backend.dao.DAO;
import com.rokagram.entities.SystemSettingsEnum;
import com.rokagram.entities.UserEntity;
import com.rokagram.entities.UserLoveEntity;

public class Instagram {

    private static final double INSTAGRAM_FETCH_DEADLINE = 10.0D;

    private static final String GETROKAGRAM = "getrokagram";

    public static final String INSTAGRAM_USERID = "25025320";

    public static final Logger log = Logger.getLogger(Instagram.class.getName());

    public static final String INSTAGRAM_API_BASE = "https://api.instagram.com/v1";

    private static URLFetchService fetcherService = com.google.appengine.api.urlfetch.URLFetchServiceFactory
            .getURLFetchService();
    private static ImagesService imagesService = ImagesServiceFactory.getImagesService();

    private static ObjectMapper mapper = new ObjectMapper();

    public static String getAuthorizationUrl(HttpServletRequest req, String code) {
        UriBuilder b = UriBuilder.fromUri("https://api.instagram.com/oauth/authorize/");

        b.queryParam("client_id", getClientId());
        b.queryParam("redirect_uri", getRedirectUri(req));
        b.queryParam("response_type", Constants.CODE);

        String state = Utils.createRandomStateString();
        HttpSession session = req.getSession();

        session.setAttribute(Constants.STATE, state);
        b.queryParam(Constants.STATE, state);

        String scope = "basic relationships likes";
        if (StringUtils.equalsIgnoreCase(code, Constants.LOVE) || Utils.isDevServer()
                && req.getServletPath().contains("link")) {
            scope = "basic";
        }
        b.queryParam("scope", scope);

        return b.toString();
    }

    private static String getRedirectUri(HttpServletRequest req) {
        String redirecti_uri = "http://" + req.getServerName();
        if (Utils.isDevServer()) {
            redirecti_uri = "http://localhost:8888";
        }
        return redirecti_uri;
    }

    public static void updateUserFromInstagram(UserEntity user) {
        UriBuilder b = UriBuilder.fromUri("https://api.instagram.com/v1/users");
        b.path(user.getId());
        b.queryParam("access_token", user.getAccess_token());

        HTTPRequest request = new HTTPRequest(b.toURL());
        try {
            HTTPResponse response = fetcherService.fetch(request);
            if (response.getResponseCode() == 200) {

                JsonNode rootNode = mapper.readTree(response.getContent());
                JsonNode userNode = rootNode.path("data");

                int code = rootNode.path("meta").path("code").asInt();

                if (code == 200) {
                    String userid = userNode.path("id").asText();

                    if (userid.equals(user.getId())) {
                        user.setUsername(userNode.path("username").asText());
                        user.setFull_name(userNode.path("full_name").asText());
                        user.setProfile_picture(userNode.path("profile_picture").asText());
                        user.setIgnoreLifecycle(true);
                        DAO.ofy().save().entity(user);
                    }
                }
            } else if (response.getResponseCode() == 400) {
                user.setAccess_token(null);
                DAO.ofy().save().entity(user);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static UserEntity findUser(String username) {
        UserEntity ret = null;

        for (UserEntity user : searchForUser(username)) {
            if (user.getUsername().equals(username)) {
                ret = user;
                break;
            }
        }

        return ret;

    }

    public static List<UserEntity> searchForUser(String q) {

        List<UserEntity> ret = new ArrayList<UserEntity>();
        UriBuilder b = UriBuilder.fromUri("https://api.instagram.com/v1/users/search");
        b.queryParam("q", q);
        b.queryParam("client_id", getClientId());

        String string = b.toString();
        HTTPRequest request = buildRequest(string);
        try {
            HTTPResponse response = fetcherService.fetch(request);
            if (response.getResponseCode() == 200) {

                JsonNode rootNode = mapper.readTree(response.getContent());
                JsonNode dataNode = rootNode.path("data");

                int code = rootNode.path("meta").path("code").asInt();

                if (code == 200) {

                    for (JsonNode userNode : dataNode) {
                        UserEntity user = new UserEntity();
                        String userid = userNode.path("id").asText();

                        user.setId(userid);
                        user.setUsername(userNode.path("username").asText());
                        user.setFull_name(userNode.path("full_name").asText());
                        user.setProfile_picture(userNode.path("profile_picture").asText());

                        ret.add(user);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static UserLoveEntity makeLove(String fromUsername, String toUsername, ServletContext servletContext,
            boolean facebook) throws IOException, InterruptedException, ExecutionException {

        UserLoveEntity love = null;

        // 470 x 246 for a facebook post...

        UserEntity userFrom = findUser(fromUsername);
        UserEntity userTo = findUser(toUsername);
        boolean fromRokagram = false;
        boolean toRokagram = false;

        Future<HTTPResponse> fromProfileFetch = null;
        Future<HTTPResponse> toProfileFetch = null;
        if (userFrom != null) {
            if (userFrom.getUsername().equals(GETROKAGRAM)) {
                fromRokagram = true;
            } else {
                HTTPRequest request = buildRequest(userFrom.getProfile_picture());

                fromProfileFetch = fetcherService.fetchAsync(request);
            }
        }
        if (userTo != null) {
            if (userTo.getUsername().equals(GETROKAGRAM)) {
                toRokagram = true;
            } else {
                HTTPRequest request = buildRequest(userTo.getProfile_picture());

                toProfileFetch = fetcherService.fetchAsync(request);
            }
        }

        InputStream profilePlaceholderStream = null;
        if (userFrom == null || userTo == null) {
            profilePlaceholderStream = servletContext.getResourceAsStream("/WEB-INF/love/orientation-portrait-150.png");
        }
        InputStream heartStream = servletContext.getResourceAsStream("/WEB-INF/love/roka-heart150x150.png");

        InputStream cameraStream = null;
        if (toRokagram || fromRokagram) {
            cameraStream = servletContext.getResourceAsStream("/WEB-INF/love/camera175x150.png");
        }
        InputStream rokagramStream = servletContext.getResourceAsStream("/WEB-INF/love/rokagram359x90.jpg");
        InputStream getrokagramStream = servletContext.getResourceAsStream("/WEB-INF/love/rokagram-com-slash-love.png");

        Image fromProfileImage = null;
        Image toProfileImage = null;
        Image placeHolderImage = null;
        if (fromProfileFetch != null) {
            fromProfileImage = ImagesServiceFactory.makeImage(fromProfileFetch.get().getContent());
        }
        if (toProfileFetch != null) {
            toProfileImage = ImagesServiceFactory.makeImage(toProfileFetch.get().getContent());
        }
        if (profilePlaceholderStream != null) {
            placeHolderImage = ImagesServiceFactory.makeImage(IOUtils.toByteArray(profilePlaceholderStream));
        }
        Image heartImage = ImagesServiceFactory.makeImage(IOUtils.toByteArray(heartStream));

        Image cameraImage = null;
        if (cameraStream != null) {
            cameraImage = ImagesServiceFactory.makeImage(IOUtils.toByteArray(cameraStream));
        }

        Image rokagramImage = ImagesServiceFactory.makeImage(IOUtils.toByteArray(rokagramStream));
        Image getrokagramImage = ImagesServiceFactory.makeImage(IOUtils.toByteArray(getrokagramStream));

        List<Composite> listComposites = new ArrayList<Composite>();

        int rokagramImageOffset = 80;
        if (facebook) {
            rokagramImageOffset = 140;
        }

        listComposites.add(ImagesServiceFactory.makeComposite(rokagramImage, 0, rokagramImageOffset, 1f,
                Composite.Anchor.TOP_CENTER));

        int fromProfileOffset = 40;
        int toProfileOffset = 414;

        if (fromProfileImage != null) {
            listComposites.add(ImagesServiceFactory.makeComposite(fromProfileImage, fromProfileOffset, 0, 1f,
                    Composite.Anchor.CENTER_LEFT));
        } else {
            if (fromRokagram) {
                listComposites.add(ImagesServiceFactory.makeComposite(cameraImage, fromProfileOffset, 0, 1f,
                        Composite.Anchor.CENTER_LEFT));

            } else {
                listComposites.add(ImagesServiceFactory.makeComposite(placeHolderImage, fromProfileOffset, 0, 1f,
                        Composite.Anchor.CENTER_LEFT));
            }

        }
        listComposites.add(ImagesServiceFactory.makeComposite(heartImage, 231, 0, 1f, Composite.Anchor.CENTER_LEFT));
        if (toProfileImage != null) {
            listComposites.add(ImagesServiceFactory.makeComposite(toProfileImage, toProfileOffset, 0, 1f,
                    Composite.Anchor.CENTER_LEFT));

        } else {
            if (toRokagram) {
                listComposites.add(ImagesServiceFactory.makeComposite(cameraImage, toProfileOffset, 0, 1f,
                        Composite.Anchor.CENTER_LEFT));

            } else {
                listComposites.add(ImagesServiceFactory.makeComposite(placeHolderImage, toProfileOffset, 0, 1f,
                        Composite.Anchor.CENTER_LEFT));
            }
        }

        int urlOffset = -100;
        if (facebook) {
            urlOffset = -170;
        }
        listComposites.add(ImagesServiceFactory.makeComposite(getrokagramImage, 0, urlOffset, 1f,
                Composite.Anchor.BOTTOM_CENTER));

        try {
            long background = 0xffebebebL;
            // long background = 0x00000000L;
            Image newImage = imagesService.composite(listComposites, 612, 612, background,
                    ImagesService.OutputEncoding.JPEG);

            love = new UserLoveEntity();
            // love.setUsername(userFrom.getUsername());
            love.setImageBlob(newImage.getImageData());
            love.setWidth(newImage.getWidth());
            love.setHeight(newImage.getHeight());
            love.setFormat(newImage.getFormat().toString());

            // DAO.ofy().save().entities(love).now();
        } catch (IllegalArgumentException iae) {
            log.warning("IllegalArgumentException:" + iae.getMessage());
        } catch (ImagesServiceFailureException isfe) {
            log.warning("ImagesServiceFailureException:" + isfe.getMessage());
        }
        return love;
    }

    private static HTTPRequest buildRequest(String url) {
        HTTPRequest request = null;
        try {
            request = new HTTPRequest(new URL(url), HTTPMethod.GET,
                    FetchOptions.Builder.withDeadline(INSTAGRAM_FETCH_DEADLINE));
        } catch (MalformedURLException e) {
        }
        return request;
    }

    public static UserEntity fetchAccessToken(HttpServletRequest req, String code) throws IOException {
        UserEntity ret = null;

        UriBuilder b = UriBuilder.fromUri("https://api.instagram.com/oauth/access_token");

        URL url = b.toURL();

        b.queryParam("client_id", getClientId());
        b.queryParam("client_secret", getClientSecret());
        b.queryParam("grant_type", "authorization_code");
        b.queryParam("redirect_uri", getRedirectUri(req));
        b.queryParam(Constants.CODE, code);

        String body = b.getQuery();

        HTTPRequest postRequest = new HTTPRequest(url, HTTPMethod.POST,
                FetchOptions.Builder.withDeadline(INSTAGRAM_FETCH_DEADLINE));

        postRequest.setPayload(body.getBytes());

        try {
            HTTPResponse postResponse = fetcherService.fetch(postRequest);

            if (postResponse.getResponseCode() == 200) {

                JsonNode rootNode = mapper.readTree(postResponse.getContent());
                JsonNode userNode = rootNode.path("user");
                String userid = userNode.path("id").asText();

                ret = new UserEntity();
                ret.setId(userid);
                ret.setAccess_token(rootNode.path("access_token").asText());
                ret.setUsername(userNode.path("username").asText());
                ret.setFull_name(userNode.path("full_name").asText());
                ret.setProfile_picture(userNode.path("profile_picture").asText());
                ret.setCode(code);

                DAO.ofy().save().entity(ret);

            } else {
                JsonNode rootNode = mapper.readTree(postResponse.getContent());
                Utils.warn(rootNode.toString());
                Alert.setErrorAlert(req, "Error",
                        "Trouble communicating with Instagram right now.. please try again later.");
            }
        } catch (java.net.SocketTimeoutException sex) {
            Alert.setErrorAlert(req, "Error", "Can't communicate with Instagram right now.. please try again later.");
            Utils.warn(sex.getMessage());
        } catch (java.io.IOException ex) {
            Alert.setErrorAlert(req, "Error", "Can't communicate with Instagram right now.. please try again later.");
            Utils.warn(ex.getMessage());
        }
        return ret;
    }

    private static String getClientSecret() {
        String ret = null;
        if (Utils.isDevServer()) {
            ret = DAO.getSystemSetting(SystemSettingsEnum.INSTAGRAM_LOCALHOST_CLIENT_SECRET);
        } else {
            ret = DAO.getSystemSetting(SystemSettingsEnum.INSTAGRAM_CLIENT_SECRET);
        }
        return ret;
    }

    public static String getClientId() {
        String ret = null;
        if (Utils.isDevServer()) {
            ret = DAO.getSystemSetting(SystemSettingsEnum.INSTAGRAM_LOCALHOST_CLIENT_ID);
        } else {
            ret = DAO.getSystemSetting(SystemSettingsEnum.INSTAGRAM_CLIENT_ID);
        }
        return ret;
    }

}
