package io.myweb;

import java.io.IOException;
import java.util.List;

import io.myweb.http.HttpException;
import io.myweb.http.HttpInternalErrorException;
import io.myweb.http.HttpNotFoundException;
import io.myweb.http.Method;
import io.myweb.http.MimeTypes;
import io.myweb.http.Request;
import io.myweb.http.Response;

public class RequestProcessor {
	private static final String INDEX_HTML = "/index.html";
	private static final String SERVICES_JSON = "/services.json";
	private final List<? extends Endpoint> endpoints;
	private final List<Filter> filters;

	public RequestProcessor(List<? extends Endpoint> endpoints, List<Filter> filters) {
		this.endpoints = endpoints;
		this.filters = filters;
	}

	public Response processRequest(Request request) throws HttpException, IOException {
		String uri = request.getURI().toString();
		String effectiveUri = uri;
		Response response = null;
		while (response == null) {
			Endpoint endpoint = findEndpoint(request.getMethod(), effectiveUri);
			// TODO think how to handle better default requests (like "/index.html" on "/")
			if (("/".equals(uri) || "".equals(uri)) && endpoint == null) {
				effectiveUri = INDEX_HTML;
				endpoint = findEndpoint(request.getMethod(), effectiveUri);
			}
			if (("/".equals(uri) || "".equals(uri)) && endpoint == null) {
				effectiveUri = SERVICES_JSON;
				endpoint = findEndpoint(request.getMethod(), effectiveUri);
			}
			if (endpoint == null) {
				throw new HttpNotFoundException("Not found " + uri);
			} else {
				try {
					Request filteredRequest = filterBefore(effectiveUri, request);
					if (filteredRequest.isRedirected()) {
						request = filteredRequest.withRedirection(null);
						continue;
					}
					response = endpoint.invoke(effectiveUri, filteredRequest);
					response = filterAfter(effectiveUri, response);
					response = ensureProperContentType(response, endpoint.produces());
				} catch (Throwable t) {
					if (response != null) response.onError(t);
					if (t instanceof HttpException) throw (HttpException) t;
					if (t instanceof IOException) throw (IOException) t;
					throw new HttpInternalErrorException(t.getMessage(), t);
				}
			}
		}
		return response;
	}

	private Response ensureProperContentType(Response response, String defaultContentType) {
		String contentType = response.getContentType();
		if (defaultContentType == null) defaultContentType = MimeTypes.MIME_TEXT_HTML;
		if (contentType == null) contentType = defaultContentType;
		// if content type is text, make sure charset is specified
		if (!contentType.contains("charset=") && isTextContent(contentType)) {
			contentType = contentType + "; charset=" + response.getCharset();
		}
		return response.withContentType(contentType);
	}

	private static boolean isTextContent(String content) {
		return content.contains("text") || content.contains("xml") || content.contains("json");
	}

	private Response filterAfter(String effectiveUri, Response response) {
		for (Filter filter: filters) {
			if (filter.matchAfter(effectiveUri)) response = filter.after(response);
		}
		return response;
	}

	private Request filterBefore(String effectiveUri, Request request) {
		for (Filter filter: filters) {
			if (filter.matchBefore(effectiveUri)) {
				request = filter.before(request);
				if (request.isRedirected()) break;
			}
		}
		return request;
	}

	private Endpoint findEndpoint(Method method, String uri) {
		for (Endpoint endpoint : endpoints) {
			if (endpoint.match(method, uri)) {
				return endpoint;
			}
		}
		return null;
	}
}
