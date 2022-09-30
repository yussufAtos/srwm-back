package com.afp.iris.sr.wm.services;


import com.afp.iptc.g2.GUIDUtil;
import com.afp.iptc.g2.config.afp.LinkRel;
import com.afp.iptc.g2.libg2api.*;
import com.afp.iris.sr.wm.clients.CmsRestClient;
import com.afp.iris.sr.wm.clients.ScomRestClient;
import com.afp.iris.sr.wm.clients.dto.CmsError;
import com.afp.iris.sr.wm.clients.dto.ResizingResults;
import com.afp.iris.sr.wm.domain.DocumentService;
import com.afp.iris.sr.wm.domain.exception.CmsErrorException;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import com.afp.iris.sr.wm.domain.exception.NotAuthenticatedException;
import com.afp.iris.sr.wm.domain.exception.UnexpectedSearchResultException;
import com.afp.iris.sr.wm.helper.G2Helper;
import com.afp.iris.sr.wm.presentation.dto.Jnews;
import com.afp.iris.sr.wm.utils.UriUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.net.URI;
import java.util.*;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {
	private final CmsRestClient cmsRestClient;
	private final ScomRestClient scomRestClient;

	public DocumentServiceImpl(CmsRestClient cmsRestClient, ScomRestClient scomRestClient) {
		this.cmsRestClient = cmsRestClient;
		this.scomRestClient = scomRestClient;
	}

	public G2Document createDocument(G2Document document) {
		ResponseEntity<String> response = null;
		try {
			response = this.cmsRestClient.createDocument(document);
            checkRedirectToCas(response);

		} catch (BadNewsMLG2Exception e) {
			log.error("Unable to create document into CMS.", e);
			throw new InternalTechnicalException(e, "Error while creating document into CMS : %s", e.getMessage());
		} catch (RestClientResponseException e) {
			handleRestClientResponseException(e);
        }

		checkCmsResponse(response, HttpStatus.CREATED);
		final String newsmlString = response.getBody();

		log.debug("Created NEWSML document : \n{}", newsmlString);

		return parseG2DocumentOrFail(newsmlString);
	}

	private void checkRedirectToCas(ResponseEntity<String> response) {
		if (response.getStatusCode() == HttpStatus.FOUND &&
			response.getHeaders().get(HttpHeaders.LOCATION) != null &&
			response.getHeaders().get(HttpHeaders.LOCATION).stream().anyMatch(location -> location.contains("cas/login"))) {
			log.error("Auth Error, the CMS redirect into CAS : {}", response.getHeaders().get(HttpHeaders.LOCATION));
		    throw new NotAuthenticatedException("CMS redirect to CAS for auth : " + response.getHeaders().get(HttpHeaders.LOCATION));
		}
	}

	@Override
	public Optional<G2Document> editDocument(G2Document document) {
		log.debug("START DocumentService-editDocument");

		ResponseEntity<String> response = null;
		try {
			response = this.cmsRestClient.editDocument(document);
			checkRedirectToCas(response);

		} catch (BadNewsMLG2Exception e) {
			log.error("Unable to edit document into CMS.", e);
			throw new InternalTechnicalException(e, "Error while editing document into CMS : %s", e.getMessage());
		} catch (RestClientResponseException e) {
			handleRestClientResponseException(e);
		}

		if (response == null
				|| (HttpStatus.OK != response.getStatusCode() && response.getStatusCode() != HttpStatus.NO_CONTENT)) {
			throw new InternalTechnicalException("Error while editing document into CMS. Response : \n%s", response);
		}

		if (HttpStatus.OK.equals(response.getStatusCode()) && response.getBody() == null) {
			throw new InternalTechnicalException("Error while editing document into CMS, response 200 without body");
		}

		Optional<G2Document> optionalDocument = Optional.empty();
		if (HttpStatus.OK.equals(response.getStatusCode())) {
			final String newsmlString = response.getBody();
			G2Document g2Document = parseG2DocumentOrFail(newsmlString);
			optionalDocument = Optional.of(g2Document);
		}

		log.debug("END DocumentService-editDocument");
		return optionalDocument;
	}

	@Override
	public G2Document validateDocument(G2Document document) {
		log.debug("START DocumentService-validateDocument");

		ResponseEntity<String> response = null;
		try {
			response = this.cmsRestClient.validateDocument(document);
			checkRedirectToCas(response);

		} catch (BadNewsMLG2Exception e) {
			throw new InternalTechnicalException(e, "Error while validating document into CMS : %s", e.getMessage());
		} catch (RestClientResponseException e) {
			handleRestClientResponseException(e);
		}

		checkCmsResponse(response, HttpStatus.CREATED, HttpStatus.OK);

		final String newsmlString = response.getBody();
		document = parseG2DocumentOrFail(newsmlString);

		log.debug("Validated NEWSML document : \n{}", newsmlString);
		log.debug("END DocumentService-validateDocument");
		return document;
	}

	@Override
	public G2Document validateNewDocument(Jnews jnews) {
		log.debug("START DocumentService-validateNewDocument");
		G2Document document = jnews.toG2WebStoryDocument();

		prepareDocumentForCreation(document);

		document = createDocument(document);

		final Optional<G2Document> optionalDocument = editDocument(document);
		if (optionalDocument.isPresent()) {
			document = optionalDocument.get();
		}

		document = validateDocument(document);

		log.debug("END DocumentService-validateNewDocument");
		return document;
	}

	/* TODO : when web manager will handle a different type of document, consider to abstract operations
	    and keep common operations : for example create a WebStoryDocumentService to handle specific to that
	    type, and even a VisualDocumentService in addition to handle each abstract level of NEWSML-G2 format
	 */
	public G2Document validateUpdateDocument(Jnews jnews) {
		log.debug("START DocumentService-validateUpdateDocument");
		G2WebStoryDocument document = jnews.toG2WebStoryDocument();

		G2Document documentInCMS = getDocumentById(UriUtils.extractIdFromUri(document.getLinkToSelf().toString()));
		// A prepared document is a document phoenixed, and cannot be edited, should validate directly
		if(documentInCMS.isPrepared()) {
			document = G2Helper.updateWebStoryDocument((G2WebStoryDocument) documentInCMS, document);

		} else {
			final Optional<G2Document> optionalDocumentInCMS = editDocument(document);
			if (optionalDocumentInCMS.isPresent()) {
				// TODO consider to generify in order to abstract
				checkIfPosterChangedAndGenerateThumbnail(document, (G2VisualDocument) optionalDocumentInCMS.get());
				// we add information/data from document to optionalDocumentInCMS received from CMS wich is more complete
				document = G2Helper.updateWebStoryDocument((G2WebStoryDocument) optionalDocumentInCMS.get(), document);
			}
		}

		document = (G2WebStoryDocument) validateDocument(document);

		log.debug("END DocumentService-validateUpdateDocument");

		return document;
	}

	private G2Document phoenixDocument(String guid, URI selfLink) {
		log.debug("START DocumentService-phoenixDocument (guid = {} selfLink = {}", guid, selfLink);
		ResponseEntity<String> response = null;
		try {
			response = this.cmsRestClient.phoenixDocument(guid, selfLink);
			checkRedirectToCas(response);

		} catch (RestClientResponseException e) {
			handleRestClientResponseException(e);
		}

		checkCmsResponse(response, HttpStatus.CREATED, HttpStatus.OK);

		final String newsmlString = response.getBody();
		G2Document document = parseG2DocumentOrFail(newsmlString);

		log.debug("phoenixed NEWSML document : \n{}", newsmlString);
		log.debug("END DocumentService-phoenixDocument");
		return document;
	}

	private void checkIfPosterChangedAndGenerateThumbnail(G2VisualDocument editedNewDocument, G2VisualDocument oldDocument) {
		if (!G2Helper.getPreviewIcon(editedNewDocument).equals(G2Helper.getPreviewIcon(oldDocument))){
			URI posterUri = G2Helper.getPreviewIcon(editedNewDocument).getHref();
			PictureRenditionInfo thumbnail = generateThumbnailFromScom(posterUri);
			G2Helper.replaceRendition(editedNewDocument, thumbnail);
			log.info("Thumbnail replaced for document : {}", editedNewDocument.getGuid());
		}
	}

	// WARN duplicated code from AmpServiceImpl to keep those two services not correlated
	private PictureRenditionInfo generateThumbnailFromScom(URI ampPosterURI) {

		final ResponseEntity<ResizingResults> response;
		try {
			response = this.scomRestClient.generateThumbnail(ampPosterURI);
		} catch (RestClientResponseException e) {
			throw new InternalTechnicalException(e, "Unable to generate thumbnail from SCOM. Reason : status code={} message={}",
					e.getRawStatusCode(), e.getResponseBodyAsString());
		}

		if (response == null || response.getBody() == null || response.getStatusCode() != HttpStatus.OK) {
			log.error("Error while generating thumbnail from SCOM, response is:\n {}", response);
			throw new InternalTechnicalException("Error while generating thumbnail from SCOM");
		}

		ResizingResults resizingResults = response.getBody();
		if(resizingResults.getResults().size() != 1) {
			final String errorMessage = "Unexpected response from SCOM, there should one and only one result for thumbnail generation";
			log.error("Unexpected response from SCOM, there should one and only one result for thumbnail generation. Response : {}", response);
			throw new InternalTechnicalException(errorMessage);
		}
		ResizingResults.Result result = resizingResults.getResults().get(0);

		return G2Helper.buildThumbnail(result);
	}

	@Override
	public G2Document depublishDocument(String guid) {
		log.debug("START DocumentService-depublishDocument {} ",guid);
		G2Document document = getDocumentForDepublication(guid);

		final Optional<G2Document> optionalDocument = editDocument(document);

		if (optionalDocument.isPresent()) {
			document = optionalDocument.get();
		}

		document = depublishDocument(document);

		log.debug("END DocumentService-depublishDocument {} ",guid);
		return document;
	}

	private G2Document depublishDocument(G2Document document) {
		log.debug("START DocumentService-depublishDocument");
		ResponseEntity<String> response = null;
		try {
			response = this.cmsRestClient.depublishDocument(document);
			checkRedirectToCas(response);

		} catch (BadNewsMLG2Exception e) {
			throw new InternalTechnicalException(e, "Error while depublishing document into CMS : %s", e.getMessage());
		} catch (RestClientResponseException e) {
			handleRestClientResponseException(e);
		}

		checkCmsResponse(response, HttpStatus.CREATED, HttpStatus.OK);

		final String newsmlString = response.getBody();
		document = parseG2DocumentOrFail(newsmlString);

		log.debug("depublished NEWSML document : \n{}", newsmlString);
		log.debug("END DocumentService-depublishDocument");
		return document;
	}

	public G2Document getDocumentById(String id) {
		log.debug("START DocumentService-getDocumentById {}", id);

		ResponseEntity<String> response = null;
		try {
			response = this.cmsRestClient.getDocumentById(id);
			checkRedirectToCas(response);

		} catch (RestClientResponseException e) {
			handleRestClientResponseException(e);
		}

		checkCmsResponse(response, HttpStatus.OK);

		final String newsmlString = response.getBody();
		final G2Document document = parseG2DocumentOrFail(newsmlString);

		log.debug("END DocumentService-getDocumentById {} ", id);
		return document;
	}

	/**
	 * Search document's last validated version in hot usage, if not found it searchs in cold usage (archives)
	 * If document is in archives, phoenixes it
	 * @param guid
	 * @return Found in hot usage or phoenixed from cold usage G2Document
	 */
	@Override
	public G2Document getDocumentByGuid(String guid) {
		log.debug("START DocumentService-getDocumentByGuid {} ",guid);

		G2Document document;
		G2SearchResult g2SearchResult = searchLastValidatedDocumentByGuid(guid, false);
		if (g2SearchResult.getReferences().size() == 0) {

			G2SearchResult archivesSearchResult = searchLastValidatedDocumentByGuid(guid, true);

			if (archivesSearchResult.getReferences().size() == 0) {
				final String searchErrorMessage = String.format("Unexpected search result received from CMS: document (%s) is not found neither in hot or cold usage", guid);
				log.error(searchErrorMessage);
				throw new UnexpectedSearchResultException(searchErrorMessage);
			}

			// we are not interested in the document but by the availability in hot usage into CMS
			log.info("Document (URI = {}) will be phoenixed", archivesSearchResult.getReferences().get(0).getURI());
			document = phoenixDocument(guid, archivesSearchResult.getReferences().get(0).getURI());
		} else {
			// document is available in hot usage
			final URI documentStoryUri = g2SearchResult.getReferences().get(0).getLinks(LinkRel.STORY.toConceptURI()).get(0);

			document = getDocumentByStoryId(UriUtils.extractIdFromUri(documentStoryUri.toString()));
		}

		log.debug("END DocumentService-getDocumentByGuid {} ", guid);
		return document;
	}

	private G2SearchResult searchLastValidatedDocumentByGuid(String guid, boolean inArchives) {
		log.debug("START DocumentService-searchLastValidatedDocumentByGuid (guid={} inArchives={})", guid, inArchives);
     
		ResponseEntity<String> response = null;
		try {

			response = this.cmsRestClient.searchLastValidatedDocumentByGuid(guid, inArchives);
			checkRedirectToCas(response);

		} catch (RestClientResponseException e) {
			handleRestClientResponseException(e);
		}

		checkCmsResponse(response, HttpStatus.OK);

		G2SearchResult g2SearchResult;
		try {
			final String newsmlString = response.getBody();
			g2SearchResult = (G2SearchResult) G2ObjectFactory.parse(newsmlString);

		} catch (BadNewsMLG2Exception e) {
			log.error("Unable to parse search result XML received from CMS.", e);
			throw new InternalTechnicalException(e, "Error while parsing search result XML received from CMS : %s",
					e.getMessage());
		}

		if (g2SearchResult.getReferences().size() > 1) {
			final String searchErrorMessage = "Unexpected search result received from CMS: must return only the last validated document reference but returned more";
			log.error(searchErrorMessage);
			throw new InternalTechnicalException(searchErrorMessage);
		}

		log.debug("END DocumentService-searchLastValidatedDocumentByGuid {} ", guid);
		return g2SearchResult;
	}
	

	private G2Document getDocumentByStoryId(String storyId) {
		log.debug("START DocumentService-getDocumentByStoryId {} ", storyId);
		ResponseEntity<String> response = null;
		try {
			response = this.cmsRestClient.getDocumentByStoryId(storyId);
			checkRedirectToCas(response);

		} catch (RestClientResponseException e) {
			handleRestClientResponseException(e);
		}

		checkCmsResponse(response, HttpStatus.OK);

		final G2Document document = parseG2DocumentOrFail(response.getBody());

		log.debug("END DocumentService-getDocumentByStoryId {} ", storyId);
		return document;
	}

	private G2Document parseG2DocumentOrFail(String newsml) {
		G2Document document;
		try {
			document = (G2Document) G2ObjectFactory.parse(newsml);

		} catch (BadNewsMLG2Exception e) {
			log.error("Unable to parse document XML received from CMS.", e);
			throw new InternalTechnicalException(e, "Error while parsing document XML received from CMS : %s",
					e.getMessage());
		}
		return document;
	}

	G2Document getDocumentForDepublication(String guid) {
		G2SearchResult g2SearchResult = searchLastValidatedDocumentByGuid(guid, false);
		final List<URI> links = g2SearchResult.getReferences().get(0).getLinks(LinkRel.SELF.toConceptURI());
		final URI documentSelfUri = links.get(0);
		G2Document document = getDocumentById(UriUtils.extractIdFromUri(documentSelfUri.toString()));
		return document;
	}

	/**
	 * Makes sure mandatory fields are filled if not, fills
	 *
	 * @param document G2Document to check and complete
	 */
	private void prepareDocumentForCreation(G2Document document) {
		if (document.getGUID() == null) {
			document.setGUID(GUIDUtil.makeLibg2GUID());
		}

		if (document.getTransmissionDate() == null) {
			try {
				document.setTransmissionDate(DatatypeFactory.newInstance()
						.newXMLGregorianCalendar((GregorianCalendar) Calendar.getInstance()));
			} catch (DatatypeConfigurationException e) {
				throw new InternalTechnicalException(e, "Error while creating document : %s", e.getMessage());
			}
		}

	}

	@Override
	public Cookie loginToBackend(HttpSession session) {
		return cmsRestClient.loginToBackend(session);
	}

	private void checkCmsResponse(ResponseEntity<String> response, HttpStatus ... statuses) {
		if (response == null || response.getBody() == null
				|| Arrays.stream(statuses).noneMatch(httpStatus -> httpStatus.equals(response.getStatusCode()))) {
			log.error("Error while calling CMS, response is:\n {}", response);
			throw new InternalTechnicalException("Error while calling CMS : unexpected response");
		}
	}

	private void handleRestClientResponseException(RestClientResponseException e) {
		log.error("Operation in CMS is not possible. Reason : status code={} message={}", e.getRawStatusCode(),
				e.getResponseBodyAsString());
		CmsError cmsError=getCmsErrorDescription(e.getResponseBodyAsString());
		HttpStatus cmsErrorStatus = HttpStatus.valueOf(e.getRawStatusCode());

		if (HttpStatus.UNAUTHORIZED.equals(cmsErrorStatus)) {
		   throw new NotAuthenticatedException(e);
		}

		if (cmsErrorStatus.is4xxClientError() || cmsErrorStatus.is5xxServerError()) {
		   throw new CmsErrorException(cmsErrorStatus, cmsError.getOrigin()+" : "+cmsError.getDiagnostic());
		}
		
		throw new InternalTechnicalException(e, "Error while calling CMS : %s", e.getMessage());
	}
	
	public CmsError getCmsErrorDescription(String errorStringXml) {
		XmlMapper xmlMapper = new XmlMapper();
		CmsError errorResponse = null;
		try {
			errorResponse = xmlMapper.readValue(errorStringXml, CmsError.class);
		} catch (JsonProcessingException e) {
			throw new InternalTechnicalException(e, "Error while parsing CMS error : %s \nCMS error = %s", e.getMessage(), errorStringXml);
		}
		return errorResponse;
	}

}
