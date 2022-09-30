package com.afp.iris.sr.wm.domain;

import com.afp.iptc.g2.libg2api.G2Document;
import com.afp.iris.sr.wm.presentation.dto.Jnews;

import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

public interface DocumentService {

    G2Document createDocument(G2Document document);

    G2Document validateDocument(G2Document document);

    Optional<G2Document> editDocument(G2Document document);

    G2Document validateNewDocument(Jnews jnews);

    G2Document getDocumentByGuid(String guid);

    G2Document validateUpdateDocument(Jnews jnews);

    G2Document depublishDocument(String guid);

    Cookie loginToBackend(HttpSession session);

    G2Document getDocumentById(String id);
}
