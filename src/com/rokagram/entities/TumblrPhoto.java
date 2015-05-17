package com.rokagram.entities;

public class TumblrPhoto {
	private String username;
	private String shortcode;
	private String thumb;
	private String standard;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getShortcode() {
		return shortcode;
	}

	public void setShortcode(String shortcode) {
		this.shortcode = shortcode;
	}

	public TumblrPhoto(String username, String shortcode) {
		super();
		this.username = username;
		this.shortcode = shortcode;
	}

	public String getThumb() {
		return thumb;
	}

	public void setThumb(String thumb) {
		this.thumb = thumb;
	}

	public String getStandard() {
		return standard;
	}

	public void setStandard(String standard) {
		this.standard = standard;
	}

	@Override
	public String toString() {
		return "TumblrPhoto [username=" + username + ", shortcode=" + shortcode + "]";
	}
}
