package org.molgenis.emx2;

import java.util.Arrays;

public class Semantics {
  public Semantics(SemanticPrefixes prefixes, String... semantics) {
    Arrays.stream(semantics).map(prefixes::map).toList();
  }
}
