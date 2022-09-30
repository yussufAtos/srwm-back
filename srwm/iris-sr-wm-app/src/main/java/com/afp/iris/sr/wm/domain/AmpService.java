package com.afp.iris.sr.wm.domain;

import com.afp.iptc.g2.libg2api.G2WebStoryDocument;
import org.springframework.web.multipart.MultipartFile;

public interface AmpService {

	G2WebStoryDocument uploadAmpContent(MultipartFile zipFile);

}
