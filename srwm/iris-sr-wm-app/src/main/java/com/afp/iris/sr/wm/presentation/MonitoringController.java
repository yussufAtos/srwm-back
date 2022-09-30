package com.afp.iris.sr.wm.presentation;

import com.afp.iris.sr.wm.config.monitoring.SoftConfigurations;
import com.afp.iris.sr.wm.config.swagger.PublicApi;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MonitoringController {

    private final SoftConfigurations softConfigurations;

    public MonitoringController(SoftConfigurations softConfigurations) {
        this.softConfigurations = softConfigurations;
    }

    @GetMapping(value = "/softsconfigurations")
    @PublicApi
    @ApiOperation(value = "getSoftConfigurations", notes = "Endpoint pour donner un descriptif sur la configuration de l'application..")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Le format de l'url n'est pas valide."),
            @ApiResponse(code = 500, message = "Erreur lors de la récupération des données de la configuration."),
            @ApiResponse(code = 200, message = "Donnés récupérées avec succès.", response = SoftConfigurations.class)})
    public ResponseEntity<Object> getSoftConfigurations () {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(softConfigurations);
    }
}
