package org.molgenis.emx2.semantics.gendecs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HpoConverterTest {

  String genesToPheno =
      "/Users/jonathan/Documents/GitHub/molgenis-emx2/data/gendecs/genes_to_phenotype.txt";

  @Test
  public void idToTerm_normalInput() {
    String hpoId = "HP:0004322";
    String termExp = "Short stature";

    assertEquals(termExp, HpoConverter.getHpoTerm(hpoId, genesToPheno));
  }

  @Test
  public void termToId_normalInput() {
    String hpoTerm = "Microcephaly";
    String idExp = "HP:0000252";

    assertEquals(idExp, HpoConverter.getHpoId(hpoTerm, genesToPheno));
  }

  @Test
  public void idToTerm_fakeId() {
    String hpoId = "noId";
    String termExp = "";

    assertEquals(termExp, HpoConverter.getHpoTerm(hpoId, genesToPheno));
  }

  @Test
  public void termToId_fakeTerm() {
    String hpoTerm = "no term";
    String idExp = "";

    assertEquals(idExp, HpoConverter.getHpoId(hpoTerm, genesToPheno));
  }

  @Test
  public void idToTerm_nullInput() {
    String termExp = "";

    assertEquals(termExp, HpoConverter.getHpoTerm(null, genesToPheno));
  }

  @Test
  public void termToId_nullInput() {
    String idExp = "";

    assertEquals(idExp, HpoConverter.getHpoId(null, genesToPheno));
  }
}
