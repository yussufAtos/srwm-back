package com.afp.iris.sr.wm.services;

import static com.afp.iris.sr.wm.TestUtils.getSampleResizingResults;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.afp.iris.sr.wm.clients.dto.ResizingResults;
import com.afp.iris.sr.wm.helper.G2Helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockMultipartFile;
import com.afp.iptc.g2.libg2api.G2WebStoryDocument;
import static com.afp.iris.sr.wm.TestUtils.createMultipartFile;
import com.afp.iris.sr.wm.clients.ScomRestClient;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import com.afp.iris.sr.wm.domain.exception.PosterNotFoundException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import com.afp.iris.sr.wm.services.AmpServiceImpl;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class AmpServiceTest {
	private static final URI MOCKED_SCOM_URI = URI.create("http://test.scom.com:8080/components/1189fec85edc347b6");
	private static final Path mockTempAmpHtmlPath = Paths.get("src/test/resources/webstory/index.amp-test.html");
	private static final Path ampHtmlPathTarget = Paths.get("src/test/resources/webstory/index.amp-test-target.html");
	private static final Path mockAmpZipPathTarget= Paths.get("src/test/resources/webstory/mock-zip-file-target.zip");
	
	@Autowired
	AmpServiceImpl ampServiceImpl;
	@MockBean
	ScomRestClient scomRestClient;

	@Test
	void should_return_expected_G2WebStoryDocument_when_amp_zip_is_valid() throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFileValide = createMultipartFile("balkany.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();
		ResizingResults sampleResizingResults = getSampleResizingResults();
		URI expectedThumbnailUri = sampleResizingResults.getResults().get(0).getResultUrl();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity.ok(sampleResizingResults);

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);
		when(scomRestClient.generateThumbnail(any())).thenReturn(mockedGenerateThumbnailResponse);
		G2WebStoryDocument g2WebStoryDocument = ampServiceImpl.uploadAmpContent(mockMultipartFileValide);

		// THEN
		assertNotNull(g2WebStoryDocument.getGUID());
		assertEquals("Balkany OK", g2WebStoryDocument.getTitle());
		assertEquals(MOCKED_SCOM_URI, g2WebStoryDocument.getRenditions().get(0).getHref());
		assertEquals(MOCKED_SCOM_URI, G2Helper.getPreviewIcon(g2WebStoryDocument).getHref());
		assertEquals(expectedThumbnailUri, G2Helper.getThumbnailIcon(g2WebStoryDocument).getHref());
	}

	@Test
	void should_throw_PosterNotFoundException_when_poster_is_blank() throws IOException {
		// GIVEN
		MockMultipartFile mockMultipartFileWithBlankPoster = createMultipartFile("balkany-blank-poster.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);

		// THEN
		assertThrows(PosterNotFoundException.class,
				() -> ampServiceImpl.uploadAmpContent(mockMultipartFileWithBlankPoster));

	}

	@Test
	void should_throw_URISyntaxException_when_poster_Uri_is_invalid() throws IOException {
		// GIVEN
		MockMultipartFile mockMultipartFileWithInvalidPoster = createMultipartFile("balkany-invalid-poster.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);

		// THEN
		assertThrows(PosterNotFoundException.class,
				() -> ampServiceImpl.uploadAmpContent(mockMultipartFileWithInvalidPoster));

	}

	@Test
	void should_throw_ResponseStatusException_when_story_html_is_missing() throws IOException {
		// GIVEN
		MockMultipartFile mockMultipartFileWithMissingStoryHtml = createMultipartFile("balkany-missing-story-html.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);

		// THEN
		final ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class,
				() -> ampServiceImpl.uploadAmpContent(mockMultipartFileWithMissingStoryHtml));
		assertEquals(HttpStatus.BAD_REQUEST, responseStatusException.getStatus());
		assertEquals("Cannot find the AMP HTML content in the ZIP file", responseStatusException.getReason());

	}

	@Test
	void should_throw_PosterNotFoundException_when_story_html_is_missing() throws IOException {
		// GIVEN
		MockMultipartFile mockMultipartFileWithMissingPoster = createMultipartFile("balkany-missing-poster.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);

		// THEN
		assertThrows(PosterNotFoundException.class,
				() -> ampServiceImpl.uploadAmpContent(mockMultipartFileWithMissingPoster));
	}

	@Test
	void should_throw_InternalTechnicalException_when_uploading_to_scom_throws_IOException() throws IOException {
		// GIVEN
		MockMultipartFile mockMultipartFile = createMultipartFile("balkany.zip");
		when(scomRestClient.uploadFileToScom(any())).thenThrow(IOException.class);

		// WHEN & THEN
		assertThrows(InternalTechnicalException.class, () -> ampServiceImpl.uploadAmpContent(mockMultipartFile));
	}

	@Test
	void should_throw_ResponseStatusException_when_zip_file_is_invalid() throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFileWithInvalidZip = createMultipartFile("somefile.txt");

		// WHEN & THEN

		final ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class,
				() -> ampServiceImpl.uploadAmpContent(mockMultipartFileWithInvalidZip));
		assertEquals(HttpStatus.BAD_REQUEST, responseStatusException.getStatus());
		assertEquals("Uploaded AMP content is not a valid zip", responseStatusException.getReason());
	}

	@Test
	void should_throw_InternalTechnicalException_when_scom_response_is_null() throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFile = createMultipartFile("balkany.zip");

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(null);

		// THEN
		assertThrows(InternalTechnicalException.class, () -> ampServiceImpl.uploadAmpContent(mockMultipartFile));
	}

	@Test
	void should_throw_InternalTechnicalException_when_scom_response_location_is_null() throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFile = createMultipartFile("balkany.zip");
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setLocation(null);
		final ResponseEntity<String> scomResponse = ResponseEntity.status(HttpStatus.CREATED).headers(responseHeaders)
				.body(null);

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(scomResponse);

		// THEN
		assertThrows(InternalTechnicalException.class, () -> ampServiceImpl.uploadAmpContent(mockMultipartFile));

	}
	
	@Test
	void should_return_expected_G2WebStoryDocument_when_amp_zip_is_storyfime() throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFileValide = createMultipartFile("amp-storifyme.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();
		ResizingResults sampleResizingResults = getSampleResizingResults();
		URI expectedThumbnailUri = sampleResizingResults.getResults().get(0).getResultUrl();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity.ok(sampleResizingResults);

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);
		when(scomRestClient.generateThumbnail(any())).thenReturn(mockedGenerateThumbnailResponse);
		G2WebStoryDocument g2WebStoryDocument = ampServiceImpl.uploadAmpContent(mockMultipartFileValide);

		// THEN
		assertNotNull(g2WebStoryDocument.getGUID());
		assertEquals("irak tourisme couleurs", g2WebStoryDocument.getTitle());
		assertEquals(MOCKED_SCOM_URI, g2WebStoryDocument.getRenditions().get(0).getHref());
		assertEquals(MOCKED_SCOM_URI, G2Helper.getPreviewIcon(g2WebStoryDocument).getHref());
		assertEquals(expectedThumbnailUri, G2Helper.getThumbnailIcon(g2WebStoryDocument).getHref());
	}
	
	@Test
	void should_getMetadataLanguage_and_getContentLanguage_of_g2WebStoryDocument_object_return_expected_lang_when_amp_zip_does_not_contains_lang_attribute()
			throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFileWithLang = createMultipartFile("queenlong.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity.ok(getSampleResizingResults());

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);
		when(scomRestClient.generateThumbnail(any())).thenReturn(mockedGenerateThumbnailResponse);
		G2WebStoryDocument g2WebStoryDocument = ampServiceImpl.uploadAmpContent(mockMultipartFileWithLang);

		// THEN
		assertEquals(g2WebStoryDocument.getMetadataLanguage(), "en");
		assertEquals(g2WebStoryDocument.getContentLanguage(), "en");
	}
	
	@Test
	void should_getMetadataLanguage_and_getContentLanguage_of_g2WebStoryDocument_object_return_null_when_amp_zip_does_not_contains_lang_attribute()
			throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFileWithLang = createMultipartFile("balkany.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity.ok(getSampleResizingResults());

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);
		when(scomRestClient.generateThumbnail(any())).thenReturn(mockedGenerateThumbnailResponse);

		G2WebStoryDocument g2WebStoryDocument = ampServiceImpl.uploadAmpContent(mockMultipartFileWithLang);

		// THEN
		assertNull(g2WebStoryDocument.getMetadataLanguage());
		assertNull(g2WebStoryDocument.getContentLanguage());
	}

	@Test
	void should_throw_InternalTechnicalException_when_RestClientResponseException_is_thrown_during_scom_call() throws IOException {
		// GIVEN
		MockMultipartFile mockMultipartFileValide = createMultipartFile("balkany.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);
		RestClientResponseException restClientResponseException = new RestClientResponseException("message", 500, "500", null, "Exception".getBytes(StandardCharsets.UTF_8), null);
		when(scomRestClient.generateThumbnail(any())).thenThrow(restClientResponseException);

		// WHEN & THEN
		assertThrows(InternalTechnicalException.class, () -> ampServiceImpl.uploadAmpContent(mockMultipartFileValide));
	}

	@Test
	void should_return_InternalTechnicalException_when_scom_returns_null_body_for_generateThumbnail() throws IOException {
		// GIVEN
		MockMultipartFile mockMultipartFileValide = createMultipartFile("balkany.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity.ok(null);

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);
		when(scomRestClient.generateThumbnail(any())).thenReturn(mockedGenerateThumbnailResponse);

		// WHEN & THEN
		assertThrows(InternalTechnicalException.class, () -> ampServiceImpl.uploadAmpContent(mockMultipartFileValide));
	}

	@Test
	void should_return_InternalTechnicalException_when_generateThumbnail_returns_empty_ResizingResults() throws IOException {
		// GIVEN
		MockMultipartFile mockMultipartFileValide = createMultipartFile("balkany.zip");
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.created(MOCKED_SCOM_URI).build();
		ResizingResults emptyResizingResults = new ResizingResults();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity.ok(emptyResizingResults);

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);
		when(scomRestClient.generateThumbnail(any())).thenReturn(mockedGenerateThumbnailResponse);

		// WHEN & THEN
		assertThrows(InternalTechnicalException.class, () -> ampServiceImpl.uploadAmpContent(mockMultipartFileValide));
	}
	
	@Test
	void should_uploadFileToScom_throw_InternalTechnicalException_when_RestClientResponseException_is_thrown_during_scom_call_to_upload_file()
			throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFileValide = createMultipartFile("balkany.zip");
		ResizingResults sampleResizingResults = getSampleResizingResults();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity
				.ok(sampleResizingResults);
		RestClientResponseException mockRestClientResponseException = new RestClientResponseException(null, 500, null,
				null, "message test".getBytes(), null);

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenThrow(mockRestClientResponseException);

		// THEN
		InternalTechnicalException internalTechnicalException = assertThrows(InternalTechnicalException.class,
				() -> ampServiceImpl.uploadAmpContent(mockMultipartFileValide));
		assertEquals("Unable to send file to scom. Reason : status code=500 message=message test",
				internalTechnicalException.getMessage());

	}
	
	@Test
	void should_uploadFileToScom_throw_InternalTechnicalException_when_response_from_scom_is_null() throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFileValide = createMultipartFile("balkany.zip");
		ResizingResults sampleResizingResults = getSampleResizingResults();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity
				.ok(sampleResizingResults);

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(null);

		// THEN
		InternalTechnicalException internalTechnicalException = assertThrows(InternalTechnicalException.class,
				() -> ampServiceImpl.uploadAmpContent(mockMultipartFileValide));
		assertEquals("Error while uploading binary to SCOM", internalTechnicalException.getMessage());

	}
	
	@Test
	void should_uploadFileToScom_throw_InternalTechnicalException_when_response_status_from_scom_is_not_201()
			throws IOException {

		// GIVEN
		MockMultipartFile mockMultipartFileValide = createMultipartFile("balkany.zip");
		ResizingResults sampleResizingResults = getSampleResizingResults();
		final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity
				.ok(sampleResizingResults);
		final ResponseEntity<String> mockedUploadFileToScomResponse = ResponseEntity.ok(null);

		// WHEN
		when(scomRestClient.uploadFileToScom(any())).thenReturn(mockedUploadFileToScomResponse);

		// THEN
		InternalTechnicalException internalTechnicalException = assertThrows(InternalTechnicalException.class,
				() -> ampServiceImpl.uploadAmpContent(mockMultipartFileValide));
		assertEquals("Error while uploading binary to SCOM", internalTechnicalException.getMessage());

	}


	@Test
	void should_updateAmpHtmlFileAndCopyItIntoZipFile_remove_meta_tags_with_attributes_site_name_and_url() throws IOException {
		// GIVEN
		Document mockDocument = Jsoup.parse(mockTempAmpHtmlPath.toFile(), StandardCharsets.UTF_8.name());

		// WHEN
		ampServiceImpl.updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPathTarget, mockDocument, mockAmpZipPathTarget, "mock-zip-file-target/index.amp.html");
		Document documentToCheckResults = Jsoup.parse(ampHtmlPathTarget.toFile(), StandardCharsets.UTF_8.name());
		Optional<Element> metaElementSiteName = documentToCheckResults.select("meta").stream().filter(element -> element.attr("name").equals("og:site_name")).findAny();
		Optional<Element> metaElementUrl = documentToCheckResults.select("meta").stream().filter(element -> element.attr("name").equals("og:url")).findAny();

		// THEN
		assertEquals(false, metaElementSiteName.isPresent());
		assertEquals(false, metaElementUrl.isPresent());

	}

	@Test
	void should_updateAmpHtmlFileAndCopyItIntoZipFile_replace_attribute_href_of_link_tag_to_empty_string() throws IOException {
		// GIVEN
		Document mockDocument = Jsoup.parse(mockTempAmpHtmlPath.toFile(), StandardCharsets.UTF_8.name());

		// WHEN
	
		ampServiceImpl.updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPathTarget, mockDocument, mockAmpZipPathTarget, "mock-zip-file-target/index.amp.html");
		Document documentToCheckResults = Jsoup.parse(ampHtmlPathTarget.toFile(), StandardCharsets.UTF_8.name());
		Optional<Element> linkElement = documentToCheckResults.select("link").stream().filter(element -> element.attr("rel").equals("canonical")).findAny();

		// THEN
		assertEquals("", linkElement.get().attr("href"));

	}
	
	@Test
	void should_updateAmpHtmlFileAndCopyItIntoZipFile_remove_script_element_when_its_type_is_article() throws IOException {
		// GIVEN
		Document mockDocument = Jsoup.parse(mockTempAmpHtmlPath.toFile(), StandardCharsets.UTF_8.name());

		// WHEN
		ampServiceImpl.updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPathTarget, mockDocument, mockAmpZipPathTarget, "mock-zip-file-target/index.amp.html");
		Document documentToCheckResults = Jsoup.parse(ampHtmlPathTarget.toFile(), StandardCharsets.UTF_8.name());
		Optional<Element> scriptElement = documentToCheckResults.select("script").stream().filter(element -> element.attr("type").equals("application/ld+json")).findAny();

		// THEN
		assertEquals(false, scriptElement.isPresent());
	}

	@Test
	void should_updateAmpHtmlFileAndCopyItIntoZipFile_do_not_remove_script_element_when_its_type_is_not_article() throws IOException {
		// GIVEN
		Path mockTempAmpHtmlPath = Paths.get("src/test/resources/webstory/index.amp-test-type-script-not-article.html");
		Document mockDocument = Jsoup.parse(mockTempAmpHtmlPath.toFile(), StandardCharsets.UTF_8.name());

		// WHEN
		ampServiceImpl.updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPathTarget, mockDocument, mockAmpZipPathTarget, "mock-zip-file-target/index.amp.html");
		Document documentToCheckResults = Jsoup.parse(ampHtmlPathTarget.toFile(), StandardCharsets.UTF_8.name());
		Optional<Element> scriptElement = documentToCheckResults.select("script").stream().filter(element -> element.attr("type").equals("application/ld+json")).findAny();

		// THEN
		assertEquals(true, scriptElement.isPresent());
	}
	@Test
	void should_updateAmpHtmlFileAndCopyItIntoZipFile_do_not_remove_script_element_when_its_type_is_null() throws IOException {
		// GIVEN
		Path mockTempAmpHtmlPath = Paths.get("src/test/resources/webstory/index.amp-test-type-script-null.html");
		Document mockDocument = Jsoup.parse(mockTempAmpHtmlPath.toFile(), StandardCharsets.UTF_8.name());

		// WHEN
		ampServiceImpl.updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPathTarget, mockDocument, mockAmpZipPathTarget, "mock-zip-file-target/index.amp.html");
		Document documentToCheckResults = Jsoup.parse(ampHtmlPathTarget.toFile(), StandardCharsets.UTF_8.name());
		Optional<Element> scriptElement = documentToCheckResults.select("script").stream().filter(element -> element.attr("type").equals("application/ld+json")).findAny();

		// THEN
		assertEquals(true, scriptElement.isPresent());
	}
	@Test
	void should_updateAmpHtmlFileAndCopyItIntoZipFile_do_not_remove_script_element_when_its_type_is_not_present() throws IOException {
		// GIVEN
		Path mockTempAmpHtmlPath = Paths.get("src/test/resources/webstory/index.amp-test-type-script-not-present.html");
		Document mockDocument = Jsoup.parse(mockTempAmpHtmlPath.toFile(), StandardCharsets.UTF_8.name());

		// WHEN
		ampServiceImpl.updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPathTarget, mockDocument, mockAmpZipPathTarget, "mock-zip-file-target/index.amp.html");
		Document documentToCheckResults = Jsoup.parse(ampHtmlPathTarget.toFile(), StandardCharsets.UTF_8.name());
		Optional<Element> scriptElement = documentToCheckResults.select("script").stream().filter(element -> element.attr("type").equals("application/ld+json")).findAny();

		// THEN
		assertEquals(true, scriptElement.isPresent());
	}
	
	@Test
	void should_updateAmpHtmlFileAndCopyItIntoZipFile_do_not_remove_script_element_if_is_not_present() throws IOException {
		// GIVEN
		Path mockTempAmpHtmlPath = Paths.get("src/test/resources/webstory/index.amp-test-script-not-present.html");
		Document mockDocument = Jsoup.parse(mockTempAmpHtmlPath.toFile(), StandardCharsets.UTF_8.name());

		// WHEN
		ampServiceImpl.updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPathTarget, mockDocument, mockAmpZipPathTarget, "mock-zip-file-target/index.amp.html");
		Document documentToCheckResults = Jsoup.parse(ampHtmlPathTarget.toFile(), StandardCharsets.UTF_8.name());
		Optional<Element> scriptElement = documentToCheckResults.select("script").stream().filter(element -> element.attr("type").equals("application/ld+json")).findAny();

		// THEN
		assertEquals(false, scriptElement.isPresent());
	}
	
	@Test
	void should_updateAmpHtmlFileAndCopyItIntoZipFile_remove_specifics_chars_from_the_beginning_of_title() throws IOException {
		// GIVEN
		Path mockTempAmpHtmlPath = Paths.get("src/test/resources/webstory/index.amp-test-title.html");
		Document mockDocument = Jsoup.parse(mockTempAmpHtmlPath.toFile(), StandardCharsets.UTF_8.name());

		// WHEN
		ampServiceImpl.updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPathTarget, mockDocument, mockAmpZipPathTarget, "mock-zip-file-target/index.amp.html");
		Document documentToCheckResults = Jsoup.parse(ampHtmlPathTarget.toFile(), StandardCharsets.UTF_8.name());
		Element titleElement = documentToCheckResults.select("title").first();

		// THEN
		assertEquals("Some - Title", titleElement.text());
	}
	

}
