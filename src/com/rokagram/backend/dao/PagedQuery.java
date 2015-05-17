package com.rokagram.backend.dao;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.cmd.Query;
import com.rokagram.backend.ServletUtils;
import com.rokagram.backend.UriBuilder;

public class PagedQuery<T> {

	private static final String URL_TOP = "url-top";
	private static final String URL_NEXT = "url-next";
	private static final String CURSOR = "cursor";

	private Class<T> clazz;
	private String entityListName;
	private String order = "-modified";

	public PagedQuery(Class<T> clazz) {
		super();
		this.clazz = clazz;

		this.entityListName = this.clazz.getSimpleName().replace("Entity", "").toLowerCase() + "s";
	}

	public void doQuery(HttpServletRequest req, String deviceId, String userId) {

		String requestUrl = ServletUtils.getFullURL(req);

		UriBuilder nextRequestBuilder = UriBuilder.fromUri(requestUrl);
		UriBuilder nextRequestTopBuilder = UriBuilder.fromUri(requestUrl);

		List<String> filterables = new ArrayList<String>();
		List<String> booleanFilterables = new ArrayList<String>();

		filterables.add("type");
		filterables.add("channelVersion");
		filterables.add("starts");
		filterables.add("country");
		filterables.add("region");
		filterables.add("city");
		filterables.add("email");
		filterables.add("audioMinutes");

		booleanFilterables.add("upgraded");
		booleanFilterables.add("expired");

		List<T> ret = new ArrayList<T>();
		int limit = 25;

		String limitParam = req.getParameter("limit");
		if (limitParam != null) {
			try {
				limit = Integer.parseInt(limitParam);
			} catch (NumberFormatException nfe) {

			}
		}

		String orderParam = req.getParameter("order");

		Query<T> query = DAO.ofy().load().type(this.clazz);

		if (orderParam != null) {
			query = query.order(orderParam);
		} else {
			query = query.order(this.order);
		}

		if (deviceId != null) {
			query = query.filter("deviceId", deviceId);
		}
		if (userId != null) {
			query = query.filter("userId", userId);
		}

		for (String filterable : filterables) {
			String filtValue = req.getParameter(filterable);
			if (filtValue != null) {
				if (filterable.equals("country")) {
					filtValue = filtValue.toUpperCase();
				}
				if (filterable.equals("region")) {
					filtValue = filtValue.toLowerCase();
				}
				query = query.filter(filterable, filtValue);
			}
		}

		for (String booleanFilterable : booleanFilterables) {
			String parameter = req.getParameter(booleanFilterable);
			if (parameter != null) {
				boolean filtValue = Boolean.parseBoolean(parameter);
				query = query.filter(booleanFilterable, filtValue);
			}
		}

		int totalCount = 0;
		req.setAttribute("total-count", totalCount);

		String cursorStr = req.getParameter(CURSOR);
		if (cursorStr != null) {
			Cursor fromWebSafeString = Cursor.fromWebSafeString(cursorStr);
			query = query.startAt(fromWebSafeString);
			// build top url
			nextRequestTopBuilder.removeQueryParam(CURSOR);
			req.setAttribute(URL_TOP, nextRequestTopBuilder.toString());
		}

		query = query.limit(limit);
		QueryResultIterator<T> iterator = query.iterator();
		int count = 0;
		while (iterator.hasNext()) {
			T entity = iterator.next();
			ret.add(entity);
			count++;
		}
		if (count == limit) {
			Cursor cursor = iterator.getCursor();
			nextRequestBuilder.queryParam(CURSOR, cursor.toWebSafeString());
			req.setAttribute(URL_NEXT, nextRequestBuilder.toString());
		}

		req.setAttribute(entityListName, ret);
	}

	public void setEntityListName(String entityListName) {
		this.entityListName = entityListName;
	}

	public String getEntityListName() {
		return entityListName;
	}

	public void doQuery(HttpServletRequest req) {
		doQuery(req, null, null);

	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}
}
