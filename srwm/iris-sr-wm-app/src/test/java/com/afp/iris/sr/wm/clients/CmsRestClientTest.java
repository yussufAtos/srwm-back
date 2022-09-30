package com.afp.iris.sr.wm.clients;

import static com.afp.iris.sr.wm.TestUtils.createTestG2WebStoryDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.AssertionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.afp.iptc.g2.libg2api.BadNewsMLG2Exception;
import com.afp.iptc.g2.libg2api.G2WebStoryDocument;
import com.afp.iris.sr.wm.TestUtils;
import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.domain.exception.NotAuthenticatedException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CmsRestClientTest {
    @Autowired
    CmsRestClient cmsRestClient;
    @Autowired
    AppProperties properties;

    @MockBean(name = "cmsRestTemplate")
    RestTemplate cmsRestTemplate;

    @BeforeEach
    void addCurrenHttpRequestBeforeEachTest() {
    	 MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
         //httpServletRequest
         ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(httpServletRequest);
         RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @Test
    void buildAuthenticatedHttpHeaders_should_decode_and_transform_SRWM_cookie_to_CMS_JSESSIONID(){
    	// GIVEN
    	HttpServletRequest currentRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    	((MockHttpServletRequest)currentRequest).setCookies(new Cookie("srwm", "SlNFU1NJT05JRD1BQkNE"),
    			                                            new Cookie("JSESSIONID", "myJsession"),
    			                                            new Cookie("srwm", "SlNFU1NJT05JRD1FRkdI"));
    	// WHEN
    	HttpHeaders httpHeaders = cmsRestClient.buildAuthenticatedHttpHeaders();

    	// THEN
    	//assertEquals(2, httpHeaders.get(HttpHeaders.COOKIE).size());
    assertTrue(httpHeaders.get(HttpHeaders.COOKIE).containsAll(Arrays.asList("JSESSIONID=ABCD", "JSESSIONID=EFGH")));
    }
    
    @Test
    void loginToBackend_should_encode_and_transform_CMS_JSESSIONID_to_SRWM_cookie(){
    	// GIVEN
    	HttpSession session = new MockHttpSession();
    	AttributePrincipal principal = mock(AttributePrincipal.class);
    	when(principal.getProxyTicketFor(any())).thenReturn("my-proxy-ticket");
    	session.setAttribute("_const_cas_assertion_", new AssertionImpl(principal));
    	
    	ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "JSESSIONID=ABCD").build();
    	String userinfoEndpoint = properties.getCms().getUserinfoEndpoint();
    	String userinfoEndpointWithCas = userinfoEndpoint+"?service="+userinfoEndpoint+"&proxy=test&ticket=my-proxy-ticket";
    	when(cmsRestTemplate.exchange(eq(userinfoEndpointWithCas), eq(HttpMethod.GET), any(), eq(String.class)))
                   .thenReturn(cmsUserInfoResponse);
    	
    	// WHEN
    	Cookie cookie = cmsRestClient.loginToBackend(session);

    	// THEN   	
    	assertEquals("srwm",cookie.getName());
    	assertEquals("SlNFU1NJT05JRD1BQkNE", cookie.getValue());
    }
    
    @Test
    void loginToBackend_should_throws_NotAuthenticatedException_if_failed_to_retrieve_proxyTicket_from_CAS(){
    	// GIVEN
    	HttpSession session = new MockHttpSession();
    	AttributePrincipal principal = mock(AttributePrincipal.class);
    	when(principal.getProxyTicketFor(any())).thenReturn(null);
    	session.setAttribute("_const_cas_assertion_", new AssertionImpl(principal));
    	
    	// WHEN & THEN
    	assertThrows(NotAuthenticatedException.class, () -> cmsRestClient.loginToBackend(session));

    }
    
    @Test
    void loginToBackend_should_throws_NotAuthenticatedException_when_CMS_returns_401(){
    	// GIVEN
    	HttpSession session = new MockHttpSession();
    	AttributePrincipal principal = mock(AttributePrincipal.class);
    	when(principal.getProxyTicketFor(any())).thenReturn(null);
    	session.setAttribute("_const_cas_assertion_", new AssertionImpl(principal));
    	
    	String userinfoEndpoint = properties.getCms().getUserinfoEndpoint();
    	String userinfoEndpointWithCas = userinfoEndpoint+"?service="+userinfoEndpoint+"&proxy=test&ticket=my-proxy-ticket";
    	when(cmsRestTemplate.exchange(eq(userinfoEndpointWithCas), eq(HttpMethod.GET), any(), eq(String.class)))
                   .thenThrow(new RestClientResponseException(null, HttpStatus.UNAUTHORIZED.value(), null, null, null, null));
    	
    	
    	// WHEN & THEN
    	assertThrows(NotAuthenticatedException.class, () -> cmsRestClient.loginToBackend(session));

    }
    

    @Test
    void createDocument_should_return_201_with_created_G2_in_response_body() throws BadNewsMLG2Exception {
        // GIVEN
        ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "test").build();
        when(cmsRestTemplate.exchange(eq(properties.getCms().getUserinfoEndpoint()), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsUserInfoResponse);

        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        final ResponseEntity<String> cmsCreateDocumentResponse = ResponseEntity.status(HttpStatus.CREATED).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestTemplate.exchange(eq(properties.getCms().getDocumentsEndpoint()), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(cmsCreateDocumentResponse);

        // WHEN
        final ResponseEntity<String> response = cmsRestClient.createDocument(mockedG2WebStoryDocument);

        // THEN
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockedG2WebStoryDocument.g2Representation(), response.getBody());
    }

    @Test
    void editDocument_should_return_200_with_G2_in_response_body() throws BadNewsMLG2Exception {
        // GIVEN
        ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "test").build();
        when(cmsRestTemplate.exchange(eq(properties.getCms().getUserinfoEndpoint()), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsUserInfoResponse);

        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        mockedG2WebStoryDocument.setLinkToSelf(URI.create("http://test.cms.com:9080/nuxeo/site/documents/myself"));
        final ResponseEntity<String> cmsEditDocumentResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestTemplate.exchange(eq("http://test.cms.com:9080/nuxeo/site/documents/myself/editor"), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(cmsEditDocumentResponse);

        // WHEN
        final ResponseEntity<String> response = cmsRestClient.editDocument(mockedG2WebStoryDocument);

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockedG2WebStoryDocument.g2Representation(), response.getBody());
    }

    @Test
    void editDocument_should_return_204_without_response_body() throws BadNewsMLG2Exception {
        // GIVEN
        ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "test").build();
        when(cmsRestTemplate.exchange(eq(properties.getCms().getUserinfoEndpoint()), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsUserInfoResponse);

        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        mockedG2WebStoryDocument.setLinkToSelf(URI.create("http://test.cms.com:9080/nuxeo/site/documents/myself"));
        final ResponseEntity<String> cmsEditDocumentResponse = ResponseEntity.noContent().build();
        when(cmsRestTemplate.exchange(eq("http://test.cms.com:9080/nuxeo/site/documents/myself/editor"), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(cmsEditDocumentResponse);

        // WHEN
        final ResponseEntity<String> response = cmsRestClient.editDocument(mockedG2WebStoryDocument);

        // THEN
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void validateDocument_should_return_200_with_G2_in_response_body() throws BadNewsMLG2Exception {
        // GIVEN
        ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "test").build();
        when(cmsRestTemplate.exchange(eq(properties.getCms().getUserinfoEndpoint()), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsUserInfoResponse);

        final G2WebStoryDocument mockedG2WebStoryDocument = createTestG2WebStoryDocument();
        mockedG2WebStoryDocument.setLinkToSelf(URI.create("http://test.cms.com:9080/nuxeo/site/documents/myself"));
        final ResponseEntity<String> cmsEditDocumentResponse = ResponseEntity.status(HttpStatus.OK).body(mockedG2WebStoryDocument.g2Representation());
        when(cmsRestTemplate.exchange(eq("http://test.cms.com:9080/nuxeo/site/documents/myself/validator"), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(cmsEditDocumentResponse);

        // WHEN
        final ResponseEntity<String> response = cmsRestClient.validateDocument(mockedG2WebStoryDocument);

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockedG2WebStoryDocument.g2Representation(), response.getBody());
    }

    @Test
    void getLastValidatedDocumentByGuid_should_return_200_with_G2SearchResult() throws IOException, BadNewsMLG2Exception {
        // GIVEN
        ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "test").build();
        when(cmsRestTemplate.exchange(eq(properties.getCms().getUserinfoEndpoint()), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsUserInfoResponse);

        final String g2SearchResultRepresentation = TestUtils.getSampleG2SearchResult().g2Representation();
        final ResponseEntity<String> cmsSearchDocumentsResponse = ResponseEntity.status(HttpStatus.OK)
                .body(g2SearchResultRepresentation);

        String cmsSearchDocumentsEndpoint = properties.getCms().getDocumentsEndpoint() +
                "?lastValidatedOrApproved=true" +
                "&query=(doc.doc_id='http://test.doc.com/123abc')" +
                "&unpublished=true";


        when(cmsRestTemplate.exchange(eq(URI.create(cmsSearchDocumentsEndpoint)), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsSearchDocumentsResponse);

        // WHEN
        final ResponseEntity<String> response = cmsRestClient.searchLastValidatedDocumentByGuid("http://test.doc.com/123abc", false);

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(g2SearchResultRepresentation, response.getBody());
    }

    @Test
    void getDocumentById_should_return_200_with_G2_in_response_body () throws BadNewsMLG2Exception {
        // GIVEN
        ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "test").build();
        when(cmsRestTemplate.exchange(eq(properties.getCms().getUserinfoEndpoint()), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsUserInfoResponse);

        String cmsGetDocumentsEndpoint = properties.getCms().getDocumentsEndpoint() + "/652e71d7-fe52-4c98-b87e-fa54aa33a1fe";
        final String g2WebStoryRepresentation = TestUtils.getSampleWebStoryDocument().g2Representation();
        final ResponseEntity<String> cmsGetDocumentsResponse = ResponseEntity.status(HttpStatus.OK)
                .body(g2WebStoryRepresentation);
        when(cmsRestTemplate.exchange(eq(URI.create(cmsGetDocumentsEndpoint)), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsGetDocumentsResponse);

        // WHEN
        final ResponseEntity<String> response = cmsRestClient.getDocumentById("652e71d7-fe52-4c98-b87e-fa54aa33a1fe");

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(g2WebStoryRepresentation, response.getBody());
    }

    @Test
    void getDocumentByStoryId_should_return_200_with_G2_in_response_body () throws BadNewsMLG2Exception {
        // GIVEN
        ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "test").build();
        when(cmsRestTemplate.exchange(eq(properties.getCms().getUserinfoEndpoint()), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsUserInfoResponse);

        String cmsStoriesEndpoint = properties.getCms().getStoriesEndpoint() + "123-story-uuid";
        final String g2WebStoryRepresentation = TestUtils.getSampleWebStoryDocument().g2Representation();
        final ResponseEntity<String> cmsResponse = ResponseEntity.status(HttpStatus.OK)
                .body(g2WebStoryRepresentation);
        when(cmsRestTemplate.exchange(eq(URI.create(cmsStoriesEndpoint)), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsResponse);

        // WHEN
        final ResponseEntity<String> response = cmsRestClient.getDocumentByStoryId("123-story-uuid");

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(g2WebStoryRepresentation, response.getBody());
    }

    @Test
    void phoenixDocument_should_return_200_with_G2_in_response_body() throws BadNewsMLG2Exception {
        // GIVEN
        ResponseEntity<String> cmsUserInfoResponse = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "test").build();
        when(cmsRestTemplate.exchange(eq(properties.getCms().getUserinfoEndpoint()), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(cmsUserInfoResponse);

        String cmsPhoenixDocumentsEndpoint = properties.getCms().getPhoenixEndpoint();
        final String g2WebStoryRepresentation = TestUtils.getSampleWebStoryDocument().g2Representation();
        final ResponseEntity<String> cmsPhoenixDocumentsResponse = ResponseEntity.status(HttpStatus.OK)
                .body(g2WebStoryRepresentation);

        when(cmsRestTemplate.exchange(eq(cmsPhoenixDocumentsEndpoint), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(cmsPhoenixDocumentsResponse);


        // WHEN
        final ResponseEntity<String> response = cmsRestClient.phoenixDocument("http://test.doc.com/123abc",
                URI.create("http://test.cms.com:9080/nuxeo/site/documents/myself/editor"));

        // THEN
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(g2WebStoryRepresentation, response.getBody());
    }
}
