package com.afp.iris.sr.wm.domain.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;

import lombok.Getter;
import lombok.ToString;
@Getter @ToString
public class NotAuthenticatedException extends ResponseStatusException {

	private static final long serialVersionUID = 1L;

	public NotAuthenticatedException(@Nullable Throwable cause) {
		super(HttpStatus.UNAUTHORIZED, null, cause);
	}
    
	public NotAuthenticatedException(String reason) {
		super(HttpStatus.UNAUTHORIZED, reason);
	}
	public NotAuthenticatedException(int statusValue ,String reason) {
		super(statusValue, reason,null);
	}
	
    @Override
    public HttpHeaders getResponseHeaders() {
      HttpHeaders headers = new HttpHeaders();

	  // FIXME : résoudre
//      headers.add(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"SRWM Realm\"");

      return headers;
    }
}
