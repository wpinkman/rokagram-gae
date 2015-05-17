package com.rokagram.entities;

import java.util.Date;
import java.util.List;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.rokagram.backend.TimeFormatUtils;
import com.rokagram.backend.dao.DAO;

@Entity
@Cache
public class WhpEntity {

	@Id
	private String tag;

	@Index
	private Date announceDate;
	private Date featureDate;

	@Load
	private Ref<IgPostEntity> announceInsta;
	@Load
	private Ref<IgPostEntity> featureInsta;

	@Load
	private Ref<TumblrPostEntity> announceTumblr;
	@Load
	private Ref<TumblrPostEntity> featureTumblr;

	@Ignore
	List<IgPostEntity> featuredPosts;

	public List<IgPostEntity> getFeaturedPosts() {
		if (featuredPosts == null) {
			featuredPosts = DAO.ofy().load().type(IgPostEntity.class).filter("whpfeatured", this.tag).list();
		}
		return featuredPosts;
	}

	public String getCapitalizedTag() {
		String ret = this.tag;
		if (ret.startsWith("whp")) {
			ret = "WHP" + this.tag.substring(3);
		}

		return ret;
	}

	public int getFeaturedPostCount() {
		return getFeaturedPosts().size();
	}

	public IgPostEntity getAnnounceInsta() {
		IgPostEntity ret = null;
		if (announceInsta != null) {
			ret = announceInsta.get();
		}
		return ret;

	}

	public void setAnnounceInsta(IgPostEntity announcePost) {
		this.announceInsta = Ref.create(announcePost);
	}

	public void setFeatureInsta(IgPostEntity featurePost) {
		this.featureInsta = Ref.create(featurePost);
	}

	public IgPostEntity getFeatureInsta() {
		IgPostEntity ret = null;
		if (featureInsta != null) {
			ret = featureInsta.get();
		}
		return ret;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public TumblrPostEntity getAnnounceTumblr() {
		TumblrPostEntity ret = null;
		if (announceTumblr != null) {
			ret = announceTumblr.get();
		}
		return ret;
	}

	public void setAnnounceTumblr(TumblrPostEntity announceTumblr) {
		this.announceTumblr = Ref.create(announceTumblr);
	}

	public TumblrPostEntity getFeatureTumblr() {
		TumblrPostEntity ret = null;
		if (featureTumblr != null) {
			ret = featureTumblr.get();
		}
		return ret;
	}

	public void setFeatureTumblr(TumblrPostEntity featureTumblr) {
		this.featureTumblr = Ref.create(featureTumblr);
	}

	public Date getAnnounceDate() {
		return announceDate;
	}

	public String getAnnounceDateHuman() {
		return TimeFormatUtils.getHuman(this.announceDate);
	}

	public void setAnnounceDate(Date announceDate) {
		this.announceDate = announceDate;
	}

	public Date getFeatureDate() {
		return featureDate;
	}

	public String getFeatureDateHuman() {
		return TimeFormatUtils.getHuman(this.featureDate);
	}

	public void setFeatureDate(Date featureDate) {
		this.featureDate = featureDate;
	}

	@Override
	public String toString() {
		return "WhpEntity [tag=" + tag + ", announceDate=" + getAnnounceDateHuman() + ", featureDate="
				+ getFeatureDateHuman() + "]";
	}

}
