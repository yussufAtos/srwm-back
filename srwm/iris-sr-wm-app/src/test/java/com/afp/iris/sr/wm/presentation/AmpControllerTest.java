package com.afp.iris.sr.wm.presentation;

import static com.afp.iris.sr.wm.TestUtils.createMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.afp.iptc.g2.libg2api.G2WebStoryDocument;
import com.afp.iris.sr.wm.TestUtils;
import com.afp.iris.sr.wm.domain.AmpService;

@WebMvcTest(AmpController.class)
public class AmpControllerTest {
	@MockBean
	private AmpService ampService;
	@Autowired
	private MockMvc mockMvc;
	
	@Test
	void upload_amp_comntent_should_return_valid_jnews_when_ampService_return_G2WebStoryDocument() throws Exception {

		// GIVEN

		final G2WebStoryDocument webStoryDocument = TestUtils.getSampleWebStoryDocument();
		MockMultipartFile mockMultipartFile = TestUtils.createMultipartFile("balkany.zip");

		// WHEN

		when(ampService.uploadAmpContent(mockMultipartFile)).thenReturn(webStoryDocument);
		RequestBuilder requestBuilder = MockMvcRequestBuilders.multipart("/amps").file(mockMultipartFile);
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();

		// THEN

		assertEquals(200, result.getResponse().getStatus());
		assertThat(result.getResponse().getContentAsString().length() > 0);
		assertEquals(result.getResponse().getContentAsString(),webStoryDocument.jnewsRepresentation());

	}

}
