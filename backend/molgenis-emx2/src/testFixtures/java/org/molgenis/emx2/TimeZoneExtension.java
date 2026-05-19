package org.molgenis.emx2;

import java.util.TimeZone;
import org.junit.jupiter.api.extension.*;

public class TimeZoneExtension implements BeforeEachCallback, AfterAllCallback {
  private static final TimeZone systemTimeZone = TimeZone.getDefault();

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    TimeZone.setDefault(
        TimeZone.getTimeZone(
            context.getRequiredTestClass().getAnnotation(WithTimeZone.class).value()));
  }

  @Override
  public void afterAll(ExtensionContext context) {
    TimeZone.setDefault(systemTimeZone);
  }
}
