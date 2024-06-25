package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import java.util.List;

public record GenomicVariant(
    String variantInternalId,
    String variantType,
    String referenceBases,
    String alternateBases,
    Position position,
    String geneId,
    String genomicHGVSId,
    List<String> proteinHGVSIds,
    List<String> transcriptHGVSIds,
    VariantLevelData variantLevelData,
    List<CaseLevelData> caseLevelData,
    String GenomicVariantsResultSetId) {}
