package com.afp.iris.sr.wm.presentation;

import com.afp.iris.sr.wm.config.swagger.PublicApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.PingHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("status")
@Api(value = "StatusController")
public class StatusController {

    final PingHealthIndicator pingHealthIndicator;
    final DiskSpaceHealthIndicator diskHealthIndicator;

    public StatusController(
            PingHealthIndicator pingHealthIndicator,
            DiskSpaceHealthIndicator diskHealthIndicator
    ) {
        this.pingHealthIndicator = pingHealthIndicator;
        this.diskHealthIndicator = diskHealthIndicator;
    }

    @GetMapping
    @PublicApi
    @ApiOperation(value = "getStatus", notes = "Endpoint pour vérifier l'état de l'application.")
    @ApiResponses(value = {
            @ApiResponse(code = 503, message = "Retourne 'KO', lorsque l'application rencontre un problème.", response = String.class),
            @ApiResponse(code = 200, message = "Retourne 'OK', lorsque l'état de l'application est OK.", response = String.class)})
    public ResponseEntity<String> getStatus() {

        final Health pingHealth = pingHealthIndicator.getHealth(true);
        final Health diskHealth = diskHealthIndicator.getHealth(true);

        if (Status.UP.getCode().equals(pingHealth.getStatus().getCode()) &&
                Status.UP.getCode().equals(diskHealth.getStatus().getCode())
        ) {

            return new ResponseEntity<>("OK", HttpStatus.OK);
        }

        return new ResponseEntity<>("KO", HttpStatus.OK);
    }

}
