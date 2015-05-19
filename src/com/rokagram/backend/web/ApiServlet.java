package com.rokagram.backend.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.rokagram.backend.GaeClientLocation;
import com.rokagram.backend.Instagram;
import com.rokagram.backend.ServletUtils;
import com.rokagram.backend.UriBuilder;
import com.rokagram.backend.Utils;
import com.rokagram.backend.dao.DAO;
import com.rokagram.backend.dto.InstaDailyDto;
import com.rokagram.backend.dto.RegistryModification;
import com.rokagram.backend.dto.TopLevelResponse;
import com.rokagram.backend.roku.Roku;
import com.rokagram.backend.tasks.DeviceRollupTask;
import com.rokagram.entities.ApiClientEntity;
import com.rokagram.entities.FeedFmSettingsEntity;
import com.rokagram.entities.LogEntity;
import com.rokagram.entities.RegistryEntity;
import com.rokagram.entities.RokuEntity;
import com.rokagram.entities.UserEntity;
import com.rokagram.entities.WhpEntity;

@SuppressWarnings("serial")
public class ApiServlet extends HttpServlet {

   public static final Logger log = Logger.getLogger(ApiServlet.class.getName());
   private static URLFetchService fetcherService = com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService();

   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

      List<String> segments = new ArrayList<String>();
      for (String segment : Splitter.on('/').omitEmptyStrings().trimResults().split(req.getPathInfo())) {
         segments.add(segment);
      }

