package com.rokagram.entities;

import com.googlecode.objectify.annotation.Ignore;
import com.rokagram.backend.dao.DAO;

public class FeedFmSettingsEntity {

	private boolean changeStation = true;

	@Ignore
	private String basicAuth = DAO.getSystemSetting(SystemSettingsEnum.FEEDFM_BASIC_AUTH);

	public boolean isChangeStation() {
		return changeStation;
	}

	public void setChangeStation(boolean changeStation) {
		this.changeStation = changeStation;
	}

	public String getBasicAuth() {
		return basicAuth;
	}

	public void setBasicAuth(String basicAuth) {
		this.basicAuth = basicAuth;
	}

}
