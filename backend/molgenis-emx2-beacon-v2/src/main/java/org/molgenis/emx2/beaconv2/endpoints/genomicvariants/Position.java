package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Position {

  private String assemblyId;
  private String refseqId;
  private Long[] start;
  private Long[] end;

  public Position(String assemblyId, String refseqId, Long[] start, Long[] end) {
    this.assemblyId = assemblyId;
    this.refseqId = refseqId;
    this.start = start;
    this.end = end;
  }

  public String getAssemblyId() {
    return assemblyId;
  }

  public String getRefseqId() {
    return refseqId;
  }

  public Long[] getStart() {
    return start;
  }

  public Long[] getEnd() {
    return end;
  }
}
