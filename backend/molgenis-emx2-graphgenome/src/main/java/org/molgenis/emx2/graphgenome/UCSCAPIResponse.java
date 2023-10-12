package org.molgenis.emx2.graphgenome;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UCSCAPIResponse {

  private String downloadTime;
  private String downloadTimeStamp;
  private String genome;
  private String chrom;
  private long start;
  private long end;
  private String dna;

  public String getDownloadTime() {
    return downloadTime;
  }

  public String getDownloadTimeStamp() {
    return downloadTimeStamp;
  }

  public String getGenome() {
    return genome;
  }

  public String getChrom() {
    return chrom;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  public String getDna() {
    return dna;
  }
}
