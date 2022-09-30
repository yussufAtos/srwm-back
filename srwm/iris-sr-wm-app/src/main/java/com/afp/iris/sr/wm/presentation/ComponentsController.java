package com.afp.iris.sr.wm.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.afp.iris.sr.wm.config.swagger.PrivateApi;
import com.afp.iris.sr.wm.domain.ComponentsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@Api(value = "ComponentsController")
public class ComponentsController {
	private final ComponentsService componentsService;

	public ComponentsController(ComponentsService componentsService) {
		this.componentsService = componentsService;
	}

	@GetMapping(value = "/components/{id}", produces = { "image/jpeg", "application/zip" })
	@PrivateApi
	@ApiOperation(value = "Get component from SCOM", notes = "Endpoint pour recuperer un composant depuis le SCOM")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Erreur serveur lors de la recherche de ressource."),
			@ApiResponse(code = 404, message = "La ressource est non trouvée."), })
	public ResponseEntity<byte[]> getComponentFromScom(@PathVariable String id) {
		log.info("START getComponentFromScom {}", id);
		ResponseEntity<byte[]> response = componentsService.getComponent(id);
		log.info("END getComponentFromScom {}", id);
		return response;
	}
}
