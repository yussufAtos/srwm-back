package com.afp.iris.sr.wm.services;

import com.afp.iptc.g2.GUIDUtil;
import com.afp.iptc.g2.config.afp.RenditionType;
import com.afp.iptc.g2.libg2api.G2ObjectFactory;
import com.afp.iptc.g2.libg2api.G2WebStoryDocument;
import com.afp.iptc.g2.libg2api.Misc;
import com.afp.iptc.g2.libg2api.PictureRenditionInfo;
import com.afp.iris.sr.wm.clients.ScomRestClient;
import com.afp.iris.sr.wm.clients.dto.ResizingResults;
import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.domain.AmpService;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import com.afp.iris.sr.wm.domain.exception.PosterNotFoundException;
import com.afp.iris.sr.wm.helper.G2Helper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import static org.jsoup.helper.StringUtil.isBlank;

@Service
@Slf4j
public class AmpServiceImpl implements AmpService {
	private final ScomRestClient scomRestClient;

	public AmpServiceImpl(ScomRestClient scomRestClient) {
		this.scomRestClient = scomRestClient;
	}

	@Override
	public G2WebStoryDocument uploadAmpContent(MultipartFile ampContent) {
		log.debug("START AmpService-uploadAmpContent");
		final ByteArrayInputStream ampTempFileInputStream;

		try {
			ampTempFileInputStream = new ByteArrayInputStream(ampContent.getBytes());
		} catch (IOException e) {
			throw new InternalTechnicalException(e, "Unable to access AMP content from request: %s", e.getMessage());
		}

		G2WebStoryDocument g2WebStoryDocument = createG2WebStoryDocumentWithAmp(ampTempFileInputStream);

		ampTempFileInputStream.reset();
		log.debug("END AmpService-uploadAmpContent");
		return g2WebStoryDocument;
	}

	private G2WebStoryDocument createG2WebStoryDocumentWithAmp(InputStream ampContentInputStream) {
		G2WebStoryDocument webStory = G2ObjectFactory.createWebStoryDocument();
		webStory.setGUID(GUIDUtil.makeLibg2GUID());
		webStory.setTransmissionDate(Misc.newXMLGregorianCalendarNow());

		Path tempZipFile = createTempZipFile(ampContentInputStream);
		List<String> filesToDelete = null;

		try {

			try (ZipFile ampZipFile = new ZipFile(tempZipFile.toFile())) {

				final ZipEntry htmlZipEntry = ampZipFile.stream()
						                                .filter(zipEntry -> zipEntry.getName().endsWith("index.amp.html") || zipEntry.getName().endsWith("story.html"))
						                                .findFirst()
						                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cannot find the AMP HTML content in the ZIP file"));

				filesToDelete = ampZipFile.stream()
						                      .filter(zipEntry -> zipEntry.getName().endsWith("index.html") || zipEntry.getName().endsWith("story.json") || zipEntry.getName().contains("public"))
						                      .map(zipEntry -> zipEntry.getName())
						                      .collect(Collectors.toList());


				fillG2WebStoryDocumentWithAmpHtml(webStory, ampZipFile, htmlZipEntry,tempZipFile);

			} catch (ZipException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded AMP content is not a valid zip", e);
			} catch (IOException e) {
				throw new InternalTechnicalException(e, "Error while processing uploaded AMP content : %s",
						e.getMessage());
			}

			deleteFilesFromAmpZipFile(filesToDelete, tempZipFile);

			try (FileInputStream fileInputStream = new FileInputStream(tempZipFile.toString())) {
				URI ampZipUri = uploadFileToScom(fileInputStream);
				webStory.getRenditions().add(G2Helper.buildWebStoryRenditionInfo(ampZipUri));
			} catch (FileNotFoundException e) {
				throw new InternalTechnicalException(e, "Error while preparing to upload zip file to SCOM : %s",
						e.getMessage());

			} catch (IOException e) {
				throw new InternalTechnicalException(e, "Error while preparing to upload zip file to SCOM : %s",
						e.getMessage());
			}

		} finally {
			FileUtils.deleteQuietly(tempZipFile.toFile());
		}
		return webStory;
	}

