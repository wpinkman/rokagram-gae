package com.rokagram.backend.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.rokagram.backend.Instagram;
import com.rokagram.backend.UriBuilder;
import com.rokagram.backend.dao.DAO;
import com.rokagram.entities.IgPostEntity;
import com.rokagram.entities.WhpEntity;

@SuppressWarnings("serial")
public class WhpFindUserPosts implements DeferredTask {
	public static final Logger log = Logger.getLogger(WhpTumblrTask.class.getName());
	private static URLFetchService fetcherService = com.google.appengine.api.urlfetch.URLFetchServiceFactory
			.getURLFetchService();

	private static ObjectMapper mapper = new ObjectMapper();

	private String tag;
	private String username;

	@Override
	public void run() {

		WhpEntity whp = DAO.ofy().load().type(WhpEntity.class).id(getTag()).now();
		if (whp != null) {
			String userid = userSearch();
			if (userid != null) {

				long minTs = whp.getAnnounceInsta().getCreated().getTime() / 1000L;
				long maxTs = whp.getFeatureInsta().getCreated().getTime() / 1000L;

				try {
					List<IgPostEntity> whpPosts = fetchPosts(userid, maxTs, minTs);

					// for (IgPostEntity igPostEntity : whpPosts) {
					// whp.addFeaturedPost(igPostEntity);
					// }

					// numFound = whpPosts.size();
					DAO.ofy().save().entities(whpPosts);
					DAO.ofy().save().entity(whp);

				} catch (MalformedURLException e) {
					log.warning(e.getMessage());
				}

			}
		}

	}

	private List<IgPostEntity> fetchPosts(String userid, Long maxTs, Long minTs) throws MalformedURLException {

		// System.out.println("depth " + depth);
		List<IgPostEntity> postEntities = new ArrayList<IgPostEntity>();

		String mediaRecentUrl = buildIgFeedUrl(userid, maxTs, minTs);

		System.out.println("apiUrl " + mediaRecentUrl);

		// if (depth++ > 10) {
		// System.out.println("Bailing after depth " + depth);
		// return;
		// }

		HTTPRequest request = new HTTPRequest(new URL(mediaRecentUrl));
		try {
			HTTPResponse response = fetcherService.fetch(request);
			int responseCode = response.getResponseCode();
			if (responseCode == 200) {

				JsonNode rootNode = mapper.readTree(response.getContent());
				JsonNode dataNode = rootNode.path("data");

				int code = rootNode.path("meta").path("code").asInt();

				if (code == 200) {

					// String nextUrl =
					// rootNode.path("pagination").path("next_url").asText();
					Long maxTsThisPage = null;

					for (JsonNode node : dataNode) {

						IgPostEntity postEntity = new IgPostEntity();
						postEntity.fillFromNode(node);

						maxTsThisPage = node.path("created_time").asLong();

						System.out.println(postEntity.getCreated() + " : " + maxTsThisPage + " : "
								+ postEntity.getCaption());

						// look for existance of WPH tag...
						boolean added = false;
						JsonNode tagsNode = node.path("tags");
						for (JsonNode tagNode : tagsNode) {
							String tag = tagNode.textValue();
							System.out.print(tag + " ");
							if (tag.equalsIgnoreCase(getTag())) {
								added = true;
								postEntities.add(postEntity);
								break;
							}
						}
						System.out.println("added:" + added);
					}

					if (maxTsThisPage != null) {
						Date max = new Date(maxTsThisPage);
						System.out.println("maxTs:" + max);
						postEntities.addAll(fetchPosts(userid, maxTsThisPage, minTs));
					} else {
						System.out.println("all done!");
					}

				}
			}
		} catch (Exception e) {
			log.warning(e.getMessage());
		}
		return postEntities;

	}

	private String userSearch() {
		String ret = null;
		UriBuilder b = UriBuilder.fromUri(Instagram.INSTAGRAM_API_BASE);

		b.path("users");
		b.path("search");
		b.queryParam("client_id", Instagram.getClientId());
		// b.queryParam("q", getUsername());

		HTTPRequest request = new HTTPRequest(b.toURL());
		HTTPResponse response;
		try {
			response = fetcherService.fetch(request);
			int responseCode = response.getResponseCode();
			if (responseCode == 200) {

				JsonNode rootNode = mapper.readTree(response.getContent());
				JsonNode dataNode = rootNode.path("data");

				int code = rootNode.path("meta").path("code").asInt();

				if (code == 200) {
					for (JsonNode node : dataNode) {
						String username = node.path("username").asText();
						if (username.equals(getUsername())) {
							ret = node.path("id").asText();
							System.out.println("Found: " + mapper.writeValueAsString(node));
							break;
						}
					}
				}
			}
		} catch (IOException e) {
			log.warning(e.getMessage());
		}

		return ret;
	}

	private String buildIgFeedUrl(String userid, Long maxTs, Long minTs) {
		UriBuilder b = UriBuilder.fromUri(Instagram.INSTAGRAM_API_BASE);

		b.path("users");
		b.path(userid);
		b.path("media");
		b.path("recent");
		b.queryParam("client_id", Instagram.getClientId());
		if (maxTs != null) {
			// Date tsDate = new Date(maxTs * 1000L);
			// System.out.println("Return media before this UNIX timestamp:" +
			// tsDate.toString());
			b.queryParam("max_timestamp", maxTs.toString());
		}
		if (minTs != null) {
			// Date tsDate = new Date(minTs * 1000L);
			// System.out.println("Return media after this UNIX timestamp:" +
			// tsDate.toString());
			b.queryParam("min_timestamp", minTs.toString());
		}

		String apiUrl = b.toString();
		return apiUrl;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
