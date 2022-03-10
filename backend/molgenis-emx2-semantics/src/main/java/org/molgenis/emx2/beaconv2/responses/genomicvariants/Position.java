package org.molgenis.emx2.beaconv2.responses.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Position {

  String assemblyId;
  String refseqId;
  int[] start;

  public Position(String assemblyId, String refseqId, int[] start) {
    this.assemblyId = assemblyId;
    this.refseqId = refseqId;
    this.start = start;
  }
}
