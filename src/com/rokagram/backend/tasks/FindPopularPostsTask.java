package com.rokagram.backend.tasks;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
import com.rokagram.entities.SystemSettingsEnum;
import com.rokagram.entities.WhpEntity;

@SuppressWarnings("serial")
public class FindPopularPostsTask implements DeferredTask {
	public static final Logger log = Logger.getLogger(FindPopularPostsTask.class.getName());
	private static URLFetchService fetcherService = com.google.appengine.api.urlfetch.URLFetchServiceFactory
			.getURLFetchService();
	private static ObjectMapper mapper = new ObjectMapper();

	private String whptag;

	public FindPopularPostsTask(String tag) {
		super();
		this.whptag = tag;
	}

	@Override
	public void run() {

		WhpEntity whpe = DAO.ofy().load().type(WhpEntity.class).id(whptag).now();

		if (whpe != null) {

			String apiUrl = null;
			UriBuilder b = UriBuilder.fromUri(Instagram.INSTAGRAM_API_BASE);
			b.path("tags");
			b.path(whptag);
			b.path("media");
			b.path("recent");
			b.queryParam("client_id", DAO.getSystemSetting(SystemSettingsEnum.INSTAGRAM_CLIENT_ID));

			apiUrl = b.toString();

			int depth = 0;
			fetchTaggedPosts(apiUrl, depth, whpe);
		}

	}

	private void fetchTaggedPosts(String apiUrl, int depth, WhpEntity whpe) {

		log.info("Fetching from:" + apiUrl);
		depth++;
		try {
			HTTPResponse apiResponse = fetcherService.fetch(new URL(apiUrl));
			if (apiResponse.getResponseCode() == 200) {
				JsonNode rootNode = mapper.readTree(apiResponse.getContent());

				String nextUrl = rootNode.path("pagination").path("next_url").asText();
				// System.out.println(mapper.writeValueAsString(rtnode));

				int code = rootNode.path("meta").path("code").asInt();

				if (code == 200) {
					List<IgPostEntity> postEntities = new ArrayList<IgPostEntity>();
					for (JsonNode postNode : rootNode.path("data")) {
						IgPostEntity ipe = new IgPostEntity();
						ipe.fillFromNode(postNode);
						ipe.setWhptag(whptag);
						postEntities.add(ipe);
					}
					if (postEntities.size() > 0) {
						log.info("Saving " + postEntities.size() + " posts for tag #" + whptag);
						DAO.ofy().save().entities(postEntities).now();
						if (depth < 100) {
							if (nextUrl != null) {
								String msg = "depth: " + depth + ", recursing";
								System.out.println(msg);
								log.info(msg);
								fetchTaggedPosts(nextUrl, depth, whpe);
							}
						}

					}
				}
			}
		} catch (IOException e) {
			log.warning(e.getMessage());
		}
	}
}
