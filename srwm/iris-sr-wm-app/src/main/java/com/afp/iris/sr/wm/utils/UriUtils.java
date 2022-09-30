package com.afp.iris.sr.wm.utils;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;

import static org.apache.http.util.TextUtils.isBlank;

public class UriUtils {

    private UriUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Extracts id from uri.
     * @param uri URI like http://test.afp.com/someId
     * @return id if id could be extracted (someId), null otherwise
     */
    public static String extractIdFromUri(final String uri) {
        if (isBlank(uri)) {
            return null;
        }
        if (uri.contains("?")) {
            return uri.substring(uri.lastIndexOf("/") + 1, uri.indexOf("?"));
        } else {
            return uri.substring(uri.lastIndexOf("/") + 1);
        }
    }
}
