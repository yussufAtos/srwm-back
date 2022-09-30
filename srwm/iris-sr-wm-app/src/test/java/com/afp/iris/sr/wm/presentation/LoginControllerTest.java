package com.afp.iris.sr.wm.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.Cookie;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.domain.DocumentService;
import com.afp.iris.sr.wm.domain.exception.NotAuthenticatedException;

@WebMvcTest(LoginController.class)
class LoginControllerTest {

    private static final String HEADER_TRANSACTION_ID = "X-AFP-TRANSACTION-ID";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;
    
    @MockBean
    private AppProperties properties;

    @Test
    void login_should_redirect_with_302_to_srwm_mainpage_location_and_with_srwm_cookie_when_authentication_succeeded() throws Exception {
        // GIVEN

        when(documentService.loginToBackend(any())).thenReturn(new Cookie("srwm","toto"));
        when(properties.getBaseUri()).thenReturn("http://localhost:8585");

        // WHEN
       

		this.mockMvc.perform(get("/login").header(HEADER_TRANSACTION_ID, "TRANSACTION-ID-FROM-TEST"))
		
		// THEN
				.andExpect(status().isFound())
				.andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:8585"))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.equalTo("srwm=toto")));
    }
    
    @Test
    void login_should_redirect_to_specifique_location_when_param_redirectUrl_is_set() throws Exception {
        // GIVEN

        when(documentService.loginToBackend(any())).thenReturn(new Cookie("srwm","toto"));
        when(properties.getBaseUri()).thenReturn("http://localhost:8585");

        // WHEN
       

		this.mockMvc.perform(get("/login?redirectUrl=https://my-url.afp.com/mytest").header(HEADER_TRANSACTION_ID, "TRANSACTION-ID-FROM-TEST"))
		
		// THEN
				.andExpect(status().isFound())
				.andExpect(header().string(HttpHeaders.LOCATION, "https://my-url.afp.com/mytest"))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.equalTo("srwm=toto")));
    }
    
    @Test
    void login_should_return_401_with_srwm_cookie_when_authentication_Failed() throws Exception {
        // GIVEN

        when(documentService.loginToBackend(any())).thenThrow(new NotAuthenticatedException("Failed to auth to CMS"));

        // WHEN
       

		this.mockMvc.perform(get("/login").header(HEADER_TRANSACTION_ID, "TRANSACTION-ID-FROM-TEST"))
		
		// THEN
				.andExpect(status().isUnauthorized());
//				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, Matchers.equalTo("Basic realm=\"SRWM Realm\""))); // FIXME
    }
    
}
