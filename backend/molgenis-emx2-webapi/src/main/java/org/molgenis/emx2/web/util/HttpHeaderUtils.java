package org.molgenis.emx2.web.util;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import io.javalin.http.Context;
import io.javalin.http.Header;
import java.util.*;
import org.jspecify.annotations.NonNull;
import org.molgenis.emx2.MolgenisException;

public class HttpHeaderUtils {
  private static final Comparator<MediaType> MEDIA_TYPE_COMPARATOR =
      Comparator.comparing((MediaType mediaType) -> !mediaType.type().equals("*"))
          .thenComparing(mediaType -> !mediaType.subtype().equals("*"));

  /**
   * Returns what media type to use as defined in <a
   * href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.3.2">rfc7231 section 5.3.2</a>.
   *
   * <p>IMPORTANT: Currently we do not process any additional ACCEPT parameters (f.e. {@code
   * text/plain; charset=utf-8}). Therefore, any media type that is more specific than type/subtype
   * is deemed unsupported. If only media types with parameters are given (excluding {@code q}),
   * this would result in no matches.
   *
   * <p>Returns:
   *
   * <ul>
   *   <li>The first {@code allowedMediaType} if no ACCEPT header is present.
   *   <li>{@link MediaType} with highest q-value (with allowedMediaTypes order as tiebraker) if one
   *       or more matches were found
   *   <li>{@code null} if no match was found.
   * </ul>
   *
   * @param allowedMediaTypes Allowed types in order of priority (in case of equal q-values).
   * @throws IllegalArgumentException if {@code allowedMediaTypes} is empty or an item in it has a
   *     non-empty {@link MediaType#parameters} (currently unsupported)
   */
  public static MediaType getContentType(Context ctx, List<MediaType> allowedMediaTypes) {
    if (allowedMediaTypes.isEmpty()) {
      throw new IllegalArgumentException("Empty collection (allowedMediaTypes) not allowed.");
    }
    if (allowedMediaTypes.stream()
        .anyMatch(
            mediaType ->
                mediaType.parameters().keySet().stream().anyMatch(key -> !key.equals("q")))) {
      throw new IllegalArgumentException(
          "allowedMediaTypes should not contain parameters! (currently not supported)");
    }

    String acceptHeader = ctx.header(Header.ACCEPT);
    if (acceptHeader == null) return allowedMediaTypes.getFirst();

    Map<MediaType, Double> allowedMediaTypeScores = new LinkedHashMap<>();
    allowedMediaTypes.forEach(i -> allowedMediaTypeScores.put(i, null));

    List<MediaType> mediaTypes =
        Arrays.stream(acceptHeader.split(","))
            .map(String::trim)
            .map(
                mediaTypeString -> {
                  try {
                    return MediaType.parse(mediaTypeString);
                  } catch (IllegalArgumentException e) {
                    throw new MolgenisException("Could not parse media type: " + mediaTypeString);
                  }
                })
            // Filters out any media types in header with non-q parameters (not supported).
            .filter(
                mediaType ->
                    mediaType.parameters().keySet().stream().allMatch(key -> key.equals("q")))
            // Order ensures more specific media types overwrite less specific ones.
            .sorted(MEDIA_TYPE_COMPARATOR)
            .toList();

    // Assign a q-value to each allowedMediaTypes.
    for (MediaType mediaType : mediaTypes) {
      for (MediaType allowedMediaType : allowedMediaTypeScores.keySet()) {
        if (allowedMediaType.is(mediaType.withoutParameters())) {
          allowedMediaTypeScores.put(allowedMediaType, getQualityScore(mediaType));
        }
      }
    }

    return allowedMediaTypeScores.entrySet().stream()
        .filter(i -> i.getValue() != null)
        .sorted( // If score is equal, uses allowedMediaTypes order as tiebreaker.
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
