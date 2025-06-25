package org.molgenis.emx2.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PodiumServiceTest {
  private HttpClient mockHttpClient;
  private HttpResponse mockResponse;
  private PodiumService podiumService;

  @BeforeEach
  public void setup() {
    mockHttpClient = mock(HttpClient.class);
    mockResponse = mock(HttpResponse.class);
    podiumService = new PodiumService(mockHttpClient);
  }

  @Test
  public void testGetResponseReturnsExpectedResponse() throws Exception {
    String jsonRequest =
        """
        {
          "podiumUrl": "https://example.com/api",
          "podiumUsername": "user",
          "podiumPassword": "pass",
          "payload": { "key": "value" }
        }
        """;

    Context context = mock(Context.class);
    when(context.body()).thenReturn(jsonRequest);

    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("success");

    HttpResponse<String> result = podiumService.getResponse(context);

    assertEquals(202, result.statusCode());
    assertEquals("success", result.body());
  }
}
