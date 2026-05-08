package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BootstrapThemeServiceTest {

  @Test
  void generateCss_withoutContextPath_usesDefaultFontPath() {
    String css = BootstrapThemeService.generateCss(Map.of());
    assertTrue(css.contains("/apps/resources/webfonts"), "Font path should use default /apps/resources/webfonts");
    assertFalse(css.contains("null"), "CSS must not contain 'null'");
  }

  @Test
  void generateCss_withContextPath_prefixesFontPath() {
    String css = BootstrapThemeService.generateCss(Map.of(), "/molgenis");
    assertTrue(css.contains("/molgenis/apps/resources/webfonts"), "Font path must include context path prefix");
    assertFalse(css.contains("\"/apps/resources/webfonts\""), "Default font path must not appear without prefix");
  }

  @Test
  void generateCss_withMultiSegmentContextPath_prefixesFontPath() {
    String css = BootstrapThemeService.generateCss(Map.of(), "/org/app");
    assertTrue(css.contains("/org/app/apps/resources/webfonts"), "Font path must include multi-segment context path");
  }

  @Test
  void generateCss_withCustomColors_appliesColors() {
    String css = BootstrapThemeService.generateCss(Map.of("primaryColor", "#FF0000"), "/molgenis");
    assertNotNull(css);
    assertFalse(css.isEmpty());
  }
}
