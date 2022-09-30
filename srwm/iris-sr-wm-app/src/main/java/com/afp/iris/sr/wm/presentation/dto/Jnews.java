package com.afp.iris.sr.wm.presentation.dto;

import com.afp.iptc.g2.config.afp.LinkRel;
import com.afp.iptc.g2.config.afp.PublicationStatus;
import com.afp.iptc.g2.libg2api.*;
import com.afp.iptc.g2.libg2api.ChangeInfo.ChangeStatus;
import com.afp.iris.sr.wm.helper.G2Helper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.afp.iris.sr.wm.domain.exception.MediaTopicNotSupportedException;

import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.DEROctetString;

import java.net.URI;
import java.util.*;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Jnews {
	URI guid;
	String metadataLanguage;
	String contentLanguage;
	String edNote;
	String headline;
	String catchline;
	URI pubStatus;
	String itemClass;
	List<RenditionInfo> remoteContents = new ArrayList<>();
	List<RenditionInfo> icons = new ArrayList<>();
	List<Subject> subjects = new ArrayList<>();
	List<Signal> signals = new ArrayList<>();
	List<Link> links = new ArrayList<>();

	@Getter
	@Setter
	public static class RenditionInfo {
		URI rendition;
		URI href;
		String contentType;
		Long height;
		Long width;
		Long size;
	}

	@Getter
	@Setter
	public static class Subject {
		URI type;
		URI uri;
	}

	@Getter
	@Setter
	public static class Signal {
		URI uri;
	}

	@Getter
	@Setter
	public static class Link {
		URI rel;
		URI href;
	}

	public G2WebStoryDocument toG2WebStoryDocument() {
		G2WebStoryDocument webStory = G2ObjectFactory.createWebStoryDocument();

		fillG2DocumentWithJnews(webStory, this);

		return webStory;
	}

	private void fillG2DocumentWithJnews(G2Document document, Jnews jnews) {
		document.setGUID(jnews.getGuid());
		document.setLinkToSelf(findLinkToSefl(jnews));
		document.setMetadataLanguage(jnews.getMetadataLanguage());
		document.setContentLanguage(jnews.getContentLanguage());
		document.setDialogClient(jnews.getEdNote());
		document.setTitle(jnews.getHeadline());
		document.setCatchline(jnews.getCatchline());
		document.setPublicationStatus(parsePublicationStatusByURI(jnews.getPubStatus()));
		document.setTransmissionDate(Misc.newXMLGregorianCalendarNow());

		// Objectif : faire un cast au plus bas niveau pour mutualiser au maximum sur tous les documents
		if (document instanceof G2VisualDocument g2VisualDocument) {
			g2VisualDocument.getIcons().addAll(parseToG2PictureRenditionInfos(jnews.getIcons()));
		}

		if (document instanceof G2WebStoryDocument g2WebStoryDocument) {
			g2WebStoryDocument.getRenditions().addAll(parseToG2WebStoryRenditionInfos(jnews.getRemoteContents()));
		}

		document.getSubjects().addAll(parseToG2Subjects(jnews.subjects));
		G2Helper.addMediatopicKeywords(document);

		document.getChanges().addAll(parseToG2ChangeInfo(jnews.getSignals()));
	}

	private URI findLinkToSefl(Jnews jnews) {
		Link selfLink = jnews.getLinks().stream().filter(link -> LinkRel.SELF.toConceptURI().equals(link.getRel()))
				.findFirst()
				.orElse(null);


		return selfLink == null ? null : selfLink.getHref();
	}

	private List<ChangeInfo> parseToG2ChangeInfo(List<Signal> signals) {

		List<ChangeInfo> changeInfos = new ArrayList<>();

		for (Signal signal : signals) {
			if (signal.getUri().equals(com.afp.iptc.g2.config.afp.Signal.UPDATE.toConceptURI())) {
				changeInfos.add(new ChangeInfo(ChangeStatus.UPDATE));
			}
			if (signal.getUri().equals(com.afp.iptc.g2.config.afp.Signal.CORRECTION.toConceptURI())) {
				changeInfos.add(new ChangeInfo(ChangeStatus.CORRECTION));
			}
		}

		return changeInfos;
	}

	private List<WebStoryRenditionInfo> parseToG2WebStoryRenditionInfos(List<RenditionInfo> renditionInfos) {

		List<WebStoryRenditionInfo> webStoryRenditionInfos = new ArrayList<>();

		for (RenditionInfo renditionInfo : renditionInfos) {
			WebStoryRenditionInfo webStoryRenditionInfo = new WebStoryRenditionInfo(null);
			webStoryRenditionInfo.setHref(renditionInfo.getHref());
			webStoryRenditionInfo.setContentType(renditionInfo.getContentType());

			webStoryRenditionInfos.add(webStoryRenditionInfo);
		}

		return webStoryRenditionInfos;
	}

	private List<PictureRenditionInfo> parseToG2PictureRenditionInfos(List<RenditionInfo> renditionInfos) {
		List<PictureRenditionInfo> g2PictureRenditionInfos = new ArrayList<>();

		for (RenditionInfo renditionInfo : renditionInfos) {
			PictureRenditionInfo pictureRenditionInfo = new PictureRenditionInfo(renditionInfo.getRendition());
			pictureRenditionInfo.setHref(renditionInfo.getHref());
			pictureRenditionInfo.setContentType(renditionInfo.getContentType());

			pictureRenditionInfo.setHeight(renditionInfo.getHeight());
			pictureRenditionInfo.setWidth(renditionInfo.getWidth());
			pictureRenditionInfo.setSize(renditionInfo.getSize());

			g2PictureRenditionInfos.add(pictureRenditionInfo);
		}

		return g2PictureRenditionInfos;
	}

	private List<com.afp.iptc.g2.libg2api.Subject> parseToG2Subjects(List<Subject> subjects) {

		List<com.afp.iptc.g2.libg2api.Subject> g2Subjects = new ArrayList<>();

		for (Subject subject : subjects) {
			com.afp.iptc.g2.libg2api.Subject g2Subject = new com.afp.iptc.g2.libg2api.Subject(subject.getUri());
			g2Subject.setType(subject.getType());

			g2Subjects.add(g2Subject);
		}

		return g2Subjects;
	}

	private URI parsePublicationStatusByURI(URI pubStatusURI) {
		final PublicationStatus publicationStatus = Arrays.stream(PublicationStatus.values())
				.filter(streamPublicationStatus -> streamPublicationStatus.toConceptURI().equals(pubStatusURI))
				.findFirst().orElseThrow();

		return publicationStatus.toConceptURI();
	}
}
