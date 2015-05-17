package com.rokagram.backend.roku;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Query;
import com.rokagram.backend.Alert;
import com.rokagram.backend.Constants;
import com.rokagram.backend.GaeClientLocation;
import com.rokagram.backend.Instagram;
import com.rokagram.backend.RegUtils;
import com.rokagram.backend.ServletUtils;
import com.rokagram.backend.Utils;
import com.rokagram.backend.dao.DAO;
import com.rokagram.backend.tasks.RokaRequestTask;
import com.rokagram.entities.RokuEntity;
import com.rokagram.entities.TokenEntity;
import com.rokagram.entities.UserEntity;

public class Roku {

	public static final Logger log = Logger.getLogger(Roku.class.getName());

	public static final String X_ROKAGRAM_RESERVED_MODEL = "X-Rokagram-Reserved-Model";
	private static final String X_ROKAGRAM_RESERVED_DISPLAY_MODE = "X-Rokagram-Reserved-Display-Mode";

	public static final String X_ROKAGRAM_RESERVED_FIRMWARE_VERSION = "X-Rokagram-Reserved-Firmware-Version";
	public static final String X_ROKAGRAM_RESERVED_CHANNEL_VERSION = "X-Rokagram-Reserved-Channel-Version";
	public static final String X_ROKAGRAM_RESERVED_DEV_UNIQUE_ID = "X-Rokagram-Reserved-Dev-Unique-Id";
	public static final String X_ROKAGRAM_RESERVED_DEV_DISPLAY_MODE = "X-Rokagram-Reserved-Display-Mode";

	// public static final String X_ROKAGRAM_RESERVED_AUDIO_MINUTES =
	// "X-Rokagram-Reserved-Audio-Minutes";
	public static final String X_ROKAGRAM_RESERVED_EMAIL = "X-Rokagram-Reserved-Email";

	public static final String X_ROKAGRAM_RESERVED_CURRENT_USER = "X-Rokagram-Reserved-CurrentUser";
	public static final String X_ROKAGRAM_RESERVED_USERS = "X-Rokagram-Reserved-Users";

	public static final String X_ROKAGRAM_RESERVED_ITEM_CODE = "X-Rokagram-Reserved-ItemCode";
	public static final String X_ROKAGRAM_RESERVED_TRIAL_DAYS = "X-Rokagram-Reserved-TrialDays";

	public static final String USER_POSTER_SCREEN = "UserPosterScreen";
	public static final String SLIDESHOW = "SlideShow";
	public static final String USERSEARCH_SCREEN = "UserSearchScreen";

	public static Map<String, String> modelMap = new HashMap<String, String>();
	static {
		modelMap.put("4200X", "Roku 3");
		modelMap.put("3050X", "Roku 2 XD");
		modelMap.put("3100X", "Roku 2 XS");
		modelMap.put("2050N", "Roku XD");
		modelMap.put("2050X", "Roku XD");
		modelMap.put("2500X", "Roku HD");
		modelMap.put("2710X", "Roku 1");
		modelMap.put("2400X", "Roku LT");

	}

	public static void updateDeviceFromRequest(HttpServletRequest req) {
		String devId = req.getHeader(Roku.X_ROKAGRAM_RESERVED_DEV_UNIQUE_ID);
		if (!Strings.isNullOrEmpty(devId)) {
			RokuEntity roku = DAO.ofy().load().type(RokuEntity.class).id(devId).now();
			if (roku != null) {
				boolean updated = false;
				{
					String users = req.getHeader(Roku.X_ROKAGRAM_RESERVED_USERS);
					if (!Strings.isNullOrEmpty(users)) {
						for (String uid : Splitter.on(' ').trimResults().split(users)) {
							Ref<UserEntity> userRef = Ref.create(Key.create(UserEntity.class, uid));
							if (!roku.getUsers().contains(userRef)) {
								roku.getUsers().add(userRef);
								updated = true;
								log.info("Adding uid: " + uid + ", to device: " + devId);
							}
						}
					}
				}
				{
					String itemCode = req.getHeader(Roku.X_ROKAGRAM_RESERVED_ITEM_CODE);
					if (!Strings.isNullOrEmpty(itemCode)) {

						if (!StringUtils.equals(itemCode, roku.getItemCode())) {
							roku.setItemCode(itemCode);
							updated = true;
							log.info("Updating itemCode: " + itemCode + " on device: " + devId);
						}
					}
				}
				{
					String channelVersion = req.getHeader(Roku.X_ROKAGRAM_RESERVED_CHANNEL_VERSION);
					if (!Strings.isNullOrEmpty(channelVersion)) {

						if (!StringUtils.equals(channelVersion, roku.getChannelVersion())) {
							roku.setChannelVersion(channelVersion);
							updated = true;
							log.info("Updating channelVersion: " + channelVersion + " on device: " + devId);
						}
					}
				}
				{
					String email = req.getHeader(Roku.X_ROKAGRAM_RESERVED_EMAIL);
					if (!Strings.isNullOrEmpty(email)) {
						email = email.toLowerCase();
						if (!StringUtils.equals(email, roku.getEmail())) {
							String was = roku.getEmail();
							if (was == null) {
								was = "null";
							}
							roku.setEmail(email);
							updated = true;

							RokaRequestTask deferredTask = new RokaRequestTask(roku.getDeviceId(), updated);
							Queue queue = QueueFactory.getDefaultQueue();
							queue.addAsync(TaskOptions.Builder.withPayload(deferredTask));

							log.info("Updating email: " + email + " on device: " + devId + ", was: " + was);
						}
					}
				}

				if (updated) {
					DAO.ofy().save().entity(roku);
				}

			}
		}
	}

