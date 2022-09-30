package com.afp.iris.sr.wm.presentation.dto;

import com.afp.iptc.g2.config.afp.ItemNature;
import com.afp.iptc.g2.config.afp.PublicationStatus;
import com.afp.iptc.g2.config.afp.RenditionType;
import com.afp.iptc.g2.libg2api.G2WebStoryDocument;
import com.afp.iptc.g2.libg2api.Keyword;
import com.afp.iptc.g2.libg2api.PictureRenditionInfo;
import com.afp.iptc.g2.libg2api.Subject;
import com.afp.iptc.g2.libg2api.WebStoryRenditionInfo;
import com.afp.iptc.g2.libg2api.ChangeInfo.ChangeStatus;
import com.afp.iris.sr.wm.domain.exception.MediaTopicNotSupportedException;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;

import static com.afp.iris.sr.wm.TestUtils.parseJnews;
import static org.junit.jupiter.api.Assertions.*;

class JnewsTest {

	@Test
	void should_return_g2_webstory_with_given_json_string() throws IOException {
		// GIVEN
		String jsonJnews = """
				  {
				    "metadataLanguage": "en",
				    "contentLanguage": "en",
				    "edNote": "A general editorial note (aka dialogue client)",
				    "headline": "A headline",
				    "catchline": "A catchline",
				    "pubStatus": "http://cv.iptc.org/newscodes/pubstatusg2/usable",
				    "itemClass": "http://cv.afp.com/itemnatures/webStory",
				       "subjects": [
				{
				"type": "http://cv.iptc.org/newscodes/cpnature/abstract",
				"uri": "http://cv.iptc.org/newscodes/mediatopic/17000000"
				}
				],
				    "remoteContents": [
				      {
				        "href": "http://somehost:someport/someEndpoint/someZipResource",
				        "contentType": "application/zip"
				      }
				    ],
				    "icons": [
				      {
				      	"rendition": "http://cv.iptc.org/newscodes/rendition/preview",
				        "href": "http://somehost:someport/someEndpoint/someImageResource",
				        "contentType": "image/jpeg"
				      }
				    ]
				  }""";

		// WHEN
		final Jnews jnews = parseJnews(jsonJnews);
		final G2WebStoryDocument g2WebStoryDocument = jnews.toG2WebStoryDocument();

		// THEN
		assertEquals("en", g2WebStoryDocument.getMetadataLanguage());
		assertEquals("en", g2WebStoryDocument.getContentLanguage());
		assertEquals("A general editorial note (aka dialogue client)", g2WebStoryDocument.getDialogClient());
		assertEquals("A headline", g2WebStoryDocument.getTitle());
		assertEquals("A catchline", g2WebStoryDocument.getCatchline());
		assertEquals(PublicationStatus.USABLE.toConceptURI(), g2WebStoryDocument.getPublicationStatus());
		assertEquals(ItemNature.WEB_STORY.toConceptURI(), g2WebStoryDocument.getItemNature());

		// icons
		assertEquals(1, g2WebStoryDocument.getIcons().size());

		PictureRenditionInfo icon = new PictureRenditionInfo(RenditionType.PREVIEW.toConceptURI());
		icon.setHref(URI.create("http://somehost:someport/someEndpoint/someImageResource"));
		icon.setContentType(MediaType.IMAGE_JPEG_VALUE);
		assertEquals(icon, g2WebStoryDocument.getIcons().get(0));

		// remoteContents
		assertEquals(1, g2WebStoryDocument.getRenditions().size());
		WebStoryRenditionInfo webStoryRenditionInfo = new WebStoryRenditionInfo(null);
		webStoryRenditionInfo.setHref(URI.create("http://somehost:someport/someEndpoint/someZipResource")); // URI SCOM
																											// DU ZIP
		webStoryRenditionInfo.setContentType("application/zip");
		assertEquals(webStoryRenditionInfo, g2WebStoryDocument.getRenditions().get(0));

		// subjects
		Subject expetedSubject = new Subject(URI.create("http://cv.iptc.org/newscodes/mediatopic/17000000"));
		expetedSubject.setType(URI.create("http://cv.iptc.org/newscodes/cpnature/abstract"));
		assertEquals(expetedSubject, g2WebStoryDocument.getSubjects().get(0));

		// keywords
		Keyword expectedKeyWord = new Keyword("weather");
		assertEquals(expectedKeyWord, g2WebStoryDocument.getKeywords().get(0));
	}

	@Test
	void should_return_expected_keyword_when_metadataLanguage_is_en() throws IOException {
		// GIVEN
		String jsonJnews = """
				{
				  "metadataLanguage": "en",
				  "contentLanguage": "en",
				  "edNote": "A general editorial note (aka dialogue client)",
				  "headline": "A headline",
				  "catchline": "A catchline",
				  "pubStatus": "http://cv.iptc.org/newscodes/pubstatusg2/usable",
				  "itemClass": "http://cv.afp.com/itemnatures/webStory",
				   "subjects": [
				      {
				      "type": "http://cv.iptc.org/newscodes/cpnature/abstract",
				      "uri": "http://cv.iptc.org/newscodes/mediatopic/17000000"
				      }
				      ],
				  "remoteContents": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someZipResource",
				      "contentType": "application/zip"
				    }
				  ],
				  "icons": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someImageResource",
				      "contentType": "image/jpeg"
				    }
				  ]
				}""";

		// WHEN
		final G2WebStoryDocument g2WebStoryDocument = createG2FromJsonNews(jsonJnews);
		// THEN
		Keyword expectedKeyWord = new Keyword("weather");
		assertEquals(expectedKeyWord, g2WebStoryDocument.getKeywords().get(0));
	}

