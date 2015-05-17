package com.rokagram.backend;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class UriBuilder {
	private String scheme;
	private String userInfo = null;
	private String host;
	private int port = -1;
	private List<String> pathSegments = new ArrayList<String>();
	private Map<String, String> queryParams = new LinkedHashMap<String, String>();
	private String fragment;
	private boolean pathTrailingSlash = false;

	private enum EncodingStyle {
		NONE, COMPATIBLE, OAUTH
	};

	private EncodingStyle encodingStyle = EncodingStyle.COMPATIBLE;

	public void setOuthEncoding(boolean oauthEncoding) {
		if (oauthEncoding) {
			this.encodingStyle = EncodingStyle.OAUTH;
		} else {
			this.encodingStyle = EncodingStyle.COMPATIBLE;
		}
	}

	private UriBuilder() {
	}

	public static UriBuilder fromUri(String requestURI) {
		URI uri = null;
		try {
			uri = new URI(requestURI);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		UriBuilder b = initFromUri(uri);

		return b;
	}

	private static UriBuilder initFromUri(URI uri) {
		UriBuilder b = new UriBuilder();
		b.host = uri.getHost();
		b.scheme = uri.getScheme();
		b.userInfo = uri.getUserInfo();
		b.host = uri.getHost();
		b.port = uri.getPort();
		String path = uri.getPath();
		b.path(path);
		String query = uri.getQuery();

		initQueryParams(b, query);
		b.fragment = uri.getFragment();
		return b;
	}

	@Override
	public String toString() {
		StringBuilder sb = buildBase();

		String query = buildQueryString();
		if (query != null) {
			sb.append("?");
			sb.append(query);
		}
		if (this.fragment != null) {
			sb.append("#");
			sb.append(this.fragment);
		}

		return sb.toString();
	}

	private StringBuilder buildBase() {
		StringBuilder sb = new StringBuilder();
		if (this.scheme != null) {
			sb.append(this.scheme);
			sb.append("://");
		}
		if (this.userInfo != null) {
			sb.append(this.userInfo);
			sb.append("@");
		}
		if (this.host != null) {
			sb.append(this.host);
			if (this.port != -1) {
				sb.append(":");
				sb.append(Integer.toString(this.port));
			}
		}

		String path = buildPath();
		sb.append(path);
		return sb;
	}

	public URI build() {
		URI ret = null;

		EncodingStyle enc = this.encodingStyle;

		this.encodingStyle = EncodingStyle.NONE;
		String path = buildPath();
		String query = buildQueryString();
		this.encodingStyle = enc;

		try {
			ret = new URI(scheme, userInfo, host, port, path, query, fragment);

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public String getEncodedQueryString() {
		String ret = buildQueryString();
		if (ret == null) {
			ret = "";
		}
		return ret;
	}

	public String getEncodedBaseUrl() {
		return buildBase().toString();
	}

	private String buildQueryString() {
		String query = null;

		int index = 0;
		for (Entry<String, String> entry : this.queryParams.entrySet()) {
			if (query == null) {
				query = "";
			}
			String name = entry.getKey();
			String value = entry.getValue();

			switch (this.encodingStyle) {
			case COMPATIBLE:
				name = encode(name);
				if (value != null) {
					value = encode(value);
				}
				break;
			case OAUTH:
				name = encodeOath(name);
				if (value != null) {
					value = encodeOath(value);
				}
				break;

			default:
				break;
			}
			query += name;
			if (value != null) {
				query += "=" + value;
			}

			if (index++ < queryParams.size() - 1) {
				query += "&";
			}
		}
		return query;
	}

	private String buildPath() {
		String path = "";
		for (int index = 0; index < pathSegments.size(); index++) {
			String segment = pathSegments.get(index);
			switch (this.encodingStyle) {
			case COMPATIBLE:
				segment = encode(segment);
				break;
			case OAUTH:
				segment = encodeOath(segment);
				break;

			default:
				break;
			}
			path += "/" + segment;
		}
		if (pathTrailingSlash) {
			path += "/";
		}
		return path;
	}

	public String getPath() {
		return buildPath();
	}

	public String getQuery() {
		return buildQueryString();
	}

	private static void initQueryParams(UriBuilder b, String query) {

		if (query != null) {
			for (String nvp : query.split("&")) {
				String[] components = nvp.split("=");
				String name = components[0];
				String value = null;
				if (components.length > 1) {
					value = components[1];
				}
				b.queryParams.put(name, value);
			}
		}
	}

	public UriBuilder host(String host) {
		this.host = host;

		return this;
	}

	public UriBuilder port(int port) {
		this.port = port;
		return this;
	}

	public UriBuilder scheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	public UriBuilder replacePath(String path) {
		this.pathSegments.clear();
		this.path(path);
		return this;

	}

	public UriBuilder queryParam(String name, String value) {
		this.queryParams.put(name, value);
		return this;
	}

	public UriBuilder fragment(String fragment) {
		this.fragment = fragment;
		return this;

	}

	public static UriBuilder fromUri(URI uri) {
		UriBuilder b = initFromUri(uri);

		return b;
	}

	public UriBuilder path(String path) {
		if (path != null) {
			for (String segment : path.split("/")) {
				if (!segment.isEmpty()) {
					this.pathSegments.add(segment);
				}
			}

			this.pathTrailingSlash = path.endsWith("/");
		}
		return this;

	}

	public UriBuilder queryParam(String name, Object... values) {
		setOneQueryParam(name, values);

		return this;
	}

	private void setOneQueryParam(String name, Object... values) {
		String value = null;

		if (values.length == 1) {
			if (values[0] != null) {
				value = values[0].toString();
			}
		} else {
			for (int i = 0; i < values.length; i++) {
				value += values[i].toString();
				if (i < values.length - 1) {
					value += ",";
				}
			}
		}

		this.queryParams.put(name, value);
	}

	public void replaceQuery(String queryString) {
		this.queryParams.clear();
		initQueryParams(this, queryString);
	}

	public void replaceQueryParam(String name, Object[] values) {
		if (values == null) {
			this.queryParams.remove(name);
		} else {
			setOneQueryParam(name, values);
		}

	}

	public URL toURL() {
		URL ret = null;
		try {
			ret = new URL(this.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public void replaceQueryParam(String name, String value) {
		Object[] values = { value };
		this.replaceQueryParam(name, values);
	}

	public void removeQueryParam(String name) {
		this.queryParams.remove(name);
	}

	public String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public String encodeOath(String s) {
		return encode(s).replace("+", "%20").replace("*", "%2A")
				.replace("%7E", "~");
	}

	public String encodeDropboxPathSegment(String s) {

		// % is legal in DropBox name, but we're using percent escaping for the
		// chars that aren't legal.. so
		// we much first encode any % char
		String replaced = s.replace("%", "%25");

		// now replace all the restricted chacters
		replaced = replaced.replace("\\", "%5C");
		replaced = replaced.replace("/", "%2F");
		replaced = replaced.replace(":", "%3A");
		replaced = replaced.replace("?", "%3F");
		replaced = replaced.replace("*", "%2A");
		replaced = replaced.replace("<", "%3C");
		replaced = replaced.replace(">", "%3E");
		replaced = replaced.replace("\"", "%22");
		replaced = replaced.replace("|", "%7C");

		return replaced;
	}

	public String decodeDropboxPathSegment(String s) {

		String decoded = s;

		decoded = decoded.replace("%7C", "|");
		decoded = decoded.replace("%22", "\"");
		decoded = decoded.replace("%3E", ">");
		decoded = decoded.replace("%3C", "<");
		decoded = decoded.replace("%2A", "*");
		decoded = decoded.replace("%3F", "?");
		decoded = decoded.replace("%3A", ":");
		decoded = decoded.replace("%2F", "/");
		decoded = decoded.replace("%5C", "\\");

		// % is legal in DropBox name, but we're using percent escaping for the
		// chars that aren't legal.. so
		// we much lastly decode any that remain
		decoded = decoded.replace("%25", "%");

		return decoded;
	}

	public String decode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public String encodeDropboxPath(String path) {
		StringBuilder sb = new StringBuilder();
		String[] segments = path.split("/");

		for (String segment : segments) {
			sb.append("/");
			sb.append(encodeDropboxPathSegment(segment));
		}

		return sb.toString();
	}

}
