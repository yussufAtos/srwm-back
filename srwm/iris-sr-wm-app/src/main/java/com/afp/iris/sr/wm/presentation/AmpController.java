package com.afp.iris.sr.wm.presentation;

import com.afp.iptc.g2.libg2api.*;
import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.config.swagger.PrivateApi;
import com.afp.iris.sr.wm.domain.AmpService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@Api(value = "AmpController")
public class AmpController extends AbstractController {

	private final AmpService ampService;

	public AmpController(AppProperties properties, AmpService ampService) {
		super(properties);
		this.ampService = ampService;
	}

	@PostMapping(value = "/amps")
	@PrivateApi
	@ApiOperation(value = "Téléchargement et extraction de données depuis le zip de l'AMP.",
			notes = "Endpoint pour charger le zip d'une nouvelle web story.")
	@ApiResponses(value = {@ApiResponse(code = 500, message = "Erreur serveur lors de l'upload du zip de l'AMP."),
			@ApiResponse(code = 400, message = "Le zip de l'AMP chargé contient des erreurs."),
			@ApiResponse(code = 200, message = "Le zip de l'AMP est chargé avec succès.")})
	public ResponseEntity<String> uploadAmpContent(@RequestParam MultipartFile webStoryZip) {
		log.debug("START uploadAmpContent");
		G2WebStoryDocument webStory = ampService.uploadAmpContent(webStoryZip);

		final String jnewsForResponse = getJnewsForResponse(webStory);

		log.debug("END uploadAmpContent");
		return ResponseEntity.status(HttpStatus.OK).body(jnewsForResponse);
	}

}
