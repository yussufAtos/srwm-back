package com.afp.iris.sr.wm.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import com.afp.iris.sr.wm.TestUtils;
import com.afp.iris.sr.wm.clients.ScomRestClient;
import com.afp.iris.sr.wm.domain.ComponentsService;
import com.afp.iris.sr.wm.domain.exception.ComponentNotFoundException;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import com.afp.iris.sr.wm.domain.exception.PosterNotFoundException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ComponentsServiceTest {

	private static final String COMPONENT_ID_TEST = "614b915d5758eb7f9fa0edb2a4625699386a3406";

	@Autowired
	ComponentsService componentsService;

	@MockBean
	ScomRestClient scomRestClient;

	@Test
	void should_getComponent_by_id_return_expected_response_entity_when_requesting_scom_for_poster()
			throws IOException {

		// GIVEN

		byte[] mediaPoster = TestUtils.getPosterContentAsByteArray();
		ResponseEntity<byte[]> mockResponseEntity = new ResponseEntity<byte[]>(mediaPoster, HttpStatus.OK);

		// WHEN

		when(scomRestClient.getComponent(COMPONENT_ID_TEST)).thenReturn(mockResponseEntity);
		ResponseEntity<byte[]> actualResponseEntity = componentsService.getComponent(COMPONENT_ID_TEST);

		// THEN

		assertEquals(mockResponseEntity, actualResponseEntity);

	}

	@Test
	void should_getComponent_by_id_throw_InternalTechnicalException_when_requesting_scom_for_poster_throw_HttpClientErrorException_with_not_found_status()
			throws IOException {

		// WHEN

		HttpClientErrorException mockHttpClientErrorException = new HttpClientErrorException(null, HttpStatus.NOT_FOUND,
				null, null, "URL not found".getBytes(), null);
		when(scomRestClient.getComponent(COMPONENT_ID_TEST)).thenThrow(mockHttpClientErrorException);

		// THEN

		InternalTechnicalException internalTechnicalException = assertThrows(InternalTechnicalException.class,
				() -> componentsService.getComponent(COMPONENT_ID_TEST));

		assertEquals("Unable to get component from SCOM. Reason : status code=404 message=URL not found",
				internalTechnicalException.getMessage());
	}

	@Test
	void should_getComponent_by_id_throw_InternalTechnicalException_exception_when_requesting_scom_for_poster_return_null_response()
			throws IOException {

		// WHEN

		when(scomRestClient.getComponent(COMPONENT_ID_TEST)).thenReturn(null);

		// THEN

		assertThrows(InternalTechnicalException.class, () -> componentsService.getComponent(COMPONENT_ID_TEST));

	}

	@Test
	void should_getComponent_by_id_throw_InternalTechnicalException_exception_when_requesting_scom_for_poster_return_null_response_body()
			throws IOException {

		// GIVEN

		ResponseEntity<byte[]> mockResponseEntity = new ResponseEntity<>(null, HttpStatus.OK);

		// WHEN

		when(scomRestClient.getComponent(COMPONENT_ID_TEST)).thenReturn(mockResponseEntity);

		// THEN

		assertThrows(InternalTechnicalException.class, () -> componentsService.getComponent(COMPONENT_ID_TEST));

	}
	
	@Test
	void should_getComponent_by_id_throw_InternalTechnicalException_exception_when_requesting_scom_for_poster_return_404()
			throws IOException {

		// GIVEN

		ResponseEntity<byte[]> mockResponseEntity = new ResponseEntity<>("test".getBytes(), HttpStatus.NOT_FOUND);

		// WHEN

		when(scomRestClient.getComponent(COMPONENT_ID_TEST)).thenReturn(mockResponseEntity);

		// THEN
		
		ComponentNotFoundException componentNotFoundException =	assertThrows(ComponentNotFoundException.class, () -> componentsService.getComponent(COMPONENT_ID_TEST));
		assertEquals("Component "+COMPONENT_ID_TEST+" not found in SCOM" ,componentNotFoundException.getMessage());

	}

}
