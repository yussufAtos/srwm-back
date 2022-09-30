package com.afp.iris.sr.wm.presentation;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.config.swagger.PrivateApi;
import com.afp.iris.sr.wm.domain.DocumentService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;



@RestController
@RequestMapping("login")
@Api(value = "LoginController")
public class LoginController {

	
	private final DocumentService documentService;
	private final AppProperties properties;

    public LoginController(DocumentService documentService, AppProperties properties) {
        this.documentService = documentService;
        this.properties = properties;
    }
    
    @GetMapping
    @PrivateApi
	@ApiOperation(value = "Authentification",
			notes = "Endpoint pour authentification au SRWM et par proxy au CMS.")
    @ApiImplicitParam(value = "L'url de redirection après l'authentification. Le paramettre n'est pas obligatoire")
	@ApiResponses(value = {@ApiResponse(code = 500, message = "Erreur serveur lors de l'authentification."),
			@ApiResponse(code = 401, message = "Erreur lors de l'authentification du SRWM au CMS."),
			@ApiResponse(code = 302, message = "Redirection vers le CAS pour authentification."),
			@ApiResponse(code = 302, message = "Authentification avec succès. Redirection vers la page d'accueil.")})
    public ResponseEntity<String> login(HttpSession session, HttpServletResponse response, @RequestParam(value = "redirectUrl", required = false) String redirectUrl) {
    	// supprimer le cookie JSESSION du wm
    	response.reset();

        response.addCookie(documentService.loginToBackend(session));
        response.addHeader(HttpHeaders.LOCATION, (redirectUrl == null || redirectUrl.isEmpty()) ? properties.getBaseUri(): redirectUrl);
        return  new ResponseEntity<>(HttpStatus.FOUND);
        
    }

}
