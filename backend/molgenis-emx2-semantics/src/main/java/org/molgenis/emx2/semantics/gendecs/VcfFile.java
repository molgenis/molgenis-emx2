package org.molgenis.emx2.semantics.gendecs;

import java.io.BufferedWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VcfFile {
  private static final Logger logger = LoggerFactory.getLogger(VcfFile.class);

  public static String getClinvarHeader() {
    return """
                    ##fileformat=VCFv4.1
                    ##source=ClinVar
                    ##reference=GRCh37
                    ##ID=<Description="ClinVar Variation ID">
                    ##INFO=<ID=AF_ESP,Number=1,Type=Float,Description="allele frequencies from GO-ESP">
                    ##INFO=<ID=AF_EXAC,Number=1,Type=Float,Description="allele frequencies from ExAC">
                    ##INFO=<ID=AF_TGP,Number=1,Type=Float,Description="allele frequencies from TGP">
                    ##INFO=<ID=ALLELEID,Number=1,Type=Integer,Description="the ClinVar Allele ID">
                    ##INFO=<ID=CLNDN,Number=.,Type=String,Description="ClinVar's preferred disease name for the concept specified by disease identifiers in CLNDISDB">
                    ##INFO=<ID=CLNDNINCL,Number=.,Type=String,Description="For included Variant : ClinVar's preferred disease name for the concept specified by disease identifiers in CLNDISDB">
                    ##INFO=<ID=CLNDISDB,Number=.,Type=String,Description="Tag-value pairs of disease database name and identifier, e.g. OMIM:NNNNNN">
                    ##INFO=<ID=CLNDISDBINCL,Number=.,Type=String,Description="For included Variant: Tag-value pairs of disease database name and identifier, e.g. OMIM:NNNNNN">
                    ##INFO=<ID=CLNHGVS,Number=.,Type=String,Description="Top-level (primary assembly, alt, or patch) HGVS expression.">
                    ##INFO=<ID=CLNREVSTAT,Number=.,Type=String,Description="ClinVar review status for the Variation ID">
                    ##INFO=<ID=CLNSIG,Number=.,Type=String,Description="Clinical significance for this single variant">
                    ##INFO=<ID=CLNSIGCONF,Number=.,Type=String,Description="Conflicting clinical significance for this single variant">
                    ##INFO=<ID=CLNSIGINCL,Number=.,Type=String,Description="Clinical significance for a haplotype or genotype that includes this variant. Reported as pairs of VariationID:clinical significance.">
                    ##INFO=<ID=CLNVC,Number=1,Type=String,Description="Variant type">
                    ##INFO=<ID=CLNVCSO,Number=1,Type=String,Description="Sequence Ontology id for variant type">
                    ##INFO=<ID=CLNVI,Number=.,Type=String,Description="the variant's clinical sources reported as tag-value pairs of database and variant identifier">
                    ##INFO=<ID=DBVARID,Number=.,Type=String,Description="nsv accessions from dbVar for the variant">
                    ##INFO=<ID=GENEINFO,Number=1,Type=String,Description="Gene(s) for the variant reported as gene symbol:gene id. The gene symbol and id are delimited by a colon (:) and each pair is delimited by a vertical bar (|)">
                    ##INFO=<ID=MC,Number=.,Type=String,Description="comma separated list of molecular consequence in the form of Sequence Ontology ID|molecular_consequence">
                    ##INFO=<ID=ORIGIN,Number=.,Type=String,Description="Allele origin. One or more of the following values may be added: 0 - unknown; 1 - germline; 2 - somatic; 4 - inherited; 8 - paternal; 16 - maternal; 32 - de-novo; 64 - biparental; 128 - uniparental; 256 - not-tested; 512 - tested-inconclusive; 1073741824 - other">
                    ##INFO=<ID=RS,Number=.,Type=String,Description="dbSNP ID (i.e. rs number)">
                    ##INFO=<ID=SSR,Number=1,Type=Integer,Description="Variant Suspect Reason Codes. One or more of the following values may be added: 0 - unspecified, 1 - Paralog, 2 - byEST, 4 - oldAlign, 8 - Para_EST, 16 - 1kg_failed, 1024 - other">
                    #CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO
                    """;
  }

  public static String getVipHeader() {
    return """
            ##fileformat=VCFv4.2
            ##FILTER=<ID=PASS,Description="All filters passed">
            ##CAPICE_CL=CAPICE classification
            ##CAPICE_SC=CAPICE score
            ##INFO=<ID=CSQ,Number=.,Type=String,Description="Consequence annotations from Ensembl VEP. Format: Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|ALLELE_NUM|DISTANCE|STRAND|FLAGS|PICK|SYMBOL_SOURCE|HGNC_ID|REFSEQ_MATCH|REFSEQ_OFFSET|SOURCE|SIFT|PolyPhen|HGVS_OFFSET|CLIN_SIG|SOMATIC|PHENO|PUBMED|CHECK_REF|MOTIF_NAME|MOTIF_POS|HIGH_INF_POS|MOTIF_SCORE_CHANGE|TRANSCRIPTION_FACTORS|SpliceAI_pred_DP_AG|SpliceAI_pred_DP_AL|SpliceAI_pred_DP_DG|SpliceAI_pred_DP_DL|SpliceAI_pred_DS_AG|SpliceAI_pred_DS_AL|SpliceAI_pred_DS_DG|SpliceAI_pred_DS_DL|SpliceAI_pred_SYMBOL|CAPICE_CL|CAPICE_SC|IncompletePenetrance|InheritanceModesGene|VKGL_CL|gnomAD|gnomAD_AF|gnomAD_HN">
            ##INFO=<ID=VIPC,Number=A,Type=String,Description="VIP decision tree classification">
            ##INFO=<ID=VIPL,Number=A,Type=String,Description="VIP decision tree labels (pipe separated)">
            ##INFO=<ID=VIPP,Number=A,Type=String,Description="VIP decision tree path (pipe separated)">
            ##INFO=<ID=gnomAD,Number=.,Type=String,Description="/groups/solve-rd/tmp10/projects/vip/git/vip/resources/GRCh37/gnomad.total.r2.1.1.sites.stripped.vcf.gz (exact)">
            ##INFO=<ID=gnomAD_AF,Number=.,Type=String,Description="AF field from /groups/solve-rd/tmp10/projects/vip/git/vip/resources/GRCh37/gnomad.total.r2.1.1.sites.stripped.vcf.gz">
            ##INFO=<ID=gnomAD_HN,Number=.,Type=String,Description="HN field from /groups/solve-rd/tmp10/projects/vip/git/vip/resources/GRCh37/gnomad.total.r2.1.1.sites.stripped.vcf.gz">
            ##IncompletePenetrance=Boolean indicating if the gene is known for incomplete penetrance.
            ##InheritanceModesGene=List of inheritance modes for the gene
            ##SpliceAI_pred_DP_AG=SpliceAI predicted effect on splicing. Delta position for acceptor gain
            ##SpliceAI_pred_DP_AL=SpliceAI predicted effect on splicing. Delta position for acceptor loss
            ##SpliceAI_pred_DP_DG=SpliceAI predicted effect on splicing. Delta position for donor gain
            ##SpliceAI_pred_DP_DL=SpliceAI predicted effect on splicing. Delta position for donor loss
            ##SpliceAI_pred_DS_AG=SpliceAI predicted effect on splicing. Delta score for acceptor gain
            ##SpliceAI_pred_DS_AL=SpliceAI predicted effect on splicing. Delta score for acceptor loss
            ##SpliceAI_pred_DS_DG=SpliceAI predicted effect on splicing. Delta score for donor gain
            ##SpliceAI_pred_DS_DL=SpliceAI predicted effect on splicing. Delta score for donor loss
            ##SpliceAI_pred_SYMBOL=SpliceAI gene symbol
            ##VEP="v105" time="2022-03-07 13:21:22" cache="/groups/solve-rd/tmp10/projects/vip/git/vip/resources/vep/cache/homo_sapiens_refseq/105_GRCh37" ensembl-io=105.2a0a40c ensembl=105.525fbcb ensembl-funcgen=105.660df8f ensembl-variation=105.ac8178e 1000genomes="phase3" COSMIC="92" ClinVar="202012" HGMD-PUBLIC="20204" assembly="GRCh37.p13" dbSNP="154" gencode="GENCODE 19" genebuild="2011-04" gnomAD="r2.1" polyphen="2.2.2" refseq="2020-10-26 17:03:42 - GCF_000001405.25_GRCh37.p13_genomic.gff" regbuild="1.0" sift="sift5.2.2"
            ##VIP_Command=nextflow -log /groups/solve-rd/tmp10/projects/vip/git/vip/test/output/test_lp/.nxf.log run --assembly GRCh37 --input /groups/solve-rd/tmp10/projects/vip/git/vip/test/resources/lp.vcf.gz --output /groups/solve-rd/tmp10/projects/vip/git/vip/test/output/test_lp --GRCh37_annotate_vep_plugin_vkgl /groups/solve-rd/tmp10/projects/vip/git/vip/test/resources/vkgl_public_consensus_empty.tsv /groups/solve-rd/tmp10/projects/vip/git/vip/test/../main.nf
            ##VIP_Version=4.0.0
            ##VIP_treeCommand=--input lp_chunk0_annotated.vcf.gz --config /groups/solve-rd/tmp10/projects/vip/git/vip/resources/decision_tree.json --labels 0 --path 0 --output lp_chunk0_classified.vcf.gz
            ##VIP_treeVersion=2.2.0
            ##VKGL_CL=VKGL consensus variant classification.
            ##contig=<ID=1,length=249250621,assembly=b37>
            ##contig=<ID=2,length=243199373,assembly=b37>
            ##contig=<ID=3,length=198022430,assembly=b37>
            ##contig=<ID=4,length=191154276,assembly=b37>
            ##contig=<ID=5,length=180915260,assembly=b37>
            ##contig=<ID=6,length=171115067,assembly=b37>
            ##contig=<ID=7,length=159138663,assembly=b37>
            ##contig=<ID=8,length=146364022,assembly=b37>
            ##contig=<ID=9,length=141213431,assembly=b37>
            ##contig=<ID=10,length=135534747,assembly=b37>
            ##contig=<ID=11,assembly=b37,length=135006516>
            ##contig=<ID=12,length=133851895,assembly=b37>
            ##contig=<ID=13,assembly=b37,length=115169878>
            ##contig=<ID=14,length=107349540,assembly=b37>
            ##contig=<ID=15,assembly=b37,length=102531392>
            ##contig=<ID=16,length=90354753,assembly=b37>
            ##contig=<ID=17,assembly=b37,length=81195210>
            ##contig=<ID=18,length=78077248,assembly=b37>
            ##contig=<ID=19,length=59128983,assembly=b37>
            ##contig=<ID=20,length=63025520,assembly=b37>
            ##contig=<ID=21,length=48129895,assembly=b37>
            ##contig=<ID=22,assembly=b37,length=51304566>
            ##contig=<ID=X,assembly=b37,length=155270560>
            ##contig=<ID=Y,length=59373566,assembly=b37>
            ##contig=<ID=MT,assembly=b37,length=16569>
            #CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO
            """;
  }

  public static void writeHeader(BufferedWriter writer, String headerType) throws IOException {
    if (headerType.equals("clinvar")) {
      for (String line : VcfFile.getClinvarHeader().split("\n")) {
        writer.write(line + System.getProperty("line.separator"));
      }
    } else if (headerType.equals("result")) {
      for (String line : VcfFile.getVipHeader().split("\n")) {
        writer.write(line + System.getProperty("line.separator"));
      }
    } else {
      logger.info("invalid header argument " + headerType);
    }
  }
}