	public static void handleDeviceLinkage(HttpServletRequest req, HttpServletResponse resp)
			throws JsonGenerationException, JsonMappingException, IOException {

		String codeParam = req.getParameter("code");

		TokenEntity token = new TokenEntity();
		if (codeParam == null) {

			token.setDeviceId(req.getHeader(X_ROKAGRAM_RESERVED_DEV_UNIQUE_ID));
			Query<TokenEntity> qTokensByDeviceId = DAO.createQueryByDeviceId(token);

			List<TokenEntity> entitiesToPurge = new ArrayList<TokenEntity>();

			for (TokenEntity existingTokenForThisDevice : qTokensByDeviceId) {
				if (existingTokenForThisDevice.getUserRef() == null) {
					entitiesToPurge.add(existingTokenForThisDevice);
				}
			}

			if (entitiesToPurge.size() > 0) {
				Utils.info("Purging " + entitiesToPurge.size() + " tokens");
				DAO.ofy().delete().entities(entitiesToPurge);
			}

			token.setDisplayMode(req.getHeader(X_ROKAGRAM_RESERVED_DISPLAY_MODE));
			token.setModel(req.getHeader(X_ROKAGRAM_RESERVED_MODEL));

			GaeClientLocation clientLocation = new GaeClientLocation(req);
			clientLocation.setDevServerDummyValues();

			token.setCountry(clientLocation.getCountry());
			token.setRegion(clientLocation.getRegionCapitalized());
			token.setCity(clientLocation.getCityCapitalized());

			TokenEntity highestToken = DAO.ofy().load().type(TokenEntity.class).orderKey(true).first().now();

			long nextId = 1;
			if (highestToken != null) {
				nextId = highestToken.getId() + 1;
			}
			log.info("Next ID: " + nextId);
			token.setId(nextId);

			String userToken = RegUtils.getToken(token.getId());
			token.setUserToken(userToken);
			DAO.ofy().save().entity(token).now();
			log.info(token.toString());

		} else {

			token = DAO.loadToken(codeParam);

		}

		RegResponse regResponse = new RegResponse();

		String code = "error";
		UserEntity user = null;
		if (token != null) {
			code = token.getUserToken();
			user = token.getUser();
		}
		regResponse.setCode(code);

		if (user != null) {
			regResponse.setLinked(true);

			// if (Utils.isDevServer()) {
			// user.setFull_name("Sean Gothman");
			// user.setUsername("seangothman");
			// user.setAccess_token("9696076.b13119e.b01cb816354748fcaad1dabafdcbd162");
			// user.setId("9696076");
			// user.setProfile_picture("http://images.ak.instagram.com/profiles/profile_9696076_75sq_1359744190.jpg");
			// }

			regResponse.setUser(user);
		}

		ServletUtils.writeJsonMetadataResponse(req, resp, regResponse);

	}

	public static void handleDeviceCodeForm(HttpServletRequest req, HttpServletResponse resp, String rokuCode)
			throws IOException {

		req.getSession().setAttribute(Constants.ROKUCODE, rokuCode);
		String authorizationUrl = Instagram.getAuthorizationUrl(req, rokuCode);
		Utils.info("Redirecting to Instagram: " + authorizationUrl);
		resp.sendRedirect(authorizationUrl);
	}

	public static boolean createDeviceUserLinkage(HttpServletRequest req, String rokuCode, UserEntity user) {

		boolean ret = false;
		TokenEntity token = null;

		token = DAO.loadToken(rokuCode);
		if (token != null) {

			// purge any old ones if they exist
			Query<TokenEntity> tokens = DAO.createQueryByDeviceId(token);
			List<TokenEntity> tokensToDelete = new ArrayList<TokenEntity>();
			for (TokenEntity tokenEntity : tokens) {
				boolean shouldDelete = false;
				if (!tokenEntity.getUserToken().equals(rokuCode)) {

					if (tokenEntity.getUserRef() == null) {
						shouldDelete = true;
					} else if (tokenEntity.getUserRef().getKey().equals(Key.create(UserEntity.class, user.getId()))) {
						shouldDelete = true;
					}

				}
				if (shouldDelete) {
					Utils.warn("Deleting token:" + tokenEntity);
					tokensToDelete.add(tokenEntity);
				}
			}

			if (tokensToDelete.size() > 0) {
				DAO.ofy().delete().entities(tokensToDelete);
			}

			token.setUser(user);

			DAO.ofy().save().entities(user, token);
			ret = true;
			Alert.setSuccessAlert(req, "Congratulations!",
					"You have successfully linked your Roku player to this account");
			log.info("linked " + token.getDeviceId() + " and user:" + user.toString());
		} else {
			if (StringUtils.equals(rokuCode, "0756")) {
				ret = true;
				Alert.setSuccessAlert(req, "Test!", "Test success");
			} else {
				Alert.setErrorAlert(req, "Oops!", "There was a problem with this code.  Please try again.");
			}

		}
		return ret;
	}

}
