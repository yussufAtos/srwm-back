package com.afp.iris.sr.wm.domain.exception;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ToString
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class UnexpectedSearchResultException extends RuntimeException {

    public UnexpectedSearchResultException(String message) {
        super(message);
    }

    public UnexpectedSearchResultException(String messageFormat, Object... formatArgs) {
        super(String.format(messageFormat, formatArgs));
    }
}
