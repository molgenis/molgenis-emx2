# BBMRI-ERIC Data Quality Flags - Summary

Total ledger rows: 29013

Rows with >=1 data_quality_error: 1084 (3.7%)


## Flagged rows by existing disposition

| disposition | count |
|---|---|
| auto | 906 |
| needs_curation | 143 |
| dropped | 35 |

## Per-check trigger counts (not row-deduplicated; a row can trigger a check more than once)

| check | triggers |
|---|---|
| SuspiciousContent | 498 |
| GeoCoordsWithinCountry | 189 |
| ContactFieldsMeaningful | 155 |
| SubcollectionTotalsExceedParent | 88 |
| IDValidator | 84 |
| TextConsistencyFFPE | 55 |
| JuridicalPersonIsPerson | 38 |
| NoCollections | 26 |

## Examples per check


### JuridicalPersonIsPerson

- `Biobanks` / `bbmri-eric:ID:FR_BB-0033-00033` -- juridical_person 'Cerebrotheque Toulouse' has person-name shape (2-3 capitalized tokens, no institute keyword) -- verify
- `Biobanks` / `bbmri-eric:ID:CH_ETH` -- juridical_person 'Eidgenössisch Technische Hochschule' has person-name shape (2-3 capitalized tokens, no institute keyword) -- verify
- `Biobanks` / `bbmri-eric:ID:DE_CIMD` -- juridical_person 'Fraunhofer Gesellschaft' has person-name shape (2-3 capitalized tokens, no institute keyword) -- verify
- `Biobanks` / `bbmri-eric:ID:DE_BMBN` -- juridical_person 'Forschungszentrum Borstel' has person-name shape (2-3 capitalized tokens, no institute keyword) -- verify
- `Biobanks` / `bbmri-eric:ID:DE_UGMLC-Giessen` -- juridical_person 'Dr. Clemens Ruppert' exactly matches a Persons.full_name on file -- verify
- `Biobanks` / `bbmri-eric:ID:EXT_UNB` -- juridical_person 'Dr. Isaac Ssewanyana' exactly matches a Persons.full_name on file -- verify
- `Biobanks` / `bbmri-eric:ID:DE_HUB` -- juridical_person 'Medizinische Hochschule Hannover' has person-name shape (2-3 capitalized tokens, no institute keyword) -- verify
- `Biobanks` / `bbmri-eric:ID:EXT_TissueSolutions` -- juridical_person 'Tissue Solutions' has person-name shape (2-3 capitalized tokens, no institute keyword) -- verify

### SuspiciousContent

- `Biobanks` / `bbmri-eric:ID:EXT_SCBBC` -- description: suspiciously short (<3 words): 'Hospital Integrated'
- `Biobanks` / `bbmri-eric:ID:FR_BB-0033-00096` -- description: placeholder value '-'
- `Biobanks` / `bbmri-eric:ID:DE_Lungenbiobank` -- description: suspiciously short (<3 words): 'Thoracic Diseases'
- `Biobanks` / `bbmri-eric:ID:NO_MoBa` -- description: suspiciously short (<3 words): 'https://www.fhi.no/en/hd/biobanks/'
- `Biobanks` / `bbmri-eric:ID:NO_NIPH` -- description: suspiciously short (<3 words): 'https://www.fhi.no/en/hd/biobanks/'
- `Biobanks` / `bbmri-eric:ID:NO_VetIns` -- description: suspiciously short (<3 words): 'https://www.vetinst.no/en/news/biobanking-at-the-norwegian-veterinary-institute'
- `Biobanks` / `bbmri-eric:ID:NL_AAAACXPAUCGZWACQK2ME25QAAE` -- juridical_person: placeholder value 'No information'
- `Biobanks` / `bbmri-eric:ID:NL_AAAACXPKUTPDUACQK2ME25QAAE` -- juridical_person: placeholder value 'No information'

### IDValidator

