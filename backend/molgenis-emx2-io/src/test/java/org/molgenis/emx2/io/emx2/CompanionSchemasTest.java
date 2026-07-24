package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.io.emx2.CompanionSchemas.CompanionDeclaration;

class CompanionSchemasTest {

  @Test
  void nonCyclicBundleRefIsSurfacedForApplyLayerRejection() {
    List<CompanionDeclaration> declarations =
        CompanionSchemas.fromSingleFile(
            "tables:\n- name: T\n  columns:\n  - name: id\n    key: 1\n"
                + "additionalSchemas:\n  Shared:\n    bundle: shared/molgenis.yaml\n");

    assertEquals(1, declarations.size());
    CompanionDeclaration shared = declarations.get(0);
    assertEquals("Shared", shared.name());
    assertEquals("shared/molgenis.yaml", shared.bundleRef());
    assertFalse(
        shared.hasInlineModel(),
        "a bundle: companion carries no inline body, so the apply layer must reject it");
  }

  @Test
  void inlineCompanionCarriesModelNotBundleRef() {
    List<CompanionDeclaration> declarations =
        CompanionSchemas.fromSingleFile(
            "additionalSchemas:\n  Inline:\n    tables:\n    - name: C\n      columns:\n      - name: id\n"
                + "        key: 1\n");

    assertEquals(1, declarations.size());
    CompanionDeclaration inline = declarations.get(0);
    assertNull(inline.bundleRef());
    assertTrue(inline.hasInlineModel());
  }
}
