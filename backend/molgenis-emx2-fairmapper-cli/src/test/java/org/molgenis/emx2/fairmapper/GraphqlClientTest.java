package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class GraphqlClientTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testBaseUrlNormalization() {
    GraphqlClient client1 = new GraphqlClient("http://localhost:8080", null);
    GraphqlClient client2 = new GraphqlClient("http://localhost:8080/", null);
    // Both should work without error
    assertNotNull(client1);
    assertNotNull(client2);
  }

  @Test
  void testExecuteWithInvalidServer() {
    GraphqlClient client = new GraphqlClient("http://localhost:59999", null);
    JsonNode variables = objectMapper.createObjectNode();

    assertThrows(IOException.class, () -> client.execute("testSchema", "{ test }", variables));
  }

  @Test
  void testExecuteWithNullVariables() {
    GraphqlClient client = new GraphqlClient("http://localhost:59999", null);

    // Should not throw NPE when variables is null
    assertThrows(IOException.class, () -> client.execute("testSchema", "{ test }", null));
  }

  @Test
  void testConstructorWithToken() {
    GraphqlClient client = new GraphqlClient("http://localhost:8080", "test-token");
    assertNotNull(client);
  }
}
