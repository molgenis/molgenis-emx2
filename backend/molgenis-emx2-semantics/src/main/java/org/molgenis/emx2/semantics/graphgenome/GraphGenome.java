package org.molgenis.emx2.semantics.graphgenome;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQuery.GENOMIC_VARIATIONS_TABLE_NAME;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.GENEID;
import static org.molgenis.emx2.semantics.rdf.RootToRDF.describeRoot;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.util.*;
import org.eclipse.rdf4j.model.IRI;
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

  // also in org.molgenis.emx2.web.RDFApi, but that cannot be reached from here
  public static final String RDF_API_LOCATION = "/api/rdf";
  public static final IRI IS_DOWNSTREAM_OF = iri("http://purl.obolibrary.org/obo/RO_0002530");
  public static final String REFERENCE = "REF";
  public static final String ALTERNATIVE = "ALT";

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

        if (situation != Situation.SITUATION_1) {
          // if not Situation 1, add new ref and clear alts.
          String variantRefNode =
              formatNodeId(apiContext, gene, nodeCounter++, REFERENCE, variant.getReferenceBases());
          // only in Situation 2: connect to previous alts
          addNode(
              builder,
              variantRefNode,
              REFERENCE,
              variant,
              upstreamRefSeqNode,
              (situation == Situation.SITUATION_2 ? upstreamVariantAltNodes : null));
          previousVariantRefNode = variantRefNode;
          previousVariantConnectedTo = upstreamRefSeqNode;
          upstreamVariantAltNodes.clear();
        }
        String variantAltNode =
            formatNodeId(apiContext, gene, nodeCounter++, ALTERNATIVE, variant.getAlternateBases());
        addNode(builder, variantAltNode, ALTERNATIVE, variant, upstreamRefSeqNode, null);
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

  public static String formatNodeId(
      String apiContext, String gene, int nodeCounter, String type, String seq) {
    return apiContext + "/" + gene + "/node" + nodeCounter + "/" + type + "/" + shorten(seq);
  }

  public static void addNode(
      ModelBuilder builder,
      String nodeId,
      String type,
      GenomicVariantsResultSetsItem variant,
      String downstreamOfRef,
      List<String> downstreamOfAlts)
      throws Exception {

    printNode(nodeId, type, downstreamOfRef, downstreamOfAlts);

    if (type.equals(REFERENCE)) {
      builder.add(nodeId, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C164388"));
    } else if (type.equals(ALTERNATIVE)) {
      builder.add(nodeId, RDF.TYPE, iri("http://ensembl.org/glossary/ENSGLOSSARY_0000187"));
    } else {
      throw new Exception("Bad type: " + type);
    }
    if (downstreamOfRef != null) {
      builder.add(nodeId, IS_DOWNSTREAM_OF, iri(downstreamOfRef));
    }
    if (downstreamOfAlts != null) {
      for (String downstreamOfAlt : downstreamOfAlts) {
        builder.add(nodeId, IS_DOWNSTREAM_OF, iri(downstreamOfAlt));
      }
    }
    if (variant != null) {
      // todo add variant properties
    }
  }

  public static String shorten(String input) {
    if (input.length() > 50) {
      return input.substring(0, 25) + "..." + input.substring(input.length() - 25);
    } else {
      return input;
    }
  }

  public static void printNode(
      String nodeId, String type, String downstreamOfRef, List<String> downstreamOfAlts) {
    System.out.println(
        "Adding new node: \n\t"
            + nodeId
            + "\n\t"
            + type
            + "\n\tdownstream of ref "
            + downstreamOfRef
            + "\n\tdownstream of alts"
            + downstreamOfAlts
            + "\n");
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
