package org.molgenis.emx2.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

  @Test
  public void testUpdateContextWith202() {
    Context context = mock(Context.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    HttpHeaders mockHeader = mock(HttpHeaders.class);
    HashMap<String, List<String>> map = new HashMap<>();
    map.put("Location", Collections.singletonList("some location"));

    when(response.statusCode()).thenReturn(202);
    when(mockHeader.map()).thenReturn(map);
    when(response.headers()).thenReturn(mockHeader);

    PodiumService.updateContext(context, response);

    verify(context).status(201);
    verify(context).header("Location", "some location");
  }

  @Test
  public void testUpdateContextWithOtherStatus() {
    Context context = mock(Context.class);
    HttpResponse<String> response = mock(HttpResponse.class);

    when(response.statusCode()).thenReturn(500);

    PodiumService.updateContext(context, response);

    verify(context).status(500);
  }
}