      if (segments.size() > 0) {
         String segment = segments.get(0);

         if (segment.equals("logs")) {
            handleLogPost(req, resp);
         } else {
            handleDevicePost(req, resp);
         }
      }

   }

   private void handleLogPost(HttpServletRequest req, HttpServletResponse resp) {

      LogEntity logEntity = new LogEntity();

      String devId = req.getHeader(Roku.X_ROKAGRAM_RESERVED_DEV_UNIQUE_ID);
      logEntity.setDeviceId(devId);

      Roku.updateDeviceFromRequest(req);

      String currentUserId = req.getHeader(Roku.X_ROKAGRAM_RESERVED_CURRENT_USER);
      logEntity.setUserId(currentUserId);

      GaeClientLocation location = new GaeClientLocation(req);
      logEntity.setLocation(location.toString());

      try {
         Document doc = parseXML(req.getInputStream());
         Element root = doc.getDocumentElement();

         logEntity.setType(getSimpleElementText(root, "type"));
         logEntity.setUserName(getSimpleElementText(root, "username"));
         logEntity.setMessage(getSimpleElementText(root, "message"));

         logEntity.setInstaReqFromString(getSimpleElementText(root, "instareq"));

         NodeList clientNodes = root.getElementsByTagName("client");

         for (int index = 0; index < clientNodes.getLength(); index++) {
            Element clientNode = (Element) clientNodes.item(index);

            String clientId = clientNode.getAttribute("client_id");
            int rateLimit = 0;
            int rateLimitRemaining = 0;
            try {
               rateLimit = Integer.parseInt(getSimpleElementText(clientNode, "rateLimit"));
               rateLimitRemaining = Integer.parseInt(getSimpleElementText(clientNode, "rateLimitRemaining"));
            } catch (NumberFormatException nfe) {

            }

            ApiClientEntity client = DAO.ofy().load().type(ApiClientEntity.class).id(clientId).now();
            boolean modified = false;
            if (client == null) {
               client = new ApiClientEntity();
               Utils.warn("Creating new ApiClientEntity, clientId:" + clientId);
               client.setClient_id(clientId);
               modified = true;
            }

            if (rateLimit != client.getRateLimit()) {
               client.setRateLimit(rateLimit);
               modified = true;
            }

            if (rateLimitRemaining < client.getRateLimitEbb()) {
               client.setRateLimitEbb(rateLimitRemaining);
               modified = true;
            }

            if (modified) {
               log.info(client.toString());
               DAO.ofy().save().entity(client).now();
            }
         }

      } catch (IOException e) {
         log.warning(e.getMessage());
         e.printStackTrace();
      } catch (Exception e) {
         log.warning(e.getMessage());
      }
      DAO.ofy().save().entity(logEntity);

      postGoogleAnalyteis(req, logEntity);
   }

   private void postGoogleAnalyteis(HttpServletRequest req, LogEntity logEntity) {
      UriBuilder b = UriBuilder.fromUri("http://www.google-analytics.com/collect");

      String tid = "UA-47056757-3";

      b.queryParam("v", "1");
      b.queryParam("tid", tid);
      b.queryParam("ds", "roku");

      RokuEntity roku = DAO.ofy().load().type(RokuEntity.class).id(logEntity.getDeviceId()).now();
      if (roku != null) {

         if (roku.getChannelVersion().contains("1.3.6")) {
            String cid = roku.getCid();

            if (cid == null) {
               roku.setCid(UUID.randomUUID().toString());
               log.info("Adding a CID for " + roku.toString());
               DAO.ofy().save().entity(roku);
            }

            b.queryParam("cid", cid);
            String uid = logEntity.getUserId();
            if (uid != null) {
               b.queryParam("uid", uid);
            }
            b.queryParam("uip", req.getRemoteAddr());

            String ua = req.getHeader("user-agent");
            if (ua != null) {
               b.queryParam("ua", ua);
            }
            if (roku.getDisplayMode() != null) {
               String sr = "1280x720";
               if (!roku.getDisplayMode().equals("720p")) {
                  sr = "640×480";
               }
               b.queryParam("sr", sr);
            }

            b.queryParam("t", "event");
            String eventType = logEntity.getType();
            String screen = "main";
            if (eventType.equals("startup")) {
               String message = logEntity.getMessage();
               if (message != null && message.equals("ScreenSaver")) {
                  eventType = "screensaver";
                  screen = "screensaver";
                  b.queryParam("ni", "1");
               }
            }
            b.queryParam("ec", eventType);
            b.queryParam("ea", "hit");

            b.queryParam("an", "rokagram");
            b.queryParam("av", roku.getChannelVersion());

            // b.queryParam("el", logEntity.getMessage());
            // b.queryParam("ev", "value");

            // screen name
            b.queryParam("cd", screen);

            String body = b.getEncodedQueryString();

            try {
               log.info("GA: POST " + b.toString());
               HTTPRequest request = new HTTPRequest(new URL("http://www.google-analytics.com/collect"), HTTPMethod.POST);
               request.setPayload(body.getBytes());

               fetcherService.fetch(request);
            } catch (IOException e) {
               log.warning("Failed to POST to GA:" + e.getMessage());
            }
         }
      }

   }

   private String getSimpleElementText(Element root, String nodeName) {
      String textContent = null;
      NodeList elementsByTagName = root.getElementsByTagName(nodeName);
      if (elementsByTagName.getLength() > 0) {
         textContent = elementsByTagName.item(0).getTextContent();
      }
      return textContent;
   }

   private Document parseXML(InputStream stream) throws Exception {
      DocumentBuilderFactory objDocumentBuilderFactory = null;
      DocumentBuilder objDocumentBuilder = null;
      Document doc = null;
      try {
         objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
         objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();

         doc = objDocumentBuilder.parse(stream);
      } catch (Exception ex) {
         throw ex;
      }

      return doc;
   }

   private void handleDevicePost(HttpServletRequest req, HttpServletResponse resp) throws IOException, JsonGenerationException,
         JsonMappingException {

      String devId = req.getHeader(Roku.X_ROKAGRAM_RESERVED_DEV_UNIQUE_ID);

      if (!Strings.isNullOrEmpty(devId)) {

         boolean newRoku = false;
         Key<RokuEntity> rokuKey = Key.create(RokuEntity.class, devId);

         RokuEntity roku = DAO.ofy().load().key(rokuKey).now();

         if (roku == null) {
            roku = new RokuEntity();
            newRoku = true;
         }

         if (StringUtils.isEmpty(roku.getCid())) {
            // create a client ID for Google Analytics
            String uuid = UUID.randomUUID().toString();
            roku.setCid(uuid);
         }

         roku.setDeviceId(devId);

         GaeClientLocation location = updateDeviceFromHeaders(req, roku);
         roku.getIncrementStarts();

         List<RegistryEntity> regEntities = handleDeviceRegistry(req, roku.getDeviceId());

         // set a few valued based on registry
         setValuesFromRegistry(roku, regEntities);

         List<Object> entitiesToSave = new ArrayList<Object>(regEntities.size() + 1);
         entitiesToSave.addAll(regEntities);
         entitiesToSave.add(roku);

         // SAVE
         DAO.ofy().save().entities(entitiesToSave).now();

         TopLevelResponse respObj = createTopLevelResponse(newRoku, roku, location);

         ServletUtils.writeJsonMetadataResponse(req, resp, respObj);

         if (!newRoku) {
            log.info("Queueing device rollup task for existing device: " + roku.getDeviceId());
            DeviceRollupTask deferredTask = new DeviceRollupTask(roku.getDeviceId());
            Queue queue = QueueFactory.getDefaultQueue();
            queue.addAsync(TaskOptions.Builder.withPayload(deferredTask));
         }
      }
   }

   private void setValuesFromRegistry(RokuEntity roku, List<RegistryEntity> regEntities) {
      for (RegistryEntity regEntity : regEntities) {
         if (StringUtils.equals(regEntity.getSection(), "feedfm")) {
            if (StringUtils.equals(regEntity.getKey(), "client_id")) {
               roku.setFeedFmClientId(regEntity.getValue());
            }
         }
         if (StringUtils.equals(regEntity.getSection(), "default")) {
            if (StringUtils.equals(regEntity.getKey(), "radio")) {
               roku.setRadio(Boolean.parseBoolean(regEntity.getValue()));
            }

            if (StringUtils.equals(regEntity.getKey(), "expired")) {
               roku.setExpired(Boolean.parseBoolean(regEntity.getValue()));
            }
         }

      }
   }

   private TopLevelResponse createTopLevelResponse(boolean newRoku, RokuEntity roku, GaeClientLocation location) {
      TopLevelResponse respObj = new TopLevelResponse();

      respObj.getLocation().setCity(location.getCityCapitalized());
      respObj.getLocation().setRegion(location.getRegionCapitalized());
      respObj.getLocation().setCountry(location.getCountry());
      String cityLatLong = location.getCityLatLong();
      if (!Strings.isNullOrEmpty(cityLatLong)) {
         String[] latlongParts = cityLatLong.split(",");
         if (latlongParts.length == 2) {
            respObj.getLocation().setLatitude(latlongParts[0]);
            respObj.getLocation().setLongitude(latlongParts[1]);
         }
      }

      respObj.getInstagram().setClient_id(Instagram.getClientId());

      respObj.setFeedfm(new FeedFmSettingsEntity());

      if (newRoku) {
         respObj.setTrialDays(5);
      } else {
         respObj.setTrialDays(null);
      }

      // if (Utils.isDevServer()) {
      // respObj.setResetTrial(false);
      // respObj.setRescindTrial(false);
      // // respObj.getFeedfm().setBasicAuth("denied");
      // // respObj.getInstagram().setClient_id("denied");
      // // respObj.getFeedfm().setChangeStation(true);
      // // try {
      // // Thread.sleep(4000L);
      // // } catch (InterruptedException e) {
      // // // TODO Auto-generated catch block
      // // e.printStackTrace();
      // // }
      // } else {
      respObj.setResetTrial(roku.isResetTrial());
      respObj.setRescindTrial(roku.isRescindTrial());
      // }

      if (roku.getDeviceId().equalsIgnoreCase("12C1AD036384")) {
         RegistryModification regmod = new RegistryModification();
         // // default hasBrowsed true
         regmod.setAction("write");
         regmod.setSection("feedfm");
         regmod.setKey("basicAuth");
         // was:
         // YjdiZDVhNDFiZjNiNjJhYWQzNTI2MjdmMzY4YjRjOTQyNDI4ZGQwYjpkZTNlZTQ4MWM5ODcyMjYyN2VjNWM5MGYwMWM3ZmU2ZmU3MTM2YjI5
         regmod.setValue("invalid-auth");
         respObj.getRegmods().add(regmod);
         log.warning("Modifying reg for device:" + roku + ", mod:" + regmod);
      }

      respObj.getGa().setCid(roku.getCid());

      respObj.getInstadaily()[0] = new InstaDailyDto("SelfieSunday", "Embrace the modern self portrait");

      respObj.getInstadaily()[1] = new InstaDailyDto("ManicMonday", "Duty calls, so booty crawls");
      // respObj.getInstadaily()[1,new InstaDailyDto("ManicureMonday",
      // "Fancy Nails"));
      respObj.getInstadaily()[2] = new InstaDailyDto("TuesdayTransformation", "Before and after");
      // respObj.getInstadaily()[2,new InstaDailyDto("WednesdayWisdom",
      // "Mid-week wisdom"));
      respObj.getInstadaily()[3] = new InstaDailyDto("WellnessWednesday", "Well?");
      respObj.getInstadaily()[4] = new InstaDailyDto("ThursdayThrowback", "Throwback Thursday");
      respObj.getInstadaily()[5] = new InstaDailyDto("FridayFunday", "Gotta get down on Friday");
      respObj.getInstadaily()[6] = new InstaDailyDto("Caturday", "Meow!");

      List<WhpEntity> wpaList = DAO.getWpaList();
      WhpEntity first = wpaList.get(0);
      respObj.getWeekendHashTagProject().setFeaturedTag(first.getTag());
      respObj.getWeekendHashTagProject().setDescription("#" + first.getCapitalizedTag());

      return respObj;
   }

   private GaeClientLocation updateDeviceFromHeaders(HttpServletRequest req, RokuEntity roku) {
      String users = req.getHeader(Roku.X_ROKAGRAM_RESERVED_USERS);
      roku.getUsers().clear();
      if (!Strings.isNullOrEmpty(users)) {
         for (String uid : Splitter.on(' ').trimResults().split(users)) {

            Ref<UserEntity> userRef = Ref.create(Key.create(UserEntity.class, uid));
            roku.getUsers().add(userRef);

         }
      }

      GaeClientLocation location = new GaeClientLocation(req);
      roku.setCity(location.getCityCapitalized());
      roku.setCountry(location.getCountry());
      roku.setRegion(location.getRegion());
      roku.setLatlong(location.getCityLatLongAsGeoPt());

      roku.setDisplayMode(req.getHeader(Roku.X_ROKAGRAM_RESERVED_DEV_DISPLAY_MODE));
      roku.setFirmware(req.getHeader(Roku.X_ROKAGRAM_RESERVED_FIRMWARE_VERSION));
      roku.setChannelVersion(req.getHeader(Roku.X_ROKAGRAM_RESERVED_CHANNEL_VERSION));
      roku.setModel(req.getHeader(Roku.X_ROKAGRAM_RESERVED_MODEL));

      String email = req.getHeader(Roku.X_ROKAGRAM_RESERVED_EMAIL);
      if (email != null) {
         email = email.toLowerCase();
      }
      roku.setEmail(email);
      roku.setItemCode(req.getHeader(Roku.X_ROKAGRAM_RESERVED_ITEM_CODE));

      String daysElapsedHeader = req.getHeader(Roku.X_ROKAGRAM_RESERVED_TRIAL_DAYS);
      if (daysElapsedHeader != null) {
         try {
            roku.setDaysElapsed(Integer.parseInt(daysElapsedHeader));
         } catch (NumberFormatException nfe) {
            log.warning("NumberFormatException: daysElapsedHeader:" + daysElapsedHeader);
         }
      }

      // String audioMinutesHeader =
      // req.getHeader(Roku.X_ROKAGRAM_RESERVED_AUDIO_MINUTES);
      // if (audioMinutesHeader != null) {
      // try {
      // roku.setAudioMinutes(Integer.parseInt(audioMinutesHeader.trim()));
      // } catch (NumberFormatException nfe) {
      // }
      // }
      return location;
   }

   private List<RegistryEntity> handleDeviceRegistry(HttpServletRequest req, String deviceId) {
      List<RegistryEntity> ret = new ArrayList<RegistryEntity>();
      try {
         Document doc = parseXML(req.getInputStream());
         Element root = doc.getDocumentElement();

         NodeList clientNodes = root.getElementsByTagName("section");

         for (int i = 0; i < clientNodes.getLength(); i++) {
            Element sectionNode = (Element) clientNodes.item(i);

            String sectionName = sectionNode.getAttribute("name");

            NodeList entryNodes = sectionNode.getElementsByTagName("entry");

            for (int j = 0; j < entryNodes.getLength(); j++) {

               RegistryEntity regEntity = new RegistryEntity();

               Element entryNode = (Element) entryNodes.item(j);
               String key = entryNode.getAttribute("key");
               String value = entryNode.getAttribute("value");

               regEntity.setId(deviceId + "/" + sectionName + "/" + key);
               regEntity.setDevice(Ref.create(Key.create(RokuEntity.class, deviceId)));
               regEntity.setSection(sectionName);
               regEntity.setKey(key);
               regEntity.setValue(value);

               ret.add(regEntity);
            }

         }

      } catch (IOException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      }

      return ret;
   }

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      Roku.handleDeviceLinkage(req, resp);
   }

}
