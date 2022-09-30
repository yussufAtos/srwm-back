package com.afp.iris.sr.wm.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class CmsErrorException extends ResponseStatusException {

	private static final long serialVersionUID = 1L;

	public CmsErrorException(@Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, null, cause);
	}

	public CmsErrorException(String reason) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
	}
	
	public CmsErrorException(HttpStatus cmsErrorStatus, String reason) {
		super(cmsErrorStatus, reason);
	}

	public CmsErrorException(int statusValue, String reason) {
		super(statusValue, reason, null);
	}
}
