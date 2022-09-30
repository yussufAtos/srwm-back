package com.afp.iris.sr.wm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest (webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WmApplicationTest {

	@Test
	void application_loaded() {
		Assertions.assertDoesNotThrow(() -> WmApplication.main(new String[] {}));
	}
}
