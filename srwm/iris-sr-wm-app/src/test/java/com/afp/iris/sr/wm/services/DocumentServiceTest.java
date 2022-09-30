package com.afp.iris.sr.wm.services;

import com.afp.iptc.g2.config.afp.RenditionType;
import com.afp.iptc.g2.libg2api.*;
import com.afp.iris.sr.wm.TestUtils;
import com.afp.iris.sr.wm.clients.CmsRestClient;
import com.afp.iris.sr.wm.clients.ScomRestClient;
import com.afp.iris.sr.wm.clients.dto.ResizingResults;
import com.afp.iris.sr.wm.domain.DocumentService;
import com.afp.iris.sr.wm.domain.exception.CmsErrorException;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import com.afp.iris.sr.wm.domain.exception.NotAuthenticatedException;
import com.afp.iris.sr.wm.helper.G2Helper;
import com.afp.iris.sr.wm.presentation.dto.Jnews;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.afp.iris.sr.wm.TestUtils.createTestG2WebStoryDocument;
import static com.afp.iris.sr.wm.TestUtils.getSampleResizingResults;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DocumentServiceTest {
    RestClientResponseException basicRestClientResponseException = new RestClientResponseException("message", 400, "400", null, "Exception".getBytes(StandardCharsets.UTF_8), null);
    
    @Autowired
    DocumentService documentService;

    @MockBean
    CmsRestClient cmsRestClient;

    @MockBean
    ScomRestClient scomRestClient;

    @Test
    void create_document_should_return_created_G2_in_CMS() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();

        final ResponseEntity<String> cmsResponse = ResponseEntity.status(HttpStatus.CREATED).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.createDocument(any())).thenReturn(cmsResponse);

        // WHEN
        final G2Document createdG2Document = documentService.createDocument(mockedG2WebStoryDocument);

        // THEN
        assertEquals(mockedG2WebStoryDocument.g2Representation(), createdG2Document.g2Representation());
    }

    @Test
    void create_document_should_return_NotAuthenticatedException_when_CMS_returns_302_to_CAS_login_location() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();

        ResponseEntity<String> cmsResponse = ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "https://test.cas.com:8443/cas/login?service=http://localhost:8585/login").build();
        when(cmsRestClient.createDocument(any())).thenReturn(cmsResponse);

        // WHEN & THEN
        assertThrows(NotAuthenticatedException.class, () -> documentService.createDocument(mockedG2WebStoryDocument));
    }

    @Test
    void create_document_should_return_InternalTechnicalException_when_CMS_returns_302_to_not_CAS_login_location() throws IOException, BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();

        ResponseEntity<String> cmsResponse = ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "http://test.cms.com/nuxeo/site/documents").build();
        when(cmsRestClient.createDocument(any())).thenReturn(cmsResponse);

        // WHEN & THEN
        assertThrows(InternalTechnicalException.class, () -> documentService.createDocument(mockedG2WebStoryDocument));

    }

    @Test
    void create_document_should_return_NotAuthenticatedException_when_CMS_returns_401ClientError() throws BadNewsMLG2Exception {
        // GIVEN
       	final String cmsXmlError ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                + "             <error xmlns=\"http://sr.iris.afp.com/httperrorcode\">\r\n"
                + "                <code>401</code>\r\n"
                + "                <origin>You don't have the right privileges to access this page</origin>\r\n"
                + "                <diagnostic>diagnostic test</diagnostic>\r\n"
                + "             </error>";
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        when(cmsRestClient.createDocument(any())).thenThrow(new RestClientResponseException(null, HttpStatus.UNAUTHORIZED.value(), null, null, cmsXmlError.getBytes(), null));

        // WHEN & THEN
        final NotAuthenticatedException notAuthenticatedException =  assertThrows(NotAuthenticatedException.class, () -> documentService.createDocument(mockedG2WebStoryDocument));
        assertEquals(401, notAuthenticatedException.getRawStatusCode());
    }
    
    @Test
    void create_document_should_return_CmsErrorException_when_CMS_returns_403ClientError() throws BadNewsMLG2Exception {
        // GIVEN
       	final String cmsXmlError ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                + "             <error xmlns=\"http://sr.iris.afp.com/httperrorcode\">\r\n"
                + "                <code>403</code>\r\n"
                + "                <origin>You don't have the right privileges to access this page</origin>\r\n"
                + "                <diagnostic>diagnostic test</diagnostic>\r\n"
                + "             </error>";
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        when(cmsRestClient.createDocument(any())).thenThrow(new RestClientResponseException(null, HttpStatus.FORBIDDEN.value(), null, null, cmsXmlError.getBytes(), null));

        // WHEN & THEN
        final CmsErrorException cmsErrorException =  assertThrows(CmsErrorException.class, () -> documentService.createDocument(mockedG2WebStoryDocument));
        assertEquals("You don't have the right privileges to access this page : diagnostic test", cmsErrorException.getReason());
        assertEquals(403, cmsErrorException.getRawStatusCode());
    }
    
    @Test
    void create_document_should_return_CmsErrorException_when_CMS_returns_500ServerError() throws BadNewsMLG2Exception {
        // GIVEN
    	final String cmsXmlError ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                + "             <error xmlns=\"http://sr.iris.afp.com/httperrorcode\">\r\n"
                + "                <code>500</code>\r\n"
                + "                <origin>Internal server error</origin>\r\n"
                + "                <diagnostic>diagnostic test</diagnostic>\r\n"
                + "             </error>";
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        when(cmsRestClient.createDocument(any())).thenThrow(new RestClientResponseException(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), null, null, cmsXmlError.getBytes(), null));

        // WHEN & THEN
        final CmsErrorException cmsErrorException =  assertThrows(CmsErrorException.class, () -> documentService.createDocument(mockedG2WebStoryDocument));
        assertEquals("Internal server error : diagnostic test", cmsErrorException.getReason());
        assertEquals(500, cmsErrorException.getRawStatusCode());
    }
    
    @Test
    void edit_document_should_return_optionnal_empty_when_CMS_response_is_204() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        ResponseEntity<String> cmsResponse = ResponseEntity.noContent().build();
        when(cmsRestClient.editDocument(mockedG2WebStoryDocument)).thenReturn(cmsResponse);

        // WHEN
        final Optional<G2Document> optionalG2Document = documentService.editDocument(mockedG2WebStoryDocument);

        // THEN
        assertEquals(Optional.empty(), optionalG2Document);
    }

    @Test
    void validate_document_should_return_validated_G2_in_CMS() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        final ResponseEntity<String> cmsResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.validateDocument(any())).thenReturn(cmsResponse);

        // WHEN
        final G2Document validatedWebStory = documentService.validateDocument(mockedG2WebStoryDocument);

        // THEN
        assertEquals(mockedG2WebStoryDocument.g2Representation(), validatedWebStory.g2Representation());
    }

    @Test
    void create_document_should_throw_an_internal_exception_when_cms_client_throws_bad_newsml_g2_exception() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();

        when(cmsRestClient.createDocument(any())).thenThrow(BadNewsMLG2Exception.class);

        // WHEN & THEN
        assertThrows(InternalTechnicalException.class, () -> documentService.createDocument(mockedG2WebStoryDocument));

    }

    @Test
    void create_document_should_throw_an_internal_exception_when_cms_client_throws_rest_client_response_exception() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();

        when(cmsRestClient.createDocument(any())).thenThrow(basicRestClientResponseException);

        // WHEN & THEN
        assertThrows(InternalTechnicalException.class, () -> documentService.createDocument(mockedG2WebStoryDocument));

    }

    @Test
    void edit_document_should_throw_an_internal_exception_when_cms_client_throws_bad_newsml_g2_exception() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        when(cmsRestClient.editDocument(mockedG2WebStoryDocument)).thenThrow(BadNewsMLG2Exception.class);

        // WHEN & THEN
        assertThrows(InternalTechnicalException.class, () -> documentService.editDocument(mockedG2WebStoryDocument));

    }

    @Test
    void edit_document_should_throw_an_internal_exception_when_cms_client_throws_rest_client_response_exception() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        when(cmsRestClient.editDocument(mockedG2WebStoryDocument)).thenThrow(basicRestClientResponseException);

        // WHEN & THEN
        assertThrows(InternalTechnicalException.class, () -> documentService.editDocument(mockedG2WebStoryDocument));

    }

    @Test
    void validate_document_should_throw_an_internal_exception_when_cms_client_throws_bad_newsml_g2_exception() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        when(cmsRestClient.validateDocument(mockedG2WebStoryDocument)).thenThrow(BadNewsMLG2Exception.class);

        // WHEN & THEN
        assertThrows(InternalTechnicalException.class, () -> documentService.validateDocument(mockedG2WebStoryDocument));

    }

    @Test
    void validate_document_should_throw_an_internal_exception_when_cms_client_throws_rest_client_response_exception() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        when(cmsRestClient.validateDocument(mockedG2WebStoryDocument)).thenThrow(basicRestClientResponseException);

        // WHEN & THEN
        assertThrows(InternalTechnicalException.class, () -> documentService.validateDocument(mockedG2WebStoryDocument));

    }

    @Test
    void validateNewWebStory_should_return_validated_document() throws IOException, BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        final ResponseEntity<String> cmsCreateResponse = ResponseEntity.status(HttpStatus.CREATED).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.createDocument(any())).thenReturn(cmsCreateResponse);
        ResponseEntity<String> cmsEditResponse = ResponseEntity.noContent().build();
        when(cmsRestClient.editDocument(any())).thenReturn(cmsEditResponse);
        final URI someGUID = URI.create("http://somehost:someport/someEndpoint/someGUID");
        mockedG2WebStoryDocument.setGuid(someGUID);
        final ResponseEntity<String> cmsValidateResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.validateDocument(any())).thenReturn(cmsValidateResponse);
        Jnews jnews = TestUtils.getSampleJnews();

        // WHEN
        final G2Document validatedWebStory = documentService.validateNewDocument(jnews);

        // THEN
        assertEquals(mockedG2WebStoryDocument.g2Representation(), validatedWebStory.g2Representation());
    }

    @Test
    void getDocumentById_should_return_G2_returned_by_CMS() throws BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();

        final ResponseEntity<String> cmsResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.getDocumentById("123-uuid")).thenReturn(cmsResponse);

        // WHEN
        final G2Document g2Document = documentService.getDocumentById("123-uuid");

        // THEN
        assertEquals(mockedG2WebStoryDocument.g2Representation(), g2Document.g2Representation());
    }

    @Test
    void validateUpdateWebStory_should_return_validated_document() throws IOException, BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        final ResponseEntity<String> cmsGetByIdResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.getDocumentById("123-uuid")).thenReturn(cmsGetByIdResponse);
        ResponseEntity<String> cmsEditResponse = ResponseEntity.noContent().build();
        when(cmsRestClient.editDocument(any())).thenReturn(cmsEditResponse);
        mockedG2WebStoryDocument.setGuid(URI.create("http://somehost:someport/someEndpoint/someGUID"));
        final ResponseEntity<String> cmsValidateResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.validateDocument(any())).thenReturn(cmsValidateResponse);

        // WHEN
        Jnews jnews = TestUtils.getSampleJnewsWithSelfLink();
        final G2Document validatedWebStory = documentService.validateUpdateDocument(jnews);

        // THEN
        assertEquals(mockedG2WebStoryDocument.g2Representation(), validatedWebStory.g2Representation());
    }

    @Test
    void getDocumentByGUID_should_return_last_validated_document_In_CMS() throws IOException, BadNewsMLG2Exception {
        // GIVEN
        final ResponseEntity<String> cmsSearchResponse = ResponseEntity.status(HttpStatus.OK)
                .body(TestUtils.getSampleG2SearchResult().g2Representation());
        when(cmsRestClient.searchLastValidatedDocumentByGuid(any(), eq(false))).thenReturn(cmsSearchResponse);

        String mockedG2DocumentXml = createTestG2WebStoryDocument().g2Representation();
        ResponseEntity<String> cmsGetByStoryIdResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2DocumentXml);
        when(cmsRestClient.getDocumentByStoryId(any())).thenReturn(cmsGetByStoryIdResponse);

        // WHEN
        G2Document document = documentService.getDocumentByGuid("someguid");

        // THEN
        assertEquals(mockedG2DocumentXml, document.g2Representation());

    }

    @Test
    void getDocumentByGUID_should_return_last_validated_document_In_Archives() throws IOException, BadNewsMLG2Exception {
        // GIVEN
        final ResponseEntity<String> searchInCmsResponse = ResponseEntity.status(HttpStatus.OK)
                .body(G2ObjectFactory.createSearchResult().g2Representation());
        when(cmsRestClient.searchLastValidatedDocumentByGuid(any(), eq(false))).thenReturn(searchInCmsResponse);

        final ResponseEntity<String> searchInArchivesResponse = ResponseEntity.status(HttpStatus.OK)
                .body(TestUtils.getSampleG2SearchResult().g2Representation());
        when(cmsRestClient.searchLastValidatedDocumentByGuid(any(), eq(true))).thenReturn(searchInArchivesResponse);

        final G2WebStoryDocument phoenixedG2WebStoryDocument = createTestG2WebStoryDocument();
        phoenixedG2WebStoryDocument.setPrepared(true);
        final ResponseEntity<String> cmsPhoenixResponse = ResponseEntity.status(HttpStatus.OK)
                .body(phoenixedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.phoenixDocument(any(), any())).thenReturn(cmsPhoenixResponse);

        // WHEN
        G2Document document = documentService.getDocumentByGuid("someguid");

        // THEN
        assertEquals(phoenixedG2WebStoryDocument.g2Representation(), document.g2Representation());
    }

    @Test
    void depublishDocument_should_return_depublished_document() throws IOException, BadNewsMLG2Exception {
        // GIVEN
        final ResponseEntity<String> cmsSearchResponse = ResponseEntity.status(HttpStatus.OK)
                .body(TestUtils.getSampleG2SearchResult().g2Representation());
        when(cmsRestClient.searchLastValidatedDocumentByGuid(any(), eq(false))).thenReturn(cmsSearchResponse);

        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        final ResponseEntity<String> cmsGetByIdResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.getDocumentById(any())).thenReturn(cmsGetByIdResponse);

        ResponseEntity<String> cmsEditResponse = ResponseEntity.noContent().build();
        when(cmsRestClient.editDocument(any())).thenReturn(cmsEditResponse);

        final ResponseEntity<String> cmsDepublishResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.depublishDocument(any())).thenReturn(cmsDepublishResponse);

        // WHEN
        final G2Document depublishedWebStory = documentService.depublishDocument("someguid");

        // THEN
        assertEquals(mockedG2WebStoryDocument.g2Representation(), depublishedWebStory.g2Representation());
    }

    @Test
    void validateUpdateWebStory_should_update_thumbnail() throws IOException, BadNewsMLG2Exception {
        // GIVEN
        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        final ResponseEntity<String> cmsGetByIdResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.getDocumentById("123-uuid")).thenReturn(cmsGetByIdResponse);

        PictureRenditionInfo newPreview = G2Helper.buildIcon(URI.create("http://somehost:someport/someEndpoint/newPoster"), RenditionType.PREVIEW);
        G2Helper.replaceRendition(mockedG2WebStoryDocument, newPreview);
        ResponseEntity<String> cmsEditResponse = ResponseEntity.ok(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.editDocument(any())).thenReturn(cmsEditResponse);
        final ResponseEntity<ResizingResults> mockedGenerateThumbnailResponse = ResponseEntity.ok(getSampleResizingResults());
        when(scomRestClient.generateThumbnail(any())).thenReturn(mockedGenerateThumbnailResponse);
        final ResponseEntity<String> cmsValidateResponse = ResponseEntity.ok(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestClient.validateDocument(any())).thenReturn(cmsValidateResponse);

        // WHEN
        Jnews jnews = TestUtils.getSampleJnewsWithSelfLink();
        final G2Document validatedWebStory = documentService.validateUpdateDocument(jnews);

        // THEN
        assertEquals(mockedG2WebStoryDocument.g2Representation(), validatedWebStory.g2Representation());
    }
}