	private void fillG2WebStoryDocumentWithAmpHtml(G2WebStoryDocument webStory, ZipFile ampZipFile,
			ZipEntry htmlZipEntry,Path tempZipFile ) {
		Path ampHtmlPath = createTempFile("story", ".html");
		try (FileOutputStream fileOutputStream = new FileOutputStream(ampHtmlPath.toFile())) {

			ampZipFile.getInputStream(htmlZipEntry).transferTo(fileOutputStream);

			Document document = Jsoup.parse(ampHtmlPath.toFile(), StandardCharsets.UTF_8.name());
			Element element = document.select("amp-story").first();
			Element elementLang = document.select("html").first();
			URI ampPosterURI = extractPosterAndUploadToScom(ampZipFile, element);

			PictureRenditionInfo thumbnail = generateThumbnailFromScom(ampPosterURI);
			webStory.getIcons().add(thumbnail);

			webStory.getIcons().add(G2Helper.buildIcon(ampPosterURI, RenditionType.PREVIEW));

			webStory.setTitle(element.attr("title"));
			if (!elementLang.attr("lang").isEmpty()) {
				webStory.setMetadataLanguage(elementLang.attr("lang"));
				webStory.setContentLanguage(elementLang.attr("lang"));
			}
			ampZipFile.close();
			updateAmpHtmlFileAndCopyItIntoZipFile(ampHtmlPath, document, tempZipFile, htmlZipEntry.getName() );
		} catch (IOException e) {
			throw new InternalTechnicalException("Error while fetching poster", e.getMessage());
		} finally {
			FileUtils.deleteQuietly(ampHtmlPath.toFile());
		}
	}

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

	private void deleteFilesFromAmpZipFile(List<String> filesToDelete, Path tempZipFile) {
		Map<String, String> zipProperties = new HashMap<String, String>();
		zipProperties.put("create", "false");
		try (FileSystem zipfs = FileSystems.newFileSystem(tempZipFile, zipProperties)) {
			for (int i = filesToDelete.size() - 1; i >= 0; i--) {
				Path pathInZipfile = zipfs.getPath(filesToDelete.get(i));
				Files.delete(pathInZipfile);
			}

		} catch (IOException e) {
			throw new InternalTechnicalException("Error while deleting files from Amp zip : %s", e.getMessage());
		}
	}

	private URI extractPosterAndUploadToScom(ZipFile ampZipFile, Element element) throws IOException {
		String posterLocationInZip = element.attr("poster-portrait-src");
		ensurePosterLocationIsValid(posterLocationInZip);

		final ZipEntry posterZipEntry = ampZipFile.stream()
				.filter(zipEntry -> zipEntry.getName().endsWith(posterLocationInZip)).findFirst()
				.orElseThrow(() -> new PosterNotFoundException("Poster not found in AMP zip file : %s",
						posterLocationInZip));

		URI ampPosterURI = uploadFileToScom(ampZipFile.getInputStream(posterZipEntry));

		return ampPosterURI;
	}

	private void ensurePosterLocationIsValid(String posterLocationInZip) {
		if (isBlank(posterLocationInZip)) {
			throw new PosterNotFoundException("Poster location is missing in AMP");
		}

		try {
			new URI(posterLocationInZip);
		} catch (URISyntaxException e) {
			throw new PosterNotFoundException("Poster location is invalid in AMP : %s", posterLocationInZip);
		}
	}

	private Path createTempZipFile(InputStream ampContentInputStream) {
		Path tempZipFile;
		FileOutputStream fileOutputStream = null;
		try {
			tempZipFile = Files.createTempFile("zip", ".zip");
			fileOutputStream = new FileOutputStream(tempZipFile.toFile());
			ampContentInputStream.transferTo(fileOutputStream);
		} catch (IOException e) {
			throw new InternalTechnicalException(e, "Error while initializing temporary zip file : %s", e.getMessage());
		} finally {
			try {
				fileOutputStream.close();
				ampContentInputStream.close();
			} catch (IOException e) {
				throw new InternalTechnicalException(e, "Error while creating temporary zip file : %s", e.getMessage());
			}
		}

		return tempZipFile;
	}
	
