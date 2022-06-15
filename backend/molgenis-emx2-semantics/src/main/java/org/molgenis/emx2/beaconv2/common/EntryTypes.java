package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypes {

  EntryType analysis =
      new EntryType(
          "analysis",
          "analyses",
          "Bioinformatics analysis",
          "Apply analytical methods to existing data of a specific type.",
          "edam:operation_2945",
          "Analysis");
  EntryType biosample =
      new EntryType(
          "biosample",
          "biosamples",
          "Biological Sample",
          "Any material sample taken from a biological entity for testing, diagnostic, propagation, treatment or research purposes, including a sample obtained from a living organism or taken from the biological object after halting of all its life functions. Biospecimen can contain one or more components including but not limited to cellular molecules, cells, tissues, organs, body fluids, embryos, and body excretory products.",
          "NCIT:C70699",
          "Biospecimen");
  EntryType cohort =
      new EntryType(
          "cohort",
          "cohorts",
          "Cohort",
          "A group of individuals, identified by a common characteristic.",
          "NCIT:C61512",
          "Cohort");
  EntryType dataset =
      new EntryType(
          "dataset",
          "datasets",
          "Dataset",
          "A Dataset is a collection of related sets of information, e.g. genomic variations together with associated procedural and biological metadata. In a Beacon context, a datasets may consist of information generated in a specific study or project, or represent the main content of the Beacon resource.",
          "NCIT:C47824",
          "Data set");
  EntryType genomicVariant =
      new EntryType(
          "genomicVariant",
          "genomicVariations",
          "Genomic Variants",
          "The location of a sequence.",
          "ENSGLOSSARY:0000092",
          "Variant");
  EntryType individual =
      new EntryType(
          "individual",
          "individuals",
          "Individual",
          "A human being. It could be a Patient, a Tissue Donor, a Participant, a Human Study Subject, etc.",
          "NCIT:C25190",
          "Person");
  EntryType run =
      new EntryType(
          "run",
          "runs",
          "Sequencing run",
          "The valid and completed operation of a high-throughput sequencing instrument for a single sequencing process. ",
          "NCIT:C148088",
          "Sequencing run");

  // TODO: others e.g. genomic variant that follow the same structure as ConfigurationDataset
  // except for aCollectionOf in Dataset that announces the presence of these 'others'
}
