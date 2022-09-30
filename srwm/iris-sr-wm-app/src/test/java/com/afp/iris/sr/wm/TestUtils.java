package com.afp.iris.sr.wm;

import com.afp.iptc.g2.GUIDUtil;
import com.afp.iptc.g2.config.afp.*;
import com.afp.iptc.g2.libg2api.Audience;
import com.afp.iptc.g2.libg2api.BadNewsMLG2Exception;
import com.afp.iptc.g2.libg2api.City;
import com.afp.iptc.g2.libg2api.Contributor;
import com.afp.iptc.g2.libg2api.Country;
import com.afp.iptc.g2.libg2api.Creator;
import com.afp.iptc.g2.libg2api.G2ObjectFactory;
import com.afp.iptc.g2.libg2api.G2SearchResult;
import com.afp.iptc.g2.libg2api.G2WebStoryDocument;
import com.afp.iptc.g2.libg2api.InfoSource;
import com.afp.iptc.g2.libg2api.PictureRenditionInfo;
import com.afp.iptc.g2.libg2api.ProviderInfo;
import com.afp.iptc.g2.libg2api.Subject;
import com.afp.iptc.g2.libg2api.WebStoryRenditionInfo;
import com.afp.iris.sr.wm.clients.dto.ResizingResults;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import com.afp.iris.sr.wm.presentation.dto.Jnews;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.FileCopyUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TestUtils {

    public static final String SAMPLE_JNEWS_STRING = """
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
                  "href": "http://somehost/components/someZipResource",
                  "contentType": "application/zip"
                }
              ],
              "icons": [
                {
                  "rendition": "http://cv.iptc.org/newscodes/rendition/preview",
                  "href": "http://somehost/components/someImageResource",
                  "contentType": "image/jpeg"
                }
              ]
            }""";

    public static final String SAMPLE_JNEWS_WITH_SELF_LINK_STRING = """
            {
              "guid": "http://somehost:someport/someEndpoint/someGUID",
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
                  "href": "http://somehost/components/someZipResource",
                  "contentType": "application/zip"
                }
              ],
              "links": [
                {
                  "rel": "http://cv.afp.com/linkrels/self",
                  "href": "http://somehost:someport/someEndpoint/123-uuid"
                }
              ],
              "icons": [
                {
                  "rendition": "http://cv.iptc.org/newscodes/rendition/preview",
                  "href": "http://somehost/components/someImageResource",
                  "contentType": "image/jpeg"
                }
              ]
            }""";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final XmlMapper xmlMapper = new XmlMapper();

    public static byte[] getFileBytes(String path) throws IOException {
        ClassLoader classLoader = TestUtils.class.getClassLoader();

        InputStream is = classLoader.getResourceAsStream(path);
        return FileCopyUtils.copyToByteArray(is);
    }

    public static URL getFileUrl(String path) throws IOException {
        ClassLoader classLoader = TestUtils.class.getClassLoader();

        return classLoader.getResource(path);
    }

    public static Resource getClassPathResource(String filepath) {
        return new ClassPathResource(filepath);
    }

    public static G2WebStoryDocument createTestG2WebStoryDocument () {
        final URI posterUri = URI.create("http://somehost/components/someImageResource");
        final URI zipUri = URI.create("http://somehost/components/someZipResource");
        return createMockedG2WebStoryDocument(zipUri, posterUri, "A headline");
    }

    public static Jnews getSampleJnews() throws IOException {
        return mapper.readValue(SAMPLE_JNEWS_STRING.getBytes(StandardCharsets.UTF_8), Jnews.class);
    }

    public static Jnews getSampleJnewsWithSelfLink() throws IOException {
        return mapper.readValue(SAMPLE_JNEWS_WITH_SELF_LINK_STRING.getBytes(StandardCharsets.UTF_8), Jnews.class);
    }

    public static Jnews parseJnews (String json) throws IOException {
        return mapper.readValue(json.getBytes(StandardCharsets.UTF_8), Jnews.class);
    }

    public static G2WebStoryDocument getSampleWebStoryDocument () {
        G2WebStoryDocument webStory = G2ObjectFactory.createWebStoryDocument();

        webStory.setGUID(URI.create("http://d.afp.com/3FQ2UY"));
        webStory.setLinkToSelf(URI.create("http://test/documents/123uuid"));
        webStory.setMetadataLanguage("en");
        webStory.setContentLanguage("en");
        webStory.setDialogClient("A general editorial note (aka dialogue client)");
        webStory.setTitle("A headline");
        webStory.setCatchline("A catchline");

        webStory.setPublicationStatus(PublicationStatus.USABLE.toConceptURI());

        WebStoryRenditionInfo webStoryRenditionInfo = new WebStoryRenditionInfo(null);
        webStoryRenditionInfo.setHref(URI.create("http://somehost:someport/someEndpoint/someZipResource")); // URI SCOM DU ZIP
        webStoryRenditionInfo.setContentType("application/zip");
        webStory.getRenditions().add(webStoryRenditionInfo);

        PictureRenditionInfo icon = new PictureRenditionInfo(null);
        icon.setHref(URI.create("http://somehost:someport/somendpoint/someCoverResource")); // URI SCOM DE LA COVER
        icon.setContentType(MediaType.IMAGE_JPEG_VALUE);
        webStory.getIcons().add(icon);

        try {
            webStory.setTransmissionDate(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar) Calendar.getInstance()));
        } catch (DatatypeConfigurationException e) {
            throw new InternalTechnicalException(e, "Error while creating document : %s", e.getMessage());
        }

        return webStory;
    }

    public static G2WebStoryDocument createMockedG2WebStoryDocument(URI webStoryZipURI, URI posterURI, String title) {

        G2WebStoryDocument myWebStory = G2ObjectFactory.createWebStoryDocument();

        try {
            myWebStory.setVersionDate(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar) Calendar.getInstance()));
            myWebStory.setTransmissionDate(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar) Calendar.getInstance()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.setURI(URI.create("http://cv.iptc.org/newscodes/newsprovider/AFP"));
        providerInfo.setName("AFP");
        myWebStory.setProvider(providerInfo);

        myWebStory.setGuid(GUIDUtil.makeLibg2GUID());

        myWebStory.setPublicationStatus(PublicationStatus.USABLE.toConceptURI());

        WebStoryRenditionInfo webStoryRenditionInfo = new WebStoryRenditionInfo(null);
        webStoryRenditionInfo.setHref(webStoryZipURI); // URI SCOM DU ZIP
        webStoryRenditionInfo.setContentType("application/zip");
        myWebStory.getRenditions().add(webStoryRenditionInfo);

        PictureRenditionInfo icon = new PictureRenditionInfo(RenditionType.PREVIEW.toConceptURI());
        icon.setHref(posterURI); // URI SCOM DE LA COVER
        icon.setContentType(MediaType.IMAGE_JPEG_VALUE);
        myWebStory.getIcons().add(icon);

        myWebStory.setTitle(title);

        myWebStory.setCatchline("A catchline");

        Subject medtop1 = new Subject(URI.create("http://cv.iptc.org/newscodes/mediatopic/16000000"), "conflicts, war and peace");
        medtop1.setType(ConceptNature.ABSTRACT.toConceptURI());
        myWebStory.getSubjects().add(medtop1);

        Subject medtop2 = new Subject(URI.create("http://cv.iptc.org/newscodes/mediatopic/04000000"), "economy, business and finance");
        medtop2.setType(ConceptNature.ABSTRACT.toConceptURI());
        myWebStory.getSubjects().add(medtop2);

        Subject vp = new Subject(URI.create("http://ref.afp.com/persons/32639"));
        vp.setGivenName("Vladimir");
        vp.setFamilyName("Putin");
        vp.setType(ConceptNature.PERSON.toConceptURI());
        myWebStory.getSubjects().add(vp);

        Subject event1 = new Subject(URI.create("http://eventmanager.afp.com/events/IZT27"), "#IZT27 : Russian invasion of Ukraine");
        event1.setType(ConceptNature.EVENT.toConceptURI());
        myWebStory.getSubjects().add(event1);

        Subject event2 = new Subject(URI.create("http://eventmanager.afp.com/events/ITA94"), "#ITA94 : Russia-West tensions on Ukraine");
        event1.setType(ConceptNature.EVENT.toConceptURI());
        myWebStory.getSubjects().add(event2);

        Subject urkraine = new Subject(URI.create("http://ref.afp.com/locations/204"), "Ukraine");
        urkraine.setType(ConceptNature.GEOAREA.toConceptURI());
        myWebStory.getSubjects().add(urkraine);

        InfoSource getty = new InfoSource(URI.create("http://ref.afp.com/sources/2275"), "GETTY IMAGES NORTH AMERICA", ConceptNature.ORGANISATION.toConceptURI(), InfoSourceRole.ORIG_OF_CONT.toConceptURI());
        myWebStory.getInfoSources().add(getty);

        HashMap<String, String> franceNames = new HashMap<String, String>();
        franceNames.put("en", "France");
        Country france = new Country(URI.create("http://ref.afp.com/locations/67"), franceNames, "FRA");

        HashMap<String, String> parisNames = new HashMap<String, String>();
        parisNames.put("en", "Paris");

        City paris = new City(URI.create("http://ref.afp.com/locations/2500"), parisNames);
        paris.setBroader(france);

        myWebStory.getLocationsOfOriginOfContent().add(paris);

        Creator creator1 = new Creator(URI.create("http://example.com/1234"), "John Doe", null, CreatorRole.VIDEO_JOURNALIST.toConceptURI());
        creator1.getRoles().add(ContributorRole.FOR_BY_LINE.toConceptURI());

        Creator creator2 = new Creator(URI.create("http://example.com/5678"), "Jeanne Dupont", null, CreatorRole.FIELD_PRODUCER.toConceptURI());
        creator2.getRoles().add(ContributorRole.FOR_BY_LINE.toConceptURI());

        Contributor contributor1 = new Contributor(URI.create("http://example.com/abcd"), "Virginia Fox", null, ContributorRole.EDITOR.toConceptURI());
        contributor1.getRoles().add(ContributorRole.FOR_BY_LINE.toConceptURI());

        myWebStory.getChosenCreators().addAll(Arrays.asList(creator1, creator2));
        myWebStory.getChosenContributors().add(contributor1);

        myWebStory.setMetadataLanguage("en");
        myWebStory.setContentLanguage("en");

        myWebStory.setDialogClient("A general editorial note (aka dialogue client)");

        myWebStory.getExcludedAudiences().add(new Audience(ContentWarning.LANGUAGE.toConceptURI()));
        myWebStory.getExcludedAudiences().add(new Audience(ContentWarning.NUDITY.toConceptURI()));

        return myWebStory;
    }
    
	public static MockMultipartFile createMultipartFile(String fileName) throws IOException {
		String path = "src/test/resources/webstory/" + fileName;
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		MockMultipartFile mockMultipartFile = new MockMultipartFile("webStoryZip", "fileName", "application/zip", inputStream);
		return mockMultipartFile;

	}

    public static G2SearchResult getSampleG2SearchResult () throws IOException, BadNewsMLG2Exception {
        final File g2SearchResultFile = getClassPathResource("g2/g2_search_result.xml").getFile();
        return (G2SearchResult) G2ObjectFactory.parse(g2SearchResultFile);
    }

    public static G2SearchResult getSampleEmptyG2SearchResult () {
        return G2ObjectFactory.createSearchResult();
    }

    @Test
    void jnews_with_selflink() throws IOException, BadNewsMLG2Exception {

        G2WebStoryDocument g2WebStoryDocument = getSampleWebStoryDocument();

        URI linkToSelf = URI.create("http://test/documents/fed1e74f-0923-40ca-acb7-7677a744e70a");
        g2WebStoryDocument.setLinkToSelf(linkToSelf);

        String jnewsString = g2WebStoryDocument.jnewsRepresentation();
        String xmlString = g2WebStoryDocument.g2Representation();
        Jnews jnews = parseJnews(jnewsString);

        Assertions.assertTrue(jnews.getLinks().size() > 0);
        Assertions.assertEquals(linkToSelf, jnews.getLinks().get(0).getHref());
    }

    public static ResizingResults getSampleResizingResults () throws JsonProcessingException {
        String resizingResultsString = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <ResizingResults xmlns="http://sr.iris.afp.com/ResizingResults">
                    <result url="http://test.scom/components/3a794821d60d7e896baa6b38dcbf21de59d77963"
                            status="OK"
                            resultUrl="http://test.scom/components/5cf89b88c72b6275e083a9c862a322b195530fd7"
                            width="240" height="320" length="15756"/>
                </ResizingResults>
                """;

        return xmlMapper.readValue(resizingResultsString, ResizingResults.class);
    }
    
	public static byte[] getPosterContentAsByteArray() throws IOException {

		String path = "src/test/resources/webstory/poster-test.jpeg";
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return IOUtils.toByteArray(inputStream);

	}

}
