package com.rokagram.backend.dto;

import java.util.ArrayList;
import java.util.List;

import com.rokagram.entities.FeedFmSettingsEntity;

public class TopLevelResponse {
	private LocationDto location = new LocationDto();
	private FeedFmSettingsEntity feedfm = new FeedFmSettingsEntity();
	private InstagramSettings instagram = new InstagramSettings();
	private RadioSettings radio = new RadioSettings();
	private List<RegistryModification> regmods = new ArrayList<RegistryModification>();
	private boolean resetTrial = false;
	private boolean rescindTrial = false;
	private Integer trialDays = null;
	private String sslPatchDomain;
	private GoogleAnalyticsDto ga = new GoogleAnalyticsDto();
	private InstaDailyDto[] instadaily = new InstaDailyDto[7];
	private InstaDailyDto weekendHashTagProject = new InstaDailyDto();

	public void setLocation(LocationDto location) {
		this.location = location;
	}

	public LocationDto getLocation() {
		return location;
	}

	public void setFeedfm(FeedFmSettingsEntity feedfm) {
		this.feedfm = feedfm;
	}

	public FeedFmSettingsEntity getFeedfm() {
		return feedfm;
	}

	public boolean isResetTrial() {
		return resetTrial;
	}

	public void setResetTrial(boolean resetTrial) {
		this.resetTrial = resetTrial;
	}

	public boolean isRescindTrial() {
		return rescindTrial;
	}

	public void setRescindTrial(boolean rescindTrial) {
		this.rescindTrial = rescindTrial;
	}

	public InstagramSettings getInstagram() {
		return instagram;
	}

	public void setInstagram(InstagramSettings instagram) {
		this.instagram = instagram;
	}

	public Integer getTrialDays() {
		return trialDays;
	}

	public void setTrialDays(Integer trialDays) {
		this.trialDays = trialDays;
	}

	public RadioSettings getRadio() {
		return radio;
	}

	public void setRadio(RadioSettings radio) {
		this.radio = radio;
	}

	public List<RegistryModification> getRegmods() {
		return regmods;
	}

	public void setRegmods(List<RegistryModification> regmods) {
		this.regmods = regmods;
	}

	public String getSslPatchDomain() {
		return sslPatchDomain;
	}

	public void setSslPatchDomain(String sslPatchDomain) {
		this.sslPatchDomain = sslPatchDomain;
	}

	public GoogleAnalyticsDto getGa() {
		return ga;
	}

	public InstaDailyDto getWeekendHashTagProject() {
		return weekendHashTagProject;
	}

	public void setWeekendHashTagProject(InstaDailyDto weekendHashTagProject) {
		this.weekendHashTagProject = weekendHashTagProject;
	}

	public InstaDailyDto[] getInstadaily() {
		return instadaily;
	}

}