- `Collections` / `bbmri-eric:ID:NO_moba:collection:all_samples` -- id-embedded biobank prefix 'bbmri-eric:ID:NO_moba' is stale -- collection's actual biobank FK is 'bbmri-eric:ID:NO_NIPH' (id was never regenerated after a biobank id change)
- `Collections` / `bbmri-eric:ID:DE_P2N-Popgen:collection:BMB-CCC` -- id-embedded biobank prefix 'bbmri-eric:ID:DE_P2N-Popgen' is stale -- collection's actual biobank FK is 'bbmri-eric:ID:DE_BMB-CCCKiel' (id was never regenerated after a biobank id change)
- `Collections` / `bbmri-eric:ID:CH_HUG:collection:CH_Screen-RA` -- id-embedded biobank prefix 'bbmri-eric:ID:CH_HUG' is stale -- collection's actual biobank FK is 'bbmri-eric:ID:CH_HopitauxUniversitairesGeneve' (id was never regenerated after a biobank id change)
- `Collections` / `bbmri-eric:ID:DE_BMBH:collection:Biobank_Haut` -- id-embedded biobank prefix 'bbmri-eric:ID:DE_BMBH' is stale -- collection's actual biobank FK is 'bbmri-eric:ID:DE_Biobank_Haut' (id was never regenerated after a biobank id change)
- `Collections` / `bbmri-eric:ID:DE_BMBH:collection:Biobank_Kinderkardiologie` -- id-embedded biobank prefix 'bbmri-eric:ID:DE_BMBH' is stale -- collection's actual biobank FK is 'bbmri-eric:ID:DE_Biobank_Kinderkardiologie' (id was never regenerated after a biobank id change)
- `Collections` / `bbmri-eric:ID:DE_BMBH:collection:NCT_CLB_POP-HIPO` -- id-embedded biobank prefix 'bbmri-eric:ID:DE_BMBH' is stale -- collection's actual biobank FK is 'bbmri-eric:ID:DE_NCT-CLB' (id was never regenerated after a biobank id change)
- `Collections` / `bbmri-eric:ID:DE_KDEB:collection:UCCH-Biobank` -- id-embedded biobank prefix 'bbmri-eric:ID:DE_KDEB' is stale -- collection's actual biobank FK is 'bbmri-eric:ID:DE_BMBUKE' (id was never regenerated after a biobank id change)
- `Collections` / `bbmri-eric:ID:DE_BMBH:collection:COVID19_tissue` -- id-embedded biobank prefix 'bbmri-eric:ID:DE_BMBH' is stale -- collection's actual biobank FK is 'bbmri-eric:ID:DE_DZIF-GB' (id was never regenerated after a biobank id change)

### GeoCoordsWithinCountry

- `Biobanks` / `bbmri-eric:ID:AT_MUI` -- coordinates present but unparseable: lat='47,259917', lon='11,387718'
- `Biobanks` / `bbmri-eric:ID:AT_MUW` -- coordinates present but unparseable: lat='48,220937', lon='16,345041'
- `Biobanks` / `bbmri-eric:ID:DE_UGMLC-Giessen` -- coordinates present but unparseable: lat='N50 34.275', lon='E8 39.889'
- `Biobanks` / `bbmri-eric:ID:DE_UTZBD` -- only one of latitude/longitude provided (lat='51.19574735414634, 6.789824456332765', lon=None)
- `Biobanks` / `bbmri-eric:ID:DE_BBMUL-CT` -- coordinates present but unparseable: lat='5.174.357', lon='1.432.101'
- `Biobanks` / `bbmri-eric:ID:AT_VET` -- coordinates present but unparseable: lat='48,254580', lon='16,431660'
- `Biobanks` / `bbmri-eric:ID:IT_1384353239378890` -- coordinates present but unparseable: lat='43°46′45″ N', lon='11°14′46″ E'
- `Biobanks` / `bbmri-eric:ID:EXT_GBR-1-198` -- coordinates present but unparseable: lat='54°58\'05.6"N', lon='1°37\'16.6"W'

### NoCollections

- `Biobanks` / `bbmri-eric:ID:DE_UDS_Biobank_MedicalFaculty` -- biobank has zero collections (CEX:NoCollections)
- `Biobanks` / `bbmri-eric:ID:DE_BBMUL-CT` -- biobank has zero collections (CEX:NoCollections)
- `Biobanks` / `bbmri-eric:ID:DE_BOWL` -- biobank has zero collections (CEX:NoCollections)
- `Biobanks` / `bbmri-eric:ID:NO_BiobankHaukeland` -- biobank has zero collections (CEX:NoCollections)
- `Biobanks` / `bbmri-eric:ID:NO_VetIns` -- biobank has zero collections (CEX:NoCollections)
- `Biobanks` / `bbmri-eric:ID:SE_1718` -- biobank has zero collections (CEX:NoCollections)
- `Biobanks` / `bbmri-eric:ID:SE_1789` -- biobank has zero collections (CEX:NoCollections)
- `Biobanks` / `bbmri-eric:ID:SE_1756` -- biobank has zero collections (CEX:NoCollections)

