package com.afp.iris.sr.wm.domain.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.ToString;
@Getter @ToString
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class ComponentNotFoundException extends RuntimeException{
    public ComponentNotFoundException(Exception e, String messageFormat, Object ... formatArgs) {
        super(String.format(messageFormat, formatArgs), e);
    }

    public ComponentNotFoundException(String message) {
        super(message);
    }

    public ComponentNotFoundException(String messageFormat, Object ... formatArgs) {
        super(String.format(messageFormat, formatArgs));
    }
}
