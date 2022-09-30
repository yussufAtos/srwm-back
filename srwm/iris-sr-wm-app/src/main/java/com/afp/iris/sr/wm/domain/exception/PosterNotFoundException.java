package com.afp.iris.sr.wm.domain.exception;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter @ToString
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class PosterNotFoundException extends RuntimeException{
    public PosterNotFoundException(Exception e, String messageFormat, Object ... formatArgs) {
        super(String.format(messageFormat, formatArgs), e);
    }

    public PosterNotFoundException(String message) {
        super(message);
    }

    public PosterNotFoundException(String messageFormat, Object ... formatArgs) {
        super(String.format(messageFormat, formatArgs));
    }
}
