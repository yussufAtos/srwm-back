package com.afp.iris.sr.wm.presentation;

import com.afp.iptc.g2.libg2api.BadNewsMLG2Exception;
import com.afp.iptc.g2.libg2api.G2Document;
import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.config.swagger.PrivateApi;
import com.afp.iris.sr.wm.domain.DocumentService;
import com.afp.iris.sr.wm.domain.exception.InternalTechnicalException;
import com.afp.iris.sr.wm.presentation.dto.Jnews;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents/")
@Slf4j
@Api(value = "DocumentController")
public class DocumentController extends AbstractController {
	private final DocumentService documentService;

	public DocumentController(AppProperties properties, DocumentService documentService) {
		super(properties);
		this.documentService = documentService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@PrivateApi
	@ApiOperation(value = "Créer un document validé dans le SR", notes = "Endpoint pour créer un document validé dans le SR")
	@ApiImplicitParam(name = "X-AFP-TRANSACTION-ID", value = "L'Id de la transaction (Quasi-Obligatoire!)", paramType = "header")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Erreur serveur lors de la création du document."),
			@ApiResponse(code = 400, message = "Le format du fichier n'est pas supporté."),
			@ApiResponse(code = 401, message = "L'utilisateur n'est plus authentifié."),
            @ApiResponse(code = 201, message = "Document validé avec succès dans le SR.", response = Jnews.class) })
	public ResponseEntity<String> validateNewDocument(@RequestBody Jnews jnews) {
		log.info("START validateNewDocument");

		removeHttpsProxyFrom(jnews);
		final G2Document document = documentService.validateNewDocument(jnews);

		String jnewsForResponse = getJnewsForResponse(document);

		final ResponseEntity<String> response = ResponseEntity.created(buildSrwmDocumentsEndpoint(document)).body(jnewsForResponse);

		log.info("END validateNewDocument");
		return response;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@PrivateApi
	@ApiOperation(value = "Retourne la dernière version validée d'un document dans le SR trouvé par son GUID", notes = "Endpoint pour récupérer la dernière version validée d'un document dans le SR")
	@ApiImplicitParam(name = "X-AFP-TRANSACTION-ID", value = "L'Id de la transaction (Quasi-Obligatoire!)", paramType = "header")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Erreur serveur lors de la récupération du document."),
			@ApiResponse(code = 200, message = "Document trouvé dans le SR.", response = Jnews.class),
            @ApiResponse(code = 401, message = "L'utilisateur n'est plus authentifié.")})
	public ResponseEntity<String> getDocumentByGuid(@RequestParam String guid) {
		log.info("START getDocumentByGuid {}", guid);
		
		final G2Document document = documentService.getDocumentByGuid(guid);

		String jnewsForResponse = getJnewsForResponse(document);

		final ResponseEntity<String> response = ResponseEntity.ok(jnewsForResponse);

		log.info("END getDocumentByGuid {}", guid);
		return response;
	}

	@GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PrivateApi
	@ApiOperation(value = "Retourne le document identifié par son id", notes = "Endpoint pour récupérer un document dans le SR par son id")
	@ApiImplicitParam(name = "X-AFP-TRANSACTION-ID", value = "L'Id de la transaction (Quasi-Obligatoire!)", paramType = "header")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Erreur serveur lors de la récupération du document."),
			@ApiResponse(code = 200, message = "Document retourné.", response = Jnews.class),
			@ApiResponse(code = 401, message = "L'utilisateur n'est plus authentifié.")})
	public ResponseEntity<String> getDocumentById(@PathVariable String id) {
		log.info("START getDocumentById {}", id);

		final G2Document document = documentService.getDocumentById(id);

		String jnewsForResponse = getJnewsForResponse(document);

		final ResponseEntity<String> response = ResponseEntity.ok(jnewsForResponse);

		log.info("END getDocumentById {}", id);
		return response;
	}

	@PostMapping(params = "guid",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@PrivateApi
	@ApiOperation(value = "Validation du Mise à jour d'un document", notes = "Endpoint pour valider la mise à jour d'un document dans le SR")
	@ApiResponses(value = {
			@ApiResponse(code = 500, message = "Erreur serveur lors de la validation du mise à jour du document."),
			@ApiResponse(code = 201, message = "Document Mis à jour dans le SR.", response = Jnews.class),
            @ApiResponse(code = 401, message = "L'utilisateur n'est plus authentifié.") })
	public ResponseEntity<String> validateUpdateDocument(@RequestBody Jnews jnews) {
		log.info("START validateUpdateDocument");

		removeHttpsProxyFrom(jnews);
		final G2Document document = documentService.validateUpdateDocument(jnews);

		String jnewsForResponse = getJnewsForResponse(document);

		final ResponseEntity<String> response = ResponseEntity.ok(jnewsForResponse);

		log.info("END validateUpdateDocument (GUID = {})", document.getGUID());
		return response;
	}

	@PostMapping(value = "/depublisher", produces = MediaType.APPLICATION_JSON_VALUE)
	@PrivateApi
	@ApiOperation(value = "Depublication du Web Story", notes = "Endpoint pour depublier un web story")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Erreur serveur lors de la depublication."),
			@ApiResponse(code = 201, message = "Document depublié.", response = Jnews.class) })
	public ResponseEntity<String> depublishWebStory(@RequestParam String guid) {
		log.info("START depublishWebStory {} ",guid);

		final G2Document document = documentService.depublishDocument(guid);

		String jnewsForResponse = getJnewsForResponse(document);

		final ResponseEntity<String> response = ResponseEntity.ok(jnewsForResponse);

		log.info("END depublishWebStory {} ",guid);
		return response;
	}

}
