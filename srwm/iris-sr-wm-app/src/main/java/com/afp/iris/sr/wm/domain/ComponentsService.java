package com.afp.iris.sr.wm.domain;

import org.springframework.http.ResponseEntity;

public interface ComponentsService {
	ResponseEntity<byte[]> getComponent(String id);
}
