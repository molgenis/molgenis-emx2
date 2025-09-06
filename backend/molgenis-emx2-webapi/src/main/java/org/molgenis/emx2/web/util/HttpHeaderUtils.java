package org.molgenis.emx2.web.util;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.NotAcceptableResponse;
import java.util.*;
import org.jspecify.annotations.NonNull;

public class HttpHeaderUtils {
  private static final Comparator<MediaType> MEDIA_TYPE_COMPARATOR =
      Comparator.comparing(
              (MediaType mediaType) -> mediaType.type().equals("*"), Comparator.reverseOrder())
          .thenComparing(mediaType -> mediaType.subtype().equals("*"), Comparator.reverseOrder());

  /**
   * @param allowedMediaTypes Allowed types in order of priority (in case of equal q-values).
   *     Missing ACCEPT header means the first allowed media type is used.
   * @return matching {@link MediaType} or {@code null} if no match was found.
   */
  public static MediaType getContentType(Context ctx, List<MediaType> allowedMediaTypes) {
    if (allowedMediaTypes.isEmpty()) {
      throw new IllegalArgumentException("Empty collection not allowed.");
    }

    String acceptHeader = ctx.header(Header.ACCEPT);
    if (acceptHeader == null) return allowedMediaTypes.getFirst();

    Map<MediaType, Double> allowedMediaTypeScores = new LinkedHashMap<>();
    allowedMediaTypes.forEach(i -> allowedMediaTypeScores.put(i.withoutParameters(), null));

    // Wildcards first so that they get overwritten by more specific ones.
    List<MediaType> mediaTypes =
        Arrays.stream(acceptHeader.split(","))
            .map(String::trim)
            .map(MediaType::parse)
            .sorted(MEDIA_TYPE_COMPARATOR)
            .toList();

    for (MediaType mediaType : mediaTypes) {
      for (MediaType allowedMediaType : allowedMediaTypeScores.keySet()) {
        if (allowedMediaType.is(mediaType.withoutParameters())) {
          allowedMediaTypeScores.put(allowedMediaType, getQualityScore(mediaType));
        }
      }
    }

    return allowedMediaTypeScores.entrySet().stream()
        .filter(i -> i.getValue() != null)
        .sorted(
            Map.Entry.<MediaType, Double>comparingByValue()
                .reversed()
                .thenComparing(entry -> allowedMediaTypes.indexOf(entry.getKey())))
        .findFirst()
        .map(Map.Entry::getKey)
        .orElseThrow(
            () ->
                new NotAcceptableResponse(
                    "Only the following accept-header values are supported: "
                        + allowedMediaTypes.stream().map(MediaType::toString).toList()));
  }

  public static Double getQualityScore(MediaType mediaType) {
    ImmutableList<@NonNull String> param = mediaType.parameters().get("q");
    if (param.isEmpty()) return 1.0;
    return Double.parseDouble(param.getFirst());
  }
}
