package org.molgenis.emx2.semantics.graphgenome;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQuery.GENOMIC_VARIATIONS_TABLE_NAME;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.GENEID;
import static org.molgenis.emx2.semantics.rdf.RootToRDF.describeRoot;

import java.io.OutputStream;
import java.util.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.genomicvariants.*;
import org.molgenis.emx2.semantics.RDFService;
import spark.Request;
import spark.Response;

public class GraphGenome {

  private static final int DNA_PADDING = 10;

  // also in org.molgenis.emx2.web.RDFApi, but that cannot be reached from here
  public static final String RDF_API_LOCATION = "/api/rdf";

  public static void graphGenomeAsRDF(
      OutputStream outputStream,
      Request request,
      Response response,
      String graphGenomeApiLocation,
      List<Table> tables) {
    try {
      String gene = request.queryParams("gene");
      String assembly = request.queryParams("assembly");
      String ucscgenome = request.queryParams("ucscgenome");

      if (gene == null || gene.isEmpty()) {
        throw new Exception(
            "Must supply request parameter: HGNC gene name (e.g. COL7A1, so /api/graphgenome?gene=COL7A1&assembly=GRCh37&ucscgenome=hg19");
      }
      if (assembly == null || assembly.isEmpty()) {
        throw new Exception(
            "Must supply request parameter: position_assemblyId from GenomicVariations (e.g. GRCh37, so /api/graphgenome?gene=COL7A1&assembly=GRCh37&ucscgenome=hg19");
      }
      if (ucscgenome == null || ucscgenome.isEmpty()) {
        throw new Exception(
            "Must supply request parameter: UCSC reference genome (e.g. hg19, so /api/graphgenome?gene=COL7A1&assembly=GRCh37&ucscgenome=hg19");
      }

      // query variants
      List<GenomicVariantsResultSets> variants = new ArrayList<>();
      for (Table table : tables) {
        variants.addAll(
            GenomicQuery.genomicQuery(table, GENEID, assembly, gene, null, null, null, null));
      }
      if (variants.size() == 0) {
        throw new Exception("No data available for this gene");
      }

      // get first and last positions across all starting positions
      // compatible with 'uncertainty range' SV start positions
      // also, check that there is only 1 chromosome for all variants
      String chromosome = null;
      Long earliestStart = Long.MAX_VALUE;
      Long latestEnd = Long.MIN_VALUE;
      ArrayList<GenomicVariantsResultSetsItem> sortedVariants = new ArrayList<>();
      for (GenomicVariantsResultSets variantSet : variants) {
        for (GenomicVariantsResultSetsItem variant : variantSet.getResults()) {
          variant.setGenomicVariantsResultSetId(variantSet.getId());
          sortedVariants.add(variant);
          Position position = variant.getPosition();
          if (chromosome == null) {
            chromosome = position.getRefseqId();
          } else if (!chromosome.equals(position.getRefseqId())) {
            throw new Exception(
                "At least 2 different chromosomes present: "
                    + chromosome
                    + " and "
                    + position.getRefseqId());
          }
          for (Long stLong : position.getStart()) {

            if (stLong < earliestStart) {
              earliestStart = stLong;
            }
          }
          for (Long endLong : position.getEnd()) {
            if (endLong > latestEnd) {
              latestEnd = endLong;
            }
          }
        }
      }

      // get corresponding full DNA sequence
      if (earliestStart == Long.MAX_VALUE || latestEnd == Long.MIN_VALUE) {
        throw new Exception("No start or end position available");
      }
      System.out.println("earliestStart = " + earliestStart);
      System.out.println("latestEnd = " + latestEnd);
      String chromosomeWithChr = chromosome.startsWith("chr") ? chromosome : "chr" + chromosome;
      /*
      String UCSCResponseStr =
          HTTPGet.httpGet(
              "https://api.genome.ucsc.edu/getData/sequence?genome="
                  + ucscgenome
                  + ";chrom="
                  + chromosomeWithChr
                  + ";start="
                  + (earliestStart - DNA_PADDING)
                  + ";end="
                  + (latestEnd + DNA_PADDING));
      UCSCAPIResponse UCSCResponse =
          new ObjectMapper().readValue(UCSCResponseStr, UCSCAPIResponse.class);
      String dna = UCSCResponse.getDna();
       */
      String dna =
          "TCACAAGCCCCCATTGCCGGCGAGGGGTGACGGATGCGCACGATCGGCGTTCCCCCCACCAACAGGAAAGCGAACTGCATGTGTGAGCCGAGTCCTGGGTGCACGTCCCACAGCTCAGGGAATCGCGCCGCGCGCGGGGACTCGCTCCGTTCCTCTTCCTGCGGCCTGAAAGGCCTGAACCTCGCCCTCGCCCCCGAGAGACCCGCGGCTGACAGAGCCCAACTCTTCGCGGTGGCAGTGGGTGCCTCCGGAGAAGCCCCGGGCCGACCGCGGCCTCCAGGCGGGGTTCGGGGGCTGGGCAGGCGACCCGCCGCAGGTCCCCGGGAGGGGCGAACGGGCCAGCAGCTGACATTTTTTGTTTGCTCTAGAATGAACGGTGGAAGGCGGCAGGCCGAGGCTTTTCCGCCCGCTGAAAGTCAGCGAGAAAAACAGCGCGCGGGGAGCAAAAGCACGGCGCCTACGCCCTTCTCAGTTAGGGTTAGACAAAAAATGGCCACCACCCC";

      // sort variants for lineair iteration
      Collections.sort(sortedVariants, new SortByPosition());

      RDFService rdfService = new RDFService(request, response);
      String apiContext = rdfService.getHost() + graphGenomeApiLocation;
      describeRoot(rdfService.getBuilder(), rdfService.getHost());
      ModelBuilder builder = rdfService.getBuilder();
      builder.add(apiContext, DCTERMS.IS_PART_OF, iri(rdfService.getHost()));

      int previousVariantEndLoc = 0;
      String linkNextChunkToRef = null;
      String linkNextChunkToAlt = null;
      String previousDNAchunkID = null;
      int previousVariantRefLength = 0;
      System.out.println("DNA length =  " + dna.length());
      for (int i = 0; i < sortedVariants.size(); i++) {

        GenomicVariantsResultSetsItem variant = sortedVariants.get(i);

        int endSubstrDNA =
            (int) (variant.getPosition().getStart()[0] - earliestStart + DNA_PADDING);

        System.out.println("DNA chunk from: " + previousVariantEndLoc + " tot " + endSubstrDNA);

        // previous variant was indel, causing us to 'overshoot'
        // instead, go back to previous dna chunk
        // create new piece of reference chunk that connects to previous chunk
        boolean linkToPreviousRefChunk = false;
        if (endSubstrDNA < previousVariantEndLoc) {
          previousVariantEndLoc = previousVariantEndLoc - previousVariantRefLength;
          linkToPreviousRefChunk = true;
          // connect to previousDNAchunkID
        }

        String dnaChunkVal = dna.substring(previousVariantEndLoc, endSubstrDNA);

        System.out.println(" === " + dnaChunkVal);

        // preceding chunk of reference genome
        // link to the ref and alt variant nodes we created in previous iteration
        String dnaChunk = apiContext + "/" + UUID.randomUUID();
        builder.add(dnaChunk, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C164388"));
        builder.add(dnaChunk, RDF.VALUE, dnaChunkVal);
        if (linkNextChunkToRef != null && !linkToPreviousRefChunk) {
          builder.add(
              dnaChunk, iri("http://purl.obolibrary.org/obo/RO_0002530"), iri(linkNextChunkToRef));
          builder.add(
              dnaChunk, iri("http://purl.obolibrary.org/obo/RO_0002530"), iri(linkNextChunkToAlt));
        } else if (linkToPreviousRefChunk) {
          builder.add(
              dnaChunk, iri("http://purl.obolibrary.org/obo/RO_0002530"), iri(previousDNAchunkID));
        }

        // ref part of variant
        String variantRef = apiContext + "/" + UUID.randomUUID();
        builder.add(apiContext, DC.CREATOR, iri(variantRef));
        builder.add(dnaChunk, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C164388"));
        builder.add(dnaChunk, RDF.VALUE, variant.getReferenceBases());
        builder.add(variantRef, iri("http://purl.obolibrary.org/obo/RO_0002530"), iri(dnaChunk));
        linkNextChunkToRef = variantRef;

        // alt part of variant
        String rdfApiVariantLink = getRdfApiVariantLink(rdfService, variant);
        builder.add(
            rdfApiVariantLink, RDF.TYPE, iri("http://ensembl.org/glossary/ENSGLOSSARY_0000187"));
        builder.add(rdfApiVariantLink, RDF.VALUE, variant.getAlternateBases());
        builder.add(
            rdfApiVariantLink, iri("http://purl.obolibrary.org/obo/RO_0002530"), iri(dnaChunk));
        linkNextChunkToAlt = rdfApiVariantLink;

        previousVariantEndLoc = endSubstrDNA + variant.getReferenceBases().length();
        previousVariantRefLength = variant.getReferenceBases().length();
        previousDNAchunkID = dnaChunk;

        System.out.println(
            "VARIANT start loc "
                + variant.getPosition().getStart()[0]
                + ", ref bases = "
                + variant.getReferenceBases()
                + ", end "
                + variant.getPosition().getEnd()[0]
                + ", alt "
                + variant.getAlternateBases()
                + " --> previousVariantEndLoc = "
                + previousVariantEndLoc);
      }

      Rio.write(
          rdfService.getBuilder().build(),
          outputStream,
          rdfService.getRdfFormat(),
          rdfService.getConfig());

    } catch (Exception e) {
      throw new MolgenisException("Graph genome export failed due to an exception", e);
    }
  }

  public static String getRdfApiVariantLink(
      RDFService rdfService, GenomicVariantsResultSetsItem variant) {
    String schemaContext = rdfService.getHost() + "/" + variant.getGenomicVariantsResultSetId();
    String rdfApiVariantLink =
        schemaContext
            + RDF_API_LOCATION
            + "/"
            + GENOMIC_VARIATIONS_TABLE_NAME
            + "/"
            + variant.getVariantInternalId();
    return rdfApiVariantLink;
  }
}
