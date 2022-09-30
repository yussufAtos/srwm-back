package com.afp.iris.sr.wm.helper;

import com.afp.iptc.g2.config.afp.RenditionType;
import com.afp.iptc.g2.libg2api.*;
import com.afp.iris.sr.wm.clients.dto.ResizingResults;
import com.afp.iris.sr.wm.domain.exception.MediaTopicNotSupportedException;
import com.afp.iris.sr.wm.presentation.dto.MediaTopic;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Optional;

public class G2Helper {

    public static final URI MEDIATOPIC_URI = URI.create("http://cv.iptc.org/newscodes/mediatopic/");

    public static final URI THUMBNAIL_RENDITION_URI = RenditionType.THUMBNAIL.toConceptURI();

    private G2Helper() {
        throw new IllegalStateException("Utility class");
    }

    public static PictureRenditionInfo buildIcon(URI iconUri, RenditionType renditionType) {
        PictureRenditionInfo icon = new PictureRenditionInfo(renditionType.toConceptURI());
        icon.setHref(iconUri);
        icon.setContentType(MediaType.IMAGE_JPEG_VALUE);

        return icon;
    }

    public static G2WebStoryDocument updateWebStoryDocument(G2WebStoryDocument webStoryToUpdate, G2WebStoryDocument newWebStory) {
        // Clear old information before update
        webStoryToUpdate.getRenditions().clear();
        webStoryToUpdate.getIcons().clear();
        webStoryToUpdate.getSubjects().clear();
        webStoryToUpdate.getKeywords().clear();
        webStoryToUpdate.getChanges().clear();

        webStoryToUpdate.setGUID(newWebStory.getGuid());
        webStoryToUpdate.setMetadataLanguage(newWebStory.getMetadataLanguage());
        webStoryToUpdate.setContentLanguage(newWebStory.getContentLanguage());
        webStoryToUpdate.setDialogClient(newWebStory.getDialogClient());
        webStoryToUpdate.setTitle(newWebStory.getTitle());
        webStoryToUpdate.setCatchline(newWebStory.getCatchline());
        webStoryToUpdate.setPublicationStatus(newWebStory.getPublicationStatus());
        webStoryToUpdate.setTransmissionDate(Misc.newXMLGregorianCalendarNow());

        webStoryToUpdate.getIcons().addAll(newWebStory.getIcons());

        webStoryToUpdate.getRenditions().addAll(newWebStory.getRenditions());

        webStoryToUpdate.getSubjects().addAll(newWebStory.getSubjects());

        addMediatopicKeywords(webStoryToUpdate);

        webStoryToUpdate.getChanges().addAll(newWebStory.getChanges());

        return webStoryToUpdate;
    }

    public static void addMediatopicKeywords(G2Document document) {
        for (Subject subject : document.getSubjects()) {
            boolean isMediatopic = Misc.inScheme(subject.getURI(), MEDIATOPIC_URI);
            if (isMediatopic) {
                addMediatopicKeywords(document, subject);
            }
        }
    }

    public static void addMediatopicKeywords(G2Document document, Subject subject) {
        Optional<MediaTopic> mediaTopic = MediaTopic.getSamples().stream()
                .filter(topic -> topic.getUri().equals(subject.getURI())).findFirst();

        if (mediaTopic.isPresent()) {
            if (document.getMetadataLanguage().equals("en") && mediaTopic.get().getEnKeywords() != null) {
                for (Keyword keyword : mediaTopic.get().getEnKeywords()) {
                    document.getKeywords().add(keyword);
                }
            }

            if (document.getMetadataLanguage().equals("fr") && mediaTopic.get().getFrKeywords() != null) {
                for (Keyword keyword : mediaTopic.get().getFrKeywords()) {
                    document.getKeywords().add(keyword);
                }
            }

        } else {
            throw new MediaTopicNotSupportedException("Media topic is not supported");
        }
    }

    public static WebStoryRenditionInfo buildWebStoryRenditionInfo(URI renditionUri) {
        WebStoryRenditionInfo webStoryRenditionInfo = new WebStoryRenditionInfo(null);
        webStoryRenditionInfo.setHref(renditionUri);
        webStoryRenditionInfo.setContentType("application/zip");

        return webStoryRenditionInfo;
    }

    public static PictureRenditionInfo buildThumbnail(ResizingResults.Result result) {
        PictureRenditionInfo thumbnail = new PictureRenditionInfo(THUMBNAIL_RENDITION_URI);
        thumbnail.setContentType(MediaType.IMAGE_JPEG_VALUE);

        thumbnail.setHref(result.getResultUrl());
        thumbnail.setHeight(result.getHeight());
        thumbnail.setWidth(result.getWidth());
        thumbnail.setSize(result.getLength());

        return thumbnail;
    }


    public static PictureRenditionInfo getPreviewIcon(G2VisualDocument document) {
        return getPictureRenditionInfoByRenditionType(document, RenditionType.PREVIEW);
    }

    public static PictureRenditionInfo getThumbnailIcon(G2VisualDocument document) {
        return getPictureRenditionInfoByRenditionType(document, RenditionType.THUMBNAIL);
    }

    public static PictureRenditionInfo getPictureRenditionInfoByRenditionType(G2VisualDocument document, RenditionType renditionType) {

        Optional<PictureRenditionInfo> optionalPictureRenditionInfo = document.getIcons().stream()
                .filter(pri -> pri.getRenditionType().equals(renditionType.toConceptURI()))
                .findFirst();

        return optionalPictureRenditionInfo.orElse(null);
    }

    public static void replaceRendition(G2VisualDocument document, PictureRenditionInfo pictureRenditionInfo) {
        removeRendition(document, pictureRenditionInfo.getRenditionType());
        document.getIcons().add(pictureRenditionInfo);
    }

    public static void removeRendition(G2VisualDocument document, URI renditionTypeUri) {
        document.getIcons().removeIf(pri -> renditionTypeUri.equals(pri.getRenditionType()));
    }
}
