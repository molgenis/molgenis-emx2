package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

@Tag("slow")
class TestJavaScriptSandbox {

  /** Stands in for the webapp's application-class binding ("simplePostClient"). */
  @FunctionalInterface
  interface Widget {
    Object run();
  }

  @Test
  void classLoaderPivotToRuntimeIsBlocked() {
    Map<String, Object> bindings = Map.of("simplePostClient", (Widget) () -> null);
    String rce =
        "simplePostClient.getClass().getClassLoader().loadClass('java.lang.Runtime')"
            + ".getMethod('getRuntime').invoke(null).exec('id')";
    assertThrows(MolgenisException.class, () -> executeJavascriptOnMap(rce, bindings));
  }

  @Test
  void getClassIsNotCallableOnBoundHostObject() {
    Map<String, Object> bindings = Map.of("x", (Widget) () -> null);
    assertThrows(
        MolgenisException.class, () -> executeJavascriptOnMap("x.getClass().getName()", bindings));
  }

  @Test
  void javaTypeHostClassLookupIsBlocked() {
    assertThrows(
        MolgenisException.class,
        () ->
            executeJavascriptOnMap(
                "Java.type('java.lang.Runtime').getRuntime().exec('id')", Map.of()));
  }

  @Test
  void nonExportedHostMethodIsBlocked() {
    Map<String, Object> bindings = Map.of("w", (Widget) () -> "should-not-run");
    assertThrows(MolgenisException.class, () -> executeJavascriptOnMap("w.run()", bindings));
  }

  @FunctionalInterface
  interface ExportedClient {
    Object call(String arg);
  }

  @Test
  void exportedFunctionalBindingStillCallable() {
    ExportedClient client =
        new ExportedClient() {
          @HostAccess.Export
          @Override
          public Object call(String arg) {
            return "ok:" + arg;
          }
        };
    Map<String, Object> bindings = Map.of("simplePostClient", client);
    assertEquals("ok:hi", executeJavascriptOnMap("simplePostClient('hi')", bindings));
  }

  @Test
  void legitimateExpressionsStillWork() {
    // arithmetic and string templates on auto-converted scalar bindings
    assertEquals(12, executeJavascriptOnMap("5 + 7", Map.of()));
    assertEquals(
        "a b", executeJavascriptOnMap("`${first} ${last}`", Map.of("first", "a", "last", "b")));
    // reference column value is bound as a Map; reading its members must still work
    assertEquals(
        "hello", executeJavascriptOnMap("ref.name", Map.of("ref", Map.of("name", "hello"))));
    // ref_array value is bound as a List of Maps; indexing + member access must still work
    assertEquals(
        "x", executeJavascriptOnMap("tags[0].name", Map.of("tags", List.of(Map.of("name", "x")))));
  }
}
