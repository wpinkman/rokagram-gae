package com.rokagram.backend.admin.web;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.utils.SystemProperty;
import com.google.common.base.Splitter;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.rokagram.backend.Instagram;
import com.rokagram.backend.ServletUtils;
import com.rokagram.backend.UriBuilder;
import com.rokagram.backend.Utils;
import com.rokagram.backend.dao.DAO;
import com.rokagram.backend.dao.PagedQuery;
import com.rokagram.backend.dto.LogTypeFilterDao;
import com.rokagram.backend.tasks.DeviceRollupTask;
import com.rokagram.backend.tasks.FindPopularPostsTask;
import com.rokagram.backend.tasks.ResolveFeaturedPostTask;
import com.rokagram.backend.tasks.WhpInstaTask;
import com.rokagram.backend.tasks.WhpTumblrTask;
import com.rokagram.entities.ApiClientEntity;
import com.rokagram.entities.EntityBase;
import com.rokagram.entities.LogEntity;
import com.rokagram.entities.RegistryEntity;
import com.rokagram.entities.RokuEntity;
import com.rokagram.entities.SystemSettingsEntity;
import com.rokagram.entities.SystemSettingsEnum;
import com.rokagram.entities.TumblrPhoto;
import com.rokagram.entities.TumblrPostEntity;
import com.rokagram.entities.UserEntity;
import com.rokagram.entities.WhpEntity;

@SuppressWarnings("serial")
public class AdminServlet extends HttpServlet {

	public static final Logger log = Logger.getLogger(AdminServlet.class.getName());

	private static com.google.appengine.api.users.UserService googleUserService = com.google.appengine.api.users.UserServiceFactory
			.getUserService();
	private static URLFetchService fetcherService = com.google.appengine.api.urlfetch.URLFetchServiceFactory
			.getURLFetchService();

	private static ObjectMapper mapper = new ObjectMapper();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		req.setAttribute("adminLogoutUrl", googleUserService.createLogoutURL("/"));

