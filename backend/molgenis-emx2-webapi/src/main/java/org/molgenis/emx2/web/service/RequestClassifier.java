package org.molgenis.emx2.web.service;

import io.javalin.http.Context;
import java.util.List;

public class RequestClassifier {

  private RequestClassifier() {
    // Utility class
  }

  /**
   * Matomo supports the following <a
   * href="https://matomo.org/faq/how-to/install-ai-chatbot-tracking/">list</a> of AI chatbots. It
   * can detect these by check whether the User-Agent header contains one of these.
   */
  static final List<String> AI_CHATBOT_UA =
      List.of(
          "ChatGPT-User",
          "MistralAI-User",
          "Gemini-Deep-Research",
          "Claude-User",
          "Perplexity-User",
          "Google-NotebookLM");

  public static boolean isAiChatBotRequest(Context ctx) {
    String userAgent = ctx.userAgent();
    if (userAgent == null) {
      return false;
    }

    return AI_CHATBOT_UA.stream().anyMatch(userAgent::contains);
  }
}
