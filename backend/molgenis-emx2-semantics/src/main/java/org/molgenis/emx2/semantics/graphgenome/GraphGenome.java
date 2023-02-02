package org.molgenis.emx2.semantics.graphgenome;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.GENEID;
import static org.molgenis.emx2.semantics.rdf.RootToRDF.describeRoot;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.util.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
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
  public static final String REFERENCE = "REF";
  public static final String ALTERNATIVE = "ALT";

  /**
   * Construct graph genome based on Beacon v2 variants and output as RDF
   *
   * @param outputStream
   * @param request
   * @param response
   * @param graphGenomeApiLocation
   * @param tables
   */
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
      String chromosomeWithChr = chromosome.startsWith("chr") ? chromosome : "chr" + chromosome;

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

      // sort variants for lineair iteration
      Collections.sort(sortedVariants, new SortByPosition());

      // prep RDF export
      RDFService rdfService = new RDFService(request, response);
      String apiContext = rdfService.getHost() + graphGenomeApiLocation;
      describeRoot(rdfService.getBuilder(), rdfService.getHost());
      ModelBuilder builder = rdfService.getBuilder();
      builder.add(apiContext, DCTERMS.IS_PART_OF, iri(rdfService.getHost()));

      int nodeCounter = 0;
      int previousRefSeqStart = 0;
      int previousRefSeqEnd = 0;
      int previousVariantRefBaseLength = 0;
      String previousVariantRefNode = apiContext; // connect first node to API endpoint
      String upstreamRefSeqNode;
      List<String> upstreamVariantAltNodes = new ArrayList<>();
      String previousVariantConnectedTo = null;

      for (int i = 0; i < sortedVariants.size(); i++) {

        GenomicVariantsResultSetsItem variant = sortedVariants.get(i);

        int refSeqStart = previousRefSeqEnd + previousVariantRefBaseLength;
        int refSeqEnd =
            (int) (variant.getPosition().getStart()[0] - earliestStart) + DNA_PADDING - 1;

        Situation situation;
        if (refSeqEnd == previousRefSeqEnd) {
          situation = Situation.SITUATION_1;
          // Situation 1: New variant at the same location as the previous variant. Do not produce
          // new reference sequence node, instead connect it to previous ref seq node or variant
          // ref. Basically, we only add an alt variant here, and do not clear list of alts that
          // next node should connect to.
          upstreamRefSeqNode = previousVariantConnectedTo;
        } else if (refSeqStart == refSeqEnd) {
          situation = Situation.SITUATION_2;
          // Situation 2: Variant directly follows the previous variant. Do not produce new
          // reference sequence node, instead connect to the reference of previous variant. Also
          // connect previous alts to new variant reference, essentially 'crossing over' the
          // variants.
          upstreamRefSeqNode = previousVariantRefNode;
        } else if (refSeqStart > refSeqEnd) {
          situation = Situation.SITUATION_3;
          // Situation 3: Variant within previous indel length causing start to overshoot. We must
          // produce a new reference sequence node between this variant and the last reference, and
          // connect the variant to that.
          refSeqStart = previousRefSeqStart;
          String refSeq = dna.substring(refSeqStart, refSeqEnd);
          String refSeqNodeId = formatNodeId(apiContext, gene, nodeCounter++, REFERENCE, refSeq);
          addNode(builder, refSeqNodeId, REFERENCE, null, previousVariantConnectedTo, null);
          upstreamRefSeqNode = refSeqNodeId;
        } else {
          situation = Situation.SITUATION_4;
          // Situation 4: No special circumstances: produce new ref seq node, and connect the
          // variant ref and alt to it.
          String refSeq = dna.substring(refSeqStart, refSeqEnd);
          String refSeqNodeId = formatNodeId(apiContext, gene, nodeCounter++, REFERENCE, refSeq);
          addNode(
              builder,
              refSeqNodeId,
              REFERENCE,
              null,
              previousVariantRefNode,
              upstreamVariantAltNodes);
          upstreamRefSeqNode = refSeqNodeId;
        }

        // define variant alt first because we need it later for linking
        String variantAltNode =
            formatNodeId(apiContext, gene, nodeCounter++, ALTERNATIVE, variant.getAlternateBases());
        addNode(builder, variantAltNode, ALTERNATIVE, variant, upstreamRefSeqNode, null);

        // define variant ref next, except for Situation 1
        if (situation != Situation.SITUATION_1) {
          String variantRefNode =
              formatNodeId(apiContext, gene, nodeCounter++, REFERENCE, variant.getReferenceBases());
          addNode(
              builder,
              variantRefNode,
              REFERENCE,
              variant,
              upstreamRefSeqNode,
              (situation == Situation.SITUATION_2 ? upstreamVariantAltNodes : null));

          // make explicit which alts replace which refs because we now do not revisit nodes,
          // causing less-than-ideal representation of graph genome when variants are within indels
          // todo ideally revisit and split up indels when another variant is located inside them
          builder.add(variantAltNode, DCTERMS.REPLACES, iri(variantRefNode));

          previousVariantRefNode = variantRefNode;
          previousVariantConnectedTo = upstreamRefSeqNode;
          upstreamVariantAltNodes.clear();
        } else {
          // only connect new alt to previous ref
          builder.add(variantAltNode, DCTERMS.REPLACES, iri(previousVariantRefNode));
        }

        upstreamVariantAltNodes.add(variantAltNode);
        previousVariantRefBaseLength = variant.getReferenceBases().length();
        previousRefSeqEnd = refSeqEnd;
        previousRefSeqStart = refSeqStart;
      }
      // one last part of reference sequence remaining
      String refSeq = dna.substring(previousRefSeqEnd + previousVariantRefBaseLength);
      String refSeqNode = apiContext + "/" + gene + "/" + (nodeCounter++) + "/" + shorten(refSeq);
      addNode(
          builder, refSeqNode, REFERENCE, null, previousVariantRefNode, upstreamVariantAltNodes);

      Rio.write(
          rdfService.getBuilder().build(),
          outputStream,
          rdfService.getRdfFormat(),
          rdfService.getConfig());

    } catch (Exception e) {
      throw new MolgenisException("Graph genome export failed due to an exception", e);
    }
  }

  /**
   * Format node identifiers
   *
   * @param apiContext
   * @param gene
   * @param nodeCounter
   * @param type
   * @param seq
   * @return
   */
  public static String formatNodeId(
      String apiContext, String gene, int nodeCounter, String type, String seq) {
    return apiContext + "/" + gene + "/node" + nodeCounter + "/" + type + "/" + shorten(seq);
  }

  /**
   * Add a variant node to the graph
   *
   * @param builder
   * @param nodeId
   * @param type
   * @param variant
   * @param downstreamOfRef
   * @param downstreamOfAlts
   * @throws Exception
   */
  public static void addNode(
      ModelBuilder builder,
      String nodeId,
      String type,
      GenomicVariantsResultSetsItem variant,
      String downstreamOfRef,
      List<String> downstreamOfAlts)
      throws Exception {
    if (type.equals(REFERENCE)) {
      builder.add(nodeId, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C164388"));
    } else if (type.equals(ALTERNATIVE)) {
      builder.add(nodeId, RDF.TYPE, iri("http://ensembl.org/glossary/ENSGLOSSARY_0000187"));
    } else {
      throw new Exception("Bad type: " + type);
    }
    if (downstreamOfRef != null) {
      builder.add(nodeId, iri("http://purl.obolibrary.org/obo/RO_0002530"), iri(downstreamOfRef));
    }
    if (downstreamOfAlts != null) {
      for (String downstreamOfAlt : downstreamOfAlts) {
        builder.add(nodeId, iri("http://purl.obolibrary.org/obo/RO_0002530"), iri(downstreamOfAlt));
      }
    }
    if (variant != null) {
      // todo add variant properties
    }
  }

  /**
   * Shorten lengthy strings of DNA
   *
   * @param input
   * @return
   */
  public static String shorten(String input) {
    if (input.length() > 50) {
      return input.substring(0, 25) + "..." + input.substring(input.length() - 25);
    } else {
      return input;
    }
  }
}
