package com.afp.iris.sr.wm.clients;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jasig.cas.client.validation.Assertion;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import com.afp.iptc.g2.libg2api.BadNewsMLG2Exception;
import com.afp.iptc.g2.libg2api.G2Document;
import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.domain.exception.NotAuthenticatedException;
import com.afp.iris.sr.wm.utils.UriUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CmsRestClient extends AbstractClient {
	private static final String ASHES_XML_TEMPLATE = """
			<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
			<ashes xmlns="http://sr.iris.afp.com/request">
				<documentRef href="{documentRef}"/>
				<guid>{guid}</guid>
			</ashes>
			""";
	private final AppProperties properties;
	private final RestTemplate restTemplate;

	public CmsRestClient(AppProperties properties, RestTemplate cmsRestTemplate) {
		this.properties = properties;

        // Necessary for the CAS authentication (CMS userinfo redirect)
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        final CloseableHttpClient httpClient = HttpClientBuilder.create()
        		.disableCookieManagement()
				.build();
		factory.setHttpClient(httpClient);

		this.restTemplate = cmsRestTemplate;
		this.restTemplate.setRequestFactory(factory);

		this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
	}

	public ResponseEntity<String> createDocument(G2Document g2Document) throws BadNewsMLG2Exception {
		log.debug("START CmsRestClient-createDocument");

		HttpHeaders headers = buildAuthenticatedHttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_DOCUMENT_NEWSML);
		headers.add(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.toString());

		HttpEntity<String> requestEntity = new HttpEntity<>(g2Document.g2Representation(), headers);

		String documentsEndpoint = properties.getCms().getDocumentsEndpoint();

		final ResponseEntity<String> response = restTemplate.exchange(documentsEndpoint, HttpMethod.POST, requestEntity,
				String.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, CMS, HttpMethod.POST, documentsEndpoint, response.getStatusCode());
		log.debug("END CmsRestClient-createDocument");

		return response;
	}

	public ResponseEntity<String> editDocument(G2Document g2Document) throws BadNewsMLG2Exception {
		log.debug("START CmsRestClient-editDocument");

		// HEADERS
		HttpHeaders headers = buildAuthenticatedHttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_DOCUMENT_NEWSML);
		headers.add(HEADER_X_AFP_DOCUMENT_ETag, g2Document.getEditorialTag());
		headers.add(X_AFP_LOCK_FORCED, "true");
		HttpEntity<String> requestEntity = new HttpEntity<>(g2Document.g2Representation(), headers);

		// EDITOR ENDPOINT TODO : récupérer depuis le G2 LINK TO EDITOR ?
		String documentEditorEndpoint = buildEndpointWithId(g2Document,
				properties.getCms().getDocumentEditorEndpointTemplate());
		final ResponseEntity<String> response = restTemplate.exchange(documentEditorEndpoint, HttpMethod.POST,
				requestEntity, String.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, CMS, HttpMethod.POST, documentEditorEndpoint, response.getStatusCode());
		log.debug("END CmsRestClient-editDocument");

		return response;
	}

	public ResponseEntity<String> validateDocument(G2Document g2Document) throws BadNewsMLG2Exception {
		log.debug("START CmsRestClient-validateDocument");

		final ResponseEntity<String> response = validator(g2Document, DEFAULT_XML_PRODUCTS);

		log.debug("END CmsRestClient-validateDocument"+response);

		return response;
	}

	public ResponseEntity<String> searchLastValidatedDocumentByGuid(String guid, boolean inArchives) {
		log.debug("START CmsRestClient-searchLastValidatedDocumentByGuid (guid={} inArchives={})", guid, inArchives);
		// HEADERS
		HttpHeaders headers = buildAuthenticatedHttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XHTML_XML_VALUE);
		headers.add("X-AFP-ARCHIVE", String.valueOf(inArchives));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		String documentsEndpoint = properties.getCms().getDocumentsEndpoint();
		final String queryValue = String.format("(doc.doc_id='%s')", guid);
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(documentsEndpoint)
				.queryParam("lastValidatedOrApproved", true)
				.queryParam("query", queryValue);

		if (!inArchives) {
			// unpublished param is discriminant when searching in archives
			uriBuilder.queryParam("unpublished", true);
		}

		final URI searchEndpointUri = uriBuilder.build().toUri();

		final ResponseEntity<String> response = restTemplate.exchange(searchEndpointUri, HttpMethod.GET, requestEntity,
				String.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, CMS, HttpMethod.GET, searchEndpointUri, response.getStatusCode());
		log.debug("END CmsRestClient-searchLastValidatedDocumentByGuid {}", guid);

		return response;
	}

	public ResponseEntity<String> depublishDocument(G2Document g2Document) throws BadNewsMLG2Exception {
		log.debug("START CmsRestClient-depublishDocument");

		final ResponseEntity<String> response = validator(g2Document, DEPUBLISH_XML_PRODUCTS);

		log.debug("END CmsRestClient-depublishDocument");

		return response;
	}

	private ResponseEntity<String> validator(G2Document g2Document, String products) throws BadNewsMLG2Exception {

		// HEADERS
		HttpHeaders headers = buildAuthenticatedHttpHeaders();
		// TODO check if HEADER_X_AFP_DOCUMENT_GLOBAL_ETAG is necessary
		headers.add(HEADER_X_AFP_DOCUMENT_ETag, g2Document.getEditorialTag());
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_MIXED_VALUE);
		// BODY
		final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

		// PART 1 - G2 NEWSML
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.parseMediaType(CONTENT_TYPE_DOCUMENT_NEWSML));
		HttpEntity<?> httpEntity = new HttpEntity<>(g2Document.g2Representation(), httpHeaders);
		body.add("Part1", httpEntity);

		// PART 2 - PRODUITS
		httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.parseMediaType(CONTENT_TYPE_VALIDATION_ORDERS));
		httpEntity = new HttpEntity<>(products, httpHeaders);
		body.add("Part2", httpEntity);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		// VALIDATOR ENDPOINT
		String documentValidateEndpoint = buildEndpointWithId(g2Document,
				properties.getCms().getDocumentValidateEndpointTemplate());

		final ResponseEntity<String> response = restTemplate.exchange(documentValidateEndpoint, HttpMethod.POST,
				requestEntity, String.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, CMS, HttpMethod.POST, documentValidateEndpoint,
				response.getStatusCode());
		return response;
	}

	public ResponseEntity<String> getDocumentById(String id) {
		log.debug("START CmsRestClient-getDocumentById {} ", id);
		// HEADERS
		HttpHeaders headers = buildAuthenticatedHttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XHTML_XML_VALUE);
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		String documentsEndpoint = properties.getCms().getDocumentsEndpoint();
		final URI documentByIdEndpointUri = UriComponentsBuilder.fromUriString(documentsEndpoint)
				.pathSegment(id)
				.build()
				.toUri();
       
		final ResponseEntity<String> response = restTemplate.exchange(documentByIdEndpointUri, HttpMethod.GET,
				requestEntity, String.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, CMS, HttpMethod.GET, documentByIdEndpointUri, response.getStatusCode());
		log.debug("END CmsRestClient-getDocumentById {} ",id);

		return response;
	}
	
	public ResponseEntity<String> getDocumentByStoryId(String id) {
		log.debug("START CmsRestClient-getDocumentByStoryId {}", id);

		// HEADERS
		HttpHeaders headers = buildAuthenticatedHttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XHTML_XML_VALUE);

		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		String documentsEndpoint = properties.getCms().getStoriesEndpoint();
		final URI documentByIdEndpointUri = UriComponentsBuilder.fromUriString(documentsEndpoint)
				.pathSegment(id)
				.build()
				.toUri();
		final ResponseEntity<String> response = restTemplate.exchange(documentByIdEndpointUri, HttpMethod.GET,
				requestEntity, String.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, CMS, HttpMethod.GET, documentByIdEndpointUri, response.getStatusCode());
		log.debug("END CmsRestClient-getDocumentByStoryId {}", id);

		return response;
	}


	private String buildEndpointWithId(G2Document g2Document, String endpointTemplate) {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(endpointTemplate).build();
		final String did = UriUtils.extractIdFromUri(g2Document.getLinkToSelf().toString());
		return uriComponents.expand(Collections.singletonMap("did", did)).toUriString();
	}

	public HttpHeaders buildAuthenticatedHttpHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HEADER_X_AFP_TRANSACTION_ID, MDC.get(HEADER_X_AFP_TRANSACTION_ID));
		  final List<String> authCookies = getAuthCookies();
	        for (String authCookie : authCookies) {
	            headers.add(HttpHeaders.COOKIE, authCookie);
			}
	        return headers;
	}
	
    private List<String> getAuthCookies() {
    	HttpServletRequest currentRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    	List<String> cmsJsessionCookieList = new ArrayList<String>();
    	if(currentRequest.getCookies() != null) {
    		cmsJsessionCookieList = Arrays.stream(currentRequest.getCookies())
					.filter(cookie -> COOKIE_NAME.equalsIgnoreCase(cookie.getName()))
					.map(cookie -> new String(Base64.decodeBase64(cookie.getValue())))
					.collect(Collectors.toList());
    	}

    	return cmsJsessionCookieList;
	}

	public Cookie loginToBackend(HttpSession session) {
		HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_X_AFP_TRANSACTION_ID, MDC.get(HEADER_X_AFP_TRANSACTION_ID));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String userinfoEndpoint = properties.getCms().getUserinfoEndpoint();
        String proxyTicket = ((Assertion) (session.getAttribute("_const_cas_assertion_"))).getPrincipal().getProxyTicketFor(userinfoEndpoint);

        if(proxyTicket == null) {
        	log.error("Failed to retrieve proxyTicket from CAS server");
        	throw new NotAuthenticatedException("Failed to retrieve proxyTicket from CAS server");
        }

        String userinfoEndpointWithCas = userinfoEndpoint+"?service="+userinfoEndpoint+"&proxy=test&ticket="+proxyTicket;
        ResponseEntity<String> response;
        try {

            response = restTemplate.exchange(userinfoEndpointWithCas, HttpMethod.GET, entity, String.class);
            log.info(LOG_REQUEST_WITH_URL_RESPONSE, CMS, HttpMethod.GET, userinfoEndpointWithCas, response.getStatusCode());

        } catch (RestClientResponseException e) {
            log.error(AUTHENTICATE_ERROR_MESSAGE_FORMAT, e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new NotAuthenticatedException(e);
        }

        if (response.getStatusCode() != HttpStatus.OK) {
        	log.error("Unable to authenticate to CMS. Status code = {}", response.getStatusCode());
            throw new NotAuthenticatedException("Unable to authenticate to CMS. Status code = " + response.getStatusCode());
        }

        String cookie  = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        Cookie srwmCookie = new Cookie(COOKIE_NAME, new String(Base64.encodeBase64(cookie.getBytes())));
		srwmCookie.setSecure(properties.getSrwmCookie().isSecure());
		srwmCookie.setHttpOnly(true);
		srwmCookie.setDomain(properties.getSrwmCookie().getDomain());
		srwmCookie.setPath(properties.getSrwmCookie().getPath());

		return srwmCookie;
	}

	public ResponseEntity<String> phoenixDocument (String guid, URI selfLink) {
		log.debug("START CmsRestClient-phoenixDocument (guid = {} selfLink = {})", guid, selfLink);

		// HEADERS
		HttpHeaders headers = buildAuthenticatedHttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.afp.iris.sr.ashes+xml");

		String ashesXml = ASHES_XML_TEMPLATE.replace("{documentRef}", selfLink.toString());
		ashesXml = ashesXml.replace("{guid}", guid);
		HttpEntity<String> requestEntity = new HttpEntity<>(ashesXml, headers);

		String documentsEndpoint = properties.getCms().getPhoenixEndpoint();

		final ResponseEntity<String> response = restTemplate.exchange(documentsEndpoint, HttpMethod.POST, requestEntity,
				String.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, CMS, HttpMethod.POST, documentsEndpoint, response.getStatusCode());
		log.debug("END CmsRestClient-phoenixDocument");

		return response;
	}
	
}
