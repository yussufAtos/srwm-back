package com.afp.iris.sr.wm.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class MediaTopicNotSupportedException extends RuntimeException {
	public MediaTopicNotSupportedException(Exception e, String messageFormat, Object... formatArgs) {
		super(String.format(messageFormat, formatArgs), e);
	}

	public MediaTopicNotSupportedException(String message) {
		super(message);
	}

	public MediaTopicNotSupportedException(String messageFormat, Object... formatArgs) {
		super(String.format(messageFormat, formatArgs));
	}

}
