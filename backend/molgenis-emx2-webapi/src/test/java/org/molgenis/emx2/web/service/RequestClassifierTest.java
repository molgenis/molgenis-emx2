package org.molgenis.emx2.web.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.javalin.http.Context;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class RequestClassifierTest {

  private final Context ctx = mock(Context.class);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ChatGPT-User",
        "MistralAI-User",
        "Gemini-Deep-Research",
        "Claude-User",
        "Perplexity-User",
        "Google-NotebookLM",
        "PREFIXChatGPT-UserSUFFIX"
      })
  void shouldClassify(String userAgent) {
    prepareMock(userAgent);
    assertTrue(RequestClassifier.isAiChatBotRequest(ctx));
  }

  private static Stream<Arguments> falseArgs() {
    return Stream.of(
        Arguments.of(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:156.0) Gecko/20100101 Firefox/156.0"),
        Arguments.of("chatgpt-user"),
        Arguments.of("CHATGPT-USER"),
        Arguments.of("MistralAI--User"),
        null);
  }

  @ParameterizedTest
  @MethodSource("falseArgs")
  void shouldNotClassify(String userAgent) {
    prepareMock(userAgent);
    assertFalse(RequestClassifier.isAiChatBotRequest(ctx));
  }

  private void prepareMock(String userAgent) {
    doReturn(userAgent).when(ctx).userAgent();
  }
}