### SubcollectionTotalsExceedParent

- `Collections` / `bbmri-eric:ID:AT_MUW:collection:985398549856-MECFSBiobankAustria` -- sum of direct sub-collection donor counts (735) exceeds parent number_of_donors (93) (CP:DonorOver)
- `Collections` / `bbmri-eric:ID:AT_MUW:collection:9635-IMPROVE-LIP` -- sum of direct sub-collection donor counts (2728) exceeds parent number_of_donors (914) (CP:DonorOver)
- `Collections` / `bbmri-eric:ID:AT_MUW:collection:9836-AROSprojectplus` -- sum of direct sub-collection donor counts (34) exceeds parent number_of_donors (17) (CP:DonorOver)
- `Collections` / `bbmri-eric:ID:AT_MUW:collection:9727-Atrialfibrillation` -- sum of direct sub-collection donor counts (570) exceeds parent number_of_donors (115) (CP:DonorOver)
- `Collections` / `bbmri-eric:ID:AT_MUW:collection:9741-Aorticstenosis` -- sum of direct sub-collection donor counts (1936) exceeds parent number_of_donors (648) (CP:DonorOver)
- `Collections` / `bbmri-eric:ID:AT_MUW:collection:BB9271` -- sum of direct sub-collection donor counts (2633) exceeds parent number_of_donors (535) (CP:DonorOver)
- `Collections` / `bbmri-eric:ID:AT_MUW:collection:BB9067` -- sum of direct sub-collection donor counts (59115) exceeds parent number_of_donors (14876) (CP:DonorOver)
- `Collections` / `bbmri-eric:ID:AT_MUW:collection:9787-CGM-Diabetes-Register` -- sum of direct sub-collection donor counts (737) exceeds parent number_of_donors (246) (CP:DonorOver)

### TextConsistencyFFPE

- `Collections` / `bbmri-eric:ID:DE_zbiobmd:collection:blood` -- name/description mentions FFPE/paraffin but materials includes neither TISSUE_PARAFFIN_EMBEDDED nor TISSUE_STAINED (materials='') (TXT:FFPEMaterial)
- `Collections` / `bbmri-eric:ID:DE_KDEB:collection:UCCH-Biobank` -- name/description mentions FFPE/paraffin but materials includes neither TISSUE_PARAFFIN_EMBEDDED nor TISSUE_STAINED (materials='BUFFY_COAT,PERIPHERAL_BLOOD_CELLS,PLASMA,SERUM,DNA,RNA,TISSUE_FROZEN') (TXT:FFPEMaterial)
- `Collections` / `bbmri-eric:ID:AT_MUI:collection:45` -- name/description mentions FFPE/paraffin but materials includes neither TISSUE_PARAFFIN_EMBEDDED nor TISSUE_STAINED (materials='TISSUE_FROZEN') (TXT:FFPEMaterial)
- `Collections` / `bbmri-eric:ID:UK_GBR-1-156:collection:371-1044` -- name/description mentions FFPE/paraffin but materials includes neither TISSUE_PARAFFIN_EMBEDDED nor TISSUE_STAINED (materials='TISSUE_FROZEN') (TXT:FFPEMaterial)
- `Collections` / `bbmri-eric:ID:UK_GBR-1-51:collection:533-1557` -- name/description mentions FFPE/paraffin but materials includes neither TISSUE_PARAFFIN_EMBEDDED nor TISSUE_STAINED (materials='SERUM') (TXT:FFPEMaterial)
- `Collections` / `bbmri-eric:ID:UK_GBR-1-51:collection:533-1555` -- name/description mentions FFPE/paraffin but materials includes neither TISSUE_PARAFFIN_EMBEDDED nor TISSUE_STAINED (materials='PLASMA') (TXT:FFPEMaterial)
- `Collections` / `bbmri-eric:ID:UK_GBR-1-134:collection:1` -- name/description mentions FFPE/paraffin but materials includes neither TISSUE_PARAFFIN_EMBEDDED nor TISSUE_STAINED (materials='') (TXT:FFPEMaterial)
- `Collections` / `bbmri-eric:ID:NL_AAAACXSW447PSACQK2MBZ5YAAM:collection:AAAACXVECGGBUACQK2MBZ5YAAE` -- name/description mentions FFPE/paraffin but materials includes neither TISSUE_PARAFFIN_EMBEDDED nor TISSUE_STAINED (materials='OTHER') (TXT:FFPEMaterial)

