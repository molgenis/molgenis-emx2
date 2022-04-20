package org.molgenis.emx2.io.readers.vcf;

import java.util.Map;
import java.util.UUID;
import org.molgenis.emx2.Row;

public class VcfRow extends Row {

  public static String DBID = "Identifier";
  public static String SRCFILE = "VCFSourceFile";
  public static String CHROM = "Chromosome";
  public static String POS = "Position";
  public static String RSID = "RefSNPNumber";
  public static String REF = "Reference";
  public static String ALT = "Alternative";
  public static String FILTER = "Filter";
  public static String QUAL = "Quality";
  public static String INFO = "Information";
  public static String FORMAT = "Format";
  public static String SAMPLES = "SampleGenotypes";

  public VcfRow(Map<String, ?> values, String srcFile) {
    super();
    this.set(DBID, UUID.randomUUID());
    this.set(SRCFILE, srcFile);
    for (Map.Entry<String, ?> e : values.entrySet()) {

      switch (e.getKey()) {
        case "#CHROM":
          this.set(CHROM, e.getValue());
          break;
        case "POS":
          this.set(POS, e.getValue());
          break;
        case "ID":
          this.set(RSID, e.getValue());
          break;
        case "REF":
          this.set(REF, e.getValue());
          break;
        case "ALT":
          this.set(ALT, e.getValue());
          break;
        case "FILTER":
          this.set(FILTER, e.getValue());
          break;
        case "QUAL":
          this.set(QUAL, e.getValue());
          break;
        case "INFO":
          this.set(INFO, e.getValue());
          break;
        case "FORMAT":
          this.set(FORMAT, e.getValue());
          break;
        default:
          String existingSampleGenotypes =
              this.getString(SAMPLES) != null ? this.getString(SAMPLES) + ", " : "";
          this.set(SAMPLES, existingSampleGenotypes + e.getKey() + ": \"" + e.getValue() + "\"");
      }
    }
  }
}
