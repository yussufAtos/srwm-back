package com.afp.iris.sr.wm.presentation;

import com.afp.iptc.g2.libg2api.BadNewsMLG2Exception;
import com.afp.iptc.g2.libg2api.G2Document;
import com.afp.iptc.g2.libg2api.G2WebStoryDocument;
import com.afp.iris.sr.wm.TestUtils;
import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.domain.DocumentService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    private static final String HEADER_TRANSACTION_ID = "X-AFP-TRANSACTION-ID";
    private static final String GUID_TEST = "http://somehost:someport/someEndpoint/someGUID";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppProperties properties;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private G2Document webStoryDocument;

    @Test
    void post_validate_new_webstory_should_return_validated_document_endpoint_in_header_location() throws Exception {
        // GIVEN
        final G2WebStoryDocument webStoryDocument = TestUtils.getSampleWebStoryDocument();
        when(documentService.validateNewDocument(any())).thenReturn(webStoryDocument);

        // WHEN
        String expectedLocationUri = URI.create(properties.getDocumentsEndpoint()).resolve("?guid=" + webStoryDocument.getGUID()).toString();
        this.mockMvc.perform(

                        post("/documents/")
                                .header(HEADER_TRANSACTION_ID, "TRANSACTION-ID-FROM-TEST")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtils.SAMPLE_JNEWS_STRING)
                )
                // THEN
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.equalTo(expectedLocationUri)))
                .andDo(MockMvcResultHandlers.print());
    }

	@Test
	void get_document_by_guid_should_return_expected_document_for_a_given_guid() throws Exception {

		// GIVEN

		final G2WebStoryDocument webStoryDocument = TestUtils.getSampleWebStoryDocument();
		when(documentService.getDocumentByGuid(GUID_TEST)).thenReturn(webStoryDocument);

		// WHEN
		RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/documents/").param("guid", GUID_TEST).accept(MediaType.APPLICATION_JSON);
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();

		// THEN
		assertEquals("application/json", result.getResponse().getContentType());
		assertEquals(webStoryDocument.jnewsRepresentation(), result.getResponse().getContentAsString());
	}

	@Test
	void get_document_by_guid_should_throw_internalTechnical_exception_when_jnews_representation_throw_bad_newsML_g2_exception() throws Exception {

		// GIVEN
		when(documentService.getDocumentByGuid(GUID_TEST)).thenReturn(webStoryDocument);
		when(webStoryDocument.jnewsRepresentation()).thenThrow(BadNewsMLG2Exception.class);

		// WHEN & THEN
		mockMvc.perform(get("/documents/").param("guid", GUID_TEST).accept(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError());
	}

	@Test
	void post_validate_update_document_should_return_validated_document() throws Exception {

		// GIVEN
		final G2WebStoryDocument webStoryDocument = TestUtils.getSampleWebStoryDocument();
		when(documentService.validateUpdateDocument(any())).thenReturn(webStoryDocument);

		// WHEN
		RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/documents/")
				                                              .header(HEADER_TRANSACTION_ID, "TRANSACTION-ID-FROM-TEST")
				                                              .param("guid", "guid")
				                                              .contentType(MediaType.APPLICATION_JSON)
				                                              .content(TestUtils.SAMPLE_JNEWS_WITH_SELF_LINK_STRING);

		MvcResult result = mockMvc.perform(requestBuilder).andReturn();

		// THEN
		assertEquals("application/json", result.getResponse().getContentType());
		assertEquals(webStoryDocument.jnewsRepresentation(), result.getResponse().getContentAsString());
	}

	@Test
	void post_validate_update_document_should_throw_internal_technical_exception_when_jnews_representation_throw_bad_newsML_g2_exception() throws Exception {

		// GIVEN
		when(documentService.validateUpdateDocument(any())).thenReturn(webStoryDocument);
		when(webStoryDocument.jnewsRepresentation()).thenThrow(BadNewsMLG2Exception.class);

		// WHEN & THEN
		mockMvc.perform(
				    post("/documents/").header(HEADER_TRANSACTION_ID, "TRANSACTION-ID-FROM-TEST")
				                       .param("guid", "guid")
				                       .contentType(MediaType.APPLICATION_JSON).content(TestUtils.SAMPLE_JNEWS_STRING))
		                               .andExpect(status().isInternalServerError());
	}

	@Test
	void post_depublish_webStory_should_return_depublished_document() throws Exception {

		// GIVEN
		final G2WebStoryDocument webStoryDocument = TestUtils.getSampleWebStoryDocument();
		when(documentService.depublishDocument(any())).thenReturn(webStoryDocument);


		RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/documents/depublisher")
				                                              .header(HEADER_TRANSACTION_ID, "TRANSACTION-ID-FROM-TEST")
				                                              .param("guid", "guid")
				                                              .contentType(MediaType.APPLICATION_JSON)
				                                              .content(TestUtils.SAMPLE_JNEWS_WITH_SELF_LINK_STRING);

		MvcResult result = mockMvc.perform(requestBuilder).andReturn();



        // THEN
		assertEquals(webStoryDocument.jnewsRepresentation(), result.getResponse().getContentAsString());
		}

	@Test
	void post_should_depublish_webStory_throw_Internal_technical_exception_when_jnews_representation_throw_bad_newsML_g2_exception() throws Exception {

		// GIVEN
		when(documentService.depublishDocument(any())).thenReturn(webStoryDocument);
		when(webStoryDocument.jnewsRepresentation()).thenThrow(BadNewsMLG2Exception.class);

		// WHEN & THEN
		mockMvc.perform(
				post("/documents/depublisher")
				                      .header(HEADER_TRANSACTION_ID, "TRANSACTION-ID-FROM-TEST").param("guid", "guid")
				                      .contentType(MediaType.APPLICATION_JSON).content(TestUtils.SAMPLE_JNEWS_WITH_SELF_LINK_STRING))
		                              .andExpect(status()
		                              .isInternalServerError());
	}

	@Test
	void get_document_by_id_should_return_expected_document() throws Exception {
		// GIVEN
		final G2WebStoryDocument webStoryDocument = TestUtils.getSampleWebStoryDocument();
		when(documentService.getDocumentById("123sha1")).thenReturn(webStoryDocument);

		// WHEN
		RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/documents/" + "123sha1");
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();

		// THEN
		assertEquals(webStoryDocument.jnewsRepresentation(), result.getResponse().getContentAsString());
	}

}
