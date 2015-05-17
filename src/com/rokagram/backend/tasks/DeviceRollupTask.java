package com.rokagram.backend.tasks;

import java.util.logging.Logger;

import com.google.appengine.api.taskqueue.DeferredTask;
import com.googlecode.objectify.cmd.Query;
import com.rokagram.backend.dao.DAO;
import com.rokagram.entities.LogEntity;
import com.rokagram.entities.RokuEntity;

public class DeviceRollupTask implements DeferredTask {

	private static final long serialVersionUID = 1L;
	public static final Logger log = Logger.getLogger(DeviceRollupTask.class.getName());

	private String deviceId;

	public DeviceRollupTask(String deviceId) {
		super();
		this.deviceId = deviceId;
	}

	@Override
	public void run() {
		if (this.deviceId != null) {

			RokuEntity roku = DAO.ofy().load().type(RokuEntity.class).id(this.deviceId).now();
			log.info("runing rollup: " + roku.toString());

			boolean updated = false;
			if (roku != null) {
				Query<LogEntity> query = DAO.ofy().load().type(LogEntity.class);
				query = query.filter("deviceId", this.deviceId);
				query = query.filter("type", "ffmstart");

				int ffmstartCount = query.count();

				if (ffmstartCount != roku.getAudioMinutes()) {
					log.info("Updating ffmstartCount=" + ffmstartCount + " on " + roku.toString());
					roku.setAudioMinutes(ffmstartCount);
					updated = true;
				}

				if (!roku.isUpgraded()) {
					query = DAO.ofy().load().type(LogEntity.class);
					query = query.filter("deviceId", this.deviceId);
					query = query.filter("type", "startup");

					boolean trial = false;
					boolean paid = false;
					for (LogEntity log : query) {
						String message = log.getMessage();

						if (message != null) {
							if (message.contains("Version")) {
								if (message.contains("T")) {
									trial = true;
								} else {
									paid = true;
								}
								if (trial && paid) {
									roku.setUpgraded(true);
									updated = true;
									break;
								}
							}
						}

					}

				}
				if (updated) {
					roku.setIgnoreLifecycle(true);
					DAO.ofy().save().entity(roku);
				}

			}
		}

	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

}
