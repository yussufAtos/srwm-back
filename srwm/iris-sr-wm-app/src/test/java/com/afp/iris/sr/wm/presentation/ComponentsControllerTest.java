package com.afp.iris.sr.wm.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.afp.iris.sr.wm.TestUtils;
import com.afp.iris.sr.wm.domain.ComponentsService;
import org.apache.commons.io.IOUtils;

@WebMvcTest(ComponentsController.class)
public class ComponentsControllerTest {

	private static final String COMPONENT_ID_TEST = "614b915d5758eb7f9fa0edb2a4625699386a3406";
	private static final int MEDIA_POSTER_SIZE = 126_993;

	@MockBean
	private ComponentsService componentsService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void get_component_from_scom_should_return_expected_response_when_requesting_for_poster_image() throws Exception {

		// GIVEN

		byte[] mediaPoster = TestUtils.getPosterContentAsByteArray();
		ResponseEntity<byte[]> mockResponseEntity = new ResponseEntity<byte[]>(mediaPoster, HttpStatus.OK);

		// WHEN

		when(componentsService.getComponent(COMPONENT_ID_TEST)).thenReturn(mockResponseEntity);
		RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/components/614b915d5758eb7f9fa0edb2a4625699386a3406");
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();

		// THEN

		assertEquals(MEDIA_POSTER_SIZE, result.getResponse().getContentLength());
		assertEquals(200, result.getResponse().getStatus());
		assertEquals("image/jpeg", result.getResponse().getContentType());

	}

}
