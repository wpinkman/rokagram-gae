package com.rokagram.backend.dto;

public class InstaDailyDto {
	private String featuredTag;
	private String description;

	public InstaDailyDto(String featuredTag, String description) {
		this.featuredTag = featuredTag;
		this.description = description;
	}

	public InstaDailyDto() {
	}

	public String getFeaturedTag() {
		return featuredTag;
	}

	public void setFeaturedTag(String featuredTag) {
		this.featuredTag = featuredTag;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "InstaDailyDto [featuredTag=" + featuredTag + ", description=" + description + "]";
	}
}
