package org.obeci.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = PlatformApplication.class)
@ActiveProfiles("test")
class PlatformApplicationTests {

	@Test
	void contextLoads() {
	}

}
