package eu.neclab.ngsildbroker.infoserver.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/version")
public class VersionController {

	private final String RESULT = "{\r\n" + "    \"scorpio version\": \"1.0.0\"\r\n" + "}";

	@GetMapping
	public ResponseEntity<Object> getHealth(ServerHttpRequest request) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).header("Content-Type", "application/json").body(RESULT);
	}
}
