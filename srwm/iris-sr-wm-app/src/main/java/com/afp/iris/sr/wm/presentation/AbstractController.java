package com.afp.iris.sr.wm.presentation;

import com.afp.iptc.g2.libg2api.BadNewsMLG2Exception;
import com.afp.iptc.g2.libg2api.G2Document;
import com.afp.iptc.g2.libg2api.G2VisualDocument;
import com.afp.iptc.g2.libg2api.RenditionInfo;
import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import com.afp.iris.sr.wm.presentation.dto.Jnews;
import com.afp.iris.sr.wm.utils.UriUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
public abstract class AbstractController {
    private final AppProperties properties;

    public AbstractController(AppProperties properties) {
        this.properties = properties;
    }

    protected String getJnewsForResponse(G2Document document) {
        String jnewsForResponse;

        try {
            proxifyDocumentForJnewsRepresentation(document);
            jnewsForResponse = document.jnewsRepresentation();

        } catch (BadNewsMLG2Exception e) {
            log.error("Unable to get a Jnews representation of the G2Document.", e);
            throw new InternalTechnicalException(e, "Error while representing G2Document as Jnews: %s", e.getMessage());
        }

        return jnewsForResponse;
    }

    protected URI buildSrwmDocumentsEndpoint(G2Document document) {
        return UriComponentsBuilder.fromUriString(properties.getDocumentsEndpoint())
                .queryParam("guid", document.getGUID())
                .build()
                .toUri();
    }

    protected void removeHttpsProxyFrom(Jnews jnews) {
        removeCmsLinksProxy(jnews);
        removeScomComponentsHttpsProxy(jnews);
    }

    private void proxifyDocumentForJnewsRepresentation(G2Document document) {
        httpsProxifyCmsLinks(document);
        httpsProxifyScomComponent(document);
    }

    private void removeCmsLinksProxy(Jnews jnews) {
        jnews.getLinks().forEach(this::removeCmsLinkHttpsProxy);
    }

    private void removeCmsLinkHttpsProxy(Jnews.Link link) {
        String documentsEndpoint = properties.getCms().getDocumentsEndpoint();
        if (!documentsEndpoint.endsWith("/")) {
            documentsEndpoint += "/";
        }

        String id = UriUtils.extractIdFromUri(link.getHref().toString());
        URI deproxifiedUri = URI.create(documentsEndpoint).resolve(id);
        link.setHref(deproxifiedUri);
    }

    private void httpsProxifyCmsLinks(G2Document document) {
        if (document.getLinkToSelf() != null) {
            String id = UriUtils.extractIdFromUri(document.getLinkToSelf().toString());
            URI proxifiedUri = URI.create(properties.getDocumentsEndpoint()).resolve(id);
            document.setLinkToSelf(proxifiedUri);
        }
    }

    private void httpsProxifyScomComponent(G2Document document) {

        if (document instanceof G2VisualDocument g2VisualDocument) {
            g2VisualDocument.getIcons().forEach(this::httpsProxifyScomComponent);
            g2VisualDocument.getRenditions().forEach(this::httpsProxifyScomComponent);
        }
    }

    private void removeScomComponentsHttpsProxy(Jnews jnews) {
        jnews.getIcons().forEach(this::removeScomComponentHttpsProxy);
        jnews.getRemoteContents().forEach(this::removeScomComponentHttpsProxy);
    }

    private void removeScomComponentHttpsProxy(Jnews.RenditionInfo renditionInfo) {
        URI deproxifiedUri = URI.create(properties.getScom().getComponentsEndpoint()).resolve(renditionInfo.getHref().getPath());
        renditionInfo.setHref(deproxifiedUri);
    }

    private void httpsProxifyScomComponent(RenditionInfo renditionInfo) {
        URI proxifiedUri = URI.create(properties.getSrwmComponentsEndpoint()).resolve(renditionInfo.getHref().getPath());
        renditionInfo.setHref(proxifiedUri);
    }

}
