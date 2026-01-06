package org.molgenis.emx2.web;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

@Tag("starter-kit")
public class StarterKitWebApiSmokeTest extends WebApiSmokeTests {

  @BeforeAll
  static void setup() {
    RestAssured.port = 8080;
    setAdminSession();
  }
}