	@Test
	void should_return_expected_keyword_when_metadata_language_is_fr() throws IOException {
		// GIVEN
		String jsonJnews = """
				{
				  "metadataLanguage": "fr",
				  "contentLanguage": "fr",
				  "edNote": "A general editorial note (aka dialogue client)",
				  "headline": "A headline",
				  "catchline": "A catchline",
				  "pubStatus": "http://cv.iptc.org/newscodes/pubstatusg2/usable",
				  "itemClass": "http://cv.afp.com/itemnatures/webStory",
				   "subjects": [
				      {
				      "type": "http://cv.iptc.org/newscodes/cpnature/abstract",
				      "uri": "http://cv.iptc.org/newscodes/mediatopic/17000000"
				      }
				      ],
				  "remoteContents": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someZipResource",
				      "contentType": "application/zip"
				    }
				  ],
				  "icons": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someImageResource",
				      "contentType": "image/jpeg"
				    }
				  ]
				}""";

		// WHEN
		final G2WebStoryDocument g2WebStoryDocument = createG2FromJsonNews(jsonJnews);
		// THEN
		Keyword expectedKeyWord = new Keyword("météo");
		assertEquals(expectedKeyWord, g2WebStoryDocument.getKeywords().get(0));
	}

	@Test
	void should_throw_InternalTechnicalException_when_media_topic_is_not_supported() {
		// GIVEN
		String jsonJnews = """
				{
				  "metadataLanguage": "fr",
				  "contentLanguage": "fr",
				  "edNote": "A general editorial note (aka dialogue client)",
				  "headline": "A headline",
				  "catchline": "A catchline",
				  "pubStatus": "http://cv.iptc.org/newscodes/pubstatusg2/usable",
				  "itemClass": "http://cv.afp.com/itemnatures/webStory",
				   "subjects": [
				      {
				      "type": "http://cv.iptc.org/newscodes/cpnature/abstract",
				      "uri": "http://cv.iptc.org/newscodes/mediatopic/10000033"
				      }
				      ],
				  "remoteContents": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someZipResource",
				      "contentType": "application/zip"
				    }
				  ],
				  "icons": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someImageResource",
				      "contentType": "image/jpeg"
				    }
				  ]
				}""";

		// WHEN & THEN
		MediaTopicNotSupportedException mediaTopicNotSupporetdException = assertThrows(MediaTopicNotSupportedException.class,
				() -> createG2FromJsonNews(jsonJnews));
		assertEquals("Media topic is not supported", mediaTopicNotSupporetdException.getMessage());
	}

	@Test
	void should_return_G2WebStoryDocument_with_correction_signal_when_jnews_contains_correction_signal() throws IOException {
		// GIVEN
		String jsonJnews = """
				{
				  "metadataLanguage": "fr",
				  "contentLanguage": "fr",
				  "edNote": "A general editorial note (aka dialogue client)",
				  "headline": "A headline",
				  "catchline": "A catchline",
				  "pubStatus": "http://cv.iptc.org/newscodes/pubstatusg2/usable",
				  "itemClass": "http://cv.afp.com/itemnatures/webStory",
				   "subjects": [
				      {
				      "type": "http://cv.iptc.org/newscodes/cpnature/abstract",
				      "uri": "http://cv.iptc.org/newscodes/mediatopic/11000000"
				      }
				      ],
				  	   "signals": [
				                          {
				                         "uri": "http://cv.iptc.org/newscodes/signal/correction"
				                          }
				                              ],
				  "remoteContents": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someZipResource",
				      "contentType": "application/zip"
				    }
				  ],
				  "icons": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someImageResource",
				      "contentType": "image/jpeg"
				    }
				  ]
				}""";

		// WHEN & THEN
		final G2WebStoryDocument g2WebStoryDocument = createG2FromJsonNews(jsonJnews);
		ChangeStatus expectedStatus = ChangeStatus.CORRECTION;
		assertEquals(g2WebStoryDocument.getChanges().get(0).getStatus(), expectedStatus);
	}
	
	@Test
	void should_return_G2WebStoryDocument_with_update_signal_when_jnews_contains_update_signal() throws IOException {
		// GIVEN
		String jsonJnews = """
				{
				  "metadataLanguage": "fr",
				  "contentLanguage": "fr",
				  "edNote": "A general editorial note (aka dialogue client)",
				  "headline": "A headline",
				  "catchline": "A catchline",
				  "pubStatus": "http://cv.iptc.org/newscodes/pubstatusg2/usable",
				  "itemClass": "http://cv.afp.com/itemnatures/webStory",
				   "subjects": [
				      {
				      "type": "http://cv.iptc.org/newscodes/cpnature/abstract",
				      "uri": "http://cv.iptc.org/newscodes/mediatopic/11000000"
				      }
				      ],
				  	   "signals": [
				                          {
				                         "uri": "http://cv.iptc.org/newscodes/signal/update"
				                          }
				                              ],
				  "remoteContents": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someZipResource",
				      "contentType": "application/zip"
				    }
				  ],
				  "icons": [
				    {
				      "href": "http://somehost:someport/someEndpoint/someImageResource",
				      "contentType": "image/jpeg"
				    }
				  ]
				}""";

		// WHEN & THEN
		final G2WebStoryDocument g2WebStoryDocument = createG2FromJsonNews(jsonJnews);
		ChangeStatus expectedStatus = ChangeStatus.UPDATE;
		assertEquals(g2WebStoryDocument.getChanges().get(0).getStatus(), expectedStatus);
	}

	private G2WebStoryDocument createG2FromJsonNews(String jsonJnews) throws IOException {
		final Jnews jnews = parseJnews(jsonJnews);
		final G2WebStoryDocument g2WebStoryDocument = jnews.toG2WebStoryDocument();
		return g2WebStoryDocument;
	}
}