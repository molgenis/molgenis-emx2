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
              (MediaType mediaType) -> mediaType.withParameter("q", "0").parameters().size())
          .thenComparing(mediaType -> !mediaType.type().equals("*"))
          .thenComparing(mediaType -> !mediaType.subtype().equals("*"));

  /**
   * Returns what media type to use as defined in <a
   * href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.3.2">rfc7231 section 5.3.2</a>.
   *
   * <p>Comparison is based on type, subtype & parameters except the quality value (f.e. in
   * "text/plain; format=flowed; q=0.5" only the "q=0.5" will be ignored).
   *
   * <p>Missing ACCEPT header will result in the first allowed media type to be returned.
   *
   * @param allowedMediaTypes Allowed types in order of priority (in case of equal q-values).
   * @return matching {@link MediaType} (if any q-value is present, this the q-value as present in
   *     {@code allowedMediaTypes} and NOT the {@link Context}!)
   * @throws NotAcceptableResponse if no valid match was found
   */
  public static MediaType getContentType(Context ctx, List<MediaType> allowedMediaTypes) {
    if (allowedMediaTypes.isEmpty()) {
      throw new IllegalArgumentException("Empty collection not allowed.");
    }

    String acceptHeader = ctx.header(Header.ACCEPT);
    if (acceptHeader == null) return allowedMediaTypes.getFirst();

    Map<MediaType, Double> allowedMediaTypeScores = new LinkedHashMap<>();
    allowedMediaTypes.forEach(i -> allowedMediaTypeScores.put(i, null));

    // Order ensures more specific media types overwrite less specific ones.
    List<MediaType> mediaTypes =
        Arrays.stream(acceptHeader.split(","))
            .map(String::trim)
            .map(MediaType::parse)
            .sorted(MEDIA_TYPE_COMPARATOR)
            .toList();

    // Assign a q-value to each allowedMediaTypes.
    for (MediaType mediaType : mediaTypes) {
      for (MediaType allowedMediaType : allowedMediaTypeScores.keySet()) {
        // Equalizes q-value for comparison.
        if (allowedMediaType.withParameter("q", "0").is(mediaType.withParameter("q", "0"))) {
          allowedMediaTypeScores.put(allowedMediaType, getQualityScore(mediaType));
        }
      }
    }

    return allowedMediaTypeScores.entrySet().stream()
        .filter(i -> i.getValue() != null)
        .sorted( // If score is equal, uses allowed media type order as tiebreaker.
            Map.Entry.<MediaType, Double>comparingByValue()
                .reversed()
                .thenComparing(entry -> allowedMediaTypes.indexOf(entry.getKey())))
        .findFirst()
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  public static Double getQualityScore(MediaType mediaType) {
    ImmutableList<@NonNull String> param = mediaType.parameters().get("q");
    if (param.isEmpty()) return 1.0;
    return Double.parseDouble(param.getFirst());
  }
}
