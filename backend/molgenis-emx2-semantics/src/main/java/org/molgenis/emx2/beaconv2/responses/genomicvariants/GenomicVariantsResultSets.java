package org.molgenis.emx2.beaconv2.responses.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariantsResultSets {

  String id;
  String type = "dataset";
  String setType = "setype";
  Boolean exists = true;
  Integer resultsCount;
  GenomicVariantsResultSetsItem[] results;

  /*

  return all sources that were queried and whether the variant was found or not e.g.

  "response": {
    "resultSets": [
      {
        "id": "cafe-central",
        "setType": "genomicVariant",
        "exists": false,
        "resultsCount": 0,
        "results": [],
        "resultsHandovers": {
          "handoverType": {
            "id": "CUSTOM",
            "label": "Link to  Cafe Variome Central."
          },
          "note": "Link to  Cafe Variome Central.",
          "url": "https://central.cafevariome.org"
        }
      },

   */

  // see
  // https://github.com/ga4gh-beacon/beacon-v2-Models/blob/94bd059442c386c8306b08b34ec7db547d6df13d/BEACON-V2-Model/genomicVariations/examples/genomicVariant-MID-example.json
  public GenomicVariantsResultSets() {
    this.id = "correctEmptyResultSet";
    this.type = "dataset";
    this.setType = "array";
    this.exists = true;
    this.resultsCount = 1;

    List<GenomicVariantsResultSetsItem> iList = new ArrayList<>();
    Position p = new Position("GRCh38", "chr1", new int[] {55039979});
    iList.add(new GenomicVariantsResultSetsItem("rs123", "SNP", "A", p));
    this.results = iList.toArray(new GenomicVariantsResultSetsItem[iList.size()]);
  }
}
