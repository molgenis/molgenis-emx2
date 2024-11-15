package org.molgenis.emx2.beaconv2.vcfimport;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;

public class VcfImport {

  private File vcfTmpFile;
  private Table genomicVariations;
  private Table individuals;

  public VcfImport(File vcfTmpFile, Table genomicVariations, Table individuals) {
    this.vcfTmpFile = vcfTmpFile;
    this.genomicVariations = genomicVariations;
    this.individuals = individuals;
  }

  public int start() {

    VCFFileReader vcfFileReader = new VCFFileReader(vcfTmpFile, false);
    // Retrieve the header
    VCFHeader header = vcfFileReader.getFileHeader();
    System.out.println("VCF Header: " + header);

    int count = 0;
    List<Row> rows = new ArrayList<>();
    for (VariantContext variant : vcfFileReader) {

      Row row = new Row();
      row.set("variantInternalId", UUID.randomUUID());
      row.set("position_assemblyId", "todo");
      row.set("position_refseqId", variant.getContig());
      row.set("position_start", variant.getStart());
      row.set("referenceBases", variant.getReference());
      row.set("alternateBases", variant.getAlternateAlleles());
      rows.add(row);
      count++;
    }

    genomicVariations.save(rows);
    return count;
  }

  public static void main(String[] args) {
    String vcfFilePath =
        "/Users/joeri/git/molgenis-emx2/backend/molgenis-emx2-beacon-v2/src/test/resources/sample.vcf";

    try (VCFFileReader vcfFileReader = new VCFFileReader(new File(vcfFilePath), false)) {
      // Retrieve the header
      VCFHeader header = vcfFileReader.getFileHeader();
      System.out.println("VCF Header: " + header);

      // Iterate over each record in the VCF file
      for (VariantContext variant : vcfFileReader) {
        System.out.println("Contig: " + variant.getContig());
        System.out.println("Position: " + variant.getStart());
        System.out.println("ID: " + variant.getID());
        System.out.println("Reference Allele: " + variant.getReference());
        System.out.println("Alternate Alleles: " + variant.getAlternateAlleles());

        // Additional information (like genotype, quality, filters)
        System.out.println("Genotype: " + variant.getGenotypes());
        System.out.println("Quality: " + variant.getPhredScaledQual());
        System.out.println("Filters: " + variant.getFilters());
        System.out.println("------------------------");
      }
    } catch (Exception e) {
      System.err.println("Error reading VCF file: " + e.getMessage());
    }
  }
}
