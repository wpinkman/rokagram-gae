package com.rokagram.backend.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;
import com.rokagram.backend.RegUtils;
import com.rokagram.entities.ApiClientEntity;
import com.rokagram.entities.EventRollupEntity;
import com.rokagram.entities.IgPostEntity;
import com.rokagram.entities.LogEntity;
import com.rokagram.entities.RegistryEntity;
import com.rokagram.entities.RokuEntity;
import com.rokagram.entities.SystemSettingsEntity;
import com.rokagram.entities.SystemSettingsEnum;
import com.rokagram.entities.TemplateEntity;
import com.rokagram.entities.TokenEntity;
import com.rokagram.entities.TumblrPostEntity;
import com.rokagram.entities.UserEntity;
import com.rokagram.entities.UserLoveEntity;
import com.rokagram.entities.WhpEntity;

public class DAO {

	public static final Logger log = Logger.getLogger(DAO.class.getName());

	static {
		ObjectifyService.register(TokenEntity.class);
		ObjectifyService.register(UserEntity.class);
		ObjectifyService.register(UserLoveEntity.class);
		ObjectifyService.register(TemplateEntity.class);
		ObjectifyService.register(RokuEntity.class);
		ObjectifyService.register(LogEntity.class);
		ObjectifyService.register(SystemSettingsEntity.class);
		ObjectifyService.register(ApiClientEntity.class);
		ObjectifyService.register(EventRollupEntity.class);
		ObjectifyService.register(RegistryEntity.class);
		// WHP
		ObjectifyService.register(IgPostEntity.class);
		ObjectifyService.register(TumblrPostEntity.class);
		ObjectifyService.register(WhpEntity.class);

	}

	public static Objectify ofy() {
		return ObjectifyService.ofy();
	}

	public static TokenEntity loadToken(String userToken) {
		TokenEntity userTokenEntity = null;
		Long id = RegUtils.getId(userToken);

		if (id != null) {
			userTokenEntity = ofy().load().key(Key.create(TokenEntity.class, id)).now();
		}
		return userTokenEntity;
	}

	public static Query<TokenEntity> createQueryByDeviceId(TokenEntity token) {
		Query<TokenEntity> tokens = ofy().load().type(TokenEntity.class).filter("deviceId", token.getDeviceId());
		return tokens;
	}

	private static Query<WhpEntity> getWhpOrderByDate() {
		return ofy().load().type(WhpEntity.class).order("-announceDate");
	}

	public static List<WhpEntity> getWpaList() {
		QueryKeys<WhpEntity> keys = getWhpOrderByDate().keys();
		Collection<WhpEntity> list = ofy().load().keys(keys).values();
		List<WhpEntity> wpaList = new ArrayList<WhpEntity>(list.size());

		for (WhpEntity whp : list) {
			if (whp.getAnnounceInsta() != null && whp.getAnnounceInsta() != null) {

				if ((whp.getFeatureTumblr() == null && whp.getFeatureInsta() == null)
						|| (whp.getFeatureTumblr() != null && whp.getFeatureInsta() != null)) {

					wpaList.add(whp);
				}
			}
		}

		return wpaList;
	}

	public static String getSystemSetting(SystemSettingsEnum setting) {
		String ret = null;

		SystemSettingsEntity sse = ofy().load().type(SystemSettingsEntity.class).id(setting.toString()).now();

		ret = sse.getValue();

		return ret;
	}

}
