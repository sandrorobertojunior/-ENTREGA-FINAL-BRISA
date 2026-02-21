package org.obeci.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * Ponto de entrada da aplicação Spring Boot (back-end OBECI).
 *
 * <p>Responsável por iniciar o contexto Spring, fazer scan de componentes e subir a API.</p>
 */
public class PlatformApplication {

	public static void main(String[] args) {
		// Inicializa o Spring Boot.
		SpringApplication.run(PlatformApplication.class, args);
	}

}