	protected void updateAmpHtmlFileAndCopyItIntoZipFile(Path ampHtmlPath, Document document, Path tempZipFile,String htmlFileName ) {

		document.getAllElements().forEach(element -> {

			if ("meta".equals(element.tagName()) && (element.attr("name").equals("og:site_name") || element.attr("name").equals("og:url"))) {
				element.remove();
			}
			if ("title".equals(element.tagName())) {
				element.text(formatTitleContentFromAmpHtml(element.text()));
			}
			
			if ("link".equals(element.tagName()) && element.attr("rel").equals("canonical")) {
				element.attr("href", "");
			}
			
			if ("script".equals(element.tagName()) && element.attr("type").equals("application/ld+json")) {

				if (checkIfScriptToremoveIsTypeArticle(element)) {

					element.remove();
				}

			}

		});

		try (FileWriter writer = new FileWriter(ampHtmlPath.toFile())) {
			writer.write(document.toString());
			writer.flush();
		} catch (IOException e) {
			throw new InternalTechnicalException("Error while updating amp html : %s", e.getMessage());
		}

		Map<String, String> zipProperties = new HashMap<String, String>();
		zipProperties.put("create", "true");
		try (FileSystem zipfs = FileSystems.newFileSystem(tempZipFile, zipProperties)) {
			Path source = ampHtmlPath;
			Path target = zipfs.getPath(htmlFileName);
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		} catch (IOException e) {
			throw new InternalTechnicalException("Error while copying updated amp html : %s", e.getMessage());
		}
	}

	private URI uploadFileToScom(InputStream inputStream) {
		final ResponseEntity<String> response;
		try {
			response = this.scomRestClient.uploadFileToScom(inputStream);

		} catch (IOException e) {
			log.error("Unable to upload file to SCOM.", e);
			throw new InternalTechnicalException(e, "Error while uploading file to SCOM : %s", e.getMessage());
		} catch (RestClientResponseException e) {
			throw new InternalTechnicalException(e, "Unable to send file to scom. Reason : status code=%s message=%s",
					e.getRawStatusCode(), e.getResponseBodyAsString());
		}

		if (response == null || response.getStatusCode() != HttpStatus.CREATED) {
			log.error("Error while uploading binary to SCOM, response is:\n {}", response);
			throw new InternalTechnicalException("Error while uploading binary to SCOM");
		}

		final URI uploadedComponentUri = response.getHeaders().getLocation();
		if (uploadedComponentUri == null) {
			log.error("Error while uploading binary to SCOM, response is:\n {}", response);
			throw new InternalTechnicalException(
					"Error while uploading binary to SCOM, cannot find updated resource location in headers");
		}

		return uploadedComponentUri;
	}
	
	private boolean checkIfScriptToremoveIsTypeArticle(Element element) {

		boolean isTypeArticle = false;
		try {
			JSONObject jsonObj = new JSONObject(element.data());
			if (jsonObj.has("@type") && !jsonObj.isNull("@type") && jsonObj.getString("@type").toUpperCase().equals("ARTICLE")) {
				isTypeArticle = true;
			}
		} catch (JSONException e) {
			throw new InternalTechnicalException(e.getMessage());
		}

		return isTypeArticle;
	}

	private Path createTempFile(String prefix, String sufix) {
		Path path = null;
		try {
			path = Files.createTempFile(prefix, sufix);
		} catch (IOException e) {
			throw new InternalTechnicalException("Error while creating temporary file : %s", e.getMessage());
		}
		return path;
	}
	
	private String formatTitleContentFromAmpHtml(String title) {

		while (title.startsWith(" ") || title.startsWith("-")) {

			title = title.substring(1);

		}
		while (title.endsWith(" ") || title.endsWith("-")) {

			title = title.substring(0,title.length()-1);

		}

		return title;

	}

}
