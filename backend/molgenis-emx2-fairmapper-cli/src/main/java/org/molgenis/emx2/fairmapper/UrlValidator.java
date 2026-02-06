package org.molgenis.emx2.fairmapper;

import java.net.URI;
import java.util.Set;

public final class UrlValidator {

  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

  private final String allowedDomain;
  private final boolean allowExternal;

  public UrlValidator(String sourceUrl) {
    this(sourceUrl, false);
  }

  public UrlValidator(String sourceUrl, boolean allowExternal) {
    this.allowedDomain = extractDomain(sourceUrl);
    this.allowExternal = allowExternal;
  }

  private static String extractDomain(String urlString) {
    if (urlString == null || urlString.isBlank()) {
      throw new FairMapperException("Source URL is required");
    }
    try {
      URI uri = URI.create(urlString);
      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        throw new FairMapperException("Source URL must have a host: " + urlString);
      }
      return host.toLowerCase();
    } catch (IllegalArgumentException e) {
      throw new FairMapperException("Invalid source URL: " + urlString);
    }
  }

  public void validate(String urlString) {
    URI uri;
    try {
      uri = URI.create(urlString);
    } catch (IllegalArgumentException e) {
      throw new FairMapperException("Invalid URL: " + urlString);
    }

    validateScheme(uri);
    if (!allowExternal) {
      validateDomain(uri);
    }
  }

  private void validateScheme(URI uri) {
    String scheme = uri.getScheme();
    if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
      throw new FairMapperException(
          "Invalid URL scheme: " + scheme + ". Allowed: " + ALLOWED_SCHEMES);
    }
  }

  private void validateDomain(URI uri) {
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new FairMapperException("URL must have a host: " + uri);
    }

    String requestDomain = host.toLowerCase();

    if (!isSameDomain(requestDomain)) {
      throw new FairMapperException(
          "External domain blocked: " + host + ". Only " + allowedDomain + " is allowed.");
    }
  }

  private boolean isSameDomain(String requestDomain) {
    if (requestDomain.equals(allowedDomain)) {
      return true;
    }
    if (requestDomain.endsWith("." + allowedDomain)) {
      int dotIndex = requestDomain.length() - allowedDomain.length() - 1;
      return dotIndex >= 0 && requestDomain.charAt(dotIndex) == '.';
    }
    return false;
  }

  public String getAllowedDomain() {
    return allowedDomain;
  }

  public boolean isAllowExternal() {
    return allowExternal;
  }
}
