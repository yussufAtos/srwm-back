package com.afp.iris.sr.wm.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import com.afp.iris.sr.wm.clients.ScomRestClient;
import com.afp.iris.sr.wm.domain.ComponentsService;
import com.afp.iris.sr.wm.domain.exception.ComponentNotFoundException;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ComponentsServiceImpl implements ComponentsService {
	private final ScomRestClient scomRestClient;

	public ComponentsServiceImpl(ScomRestClient scomRestClient) {
		this.scomRestClient = scomRestClient;
	}

	public ResponseEntity<byte[]> getComponent(String id) {
		log.debug("START ComponentsService-getComponent {}", id);
		ResponseEntity<byte[]> response = null;

		try {
			response = this.scomRestClient.getComponent(id);
		} catch (RestClientResponseException e) {
			throw new InternalTechnicalException(e,
					"Unable to get component from SCOM. Reason : status code=%s message=%s", e.getRawStatusCode(),
					e.getResponseBodyAsString());
		}

		if (response == null || response.getBody() == null ) {
			throw new InternalTechnicalException("Unable to get component from SCOM Response : %s ", response);
		}
		
		if(response.getStatusCode() == HttpStatus.NOT_FOUND) {
			throw new ComponentNotFoundException("Component %s not found in SCOM", id);
		}
	
		log.debug("END ComponentsService-getComponent {}", id);

		return response;
	}
}
