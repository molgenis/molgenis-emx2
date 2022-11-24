package org.molgenis.emx2.datamodels.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

/**
 * Tiny standalone helper tool to convert public VKGL release data into EMX2 'FAIR Data Hub'
 * compliant data. See: https://www.vkgl.nl, https://vkgl.molgeniscloud.org,
 * https://onlinelibrary.wiley.com/doi/10.1002/humu.23896.
 */
public class VKGLVariantsToEMX2Beacon {

  /**
   * How to use this tool:
   *
   * <ol>
   *   <li>Download latest VKGL release file from
   *       https://downloads.molgeniscloud.org/downloads/VKGL/
   *   <li>Run this tool on the downloaded file (see hardcoded paths)
   *   <li>Use cURL (or similar) to import results into a EMX2 'FAIR Data Hub' (see hardcoded URLs),
   *       of course permissions allowing or using an auth token.
   *       <ul>
   *         <li>curl -X POST --data-binary @GenomicVariationsClinInterpr.tsv
   *             https://vkgl-emx2.molgeniscloud.org/VKGL_public_consensus_sep2022/api/csv/GenomicVariationsClinInterpr
   *         <li>curl -X POST --data-binary @GenomicVariations.tsv
   *             https://vkgl-emx2.molgeniscloud.org/VKGL_public_consensus_sep2022/api/csv/GenomicVariations
   *       </ul>
   * </ol>
   */
  public static void main(String args[]) throws Exception {
    System.out.println("Starting...");

    File dataDirectory = new File("/Users/joeri/Documents/VKGL/vkgl-emx2/sep2022");
    File VKGLPublicConsensusFile = new File(dataDirectory, "VKGL_public_consensus_sep2022.tsv");

    Scanner scanner = new Scanner(VKGLPublicConsensusFile);
    FileWriter genomicVariationsFileWriter =
        new FileWriter(new File(dataDirectory, "GenomicVariations.tsv"));
    BufferedWriter genomicVariationsBufferedWriter =
        new BufferedWriter(genomicVariationsFileWriter);
    genomicVariationsBufferedWriter.write(
        "variantInternalId,position_refseqId,position_start,position_end,referenceBases,alternateBases,genomicHGVSId,transcriptHGVSIds,proteinHGVSIds,geneId,variantType,position_assemblyId,clinicalInterpretations\n");

    // skip header
    scanner.nextLine();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      String[] split = line.split("\t", -1);
      if (split.length != 14) {
        System.out.println("line has " + split.length + " elements and not 14:" + line);
      }

      String variantType = split[5].length() == 1 && split[6].length() == 1 ? "SNP" : "INDEL";
      String classification = split[12].trim();
      String classificationCoded;
      if (classification.equals("LB")) {
        classificationCoded = "Likely benign";
      } else if (classification.equals("LP")) {
        classificationCoded = "Likely pathogenic";
      } else if (classification.equals("VUS")) {
        classificationCoded = "Unknown Significance";
      } else {
        throw new Exception("Unknown classification: " + classification);
      }

      String hgvsCNotation = (split[9] + ":" + split[7]);
      hgvsCNotation = hgvsCNotation.length() < 256 ? hgvsCNotation : "n/a";
      String hgvsPNotation = (split[9] + ":" + split[8]);
      hgvsPNotation = hgvsPNotation.length() < 256 ? hgvsPNotation : "n/a";

      genomicVariationsBufferedWriter.write(
          split[0] // ID
              + ","
              + split[2] // chromosome
              + ","
              + split[3] // start
              + ","
              + split[4] // stop
              + ","
              + split[5] // ref
              + ","
              + split[6] // alt
              + ",\""
              + (split[10].length() < 256 ? split[10] : "n/a") // hgvs genomic
              + "\",\""
              + hgvsCNotation
              + "\",\""
              + hgvsPNotation
              + "\","
              + split[11] // gene
              + ","
              + variantType
              + ","
              + "GRCh37"
              + ","
              + classificationCoded
              + "\n");
    }

    genomicVariationsBufferedWriter.flush();
    genomicVariationsBufferedWriter.close();

    // write GenomicVariationsClinInterpr
    FileWriter genomicVariationsClinInterprFileWriter =
        new FileWriter(new File(dataDirectory, "GenomicVariationsClinInterpr.tsv"));
    BufferedWriter genomicVariationsClinInterprBufferedWriter =
        new BufferedWriter(genomicVariationsClinInterprFileWriter);
    genomicVariationsClinInterprBufferedWriter.write("id,clinicalRelevance\n");
    genomicVariationsClinInterprBufferedWriter.write("Likely benign,Likely benign\n");
    genomicVariationsClinInterprBufferedWriter.write("Likely pathogenic,Likely pathogenic\n");
    genomicVariationsClinInterprBufferedWriter.write("Unknown Significance,Unknown Significance\n");
    genomicVariationsClinInterprBufferedWriter.flush();
    genomicVariationsClinInterprBufferedWriter.close();

    System.out.println("Finished!");
  }
}
