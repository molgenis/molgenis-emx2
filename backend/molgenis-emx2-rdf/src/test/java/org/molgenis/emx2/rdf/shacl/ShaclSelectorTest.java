package org.molgenis.emx2.rdf.shacl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ShaclSelectorTest {
  @Test
  void testShaclSelector() {
    ShaclSet expected =
        new ShaclSet(
            "dcat-ap-v3",
            "DCAT-AP",
            "3.0.0",
            new String[] {
              "https://semiceu.github.io/DCAT-AP/releases/3.0.0/#validation-of-dcat-ap"
            },
            new String[] {
              "dcat-ap/v3.0.0/imports.ttl",
              "dcat-ap/v3.0.0/mdr-vocabularies.shape.ttl",
              "dcat-ap/v3.0.0/mdr_imports.ttl",
              "dcat-ap/v3.0.0/range.ttl",
              "dcat-ap/v3.0.0/shapes_recommended.ttl"
            });
    ShaclSet actual = ShaclSelector.get("dcat-ap-v3");
    assertEquals(expected, actual);
  }
}
