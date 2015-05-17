package com.rokagram.entities;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

@Entity
@Cache
public class TokenEntity {

	@Id
	private long id;
	private String userToken;

	@Index
	private String deviceId;
	private String model;
	private String displayMode;

	private String country;

	private String region;
	private String city;

	@Load
	@Index
	private Ref<UserEntity> user;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getModel() {
		return model;
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

	public void setUser(Ref<UserEntity> user) {
		this.user = user;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setUser(UserEntity user) {
		this.user = Ref.create(user);
	}

	public UserEntity getUser() {
		UserEntity ret = null;
		if (this.user != null) {
			ret = this.user.get();
		}
		return ret;
	}

	public Ref<UserEntity> getUserRef() {
		return this.user;
	}

	public void setUserToken(String userToken) {
		this.userToken = userToken;
	}

	public String getUserToken() {
		return userToken;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCountry() {
		return country;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getRegion() {
		return region;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCity() {
		return city;
	}

	@Override
	public String toString() {
		String ret = "TokenEntity [id=" + id + ", userToken=" + userToken + ", deviceId=" + deviceId + ", country="
				+ country + ", region=" + region + ", city=" + city;
		if (this.user == null) {
			ret += ", user: null ]";
		} else {
			ret += ", userRef:" + this.user.getKey().getName() + " ]";
		}
		return ret;
	}

}
