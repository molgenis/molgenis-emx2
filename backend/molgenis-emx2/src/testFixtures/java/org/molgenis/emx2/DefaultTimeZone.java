package org.molgenis.emx2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.TimeZone;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Sets the JVM default {@link TimeZone} for all tests within the annotated class and restores the
 * original one afterwards.
 *
 * <p>IMPORTANT: Changes JVM-global state! Watch out with running in-JVM parallel tests if multiple
 * tests change this (into a different value).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(DefaultTimeZone.Extension.class)
public @interface DefaultTimeZone {
  /** Timezone ID, such as {@code "Europe/Amsterdam"}. */
  String value();

  class Extension implements BeforeAllCallback, AfterAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(Extension.class);
    private static final String ORIGINAL = "originalTimeZone";

    @Override
    public void beforeAll(ExtensionContext context) {
      String zoneId = context.getRequiredTestClass().getAnnotation(DefaultTimeZone.class).value();
      TimeZone timeZone = TimeZone.getTimeZone(zoneId);
      if (!timeZone.getID().equals(zoneId)) {
        // TimeZone.getTimeZone() silently falls back to GMT for unknown IDs.
        throw new IllegalArgumentException("Unknown timezone: " + zoneId);
      }
      context.getStore(NAMESPACE).put(ORIGINAL, TimeZone.getDefault());
      TimeZone.setDefault(timeZone);
    }

    @Override
    public void afterAll(ExtensionContext context) {
      TimeZone.setDefault(context.getStore(NAMESPACE).get(ORIGINAL, TimeZone.class));
    }
  }
}
