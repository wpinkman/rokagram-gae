package com.rokagram.backend;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;

public class ServletUtils {
	private static ObjectMapper mapper = new ObjectMapper();

	public static ObjectMapper getMapper() {
		return mapper;
	}

	private static void configureMapper(HttpServletRequest req) {
		boolean browser = false;

		String acceptHeader = req.getHeader("Accept");
		if (!Strings.isNullOrEmpty(acceptHeader)) {
			browser = acceptHeader.startsWith("text/html");
		}

		if (Utils.isDevServer() || browser) {
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		}

		mapper.setSerializationInclusion(Include.NON_NULL);
		// mapper.setSerializationInclusion(SerializationFeature.FAIL_ON_EMPTY_BEANS);

	}

	public static void writeJsonMetadataResponse(HttpServletRequest req,
			HttpServletResponse resp, Object object) throws IOException,
			JsonGenerationException, JsonMappingException {

		configureMapper(req);
		resp.setContentType("application/json");
		mapper.writeValue(resp.getOutputStream(), object);
	}

	public static void printHeader(HttpServletRequest req, String headerName) {
		String header = req.getHeader(headerName);
		if (header != null) {
			Utils.d(headerName + ":" + header);
		}
	}

	public static String getFullURL(HttpServletRequest request) {
		StringBuffer requestURL = request.getRequestURL();
		String queryString = request.getQueryString();

		if (queryString == null) {
			return requestURL.toString();
		} else {
			return requestURL.append('?').append(queryString).toString();
		}
	}

}
