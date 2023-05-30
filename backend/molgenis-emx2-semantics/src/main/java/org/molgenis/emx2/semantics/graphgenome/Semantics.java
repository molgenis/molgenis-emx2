package org.molgenis.emx2.semantics.graphgenome;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.graphgenome.GraphGenome.*;

import java.util.List;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.molgenis.emx2.beaconv2.endpoints.genomicvariants.ClinicalInterpretations;
import org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicVariantsResultSetsItem;

public class Semantics {

  /** Add gene starting node to graph */
  public static String addGeneNode(
      String host,
      String apiContext,
      ModelBuilder builder,
      String gene,
      String chromosome,
      String assembly,
      String ucscgenome,
      Long earliestStart,
      Long latestEnd) {
    builder.add(apiContext, DCTERMS.IS_PART_OF, iri(host));

    // first node we connect is the gene with some context
    String apiContextGene = apiContext + "/" + gene;
    builder.add(apiContextGene, DCTERMS.IS_PART_OF, iri(apiContext));

    builder.add(apiContextGene, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C16612"));
    builder.add(
        apiContextGene,
        SKOS.EXACT_MATCH,
        iri("https://www.genenames.org/data/gene-symbol-report/#!/hgnc_id/" + gene));
    builder.add(
        apiContextGene,
        "http://purl.obolibrary.org/obo/NCIT_C13202",
        "https://en.wikipedia.org/wiki/Chromosome_" + chromosome);
    builder.add(apiContextGene, "http://purl.obolibrary.org/obo/NCIT_C164388", assembly);
    builder.add(apiContextGene, "http://purl.obolibrary.org/obo/NCIT_C164388", ucscgenome);
    builder.add(
        apiContextGene,
        "http://purl.obolibrary.org/obo/GENO_0000894",
        (earliestStart - DNA_PADDING));
    builder.add(
        apiContextGene, "http://purl.obolibrary.org/obo/GENO_0000895", (latestEnd + DNA_PADDING));
    return apiContextGene;
  }

  /** Add replace connection for variant alt and ref */
  public static void addAltReplaceRefNode(
      ModelBuilder builder, String variantAltNode, String variantRefNode) {
    builder.add(variantAltNode, DCTERMS.REPLACES, iri(variantRefNode));
  }

  /** Add a variant node to the graph */
  public static void addNode(
      String host,
      ModelBuilder builder,
      String nodeId,
      String type,
      GenomicVariantsResultSetsItem variant,
      String downstreamOfRef,
      List<String> downstreamOfAlts,
      String value)
      throws Exception {
    builder.add(nodeId, RDF.VALUE, value);
    if (type.equals(REFERENCE)) {
      builder.add(nodeId, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C164388"));
    } else if (type.equals(ALTERNATIVE)) {
      builder.add(nodeId, RDF.TYPE, iri("http://ensembl.org/glossary/ENSGLOSSARY_0000187"));
    } else {
      throw new Exception("Bad type: " + type);
    }
    if (downstreamOfRef != null) {
      builder.add(nodeId, "http://purl.obolibrary.org/obo/RO_0002530", iri(downstreamOfRef));
    }
    if (downstreamOfAlts != null) {
      for (String downstreamOfAlt : downstreamOfAlts) {
        builder.add(nodeId, "http://purl.obolibrary.org/obo/RO_0002530", iri(downstreamOfAlt));
      }
    }
    if (variant != null && type.equals(ALTERNATIVE)) {

      builder.add(
          nodeId,
          "http://purl.obolibrary.org/obo/GENO_0000894",
          variant.getPosition().getStart()[0]);
      builder.add(
          nodeId, "http://purl.obolibrary.org/obo/GENO_0000895", variant.getPosition().getEnd()[0]);
      String variantIRI =
          host
              + "/"
              + variant.getGenomicVariantsResultSetId()
              + "/api/rdf/GenomicVariations/"
              + variant.getVariantInternalId();
      builder.add(nodeId, RDFS.ISDEFINEDBY, iri(variantIRI));
      if (variant.getVariantType() != null) {
        builder.add(
            nodeId, "http://purl.obolibrary.org/obo/GENO_0000773", variant.getVariantType());
      }
      if (variant.getGenomicHGVSId() != null) {
        builder.add(
            nodeId, "http://purl.obolibrary.org/obo/NCIT_C172243", variant.getGenomicHGVSId());
      }
      if (variant.getProteinHGVSIds() != null) {
        for (String proteinHGVSId : variant.getProteinHGVSIds()) {
          builder.add(nodeId, "http://ensembl.org/glossary/ENSGLOSSARY_0000274", proteinHGVSId);
        }
      }
      if (variant.getTranscriptHGVSIds() != null) {
        for (String transcriptHGVSId : variant.getTranscriptHGVSIds()) {
          builder.add(nodeId, "http://purl.obolibrary.org/obo/NCIT_C172244", transcriptHGVSId);
        }
      }

      // todo predicate IRIs
      int clinIntCounter = 0;
      if (variant.getVariantLevelData() != null) {
        for (ClinicalInterpretations ci :
            variant.getVariantLevelData().getClinicalInterpretations()) {
          String clinIntNode = nodeId + "/clinical_interpretation" + (clinIntCounter++);
          builder.add(nodeId, "http://snomed.info/id/363713009", iri(clinIntNode));
          builder.add(clinIntNode, RDF.TYPE, "http://purl.obolibrary.org/obo/NCIT_C125009");
          if (ci.getCategory() != null) {
            builder.add(
                clinIntNode,
                "http://purl.obolibrary.org/obo/NCIT_C25372",
                ci.getCategory().getLabel());
            builder.add(
                clinIntNode,
                "http://purl.obolibrary.org/obo/NCIT_C25372",
                iri(ci.getCategory().URI));
          }
          if (ci.getEffect() != null) {
            builder.add(
                clinIntNode,
                "http://purl.obolibrary.org/obo/NCIT_C15607",
                ci.getEffect().getLabel());
            builder.add(
                clinIntNode, "http://purl.obolibrary.org/obo/NCIT_C15607", iri(ci.getEffect().URI));
          }
          if (ci.getClinicalRelevance() != null) {
            builder.add(
                clinIntNode,
                "http://purl.obolibrary.org/obo/HP_0045088",
                ci.getClinicalRelevance().getLabel());
            builder.add(
                clinIntNode,
                "http://purl.obolibrary.org/obo/HP_0045088",
                iri(ci.getClinicalRelevance().URI));
          }
          if (ci.getConditionId() != null) {
            builder.add(clinIntNode, "http://edamontology.org/data_3667", ci.getConditionId());
          }
        }
      }
    }
  }
}
