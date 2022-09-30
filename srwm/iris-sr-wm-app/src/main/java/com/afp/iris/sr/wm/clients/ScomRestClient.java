package com.afp.iris.sr.wm.clients;

import com.afp.iris.sr.wm.clients.dto.ResizingResults;
import com.afp.iris.sr.wm.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

@Component
@Slf4j
public class ScomRestClient extends AbstractClient {
	private static final String MULTIPART_SCOM_FILE_KEY = "file";
	private static final String OPERATIONS_XML_TEMPLATE = """
			<?xml version="1.0" encoding="utf-8" standalone="yes"?>
			<operations xmlns="http://sr.iris.afp.com/operations">
			    <operationWithDimension url="{url}">
			        <resizeWithDimension>
			            <height>{height}</height>
			            <width>{width}</width>
			            <type>{type}</type>
			        </resizeWithDimension>
			    </operationWithDimension>
			</operations>
			""";


	private final AppProperties properties;
	private final RestTemplate restTemplate;

	public ScomRestClient(AppProperties properties, RestTemplate scomRestTemplate) {
		this.properties = properties;
		this.restTemplate = scomRestTemplate;
	}

	public ResponseEntity<String> uploadFileToScom(InputStream fileInputStream) throws IOException { 
		log.debug("START ScomRestClient-uploadFileToScom");
		HttpHeaders headers = new HttpHeaders();
		headers.add(HEADER_X_AFP_TRANSACTION_ID, MDC.get(HEADER_X_AFP_TRANSACTION_ID));
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		final ByteArrayResource byteArrayResource = new ByteArrayResource(fileInputStream.readAllBytes());
		body.add(MULTIPART_SCOM_FILE_KEY, byteArrayResource);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		String scomComponentsEndpointUrl = properties.getScom().getComponentsEndpoint();
		ResponseEntity<String> response = restTemplate.exchange(scomComponentsEndpointUrl, HttpMethod.POST,
				requestEntity, String.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, SCOM, HttpMethod.POST, scomComponentsEndpointUrl,
				response.getStatusCode());
		log.debug("END ScomRestClient-uploadFileToScom");

		return response;
	}

	public ResponseEntity<ResizingResults> generateThumbnail (URI imageUri) {
		log.debug("START ScomRestClient-generateThumbnail from URI {}", imageUri);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.afp.iris.sr.rendition-operations+xml");
		headers.add(HEADER_X_AFP_TRANSACTION_ID, MDC.get(HEADER_X_AFP_TRANSACTION_ID));

		String operationsXml = buildOperationsXml(imageUri);
		HttpEntity<String> requestEntity = new HttpEntity<>(operationsXml, headers);

		String renditionsEndpoint = properties.getScom().getRenditionsEndpoint();
		final ResponseEntity<ResizingResults> response = restTemplate.exchange(renditionsEndpoint, HttpMethod.POST,
				requestEntity, ResizingResults.class);

		log.info(LOG_REQUEST_WITH_URL_RESPONSE, SCOM, HttpMethod.POST, renditionsEndpoint,
				response.getStatusCode());

		log.debug("END ScomRestClient-generateThumbnail");

		return response;
	}

	private String buildOperationsXml(URI imageUri) {
		String operationsXml = OPERATIONS_XML_TEMPLATE.replace("{height}", properties.getScom().getThumbnail().getHeight());
		operationsXml = operationsXml.replace("{width}", properties.getScom().getThumbnail().getWidth());
		operationsXml = operationsXml.replace("{type}", properties.getScom().getThumbnail().getType());

		operationsXml = operationsXml.replace("{url}", imageUri.toString());

		return operationsXml;
	}

	public ResponseEntity<byte[]> getComponent(String id) {
		log.debug("START ScomRestClient-getComponent {}", id);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HEADER_X_AFP_TRANSACTION_ID, MDC.get(HEADER_X_AFP_TRANSACTION_ID));
		HttpEntity<String> entity = new HttpEntity<>("headers", headers);
		String scomComponentsEndpointUr = properties.getScom().getComponentsEndpointById();
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(scomComponentsEndpointUr).build();
		String uri = uriComponents.expand(Collections.singletonMap("id", id)).toUriString();
		ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, byte[].class);

		log.debug("END ScomRestClient-getComponent {}", id);
		log.info(LOG_REQUEST_WITH_URL_RESPONSE, SCOM, HttpMethod.GET, uri,
				response.getStatusCode());

		return response;
	}
	
}
