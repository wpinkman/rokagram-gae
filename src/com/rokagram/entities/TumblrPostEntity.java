package com.rokagram.entities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.datastore.Link;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.rokagram.backend.TimeFormatUtils;

@Entity
@Cache
public class TumblrPostEntity {
	public static final Logger log = Logger.getLogger(TumblrPostEntity.class.getName());
	private static ObjectMapper mapper = new ObjectMapper();

	@Id
	private String id;

	private Link post_url;

	@Index
	private Date timestamp;
	private String captionHtml;

	private List<Text> photosJson = new ArrayList<Text>();

	public String getTimestampHuman() {
		return TimeFormatUtils.getHuman(this.timestamp);
	}

	public List<TumblrPhoto> getPhotos() {
		List<TumblrPhoto> ret = new ArrayList<TumblrPhoto>();

		for (Text jsonText : this.photosJson) {
			try {
				JsonNode node = mapper.readTree(jsonText.getValue());

				String caption = node.path("caption").asText();
				String[] parts = caption.split("/");

				String username = null;
				String shortcode = null;

				if (parts.length > 1) {
					String fragment = parts[parts.length - 1];
					String lastPathSeg = parts[parts.length - 2];

					if (fragment.startsWith("#")) {
						username = fragment.substring(1);
					}
					if (!lastPathSeg.contains(".com")) {
						shortcode = lastPathSeg;
					}

					if (!StringUtils.isEmpty(shortcode)) {
						TumblrPhoto p = new TumblrPhoto(username, shortcode);
						p.setStandard(node.path("original_size").path("url").asText());
						for (JsonNode asn : node.path("alt_sizes")) {
							if (asn.path("width").asInt() == 75) {
								p.setThumb(asn.path("url").asText());
							}
						}
						ret.add(p);
						System.out.println(p.toString());
					} else {
						log.warning("Failed to parse caption: " + caption);
					}
				} else {
					log.warning("Failed to parse caption: " + caption);
				}

			} catch (JsonProcessingException e) {
				log.warning("Failed to parse caption for photo: " + toString());
			} catch (IOException e) {
				log.warning("Failed to parse caption for photo: " + toString());
			}
		}

		return ret;

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Link getPost_url() {
		return post_url;
	}

	public void setPost_url(Link post_url) {
		this.post_url = post_url;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getCaptionHtml() {
		return captionHtml;
	}

	public void setCaptionHtml(String captionHtml) {
		this.captionHtml = captionHtml;
	}

	public List<Text> getPhotosJson() {
		return photosJson;
	}

	public void setPhotosJson(List<Text> photosJson) {
		this.photosJson = photosJson;
	}

	@Override
	public String toString() {
		return "TumblrPostEntity [id=" + id + ", post_url=" + post_url + ", timestamp=" + timestamp + ", captionHtml="
				+ captionHtml + ", photosJson=" + photosJson + "]";
	}

}
