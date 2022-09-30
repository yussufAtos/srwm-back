package com.afp.iris.sr.wm.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalTechnicalException extends RuntimeException {

    public InternalTechnicalException(Exception e, String messageFormat, Object ... formatArgs) {
        super(String.format(messageFormat, formatArgs), e);
    }

    public InternalTechnicalException(String message) {
        super(message);
    }

    public InternalTechnicalException(String messageFormat, Object ... formatArgs) {
        super(String.format(messageFormat, formatArgs));
    }
}
