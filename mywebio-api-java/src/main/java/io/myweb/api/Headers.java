package io.myweb.api;

import java.util.*;

public class Headers {
	private static final int HEADERS_INITIAL_CAPACITY = 20;

	private final List<Header> headerList;

	public Headers() {
		headerList = new ArrayList<Header>(HEADERS_INITIAL_CAPACITY);
	}

	public static final class REQUEST {
		public static final String ACCEPT = "Accept";
		public static final String ACCEPT_CHAR = "Accept-Charset";
		public static final String ACCEPT_ENC = "Accept-Encoding";
		public static final String ACCEPT_LANG = "Accept-Language";
		public static final String ACCEPT_DATE = "Accept-Datetime";
		public static final String AUTH = "Authorization";
		public static final String CACHE_CTRL = "Cache-Control";
		public static final String CONNECTION = "Connection";
		public static final String COOKIE = "Cookie";
		public static final String CONTENT_LEN = "Content-Length";
		public static final String CONTENT_MD5 = "Content-MD5";
		public static final String CONTENT_TYPE = "Content-Type";
		public static final String DATE = "Date";
		public static final String EXPECT = "Expect";
		public static final String FROM = "From";
		public static final String HOST = "Host";
		public static final String IF_MATCH = "If-Match";
		public static final String IF_MOD_SINCE = "If-Modified-Since";
		public static final String IF_NONE_MATCH = "If-None-Match";
		public static final String IF_RANGE = "If-Range";
		public static final String IF_UNMOD_SINCE = "If-Unmodified-Since";
		public static final String MAX_FORWARDS = "Max-Forwards";
		public static final String ORIGIN = "Origin";
		public static final String PRAGMA = "Pragma";
		public static final String PROXY_AUTH = "Proxy-Authorization";
		public static final String RANGE = "Range";
		public static final String REFERER = "Referer";
		public static final String TE = "TE";
		public static final String USER_AGENT = "User-Agent";
		public static final String UPGRADE = "Upgrade";
		public static final String VIA = "Via";
		public static final String Warning = "Warning";
	}

	public static final class RESPONSE {
		public static final String ACCESS_CTRL_ORIGIN = "Access-Control-Allow-Origin";
		public static final String ACCEPT_RANGES = "Accept-Ranges";
		public static final String AGE = "Age";
		public static final String ALLOW = "Allow";
		public static final String CACHE_CTRL = "Cache-Control";
		public static final String CONNECTION = REQUEST.CONNECTION;
		public static final String CONTENT_ENC = "Content-Encoding";
		public static final String CONTENT_LANG = "Content-Language";
		public static final String CONTENT_LEN = REQUEST.CONTENT_LEN;
		public static final String CONTENT_LOC = "Content-Location";
		public static final String CONTENT_MD5 = REQUEST.CONTENT_MD5;
		public static final String CONTENT_DISP = "Content-Disposition";
		public static final String CONTENT_RANGE = "Content-Range";
		public static final String CONTENT_TYPE = REQUEST.CONTENT_TYPE;
		public static final String DATE = REQUEST.DATE;
		public static final String ETAG = "ETag";
		public static final String EXPIRES = "Expires";
		public static final String LAST_MOD = "Last-Modified";
		public static final String LINK = "Link";
		public static final String LOCATION = "Location";
		public static final String P3P = "P3P";
		public static final String PRAGMA = REQUEST.PRAGMA;
		public static final String PROXY_AUTH = "Proxy-Authenticate";
		public static final String REFRESH = "Refresh";
		public static final String RETRY_AFTER = "Retry-After";
		public static final String SET_COOKIE = "Set-Cookie";
		public static final String STATUS = "Status";
		public static final String STRICT_TRANS_SEC = "Strict-Transport-Security";
		public static final String TRAILER = "Trailer";
		public static final String TRANSFER_ENC = "Transfer-Encoding";
		public static final String UPGRADE = REQUEST.UPGRADE;
		public static final String VARY = "Vary";
		public static final String VIA = REQUEST.VIA;
		public static final String WARNING = REQUEST.Warning;
		public static final String WWW_AUTH = "WWW-Authenticate";
	}

	public static final class X {
		public static final String FRAME_OPTIONS = "X-Frame-Options";
		public static final String REQUESTED_WITH = "X-Requested-With";
		public static final String DNT = "DNT";
		public static final String FWD_FOR = "X-Forwarded-For";
		public static final String FWD_PROTO = "X-Forwarded-Proto";
		public static final String ATT_DEVICE_ID = "X-ATT-DeviceId";
		public static final String WAP_PROFILE = "X-Wap-Profile";
		public static final String PK_PINS = "Public-Key-Pins";
		public static final String XSS_PROTECT = "X-XSS-Protection";
		public static final String CSP = "X-Content-Security-Policy";
		public static final String CTO = "X-Content-Type-Options";
		public static final String POWERED_BY = "X-Powered-By";
		public static final String UA_COMPATIBLE = "X-UA-Compatible";
	}

	public static final class Header {
		private final String name;

		private String value;

		public Header(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return name + ": " + value + "\r\n";
		}

		public static Header parse(String line) {
			if (line == null) return null;
			String[] splits = line.split(": ", 2);
			if (splits.length == 2) {
				return new Header(splits[0], splits[1]);
			} else return new Headers.Header(splits[0], "");
		}
	}

	public void add(Header h) {
		if (h != null) headerList.add(h);
	}

	public void add(String name, String value) {
		add(new Header(name, value));
	}

	public void update(String name, String value) {
		Header header = findFirst(name);
		if (header != null) header.setValue(value);
		else add(name, value);
	}

	public void setCookie(Cookie cookie) {
		add(RESPONSE.SET_COOKIE, cookie.toString());
	}

	public void remove(Header h) {
		headerList.remove(h);
	}

	public void remove(String name) {
		for (Header h : headerList) {
			if (h.getName().equals(name)) headerList.remove(h);
		}
	}

	public Header findFirst(String name) {
		for (Header h : headerList) {
			if (h.getName().equals(name)) return h;
		}
		return null;
	}

	public String get(String name) {
		Header header = findFirst(name);
		if (header != null) return header.getValue();
		return null;
	}

	public List<Header> findAll(String name) {
		List<Header> vals = new LinkedList<Header>();
		for (Header h : headerList) {
			if (h.getName().equals(name)) vals.add(h);
		}
		if (vals.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(vals);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Header h : headerList)
			sb.append(h.toString());
		return sb.toString();
	}

	public static Headers parse(String lines) {
		Headers headers = new Headers();
		for (String line : lines.split("\\r\\n")) {
			headers.add(Header.parse(line));
		}
		return headers;
	}
}
