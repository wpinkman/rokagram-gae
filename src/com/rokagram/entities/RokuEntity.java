package com.rokagram.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.appengine.api.datastore.GeoPt;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.condition.IfTrue;
import com.rokagram.backend.GaeClientLocation;
import com.rokagram.backend.dao.DAO;
import com.rokagram.backend.roku.Roku;

@Entity
@Cache
public class RokuEntity extends EntityBase {

	@Id
	private String deviceId;
	// client id for Google Analytics
	private String cid;
	private String model;
	private String displayMode;
	private String firmware;

	@Index
	private String channelVersion = "1.0";

	@Index
	private String country;
	@Index
	private String region;
	@Index
	private String city;
	private GeoPt latlong;

	@Index
	private String email;

	private String itemCode;

	@Index
	private int daysElapsed;

	@Index(IfTrue.class)
	private boolean expired = false;
	@Index(IfTrue.class)
	private boolean upgraded = false;

	private boolean resetTrial = false;
	private boolean rescindTrial = false;

	private boolean radio = true;

	@Index
	private int audioMinutes;

	@Index
	private String feedFmClientId;

	private int logLevel;

	@Index
	private long starts;

	// private FeedFmSettingsEntity feedFmSettings;

	private List<Ref<UserEntity>> users = new ArrayList<Ref<UserEntity>>();

	public long getStarts() {
		return starts;
	}

	public void setStarts(long starts) {
		this.starts = starts;
	}

	public String getDeviceId() {
		return deviceId;
	}

	@JsonIgnore
	public String getCountryName() {
		GaeClientLocation gcl = new GaeClientLocation(this.city, this.region, this.country);
		return gcl.getCountryName();
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getModel() {
		return model;
	}

	@JsonIgnore
	public String getModelHuman() {
		String ret = this.model;

		if (this.model != null && Roku.modelMap.containsKey(this.model)) {
			ret = Roku.modelMap.get(this.model);
		}
		return ret;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getDisplayMode() {
		return displayMode;
	}

	public void setDisplayMode(String displayMode) {
		this.displayMode = displayMode;
	}

	public String getCountry() {
		return country;
	}

	@JsonIgnore
	public String getCountryCapitalized() {
		GaeClientLocation gcl = new GaeClientLocation(this.city, this.region, this.country);
		return gcl.getCountryCapitalized();
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getRegion() {
		return region;
	}

	@JsonIgnore
	public String getRegionCapitalized() {
		GaeClientLocation gcl = new GaeClientLocation(this.city, this.region, this.country);
		return gcl.getRegionCapitalized();
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getCity() {
		return city;
	}

	@JsonIgnore
	public String getCityCapitalized() {
		GaeClientLocation gcl = new GaeClientLocation(this.city, this.region, this.country);
		return gcl.getCityCapitalized();
	}

	public void setCity(String city) {
		this.city = city;
	}

	@JsonIgnore
	public String getLocation() {
		GaeClientLocation gcl = new GaeClientLocation(this.city, this.region, this.country);
		return gcl.getLocation();
	}

	@JsonIgnore
	public String getLocationShort() {
		GaeClientLocation gcl = new GaeClientLocation(this.city, this.region, this.country);
		return gcl.getLocationShort();
	}

	public void setLatlong(GeoPt latlong) {
		this.latlong = latlong;
	}

	public GeoPt getLatlong() {
		return latlong;
	}

	public void setFirmware(String firmware) {
		this.firmware = firmware;
	}

	public String getFirmware() {
		return firmware;
	}

	@JsonIgnore
	public Double getFirmwareVersion() {
		Double ret = null;
		if (this.firmware != null && this.firmware.length() > 8) {

			String major = this.firmware.substring(2, 3);
			String minor = this.firmware.substring(4, 6);

			try {
				String version = Integer.parseInt(major) + "." + Integer.parseInt(minor);
				ret = Double.parseDouble(version);
			} catch (NumberFormatException nfe) {
			}
		}

		return ret;
	}

	public void setAudioMinutes(int audioMinutes) {
		this.audioMinutes = audioMinutes;
	}

	public int getAudioMinutes() {
		return audioMinutes;
	}

	// public void setFeedFmSettings(FeedFmSettingsEntity feedFmSettings) {
	// this.feedFmSettings = feedFmSettings;
	// }
	//
	// public FeedFmSettingsEntity getFeedFmSettings() {
	// return feedFmSettings;
	// }

	public List<Ref<UserEntity>> getUsers() {
		return users;
	}

	public Collection<UserEntity> loadUsers() {
		Map<Key<UserEntity>, UserEntity> refs = DAO.ofy().load().refs(this.users);
		return refs.values();
	}

	@JsonIgnore
	public int getUserCount() {
		return users.size();
	}

	@Override
	public String toString() {
		return "RokuEntity [deviceId=" + deviceId + ", model=" + model + ", displayMode=" + displayMode + ", firmware="
				+ firmware + ", country=" + country + ", region=" + region + ", city=" + city + ", latlong=" + latlong
				+ ", audioMinutes=" + audioMinutes + ", users=" + users + "]";
	}

	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}

	public int getLogLevel() {
		return logLevel;
	}

	public void setFeedFmClientId(String feedFmClientId) {
		this.feedFmClientId = feedFmClientId;
	}

	public String getFeedFmClientId() {
		return feedFmClientId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public long getIncrementStarts() {
		return this.starts++;
	}

	public String getChannelVersion() {
		return channelVersion;
	}

	public void setChannelVersion(String channelVersion) {
		this.channelVersion = channelVersion;
	}

	public String getItemCode() {
		return itemCode;
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	public int getDaysElapsed() {
		return daysElapsed;
	}

	public void setDaysElapsed(int daysElapsed) {
		this.daysElapsed = daysElapsed;
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

	public boolean isRadio() {
		return radio;
	}

	public void setRadio(boolean radio) {
		this.radio = radio;
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	public boolean isUpgraded() {
		return upgraded;
	}

	public void setUpgraded(boolean upgraded) {
		this.upgraded = upgraded;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

}
