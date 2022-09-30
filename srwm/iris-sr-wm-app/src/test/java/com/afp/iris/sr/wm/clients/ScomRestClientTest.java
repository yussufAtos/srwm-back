package com.afp.iris.sr.wm.clients;

import com.afp.iris.sr.wm.TestUtils;
import com.afp.iris.sr.wm.clients.dto.ResizingResults;
import com.afp.iris.sr.wm.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static com.afp.iris.sr.wm.TestUtils.getClassPathResource;
import static com.afp.iris.sr.wm.TestUtils.getSampleResizingResults;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ScomRestClientTest {
   
	private static final String COMPONENT_ID_TEST="614b915d5758eb7f9fa0edb2a4625699386a3406";
	private static final String URI_TEST = "http://test.scom/components/614b915d5758eb7f9fa0edb2a4625699386a3406";
	private static final int MEDIA_POSTER_SIZE = 126_993;
	
	@Autowired
    ScomRestClient scomRestClient;
    @Autowired
    AppProperties properties;

    @MockBean(name = "scomRestTemplate")
    RestTemplate scomRestTemplate;

    @Test
    void upload_should_return_location_in_header() throws IOException {
        // GIVEN
        final InputStream webStoryZipIs = getClassPathResource("webstory/balkany.zip").getInputStream();
        final URI webStoryZipIsURI = URI.create("http://somehost:someport/someEndpoint/someResource");
        final ResponseEntity<String> scomResponse = ResponseEntity.created(webStoryZipIsURI).build();

        when(scomRestTemplate.exchange(eq(properties.getScom().getComponentsEndpoint()), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(scomResponse);

        // WHEN
        final ResponseEntity<String> response = scomRestClient.uploadFileToScom(webStoryZipIs);

        // THEN
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(webStoryZipIsURI, response.getHeaders().getLocation());
    }

    @Test
    void scom_should_respond_200_with_resizing_result_in_XML () throws JsonProcessingException {
        // GIVEN
        ResizingResults sampleResizingResults = getSampleResizingResults();
        URI expectedThumbnailUri = sampleResizingResults.getResults().get(0).getResultUrl();
        final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity.ok(sampleResizingResults);

        when(scomRestTemplate.exchange(eq(properties.getScom().getRenditionsEndpoint()), eq(HttpMethod.POST), any(),
                eq(ResizingResults.class))).thenReturn(mockedGenerateThumbnailResponse);

        // WHEN
        URI mockedPosterUri = URI.create("http://test.scom.com:8080/components/1189fec85edc347b6");
        final ResponseEntity<ResizingResults> response = scomRestClient.generateThumbnail(mockedPosterUri);

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedThumbnailUri, response.getBody().getResults().get(0).getResultUrl());
    }
    
	@Test
	void getComponent_from_scom_should_return_expected_media_poster_as_byte_array() throws IOException {

		// GIVEN
		byte[] mediaPoster = TestUtils.getPosterContentAsByteArray();
		ResponseEntity<byte[]> scomResponse = new ResponseEntity<byte[]>(mediaPoster, HttpStatus.OK);
		when(scomRestTemplate.exchange(eq(URI_TEST), eq(HttpMethod.GET), any(), eq(byte[].class))).thenReturn(scomResponse);

		// WHEN
		final ResponseEntity<byte[]> response = scomRestClient.getComponent(COMPONENT_ID_TEST);

		// THEN
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(MEDIA_POSTER_SIZE, response.getBody().length);

	}

}
