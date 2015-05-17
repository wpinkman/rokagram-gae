package com.rokagram.entities;

import java.io.IOException;
import java.util.Date;

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
public class IgPostEntity {

	private static ObjectMapper mapper = new ObjectMapper();

	@Id
	private String id;

	@Index
	private Date created;
	private Link link;

	private Link lowres;
	private Link thumb;
	private Link standard;

	private Link videoLowres;
	private Link videoStandard;

	private String type;

	private String username;
	private String userid;

	@Index
	private String whptag;

	@Index
	private String whpfeatured;

	@Index
	private long likes;

	private Text json;

	public String getId() {
		return id;
	}

	public String getCreatedHuman() {
		return TimeFormatUtils.getHuman(this.created);
	}

	public void setId(String id) {
		this.id = id;
	}

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public Link getLowres() {
		return lowres;
	}

	public void setLowres(Link lowres) {
		this.lowres = lowres;
	}

	public Link getThumb() {
		return thumb;
	}

	public void setThumb(Link thumb) {
		this.thumb = thumb;
	}

	public Link getStandard() {
		return standard;
	}

	public void setStandard(Link standard) {
		this.standard = standard;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public Text getJson() {
		return json;
	}

	public void setJson(Text jsonText) {
		this.json = jsonText;
	}

	public void setJson(String json) {
		this.json = new Text(json);
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getCaption() {
		String ret = null;
		try {
			JsonNode node = mapper.readTree(getJson().getValue());
			ret = node.path("caption").path("text").asText();

		} catch (JsonProcessingException e) {
		} catch (IOException e) {
		}
		return ret;

	}

	public void fillFromNode(JsonNode node) {
		setId(node.path("id").asText());

		long created_time = node.path("created_time").asLong();
		Date created = new Date(created_time * 1000L);
		setCreated(created);

		try {
			String json = mapper.writeValueAsString(node);
			setJson(json);
		} catch (JsonProcessingException e) {
		}

		setLink(new Link(node.path("link").asText()));
		setType(node.path("type").asText());

		JsonNode imageNode = node.path("images");
		setLowres(new Link(imageNode.path("low_resolution").path("url").asText()));
		setThumb(new Link(imageNode.path("thumbnail").path("url").asText()));
		setStandard(new Link(imageNode.path("standard_resolution").path("url").asText()));

		if (getType().equals("video")) {
			JsonNode videosNode = node.path("videos");
			setVideoLowres(new Link(videosNode.path("low_resolution").path("url").asText()));

			setVideoStandard(new Link(videosNode.path("standard_resolution").path("url").asText()));
		}

		setLikes(node.path("likes").path("count").asLong());

		JsonNode userNode = node.path("user");
		setUserid(userNode.path("id").asText());
		setUsername(userNode.path("username").asText());

	}

	public long getLikes() {
		return likes;
	}

	public void setLikes(long likes) {
		this.likes = likes;
	}

	public String getWhptag() {
		return whptag;
	}

	public void setWhptag(String whptag) {
		this.whptag = whptag;
	}

	public String getWhpfeatured() {
		return whpfeatured;
	}

	public void setWhpfeatured(String whpfeatured) {
		this.whpfeatured = whpfeatured;
	}

	public Link getVideoLowres() {
		return videoLowres;
	}

	public void setVideoLowres(Link videoLowres) {
		this.videoLowres = videoLowres;
	}

	public Link getVideoStandard() {
		return videoStandard;
	}

	public void setVideoStandard(Link videoStandard) {
		this.videoStandard = videoStandard;
	}

}