		String applicationversion = "1.234 (dev)";
		if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
			applicationversion = SystemProperty.applicationVersion.get();
		}
		req.setAttribute("version", applicationversion);

		String googleAdminHref = createGoogleAdminHref(req);
		req.setAttribute("googleAdminHref", googleAdminHref);

		if (Utils.isDevServer()) {
			req.setAttribute("googleAnalyticsHref",
					"https://www.google.com/analytics/web/#realtime/rt-app-overview/a47056757w97210463p101342448");
		} else {
			req.setAttribute("googleAnalyticsHref",
					"https://www.google.com/analytics/web/#realtime/rt-app-overview/a47056757w97031312p101188201");

		}
		req.setAttribute("rokuDevHref", "https://developer.roku.com/developer");

		String method = req.getMethod();
		List<String> segments = new ArrayList<String>();
		req.setAttribute("segments", segments);

		addDatastoreStats(req);

		String pathInfo = req.getPathInfo();
		if (pathInfo == null) {
			if (method.equalsIgnoreCase("GET")) {
				doAdminGet(req, resp);
			}

		} else {
			for (String segment : Splitter.on('/').omitEmptyStrings().trimResults().split(pathInfo)) {
				segments.add(segment);
			}

			handleSubAdmin(req, resp, segments, 0);
		}

	}

	private void addDatastoreStats(HttpServletRequest req) {

		try {

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			com.google.appengine.api.datastore.Query statQuery = new com.google.appengine.api.datastore.Query(
					"__Stat_Total__");
			Entity globalStat = datastore.prepare(statQuery).asSingleEntity();
			Long totalBytes = 0L;

			if (globalStat != null) {
				totalBytes = (Long) globalStat.getProperty("bytes");
			}

			req.setAttribute("totalBytes", NumberFormat.getNumberInstance(Locale.US).format(totalBytes));

			Long rokuCount = 0L;
			Long userCount = 0L;
			Long rokuTime = 0L;
			Long userTime = 0L;
			com.google.appengine.api.datastore.Query statKingQuery = new com.google.appengine.api.datastore.Query(
					"__Stat_Kind__");
			PreparedQuery prepare = datastore.prepare(statKingQuery);
			if (prepare != null) {
				Iterator<Entity> asIterator = prepare.asIterator();

				while (asIterator.hasNext()) {
					Entity statKindStat = asIterator.next();
					if (statKindStat != null) {
						com.google.appengine.api.datastore.Key key = statKindStat.getKey();
						String name = key.getName();
						Long count = 0L;
						Long ts = 0L;

						for (Entry<String, Object> entry : statKindStat.getProperties().entrySet()) {
							String propKey = entry.getKey();
							Object value = entry.getValue();

							if (propKey.equals("count")) {
								count = (Long) value;
							}
							if (propKey.equals("timestamp")) {
								Date timestamp = (Date) value;
								ts = timestamp.getTime();
							}

						}
						if (name.equals("RokuEntity")) {
							if (ts > rokuTime) {
								rokuCount = count;
								rokuTime = ts;
							}
						}
						if (name.equals("UserEntity")) {
							if (ts > userTime) {
								userCount = count;
								userTime = ts;
							}
						}
					} else {
						log.warning("statKindStat entity is null");
					}
				}

				req.setAttribute("totalDevices", NumberFormat.getNumberInstance(Locale.US).format(rokuCount));
				req.setAttribute("totalUsers", NumberFormat.getNumberInstance(Locale.US).format(userCount));
			} else {
				log.warning("prepare is null");
			}
		} catch (Exception ex) {
			log.warning("Problem extracting stats");
			log.warning(ex.getMessage());
		}
	}

	private void handleSubAdmin(HttpServletRequest req, HttpServletResponse resp, List<String> segments, int index)
			throws IOException {
		String segment = segments.get(index);

		index++;
		if (segment.equalsIgnoreCase("devices")) {
			handleDevices(req, resp, segments, index);
		} else if (segment.equalsIgnoreCase("users")) {
			handleUsers(req, resp, segments, index);
		} else if (segment.equalsIgnoreCase("logs")) {
			handleLogs(req, resp, segments, index);
		} else if (segment.equalsIgnoreCase("insta")) {
			handleInsta(req, resp, segments, index);
		} else if (segment.equalsIgnoreCase("whp")) {
			handleWhp(req, resp, segments, index);
		} else if (segment.equalsIgnoreCase("settings")) {
			handleSettings(req, resp, segments, index);
		}

	}

	private void handleWhp(HttpServletRequest req, HttpServletResponse resp, List<String> segments, int offset)
			throws IOException {
		int numSegments = segments.size();
		String method = req.getMethod();
		if (numSegments > offset) {
			String whptag = segments.get(offset).toLowerCase();
			WhpEntity whp = DAO.ofy().load().type(WhpEntity.class).id(whptag).now();

			if (method.equalsIgnoreCase("GET")) {

				// List<IgPostEntity> popularPosts =
				// DAO.ofy().load().type(IgPostEntity.class).filter("whptag",
				// whptag)
				// .order("-likes").limit(100).list();
				// req.setAttribute("popularPosts", popularPosts);

				req.setAttribute("whp", whp);
				Utils.resolveAndRender("admin/whp", req, resp);
			} else {
				handleWhpPost(req, resp, whptag, whp);
			}
		} else {
			if (method.equalsIgnoreCase("GET")) {
				List<WhpEntity> whps = DAO.getWpaList();
				req.setAttribute("whps", whps);
				Utils.resolveAndRender("admin/whps", req, resp);
			} else {
				String action = req.getParameter("action");
				if (action != null) {
					if (action.equals("tumble")) {
						String tumblrid = req.getParameter("tumblrid");
						WhpTumblrTask deferredTask = new WhpTumblrTask();
						deferredTask.setPostId(tumblrid);
						QueueFactory.getDefaultQueue().addAsync(TaskOptions.Builder.withPayload(deferredTask));
					}
					if (action.equals("insta")) {
						WhpInstaTask deferredTask = new WhpInstaTask();
						QueueFactory.getDefaultQueue().addAsync(TaskOptions.Builder.withPayload(deferredTask));
					}
					if (action.equals("featured")) {

						for (WhpEntity whpe : DAO.ofy().load().type(WhpEntity.class)) {
							System.out.println("Enqueue'ing for #" + whpe.getTag());
							enqueueFeaturedPostTasks(whpe);
						}
					}
					if (action.equals("interate")) {

						System.out.println("Here goes nuthin!!");
						WhpEntity prefWhp = null;
						for (WhpEntity whp : DAO.getWpaList()) {

							System.out.println(whp.toString());
							if (prefWhp != null) {

								long thisAnnounce = whp.getAnnounceDate().getTime() / 1000L;
								long lastAnnounce = prefWhp.getAnnounceDate().getTime() / 1000L;

								double dblIntervalDays = (double) (thisAnnounce - lastAnnounce) / (double) 86400L;
								long intervalDays = Math.abs(Math.round(dblIntervalDays));

								if (intervalDays != 7) {
									System.out.println(intervalDays + " is a strange interval don't you think");
								}

								if (whp.getAnnounceDate() != null && whp.getFeatureDate() != null
										&& whp.getAnnounceTumblr() != null && whp.getFeatureTumblr() != null
										&& whp.getAnnounceInsta() != null && whp.getFeatureInsta() != null) {

								} else {
									System.out.println("something is a strange interval don't you think");
								}

							}

							prefWhp = whp;

						}
					}
				}

				resp.sendRedirect(req.getRequestURI());
			}
		}
	}

	private void handleSettings(HttpServletRequest req, HttpServletResponse resp, List<String> segments, int offset)
			throws IOException {
		int numSegments = segments.size();
		String method = req.getMethod();
		if (numSegments > offset) {
		} else {
			if (method.equalsIgnoreCase("GET")) {
				req.setAttribute("settings", DAO.ofy().load().type(SystemSettingsEntity.class).list());
				req.setAttribute("options", SystemSettingsEnum.values());

				Utils.resolveAndRender("admin/settings", req, resp);
			} else {
				String action = req.getParameter("action");
				String value = req.getParameter("value");
				String name = req.getParameter("name");
				if (action.equals("delete")) {
					DAO.ofy().delete().type(SystemSettingsEntity.class).id(name).now();
				} else {
					SystemSettingsEntity sse = new SystemSettingsEntity();
					sse.setName(name);
					sse.setValue(value);
					DAO.ofy().save().entity(sse).now();
				}
				resp.sendRedirect(req.getRequestURI());
			}
		}
	}

	private void handleWhpPost(HttpServletRequest req, HttpServletResponse resp, String whptag, WhpEntity whpe)
			throws IOException {
		String action = req.getParameter("action");
		if (action != null) {

			if (action.equals("bldfp")) {

				enqueueFeaturedPostTasks(whpe);

			}
			if (action.equals("bldpop")) {
				FindPopularPostsTask fppt = new FindPopularPostsTask(whpe.getTag());
				QueueFactory.getDefaultQueue().addAsync(TaskOptions.Builder.withPayload(fppt));
			}

		}
		resp.sendRedirect(req.getRequestURI());
	}

	private void enqueueFeaturedPostTasks(WhpEntity whpe) {
		TumblrPostEntity announceTumblr = whpe.getAnnounceTumblr();
		if (announceTumblr != null) {
			for (TumblrPhoto ap : announceTumblr.getPhotos()) {

				ResolveFeaturedPostTask rfpt = new ResolveFeaturedPostTask(whpe.getTag(), ap.getShortcode());
				QueueFactory.getDefaultQueue().addAsync(TaskOptions.Builder.withPayload(rfpt));

			}
		}
		TumblrPostEntity featureTumblr = whpe.getFeatureTumblr();
		if (featureTumblr != null) {
			for (TumblrPhoto ap : featureTumblr.getPhotos()) {
				ResolveFeaturedPostTask rfpt = new ResolveFeaturedPostTask(whpe.getTag(), ap.getShortcode());
				QueueFactory.getDefaultQueue().addAsync(TaskOptions.Builder.withPayload(rfpt));
			}
		}
	}

	private void handleInsta(HttpServletRequest req, HttpServletResponse resp, List<String> segments, int index)
			throws IOException {
		String instareq = req.getParameter("instareq");
		if (instareq != null) {

			req.setAttribute("instareq", instareq);

			HTTPRequest apiRequest = new HTTPRequest(new URL(instareq));

			HTTPResponse response = fetcherService.fetch(apiRequest);
			req.setAttribute("responseCode", response.getResponseCode());
			if (response.getContent().length > 0) {

				JsonNode rootNode = mapper.readTree(response.getContent());
				req.setAttribute("rootNode", rootNode);

				mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
				String json = mapper.writeValueAsString(rootNode);

				json = json.replaceAll("\"(profile_picture|url|link)\"\\s+:\\s+\"(\\S+)\"",
						"\"$1\" : \"<a href=\"$2\">$2</a>\"");

				req.setAttribute("json", json);
			}

		}
		Utils.resolveAndRender("admin/insta", req, resp);
	}

	private void handleLogs(HttpServletRequest req, HttpServletResponse resp, List<String> segments, int index)
			throws IOException {
		PagedQuery<LogEntity> pqLogs = new PagedQuery<LogEntity>(LogEntity.class);
		pqLogs.doQuery(req);

		addLogFilters(req);
		Utils.resolveAndRender("admin/logs", req, resp);

	}

	private void handleUsers(HttpServletRequest req, HttpServletResponse resp, List<String> segments, int index)
			throws IOException {
		if (segments.size() > 1) {
			String uid = segments.get(1);
			UserEntity user = DAO.ofy().load().type(UserEntity.class).id(uid).now();

			if (user == null) {
				user = DAO.ofy().load().type(UserEntity.class).filter("username", uid).first().now();
			}
			String method = req.getMethod();
			if (method.equalsIgnoreCase("GET")) {

				String editUrl = createEditUrl(user);
				if (!Utils.isDevServer()) {
					req.setAttribute("edit-url", editUrl);
				}

				if (user != null) {
					PagedQuery<LogEntity> pqLogs = new PagedQuery<LogEntity>(LogEntity.class);
					pqLogs.doQuery(req, null, user.getId());
				} else {
					try {
						Long.parseLong(uid);
					} catch (NumberFormatException nfe) {
						user = new UserEntity();
						user.setUsername(uid);

					}
				}

				req.setAttribute("user", user);
				Utils.resolveAndRender("admin/user", req, resp);
			} else if (method.equalsIgnoreCase("POST")) {
				user = DAO.ofy().load().type(UserEntity.class).id(uid).now();
				Instagram.updateUserFromInstagram(user);
				resp.sendRedirect(req.getRequestURI());
			}

		} else {
			PagedQuery<UserEntity> pqUsers = new PagedQuery<UserEntity>(UserEntity.class);
			pqUsers.setOrder("-added");
			pqUsers.doQuery(req);

			Utils.resolveAndRender("admin/users", req, resp);
		}
	}

	private String createEditUrl(EntityBase ofyEntity) {
		String editUrl = "#";

		UriBuilder b = UriBuilder.fromUri("https://appengine.google.com/datastore/edit");
		b.queryParam("app_id", "s~" + SystemProperty.applicationId.get());
		b.queryParam("version_id", SystemProperty.version.get());

		if (ofyEntity != null) {
			// work around a little bug where they call the lifecycle stuff even
			// though they don't actually save it
			Date modified = ofyEntity.getModified();
			Entity entity = DAO.ofy().save().toEntity(ofyEntity);
			ofyEntity.setModified(modified);

			String keyString = KeyFactory.keyToString(entity.getKey());
			b.queryParam("key", keyString);
			editUrl = b.toString();
		}

		return editUrl;
	}

	@SuppressWarnings("rawtypes")
	private String createEditUrl(Key key) {
		String editUrl = "#";

		UriBuilder b = UriBuilder.fromUri("https://appengine.google.com/datastore/edit");
		b.queryParam("app_id", "s~" + SystemProperty.applicationId.get());
		b.queryParam("version_id", SystemProperty.version.get());

		String keyString = KeyFactory.keyToString(key.getRaw());
		b.queryParam("key", keyString);
		editUrl = b.toString();

		if (Utils.isDevServer()) {
			editUrl = "#";
		}

		return editUrl;
	}

	private void handleDevices(HttpServletRequest req, HttpServletResponse resp, List<String> segments, int index)
			throws IOException {
		if (segments.size() > 1) {
			String rid = segments.get(1);

			String method = req.getMethod();
			if (method.equalsIgnoreCase("GET")) {
				if (segments.size() > 2) {
					String segment = segments.get(2);
					if (segment.equalsIgnoreCase("registry")) {
						handleDeviceRegistryGet(req, resp, rid);
					}

				} else {
					handleDeviceGet(req, resp, rid);
				}
			} else {
				handleDevicePost(req, resp, rid);
			}

		} else {
			PagedQuery<RokuEntity> pqRokus = new PagedQuery<RokuEntity>(RokuEntity.class);
			pqRokus.doQuery(req);
			Utils.resolveAndRender("admin/devices", req, resp);
		}
	}

	private void handleDeviceRegistryGet(HttpServletRequest req, HttpServletResponse resp, String rid)
			throws IOException {
		Key<RokuEntity> rokuKey = Key.create(RokuEntity.class, rid);

		Query<RegistryEntity> query = DAO.ofy().load().type(RegistryEntity.class);
		query = query.filter("device", rokuKey);

		List<RegistryEntity> entries = query.list();
		Collections.sort(entries);
		req.setAttribute("entries", entries);
		req.setAttribute("deviceId", rid);

		Utils.resolveAndRender("admin/registry", req, resp);

	}

	private void handleDevicePost(HttpServletRequest req, HttpServletResponse resp, String rid) throws IOException {

		if (StringUtils.equalsIgnoreCase(req.getParameter("action"), "rescindTrial")) {
			RokuEntity device = DAO.ofy().load().type(RokuEntity.class).id(rid).now();
			device.setRescindTrial(!device.isRescindTrial());
			device.setIgnoreLifecycle(true);
			DAO.ofy().save().entity(device);
		}
		if (StringUtils.equalsIgnoreCase(req.getParameter("action"), "rollupTask")) {
			log.info("Queueing device rollup task for " + rid);
			DeviceRollupTask deferredTask = new DeviceRollupTask(rid);
			Queue queue = QueueFactory.getDefaultQueue();
			queue.addAsync(TaskOptions.Builder.withPayload(deferredTask));
		}

		resp.sendRedirect(req.getRequestURL().toString());
	}

	private void handleDeviceGet(HttpServletRequest req, HttpServletResponse resp, String rid) throws IOException,
			JsonGenerationException, JsonMappingException {

		Key<RokuEntity> rokuKey = Key.create(RokuEntity.class, rid);

		RokuEntity roku = DAO.ofy().load().key(rokuKey).now();
		if (roku != null) {
			req.setAttribute("device", roku);
			String createEditUrl = createEditUrl(rokuKey);
			roku.setEditUrl(createEditUrl);
		}

		if (StringUtils.equalsIgnoreCase(req.getParameter("format"), "json")) {
			ServletUtils.writeJsonMetadataResponse(req, resp, roku);
		} else {
			PagedQuery<LogEntity> pqLogs = new PagedQuery<LogEntity>(LogEntity.class);
			if (roku != null) {
				pqLogs.doQuery(req, roku.getDeviceId(), null);
			}

			addLogFilters(req);

			Utils.resolveAndRender("admin/device", req, resp);
		}
	}

	private void addLogFilters(HttpServletRequest req) {
		List<String> filters = new ArrayList<String>();
		filters.add("startup");
		filters.add("ffmstart");
		filters.add("store");
		filters.add("search");
		filters.add("springboard");
		filters.add("slideshow");
		filters.add("video");
		filters.add("radio");
		filters.add("error");

		List<LogTypeFilterDao> logFilters = new ArrayList<LogTypeFilterDao>();

		String reqUrl = req.getRequestURL().toString();
		UriBuilder builder = UriBuilder.fromUri(reqUrl);
		builder.removeQueryParam("type");

		LogTypeFilterDao logFilter = new LogTypeFilterDao("all", builder.toString(), req.getParameter("type") == null);
		logFilters.add(logFilter);

		for (String filter : filters) {

			builder = UriBuilder.fromUri(reqUrl);
			builder.replaceQueryParam("type", filter);

			logFilter = new LogTypeFilterDao(filter, builder.toString(), StringUtils.equals(filter,
					req.getParameter("type")));

			logFilters.add(logFilter);
		}

		req.setAttribute("logfilters", logFilters);
	}

	protected void doAdminGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		List<ApiClientEntity> clients = DAO.ofy().load().type(ApiClientEntity.class).list();
		req.setAttribute("clients", clients);

		Utils.resolveAndRender("admin/admin", req, resp);

	}

	private String createGoogleAdminHref(HttpServletRequest req) {
		String googleAdminHref = "#";
		if (Utils.isDevServer()) {
			String requestURI = req.getRequestURI();
			UriBuilder b = UriBuilder.fromUri(requestURI);
			b.host("localhost");
			b.port(req.getServerPort());
			b.scheme("http");
			b.replacePath("_ah/admin");
			googleAdminHref = b.build().toString();
		} else {
			String appId = SystemProperty.applicationId.get();
			UriBuilder b = UriBuilder.fromUri("https://appengine.google.com/dashboard");
			// ?&app_id=s~rknshare-devbld
			b.queryParam("app_id", "s~" + appId);
			googleAdminHref = b.build().toString();
		}
		return googleAdminHref;
	}

}
