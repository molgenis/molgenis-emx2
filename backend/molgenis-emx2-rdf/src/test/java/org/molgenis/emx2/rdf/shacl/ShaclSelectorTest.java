package org.molgenis.emx2.rdf.shacl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ShaclSelectorTest {
  @Test
  void testRetrieveShaclSet() {
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
              "dcat-ap/v3.0.0/shapes.ttl",
              "dcat-ap/v3.0.0/shapes_recommended.ttl"
            });
    ShaclSet actual = ShaclSelector.get("dcat-ap-v3");
    assertEquals(expected, actual);
  }

  @Test
  void testRetrieveShaclSetNonExisting() {
    assertNull(ShaclSelector.get("nonExistingValue"));
  }

  @Test
  void testFilteredShaclSet() {
    ShaclSet expected =
        new ShaclSet(
            "dcat-ap-v3",
            "DCAT-AP",
            "3.0.0",
            new String[] {
              "https://semiceu.github.io/DCAT-AP/releases/3.0.0/#validation-of-dcat-ap"
            },
            null);

    Optional<ShaclSet> optional =
        Arrays.stream(ShaclSelector.getAllFiltered())
            .filter(i -> i.name().equals("dcat-ap-v3"))
            .findFirst();
    if (optional.isPresent()) {
      ShaclSet actual = optional.get();
      assertEquals(expected, actual);
    } else {
      fail("\"dcat-ap-v3\" not found");
    }
  }

  @Test
  void testAllNamesPresentInFiltered() {
    // does not test full file but only names for simplicity (1 shacl set is tested fully)
    Set<String> expected = Set.of("fdp-v1.2", "dcat-ap-v3", "hri-v1", "hri-v2", "ejp-rd-vp");
    Set<String> actual =
        Arrays.stream(ShaclSelector.getAllFiltered())
            .map(ShaclSet::name)
            .collect(Collectors.toSet());
    assertEquals(expected, actual);
  }
}