### ContactFieldsMeaningful

- `Persons` / `bbmri-eric:contactID:FR_gardette` -- email uses placeholder domain: 'unknown@unknown.fr' (CTF:EmailPlaceholder)
- `Persons` / `bbmri-eric:contactID:EXT_unknown` -- email uses placeholder domain: 'unknown@unknown.ext' (CTF:EmailPlaceholder)
- `Persons` / `bbmri-eric:contactID:DE_Draenert` -- email uses placeholder domain: 'draenert@unknown.de' (CTF:EmailPlaceholder)
- `Persons` / `bbmri-eric:contactID:DE_Hoffmann` -- email uses placeholder domain: 'hoffmann@unknown.de' (CTF:EmailPlaceholder)
- `Persons` / `bbmri-eric:contactID:DE_Schetelig` -- email uses placeholder domain: 'schetelig@unknown.de' (CTF:EmailPlaceholder)
- `Persons` / `bbmri-eric:contactID:DE_Isenberg` -- email uses placeholder domain: 'isenberg@unknown.de' (CTF:EmailPlaceholder)
- `Persons` / `bbmri-eric:contactID:DE_Corman` -- email uses placeholder domain: 'corman@unknown.de' (CTF:EmailPlaceholder)
- `Persons` / `bbmri-eric:contactID:DE_Kiess` -- email uses placeholder domain: 'kiess@unknown.de' (CTF:EmailPlaceholder)

## Checks from Section 5 NOT implemented (need live/cross-schema state)


- **5.1 AI:Curated** -- depends on an external `ai-check-cache` repository of previously
  AI-reviewed findings; not deterministic/static, no cache available here.
- **5.2 Access policies (AP:\*)** -- DUO/access-policy conflicts require the DUO ontology
  term catalogue (`{DUO_terms_research}`, `DUOs_to_url(...)`) which is not part of the
  extracted dump; `data_use`/`access_*` semantics can't be validated without it.
- **5.3 BBMRI Cohorts checks (BCO:\*)** -- require the live `BBMRICohortsList`/network
  membership semantics maintained centrally; the extracted `network` column alone isn't
  a reliable stand-in for "is this collection in the BBMRI Cohorts programme".
- **5.6 COVID-19 checks (C19:\*)** -- depend on the COVID-19 network flag semantics and a
  `covid19` biobank-capabilities attribute that isn't present in this extract's Biobanks
  columns; skipped rather than guessed.
- **5.8 Content advertised by collections, most of CC:\*** (SizeOoMMismatch, LargeNoSubcoll,
  DiagMissingDisease, RD/ORPHA checks, age-range/age-unit checks, imaging checks) --
  doable in principle but require the MIABIS ontology code lists (material/data category/
  ORPHA<->ICD mapping tables) to validate against; only the coarse geo/URL/ID/suspicious-content
  and CP:\*/CEX:\*/OC:\* checks were implemented from this section family.
- **5.11 Suspicious cross-biobank contacts (CTA:\*)** and **5.13 CTR:CrossBiobankReuse** --
  need institutional email-domain -> biobank-country reference data (beyond what's in the
  dump) to avoid false positives; only the simpler 5.12 email-shape check was implemented.
- **5.14 Fact-table checks (FT:\*)** -- most require the "all-star" k-anonymity aggregate
  row; only 7 all-star rows exist in this dump (see below), too few to meaningfully validate
  FT:AllStarMissing/OneStarMissing/KAnonViolation etc., so these were skipped; the
  Collections.size/number_of_donors vs sub_collection totals (CP:\*) were implemented instead
  since they have much better coverage (140 parents with complete child data).
- **5.15 Member-country/non-member area checks (MAC:\*)** -- these validate which BBMRI
  *staging area* (member vs EXT/EU) an institution's records live in; that's a property of
  the live Molgenis multi-schema deployment, not visible in a single flat CSV extract.
- **5.18 SNM:SubcollNetMissing** -- doable in principle (sub_collection network membership
  vs parent network membership) but low priority/skipped in this pass; can be added on request.
- **5.19 TXT:AgeRange / TXT:StudyType / TXT:CovidDiag** -- would need lightweight NLP/keyword
  heuristics on free text; only TXT:FFPEMaterial (regex-clean) was implemented as the
  representative deterministic example from this family.

