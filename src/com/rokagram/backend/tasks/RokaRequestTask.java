package com.rokagram.backend.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.rokagram.backend.Utils;
import com.rokagram.backend.dao.DAO;
import com.rokagram.entities.RokuEntity;

public class RokaRequestTask implements DeferredTask {

	private static final long serialVersionUID = 1L;

	private String deviceId;
	private boolean firstTime;

	public RokaRequestTask(String deviceId, boolean firstTime) {
		super();
		this.deviceId = deviceId;
		this.firstTime = firstTime;
	}

	@Override
	public void run() {

		if (this.deviceId != null) {

			RokuEntity roku = DAO.ofy().load().type(RokuEntity.class).id(this.deviceId).now();

			if (roku != null) {

				if (this.firstTime) {
					MailService mailService = MailServiceFactory.getMailService();
					MailService.Message message = new MailService.Message();
					message.setSender("andy@rokagram.com");

					String subject = "Rokagram Event " + roku.getLocation();
					subject = "New " + subject;

					message.setSubject(subject);
					message.setTextBody(roku.toString());

					Map<String, Object> velocityInject = new HashMap<String, Object>();
					velocityInject.put("device", roku);

					try {
						String htmlBody = Utils.createHtmlBody("admin/device-summary", velocityInject);
						message.setHtmlBody(htmlBody);

						mailService.sendToAdmins(message);

					} catch (IOException e) {
						Utils.warn("failed to send email for roku:" + roku.toString());
					}
				}
			}
		}
	}

}
