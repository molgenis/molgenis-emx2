package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UrlValidatorTest {

  @Test
  void testSameDomainAllowed() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    assertDoesNotThrow(() -> validator.validate("https://fdp.example.org/dataset/123"));
  }

  @Test
  void testSameDomainDifferentPathAllowed() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    assertDoesNotThrow(() -> validator.validate("https://fdp.example.org/distribution/456"));
  }

  @Test
  void testSameDomainDifferentSchemeAllowed() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    assertDoesNotThrow(() -> validator.validate("http://fdp.example.org/dataset/123"));
  }

  @Test
  void testDifferentDomainBlockedByDefault() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> validator.validate("https://other.org/data"));
    assertTrue(ex.getMessage().contains("External domain blocked"));
    assertTrue(ex.getMessage().contains("fdp.example.org"));
  }

  @Test
  void testDifferentDomainAllowedWithFlag() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog", true);
    assertDoesNotThrow(() -> validator.validate("https://other.org/data"));
  }

  @Test
  void testSubdomainAllowed() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    assertDoesNotThrow(() -> validator.validate("https://api.fdp.example.org/data"));
  }

  @Test
  void testParentDomainBlocked() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    FairMapperException ex =
        assertThrows(
            FairMapperException.class, () -> validator.validate("https://example.org/data"));
    assertTrue(ex.getMessage().contains("External domain blocked"));
  }

  @Test
  void testInvalidSchemeBlocked() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> validator.validate("file:///etc/passwd"));
    assertTrue(ex.getMessage().contains("Invalid URL scheme"));
  }

  @Test
  void testFtpSchemeBlocked() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    FairMapperException ex =
        assertThrows(
            FairMapperException.class, () -> validator.validate("ftp://fdp.example.org/file"));
    assertTrue(ex.getMessage().contains("Invalid URL scheme"));
  }

  @Test
  void testInvalidUrlThrows() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> validator.validate("not a valid url"));
    assertTrue(ex.getMessage().contains("Invalid URL"));
  }

  @Test
  void testNullSourceUrlThrows() {
    FairMapperException ex = assertThrows(FairMapperException.class, () -> new UrlValidator(null));
    assertTrue(ex.getMessage().contains("Source URL is required"));
  }

  @Test
  void testBlankSourceUrlThrows() {
    FairMapperException ex = assertThrows(FairMapperException.class, () -> new UrlValidator("   "));
    assertTrue(ex.getMessage().contains("Source URL is required"));
  }

  @Test
  void testInvalidSourceUrlThrows() {
    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> new UrlValidator("not a url"));
    assertTrue(ex.getMessage().contains("Invalid source URL"));
  }

  @Test
  void testCaseInsensitiveDomainMatching() {
    UrlValidator validator = new UrlValidator("https://FDP.Example.ORG/catalog");
    assertDoesNotThrow(() -> validator.validate("https://fdp.example.org/data"));
  }

  @Test
  void testGetAllowedDomain() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    assertEquals("fdp.example.org", validator.getAllowedDomain());
  }

  @Test
  void testIsAllowExternalFalseByDefault() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    assertFalse(validator.isAllowExternal());
  }

  @Test
  void testIsAllowExternalTrueWhenSet() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog", true);
    assertTrue(validator.isAllowExternal());
  }

  @Test
  void testLocalhostAsSourceAllowsLocalhost() {
    UrlValidator validator = new UrlValidator("http://localhost:8080/api");
    assertDoesNotThrow(() -> validator.validate("http://localhost:8080/other"));
  }

  @Test
  void testLocalhostAsSourceBlocksExternal() {
    UrlValidator validator = new UrlValidator("http://localhost:8080/api");
    FairMapperException ex =
        assertThrows(
            FairMapperException.class, () -> validator.validate("https://fdp.example.org/data"));
    assertTrue(ex.getMessage().contains("External domain blocked"));
  }

  @Test
  void testSubdomainBypassAttackBlocked() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    FairMapperException ex =
        assertThrows(
            FairMapperException.class,
            () -> validator.validate("https://fdp.example.org.evil.com/data"));
    assertTrue(ex.getMessage().contains("External domain blocked"));
  }

  @Test
  void testBlankHostBlocked() {
    UrlValidator validator = new UrlValidator("https://fdp.example.org/catalog");
    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> validator.validate("https:///path"));
    assertTrue(ex.getMessage().contains("URL must have a host"));
  }
}
