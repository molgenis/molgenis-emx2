package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Row.row;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PatientRegistryRd3Test extends TestLoaders {

  @Test
  void rnaSampleRejectsSampleTypeOtherThanRna() {
    Table samplesRna = patientRegistry.getTable("Samples RNA");
    Row dnaSample = row("id", "RD3-TEST-RNA-DNA", "sample type", "DNA");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> samplesRna.insert(dnaSample));
    assertTrue(
        exception.getMessage().contains("RNA samples must have sample type RNA"),
        exception.getMessage());
  }

  @Test
  void rnaSampleAcceptsSampleTypeRna() {
    Table samplesRna = patientRegistry.getTable("Samples RNA");
    Row rnaSample = row("id", "RD3-TEST-RNA-RNA", "sample type", "RNA");
    try {
      assertEquals(1, samplesRna.insert(rnaSample));
    } finally {
      samplesRna.delete(row("id", "RD3-TEST-RNA-RNA"));
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Extraction kits",
        "Library preparation kits",
        "Library prep kit manufacturers",
        "Enrichment kit manufacturers",
        "Sample collection methods"
      })
  void erderaOntologyTermsAreTaggedForTemplates(String ontologyTable) {
    List<Row> terms =
        database.getSchema(CATALOGUE_ONTOLOGIES).getTable(ontologyTable).retrieveRows();
    assertFalse(terms.isEmpty(), ontologyTable);
    for (Row term : terms) {
      String name = term.getString("name");
      assertNotEquals("Other", name, ontologyTable);
      assertTrue(tags(term).containsAll(List.of("erdera", "RNA")), ontologyTable + ": " + name);
    }
  }

  @Test
  void erderaEnrichmentKitsAreTaggedAndOtherKitsAreNot() {
    List<Row> kits =
        database
            .getSchema(CATALOGUE_ONTOLOGIES)
            .getTable("Sequencing enrichment kits")
            .retrieveRows();
    List<String> erderaKits =
        kits.stream()
            .filter(kit -> tags(kit).contains("erdera"))
            .map(kit -> kit.getString("name"))
            .toList();
    assertEquals(
        List.of(
            "NEBNext Poly(A) mRNA Magnetic Isolation Module",
            "Polaris Depletion Kit - rRNA/Globin (HMR)"),
        erderaKits.stream().sorted().toList());
    assertTrue(kits.size() > erderaKits.size());
  }

  private static List<String> tags(Row term) {
    String[] tags = term.getStringArray("tags");
    return tags == null ? List.of() : Arrays.asList(tags);
  }

  @Test
  void otherSampleSubtypesAcceptSampleTypeOtherThanRna() {
    Table samplesSrDna = patientRegistry.getTable("Samples srDNA");
    Row dnaSample = row("id", "RD3-TEST-SRDNA-DNA", "sample type", "DNA");
    try {
      assertEquals(1, samplesSrDna.insert(dnaSample));
    } finally {
      samplesSrDna.delete(row("id", "RD3-TEST-SRDNA-DNA"));
    }
  }
}
