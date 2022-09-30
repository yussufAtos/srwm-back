package com.afp.iris.sr.wm.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@AllArgsConstructor
@ToString
@ConstructorBinding
@ConfigurationProperties("app")
public class AppProperties {
	private String documentsEndpoint;
	private Cms cms;
	private Scom scom;
	private String srwmComponentsEndpoint;
	private String baseUri;
	private SrwmCookie srwmCookie;

	@Getter
	@AllArgsConstructor
	@ToString
	public static class Cms {
		private String userinfoEndpoint;
		private String documentsEndpoint;
		private String storiesEndpoint;
		private String documentValidateEndpointTemplate;
		private String documentEditorEndpointTemplate;
		private String phoenixEndpoint;
	}

	@Getter
	@AllArgsConstructor
	@ToString
	public static class Scom {
		private String componentsEndpoint;
		private String renditionsEndpoint;
		private String componentsEndpointById;
		private Thumbnail thumbnail;

		@Getter
		@AllArgsConstructor
		@ToString
		public static class Thumbnail {
			private String height;
			private String width;
			private String type;
		}
	}

	@Getter
	@AllArgsConstructor
	@ToString
	public static class SrwmCookie {
		private boolean isSecure;
		private String domain;
		private String path;
	}
}
