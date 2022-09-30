package com.afp.iris.sr.wm.clients.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement
public class CmsError {

	String code;

	String origin;
	
	String diagnostic;
}
