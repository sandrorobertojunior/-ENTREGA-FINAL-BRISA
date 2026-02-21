package org.obeci.platform.configs;

import org.obeci.platform.entities.Usuario;
import org.obeci.platform.repositories.UsuarioRepository;
import org.obeci.platform.services.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Profile("!test")
/**
 * Inicializador (bootstrap) de um usuário ADMIN padrão.
 *
 * <p>Executa na inicialização da aplicação (exceto profile test) para garantir que exista
 * um usuário administrador default, controlado por propriedades {@code app.admin.*}.</p>
 *
 * <p>Efeitos colaterais:
 * <ul>
 *   <li>Pode inserir um registro em {@code usuarios} (persistência).</li>
 *   <li>Pode gerar CPF alternativo para evitar colisões.</li>
 * </ul>
 * </p>
 *
 * <p>Observação: este mecanismo é conveniente para DEV, mas em produção recomenda-se
 * gerenciamento via processo controlado (secret manager + provisionamento).</p>
 */
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.admin.enabled:true}")
    private boolean enabled;

    @Value("${app.admin.email:admin@obeci.app}")
    private String adminEmail;

    @Value("${app.admin.username:Administrador}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.cpf:00000000000}")
    private String adminCpf;

    public AdminBootstrap(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    /**
     * Garante a existência de um admin default conforme propriedades.
     *
     * <p>Se {@code app.admin.enabled=false}, não faz nada. Se o email já existir, não recria.</p>
     */
    public void run(String... args) {
        if (!enabled) {
            log.debug("Admin bootstrap disabled by property app.admin.enabled=false");
            return;
        }

        try {
            if (usuarioService.findByEmail(adminEmail).isPresent()) {
                log.info("Admin user already exists: {}", adminEmail);
                return;
            }

            String cpfToUse = adminCpf;
            if (cpfToUse == null || cpfToUse.isBlank()) {
                cpfToUse = "00000000000";
            }
            int attempts = 0;
            while (usuarioRepository.existsByCpf(cpfToUse)) {
                // ensure uniqueness with a small suffix
                cpfToUse = adminCpf + String.format("%02d", (int) (Math.random() * 100));
                attempts++;
                if (attempts > 5) {
                    cpfToUse = adminCpf + System.currentTimeMillis() % 10000;
                    break;
                }
            }

            Usuario admin = new Usuario();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPassword(adminPassword);
            admin.setCpf(cpfToUse);
            admin.setArrayRoles(Arrays.asList("ADMIN"));

            usuarioService.register(admin);
            log.info("Default admin user created: {}", adminEmail);
        } catch (Exception e) {
            log.warn("Failed to ensure default admin user: {}", e.getMessage());
        }
    }
}
