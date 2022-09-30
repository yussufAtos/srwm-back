package com.afp.iris.sr.wm.clients.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Data
@JacksonXmlRootElement
public class ResizingResults {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "result")
    private List<Result> results = new ArrayList<>();

    @Data
    public static class Result {
        @JacksonXmlProperty(isAttribute = true)
        URI url;
        @JacksonXmlProperty(isAttribute = true)
        String status;
        @JacksonXmlProperty(isAttribute = true)
        URI resultUrl;
        @JacksonXmlProperty(isAttribute = true)
        Long width;
        @JacksonXmlProperty(isAttribute = true)
        Long height;
        @JacksonXmlProperty(isAttribute = true)
        Long length;
    }

}
