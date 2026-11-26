// Generated (on: 2026-11-17T14:17:03.909025) from Generator.java for schema: catalogue-demo

export interface IMgTableClass {
  mg_tableclass?: string;
}

export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  filename?: string;
  url?: string;
}

export interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: {
    name: string;
  };
}

export interface IOntologyNode extends ITreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
  order?: number;
}

export interface ICatalogueOntologies_AccessRights extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_AccessRights;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_AccessRights[];
}

export interface ICatalogueOntologies_AccessRights_agg {
  count: number;
}

export interface ICatalogueOntologies_AgeGroups extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_AgeGroups;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_AgeGroups[];
}

export interface ICatalogueOntologies_AgeGroups_agg {
  count: number;
}

export interface ICatalogueOntologies_AgeUnits extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_AgeUnits;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_AgeUnits[];
}

export interface ICatalogueOntologies_AgeUnits_agg {
  count: number;
}

export interface ICatalogueOntologies_AnatomicalLocation extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_AnatomicalLocation;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_AnatomicalLocation[];
}

export interface ICatalogueOntologies_AnatomicalLocation_agg {
  count: number;
}

export interface ICatalogueOntologies_AreasOfInformationCohorts
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_AreasOfInformationCohorts;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_AreasOfInformationCohorts[];
}

export interface ICatalogueOntologies_AreasOfInformationCohorts_agg {
  count: number;
}

export interface ICatalogueOntologies_AreasOfInformationDs
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_AreasOfInformationDs;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_AreasOfInformationDs[];
}

export interface ICatalogueOntologies_AreasOfInformationDs_agg {
  count: number;
}

export interface ICatalogueOntologies_CatalogueTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_CatalogueTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_CatalogueTypes[];
}

export interface ICatalogueOntologies_CatalogueTypes_agg {
  count: number;
}

export interface ICatalogues extends IMgTableClass {
  hricore?: boolean;
  id: string;
  pid?: string;
  name: string;
  localName?: string;
  acronym?: string;
  catalogueType?: IOntologyNode;
  mainCatalogue?: boolean;
  networkType?: IOntologyNode[];
  commonCollaborationTopics?: IOntologyNode[];
  description?: string;
  website?: string;
  contactEmail?: string;
  logo?: IFile;
  startYear?: number;
  endYear?: number;
  withdrawn?: boolean;
  dataResources?: ICollections[];
  countries?: IOntologyNode[];
  organisationsInvolved?: IOrganisationRoles[];
  publisher?: IOrganisationRoles;
  creator?: IOrganisationRoles[];
  peopleInvolved?: IContacts[];
  contactPoint?: IContacts;
  childNetworks?: INetworks[];
  parentNetworks?: INetworks[];
  tables?: ITables[];
  publications?: IPublications[];
  fundingStatement?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
  applicableLegislation?: IOntologyNode[];
  fdpEndpoint?: IEndpoint;
  ldpMembershipRelation?: string;
  hasMemberRelation?: string;
  conformsTo?: string;
}

export interface ICatalogues_agg {
  count: number;
}

export interface ICatalogueOntologies_ClinicalStudyTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ClinicalStudyTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ClinicalStudyTypes[];
}

export interface ICatalogueOntologies_ClinicalStudyTypes_agg {
  count: number;
}

export interface ICatalogueOntologies_CohortCollectionTypes
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_CohortCollectionTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_CohortCollectionTypes[];
}

export interface ICatalogueOntologies_CohortCollectionTypes_agg {
  count: number;
}

export interface ICatalogueOntologies_CohortDesigns extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_CohortDesigns;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_CohortDesigns[];
}

export interface ICatalogueOntologies_CohortDesigns_agg {
  count: number;
}

export interface ICatalogueOntologies_CohortStudyTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_CohortStudyTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_CohortStudyTypes[];
}

export interface ICatalogueOntologies_CohortStudyTypes_agg {
  count: number;
}

export interface ICollectionCounts extends IMgTableClass {
  id: string;
  collection: ICollections;
  sex?: IOntologyNode;
  ageGroup?: IOntologyNode;
  disease?: IOntologyNode;
  sampleType?: IOntologyNode;
  anatomy?: IOntologyNode;
  numberOfSamples?: number;
  numberOfDonors?: number;
  lastUpdate?: string;
}

export interface ICollectionCounts_agg {
  count: number;
}

export interface ICollectionEvents extends IMgTableClass {
  resource: IResources;
  name: string;
  pid?: string;
  description?: string;
  subpopulations?: ISubpopulations[];
  keywords?: string[];
  startDate?: string;
  endDate?: string;
  ageGroups?: IOntologyNode[];
  numberOfParticipants?: number;
  areasOfInformation?: IOntologyNode[];
  dataCategories?: IOntologyNode[];
  sampleCategories?: IOntologyNode[];
  standardizedTools?: IOntologyNode[];
  standardizedToolsOther?: string;
  coreVariables?: string[];
  contactPoint?: IContacts;
  publisher?: IOrganisationRoles;
  creator?: IOrganisationRoles[];
  issued?: string;
  modified?: string;
  theme?: IOntologyNode[];
  accessRights?: IOntologyNode;
  applicableLegislation?: IOntologyNode[];
  provenanceStatement?: string;
}

export interface ICollectionEvents_agg {
  count: number;
}

export interface ICollections extends IMgTableClass {
  hricore?: boolean;
  id: string;
  pid?: string;
  name: string;
  localName?: string;
  acronym?: string;
  type: IOntologyNode[];
  cohortType?: IOntologyNode[];
  clinicalStudyType?: IOntologyNode[];
  registryOrHealthRecordType?: IOntologyNode[];
  description?: string;
  keywords?: string[];
  website?: string;
  contactEmail?: string;
  logo?: IFile;
  startYear?: number;
  endYear?: number;
  timeSpanDescription?: string;
  status?: IOntologyNode;
  withdrawn?: boolean;
  linkedResources?: ILinkages[];
  internalIdentifiers?: IInternalIdentifiers[];
  externalIdentifiers?: IExternalIdentifiers[];
  design?: IOntologyNode;
  dataCollectionType?: IOntologyNode[];
  designDescription?: string;
  designSchematic?: IFile;
  subpopulations?: ISubpopulations[];
  collectionEvents?: ICollectionEvents[];
  dataCollectionDescription?: string;
  reasonSustained?: string;
  recordTrigger?: string;
  unitOfObservation?: string;
  partOfNetworks?: INetworks[];
  numberOfParticipants?: number;
  numberOfParticipantsWithSamples?: number;
  underlyingPopulation?: string;
  populationOfInterest?: IOntologyNode[];
  populationOfInterestOther?: string;
  countries?: IOntologyNode[];
  continents?: IOntologyNode[];
  regions?: IOntologyNode[];
  populationAgeGroups?: IOntologyNode[];
  ageMin?: number;
  ageMax?: number;
  ageUnit?: IOntologyNode;
  sex?: IOntologyNode[];
  inclusionCriteria?: IOntologyNode[];
  otherInclusionCriteria?: string;
  exclusionCriteria?: IOntologyNode[];
  otherExclusionCriteria?: string;
  populationEntry?: IOntologyNode[];
  populationEntryOther?: string;
  populationExit?: IOntologyNode[];
  populationExitOther?: string;
  populationDisease?: IOntologyNode[];
  populationOncologyTopology?: IOntologyNode[];
  populationOncologyMorphology?: IOntologyNode[];
  populationCoverage?: string;
  populationNotCovered?: string;
  organisationsInvolved?: IOrganisationRoles[];
  publisher?: IOrganisationRoles;
  creator?: IOrganisationRoles[];
  custodian?: IOrganisationalUnits[];
  peopleInvolved?: IContacts[];
  contactPoint?: IContacts;
  tables?: ITables[];
  counts?: ICollectionCounts[];
  numberOfRecords?: number;
  areasOfInformation?: IOntologyNode[];
  areasOfInformationRwd?: IOntologyNode[];
  qualityOfLifeOther?: string;
  causeOfDeathCodeOther?: string;
  indicationVocabularyOther?: string;
  geneticDataVocabularyOther?: string;
  careSettingOther?: string;
  medicinalProductVocabularyOther?: string;
  prescriptionsVocabularyOther?: string;
  dispensingsVocabularyOther?: string;
  proceduresVocabularyOther?: string;
  biomarkerDataVocabularyOther?: string;
  diagnosisMedicalEventVocabularyOther?: string;
  dataDictionaryAvailable?: boolean;
  sampleType?: IOntologyNode[];
  bodyPart?: IOntologyNode[];
  imagingModality?: string[];
  storageTemperatures?: IOntologyNode[];
  sampleSource?: string;
  sampleCollectionSetting?: string;
  numberOfSamples?: number;
  languages?: IOntologyNode[];
  multipleEntries?: boolean;
  hasIdentifier?: boolean;
  identifierDescription?: string;
  prelinked?: boolean;
  linkageOptions?: string;
  linkagePossibility?: boolean;
  dataAccessConditions?: IOntologyNode[];
  dataUseConditions?: IOntologyNode[];
  dataAccessConditionsDescription?: string;
  dataAccessFee?: boolean;
  informedConsentType?: IOntologyNode;
  informedConsentRequired?: IOntologyNode;
  informedConsentOther?: string;
  accessRights?: IOntologyNode;
  accessIdentifiableData?: string;
  accessIdentifiableDataRoute?: string;
  accessSubjectDetails?: boolean;
  accessSubjectDetailsRoute?: string;
  accessThirdParty?: boolean;
  accessThirdPartyConditions?: string;
  accessNonEU?: boolean;
  accessNonEUConditions?: string;
  biospecimenAccess?: boolean;
  biospecimenAccessConditions?: string;
  governanceDetails?: string;
  approvalForPublication?: boolean;
  releaseType?: IOntologyNode;
  releaseDescription?: string;
  releaseFrequency?: number;
  refreshTime?: number;
  lagTime?: number;
  refreshPeriod?: IOntologyNode[];
  dateLastRefresh?: string;
  preservation?: boolean;
  preservationDuration?: number;
  quality?: IQualityInformation[];
  standardOperatingProcedures?: boolean;
  qualification?: boolean;
  qualificationsDescription?: string;
  auditPossible?: boolean;
  completeness?: string;
  completenessOverTime?: string;
  completenessResults?: string;
  qualityDescription?: string;
  qualityOverTime?: string;
  accessForValidation?: boolean;
  qualityValidationFrequency?: string;
  qualityValidationMethods?: string;
  correctionMethods?: string;
  qualityValidationResults?: string;
  mappingsToCommonDataModels?: ITableMappings[];
  commonDataModelsOther?: string;
  eTLStandardVocabularies?: IOntologyNode[];
  eTLStandardVocabulariesOther?: string;
  publications?: IPublications[];
  fundingSources?: IOntologyNode[];
  fundingScheme?: IOntologyNode[];
  fundingStatement?: string;
  citationRequirements?: string;
  acknowledgements?: string;
  provenanceStatement?: string;
  documentation?: IDocumentation[];
  supplementaryInformation?: string;
  theme?: IOntologyNode[];
  applicableLegislation?: IOntologyNode[];
  issued?: string;
  modified?: string;
  collectionStartPlanned?: string;
  collectionStartActual?: string;
  analysisStartPlanned?: string;
  analysisStartActual?: string;
  dataSources?: IResources[];
  dataExtractionDate?: string;
  analysisPlan?: string;
  objectives?: string;
  results?: string;
}

export interface ICollections_agg {
  count: number;
}

export interface ICatalogueOntologies_CommonCollaborationTopics
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_CommonCollaborationTopics;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_CommonCollaborationTopics[];
}

export interface ICatalogueOntologies_CommonCollaborationTopics_agg {
  count: number;
}

export interface IContacts extends IMgTableClass {
  resource: IResources;
  role?: IOntologyNode[];
  roleDescription?: string;
  firstName: string;
  lastName: string;
  displayName: string;
  prefix?: string;
  initials?: string;
  title?: IOntologyNode;
  organisation?: IOrganisationRoles;
  email?: string;
  phone?: string;
  orcid?: string;
  homepage?: string;
  photo?: IFile;
  expertise?: string;
}

export interface IContacts_agg {
  count: number;
}

export interface ICatalogueOntologies_Continents extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Continents;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Continents[];
}

export interface ICatalogueOntologies_Continents_agg {
  count: number;
}

export interface ICatalogueOntologies_ContributionTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ContributionTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ContributionTypes[];
}

export interface ICatalogueOntologies_ContributionTypes_agg {
  count: number;
}

export interface ICatalogueOntologies_Countries extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Countries;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Countries[];
}

export interface ICatalogueOntologies_Countries_agg {
  count: number;
}

export interface ICatalogueOntologies_DataAccessConditions
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DataAccessConditions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DataAccessConditions[];
}

export interface ICatalogueOntologies_DataAccessConditions_agg {
  count: number;
}

export interface ICatalogueOntologies_DataCategories extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DataCategories;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DataCategories[];
}

export interface ICatalogueOntologies_DataCategories_agg {
  count: number;
}

export interface ICatalogueOntologies_DataUseConditions extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DataUseConditions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DataUseConditions[];
}

export interface ICatalogueOntologies_DataUseConditions_agg {
  count: number;
}

export interface ICatalogueOntologies_DatasourceTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DatasourceTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DatasourceTypes[];
}

export interface ICatalogueOntologies_DatasourceTypes_agg {
  count: number;
}

export interface ICatalogueOntologies_Diseases extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Diseases;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Diseases[];
}

export interface ICatalogueOntologies_Diseases_agg {
  count: number;
}

export interface ICatalogueOntologies_DocumentTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DocumentTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DocumentTypes[];
}

export interface ICatalogueOntologies_DocumentTypes_agg {
  count: number;
}

export interface IDocumentation extends IMgTableClass {
  resource: IResources;
  name: string;
  type?: IOntologyNode;
  description?: string;
  url?: string;
  file?: IFile;
}

export interface IDocumentation_agg {
  count: number;
}

export interface IEndpoint extends IMgTableClass {
  id: string;
  type: string[];
  name: string[];
  version?: string;
  description?: string;
  publisher: IOrganisationRoles[];
  language?: string[];
  license: string;
  conformsTo: string;
  rights?: string[];
  accessRights?: string[];
  contact?: IContacts;
  keyword?: string[];
  theme?: string[];
  endPointDescription?: string[];
  metadataCatalog: IResources[];
  conformsToFdpSpec: string;
  eJP_RD_personalData?: string;
  eJP_RD_vpConnection?: string;
  issued?: string;
  modified?: string;
}

export interface IEndpoint_agg {
  count: number;
}

export interface ICatalogueOntologies_ExternalIdentifierTypes
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ExternalIdentifierTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ExternalIdentifierTypes[];
}

export interface ICatalogueOntologies_ExternalIdentifierTypes_agg {
  count: number;
}

export interface IExternalIdentifiers extends IMgTableClass {
  resource: IResources;
  identifier: string;
  externalIdentifierType?: IOntologyNode;
  externalIdentifierTypeOther?: string;
}

export interface IExternalIdentifiers_agg {
  count: number;
}

export interface ICatalogueOntologies_Formats extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Formats;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Formats[];
}

export interface ICatalogueOntologies_Formats_agg {
  count: number;
}

export interface ICatalogueOntologies_FundingTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_FundingTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_FundingTypes[];
}

export interface ICatalogueOntologies_FundingTypes_agg {
  count: number;
}

export interface ICatalogueOntologies_ICDOMorphologies extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ICDOMorphologies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ICDOMorphologies[];
}

export interface ICatalogueOntologies_ICDOMorphologies_agg {
  count: number;
}

export interface ICatalogueOntologies_ICDOTopologies extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ICDOTopologies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ICDOTopologies[];
}

export interface ICatalogueOntologies_ICDOTopologies_agg {
  count: number;
}

export interface ICatalogueOntologies_InclusionExclusionCriteria
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_InclusionExclusionCriteria;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_InclusionExclusionCriteria[];
}

export interface ICatalogueOntologies_InclusionExclusionCriteria_agg {
  count: number;
}

export interface ICatalogueOntologies_InformedConsentRequired
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_InformedConsentRequired;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_InformedConsentRequired[];
}

export interface ICatalogueOntologies_InformedConsentRequired_agg {
  count: number;
}

export interface ICatalogueOntologies_InformedConsentTypes
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_InformedConsentTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_InformedConsentTypes[];
}

export interface ICatalogueOntologies_InformedConsentTypes_agg {
  count: number;
}

export interface ICatalogueOntologies_InternalIdentifierTypes
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_InternalIdentifierTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_InternalIdentifierTypes[];
}

export interface ICatalogueOntologies_InternalIdentifierTypes_agg {
  count: number;
}

export interface IInternalIdentifiers extends IMgTableClass {
  resource: IResources;
  identifier: string;
  internalIdentifierType?: IOntologyNode;
  internalIdentifierTypeOther?: string;
}

export interface IInternalIdentifiers_agg {
  count: number;
}

export interface ICatalogueOntologies_Keywords extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Keywords;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Keywords[];
}

export interface ICatalogueOntologies_Keywords_agg {
  count: number;
}

export interface ICatalogueOntologies_Languages extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Languages;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Languages[];
}

export interface ICatalogueOntologies_Languages_agg {
  count: number;
}

export interface ICatalogueOntologies_Legislations extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Legislations;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Legislations[];
}

export interface ICatalogueOntologies_Legislations_agg {
  count: number;
}

export interface ICatalogueOntologies_LinkageStrategies extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_LinkageStrategies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_LinkageStrategies[];
}

export interface ICatalogueOntologies_LinkageStrategies_agg {
  count: number;
}

export interface ILinkages extends IMgTableClass {
  resource: IResources;
  linkedResource: IResources;
  relationshipType?: string;
  sourceSelection?: string;
  linkageStrategy?: IOntologyNode;
  linkageVariable?: string;
  linkageVariableUnique?: boolean;
  linkageCompleteness?: string;
  preLinked?: boolean;
}

export interface ILinkages_agg {
  count: number;
}

export interface ICatalogueOntologies_MappingMethod extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_MappingMethod;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_MappingMethod[];
}

export interface ICatalogueOntologies_MappingMethod_agg {
  count: number;
}

export interface ICatalogueOntologies_MappingStatus extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_MappingStatus;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_MappingStatus[];
}

export interface ICatalogueOntologies_MappingStatus_agg {
  count: number;
}

export interface ICatalogueOntologies_NetworkTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_NetworkTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_NetworkTypes[];
}

export interface ICatalogueOntologies_NetworkTypes_agg {
  count: number;
}

export interface INetworks extends IMgTableClass {
  hricore?: boolean;
  id: string;
  pid?: string;
  name: string;
  localName?: string;
  acronym?: string;
  networkType?: IOntologyNode[];
  commonCollaborationTopics?: IOntologyNode[];
  description?: string;
  website?: string;
  contactEmail?: string;
  logo?: IFile;
  startYear?: number;
  endYear?: number;
  withdrawn?: boolean;
  dataResources?: ICollections[];
  countries?: IOntologyNode[];
  organisationsInvolved?: IOrganisationRoles[];
  publisher?: IOrganisationRoles;
  creator?: IOrganisationRoles[];
  peopleInvolved?: IContacts[];
  contactPoint?: IContacts;
  childNetworks?: INetworks[];
  parentNetworks?: INetworks[];
  tables?: ITables[];
  publications?: IPublications[];
  fundingStatement?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
  applicableLegislation?: IOntologyNode[];
  catalogueType?: IOntologyNode;
  mainCatalogue?: boolean;
  fdpEndpoint?: IEndpoint;
  ldpMembershipRelation?: string;
  hasMemberRelation?: string;
  conformsTo?: string;
}

export interface INetworks_agg {
  count: number;
}

export interface ICatalogueOntologies_ObservationTargets extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ObservationTargets;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ObservationTargets[];
}

export interface ICatalogueOntologies_ObservationTargets_agg {
  count: number;
}

export interface IOrganisationRoles extends IMgTableClass {
  id: string;
  resource: IResources;
  organisation?: ICatalogueOntologies_ROROrganisations;
  organisationNotInROR?: string;
  department?: string;
  email?: string;
  role?: IOntologyNode[];
  isLeadOrganisation?: boolean;
  organisationName?: string;
  organisationPid?: string;
  organisationWebsite?: string;
}

export interface IOrganisationRoles_agg {
  count: number;
}

export interface ICatalogueOntologies_OrganisationRoles extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_OrganisationRoles;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_OrganisationRoles[];
}

export interface ICatalogueOntologies_OrganisationRoles_agg {
  count: number;
}

export interface ICatalogueOntologies_OrganisationalUnitTypes
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_OrganisationalUnitTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_OrganisationalUnitTypes[];
}

export interface ICatalogueOntologies_OrganisationalUnitTypes_agg {
  count: number;
}

export interface IOrganisationalUnits extends IMgTableClass {
  hricore?: boolean;
  id: string;
  pid?: string;
  name: string;
  localName?: string;
  acronym?: string;
  organisationalUnitType?: IOntologyNode;
  description?: string;
  website?: string;
  contactEmail?: string;
  logo?: IFile;
  startYear?: number;
  endYear?: number;
  withdrawn?: boolean;
  collections?: ICollections[];
  services?: IServices[];
  countries?: IOntologyNode[];
  organisationsInvolved?: IOrganisationRoles[];
  parentOrganisation?: IOntologyNode;
  publisher?: IOrganisationRoles;
  creator?: IOrganisationRoles[];
  peopleInvolved?: IContacts[];
  contactPoint?: IContacts;
  tables?: ITables[];
  publications?: IPublications[];
  fundingStatement?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
  applicableLegislation?: IOntologyNode[];
}

export interface IOrganisationalUnits_agg {
  count: number;
}

export interface ICatalogueOntologies_PopulationEntry extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_PopulationEntry;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_PopulationEntry[];
}

export interface ICatalogueOntologies_PopulationEntry_agg {
  count: number;
}

export interface ICatalogueOntologies_PopulationExit extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_PopulationExit;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_PopulationExit[];
}

export interface ICatalogueOntologies_PopulationExit_agg {
  count: number;
}

export interface ICatalogueOntologies_PopulationOfInterest
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_PopulationOfInterest;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_PopulationOfInterest[];
}

export interface ICatalogueOntologies_PopulationOfInterest_agg {
  count: number;
}

export interface IProfiles extends IMgTableClass {
  dataCatalogueFlat?: string;
}

export interface IProfiles_agg {
  count: number;
}

export interface IPublications extends IMgTableClass {
  resource: IResources;
  doi: string;
  title: string;
  isDesignPublication?: boolean;
  reference?: string;
}

export interface IPublications_agg {
  count: number;
}

export interface IQualityInformation extends IMgTableClass {
  resource: IResources;
  name: string;
  standard?: string;
  certificationNumber?: string;
  hasCertification?: boolean;
}

export interface IQualityInformation_agg {
  count: number;
}

export interface ICatalogueOntologies_ROROrganisations extends IMgTableClass {
  name: string;
  label?: string;
  acronym?: string;
  parent?: any;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  semantics?: string;
  type?: string[];
  definition?: string;
  aliases?: string[];
  website?: string;
  country?: IOntologyNode;
  city?: string;
  latitude?: number;
  longitude?: number;
  children?: ICatalogueOntologies_ROROrganisations[];
}

export interface ICatalogueOntologies_ROROrganisations_agg {
  count: number;
}

export interface ICatalogueOntologies_RefreshPeriods extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_RefreshPeriods;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_RefreshPeriods[];
}

export interface ICatalogueOntologies_RefreshPeriods_agg {
  count: number;
}

export interface ICatalogueOntologies_Regions extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Regions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Regions[];
}

export interface ICatalogueOntologies_Regions_agg {
  count: number;
}

export interface ICatalogueOntologies_ReleaseTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ReleaseTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ReleaseTypes[];
}

export interface ICatalogueOntologies_ReleaseTypes_agg {
  count: number;
}

export interface IResourceMappings extends IMgTableClass {
  source: IResources;
  sourceVersion?: string;
  target: IResources;
  targetVersion?: string;
  cdmsOther?: string;
  mappingStatus?: IOntologyNode;
  eTLFrequency?: number;
  eTLSpecificationUrl?: string;
  eTLSpecificationDocument?: IFile;
}

export interface IResourceMappings_agg {
  count: number;
}

export interface ICatalogueOntologies_ResourceTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ResourceTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ResourceTypes[];
}

export interface ICatalogueOntologies_ResourceTypes_agg {
  count: number;
}

export interface IResources extends IMgTableClass {
  hricore?: boolean;
  id: string;
  pid?: string;
  name: string;
  localName?: string;
  acronym?: string;
  description?: string;
  website?: string;
  contactEmail?: string;
  logo?: IFile;
  startYear?: number;
  endYear?: number;
  withdrawn?: boolean;
  countries?: IOntologyNode[];
  organisationsInvolved?: IOrganisationRoles[];
  publisher?: IOrganisationRoles;
  creator?: IOrganisationRoles[];
  peopleInvolved?: IContacts[];
  contactPoint?: IContacts;
  tables?: ITables[];
  publications?: IPublications[];
  fundingStatement?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
  applicableLegislation?: IOntologyNode[];
  type: IOntologyNode[];
  cohortType?: IOntologyNode[];
  clinicalStudyType?: IOntologyNode[];
  registryOrHealthRecordType?: IOntologyNode[];
  keywords?: string[];
  timeSpanDescription?: string;
  status?: IOntologyNode;
  linkedResources?: ILinkages[];
  internalIdentifiers?: IInternalIdentifiers[];
  externalIdentifiers?: IExternalIdentifiers[];
  design?: IOntologyNode;
  dataCollectionType?: IOntologyNode[];
  designDescription?: string;
  designSchematic?: IFile;
  subpopulations?: ISubpopulations[];
  collectionEvents?: ICollectionEvents[];
  dataCollectionDescription?: string;
  reasonSustained?: string;
  recordTrigger?: string;
  unitOfObservation?: string;
  partOfNetworks?: INetworks[];
  numberOfParticipants?: number;
  numberOfParticipantsWithSamples?: number;
  underlyingPopulation?: string;
  populationOfInterest?: IOntologyNode[];
  populationOfInterestOther?: string;
  continents?: IOntologyNode[];
  regions?: IOntologyNode[];
  populationAgeGroups?: IOntologyNode[];
  ageMin?: number;
  ageMax?: number;
  ageUnit?: IOntologyNode;
  sex?: IOntologyNode[];
  inclusionCriteria?: IOntologyNode[];
  otherInclusionCriteria?: string;
  exclusionCriteria?: IOntologyNode[];
  otherExclusionCriteria?: string;
  populationEntry?: IOntologyNode[];
  populationEntryOther?: string;
  populationExit?: IOntologyNode[];
  populationExitOther?: string;
  populationDisease?: IOntologyNode[];
  populationOncologyTopology?: IOntologyNode[];
  populationOncologyMorphology?: IOntologyNode[];
  populationCoverage?: string;
  populationNotCovered?: string;
  custodian?: IOrganisationalUnits[];
  counts?: ICollectionCounts[];
  numberOfRecords?: number;
  areasOfInformation?: IOntologyNode[];
  areasOfInformationRwd?: IOntologyNode[];
  qualityOfLifeOther?: string;
  causeOfDeathCodeOther?: string;
  indicationVocabularyOther?: string;
  geneticDataVocabularyOther?: string;
  careSettingOther?: string;
  medicinalProductVocabularyOther?: string;
  prescriptionsVocabularyOther?: string;
  dispensingsVocabularyOther?: string;
  proceduresVocabularyOther?: string;
  biomarkerDataVocabularyOther?: string;
  diagnosisMedicalEventVocabularyOther?: string;
  dataDictionaryAvailable?: boolean;
  sampleType?: IOntologyNode[];
  bodyPart?: IOntologyNode[];
  imagingModality?: string[];
  storageTemperatures?: IOntologyNode[];
  sampleSource?: string;
  sampleCollectionSetting?: string;
  numberOfSamples?: number;
  languages?: IOntologyNode[];
  multipleEntries?: boolean;
  hasIdentifier?: boolean;
  identifierDescription?: string;
  prelinked?: boolean;
  linkageOptions?: string;
  linkagePossibility?: boolean;
  dataAccessConditions?: IOntologyNode[];
  dataUseConditions?: IOntologyNode[];
  dataAccessConditionsDescription?: string;
  dataAccessFee?: boolean;
  informedConsentType?: IOntologyNode;
  informedConsentRequired?: IOntologyNode;
  informedConsentOther?: string;
  accessRights?: IOntologyNode;
  accessIdentifiableData?: string;
  accessIdentifiableDataRoute?: string;
  accessSubjectDetails?: boolean;
  accessSubjectDetailsRoute?: string;
  accessThirdParty?: boolean;
  accessThirdPartyConditions?: string;
  accessNonEU?: boolean;
  accessNonEUConditions?: string;
  biospecimenAccess?: boolean;
  biospecimenAccessConditions?: string;
  governanceDetails?: string;
  approvalForPublication?: boolean;
  releaseType?: IOntologyNode;
  releaseDescription?: string;
  releaseFrequency?: number;
  refreshTime?: number;
  lagTime?: number;
  refreshPeriod?: IOntologyNode[];
  dateLastRefresh?: string;
  preservation?: boolean;
  preservationDuration?: number;
  quality?: IQualityInformation[];
  standardOperatingProcedures?: boolean;
  qualification?: boolean;
  qualificationsDescription?: string;
  auditPossible?: boolean;
  completeness?: string;
  completenessOverTime?: string;
  completenessResults?: string;
  qualityDescription?: string;
  qualityOverTime?: string;
  accessForValidation?: boolean;
  qualityValidationFrequency?: string;
  qualityValidationMethods?: string;
  correctionMethods?: string;
  qualityValidationResults?: string;
  mappingsToCommonDataModels?: ITableMappings[];
  commonDataModelsOther?: string;
  eTLStandardVocabularies?: IOntologyNode[];
  eTLStandardVocabulariesOther?: string;
  fundingSources?: IOntologyNode[];
  fundingScheme?: IOntologyNode[];
  citationRequirements?: string;
  provenanceStatement?: string;
  supplementaryInformation?: string;
  theme?: IOntologyNode[];
  issued?: string;
  modified?: string;
  collectionStartPlanned?: string;
  collectionStartActual?: string;
  analysisStartPlanned?: string;
  analysisStartActual?: string;
  dataSources?: IResources[];
  dataExtractionDate?: string;
  analysisPlan?: string;
  objectives?: string;
  results?: string;
  networkType?: IOntologyNode[];
  commonCollaborationTopics?: IOntologyNode[];
  dataResources?: ICollections[];
  childNetworks?: INetworks[];
  parentNetworks?: INetworks[];
  catalogueType?: IOntologyNode;
  mainCatalogue?: boolean;
  fdpEndpoint?: IEndpoint;
  ldpMembershipRelation?: string;
  hasMemberRelation?: string;
  conformsTo?: string;
  organisationalUnitType?: IOntologyNode;
  collections?: ICollections[];
  services?: IServices[];
  parentOrganisation?: IOntologyNode;
}

export interface IResources_agg {
  count: number;
}

export interface IReusedVariables extends IMgTableClass {
  resource: IResources;
  variable: IVariables;
}

export interface IReusedVariables_agg {
  count: number;
}

export interface ICatalogueOntologies_SampleCategories extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_SampleCategories;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_SampleCategories[];
}

export interface ICatalogueOntologies_SampleCategories_agg {
  count: number;
}

export interface ICatalogueOntologies_SampleType extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_SampleType;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_SampleType[];
}

export interface ICatalogueOntologies_SampleType_agg {
  count: number;
}

export interface ICatalogueOntologies_SampleTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_SampleTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_SampleTypes[];
}

export interface ICatalogueOntologies_SampleTypes_agg {
  count: number;
}

export interface ISamplesets extends IMgTableClass {
  resource: IResources;
  name: string;
  sampleTypes?: IOntologyNode[];
}

export interface ISamplesets_agg {
  count: number;
}

export interface ICatalogueOntologies_ServiceTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_ServiceTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_ServiceTypes[];
}

export interface ICatalogueOntologies_ServiceTypes_agg {
  count: number;
}

export interface IServices extends IMgTableClass {
  name: string;
  acronym?: string;
  description?: string;
  contactEmail: string;
  website?: string;
  provider: IOrganisationalUnits;
  serviceType: IOntologyNode[];
  keywords?: string[];
  technology?: ITechnologies[];
  accessDescription?: string;
  accessDescriptionUrl?: string;
  status?: IOntologyNode;
}

export interface IServices_agg {
  count: number;
}

export interface ICatalogueOntologies_Sex extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Sex;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Sex[];
}

export interface ICatalogueOntologies_Sex_agg {
  count: number;
}

export interface ICatalogueOntologies_StandardizedTools extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_StandardizedTools;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_StandardizedTools[];
}

export interface ICatalogueOntologies_StandardizedTools_agg {
  count: number;
}

export interface ICatalogueOntologies_Status extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Status;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Status[];
}

export interface ICatalogueOntologies_Status_agg {
  count: number;
}

export interface ICatalogueOntologies_StatusDetails extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_StatusDetails;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_StatusDetails[];
}

export interface ICatalogueOntologies_StatusDetails_agg {
  count: number;
}

export interface ICatalogueOntologies_StorageConditions extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_StorageConditions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_StorageConditions[];
}

export interface ICatalogueOntologies_StorageConditions_agg {
  count: number;
}

export interface ICatalogueOntologies_StudyFunding extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_StudyFunding;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_StudyFunding[];
}

export interface ICatalogueOntologies_StudyFunding_agg {
  count: number;
}

export interface ICatalogueOntologies_StudyStatus extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_StudyStatus;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_StudyStatus[];
}

export interface ICatalogueOntologies_StudyStatus_agg {
  count: number;
}

export interface ISubpopulationCounts extends IMgTableClass {
  resource: IResources;
  subpopulation: ISubpopulations;
  ageGroup: IOntologyNode;
  nTotal?: number;
  nFemale?: number;
  nMale?: number;
}

export interface ISubpopulationCounts_agg {
  count: number;
}

export interface ISubpopulations extends IMgTableClass {
  resource: IResources;
  name: string;
  pid?: string;
  description?: string;
  keywords?: string[];
  numberOfParticipants?: number;
  counts?: ISubpopulationCounts[];
  inclusionStart?: number;
  inclusionEnd?: number;
  ageGroups?: IOntologyNode[];
  ageMin?: number;
  ageMax?: number;
  mainMedicalCondition?: IOntologyNode[];
  comorbidity?: IOntologyNode[];
  countries?: IOntologyNode[];
  regions?: IOntologyNode[];
  inclusionCriteria?: IOntologyNode[];
  otherInclusionCriteria?: string;
  exclusionCriteria?: IOntologyNode[];
  otherExclusionCriteria?: string;
  contactPoint?: IContacts;
  publisher?: IOrganisationRoles;
  creator?: IOrganisationRoles[];
  issued?: string;
  modified?: string;
  theme?: IOntologyNode[];
  accessRights?: IOntologyNode;
  applicableLegislation?: IOntologyNode[];
  provenanceStatement?: string;
}

export interface ISubpopulations_agg {
  count: number;
}

export interface ITableMappings extends IMgTableClass {
  source: IResources;
  sourceTable: ITables;
  target: IResources;
  targetTable: ITables;
  order?: number;
  description?: string;
  syntax?: string;
}

export interface ITableMappings_agg {
  count: number;
}

export interface ICatalogueOntologies_TableTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_TableTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_TableTypes[];
}

export interface ICatalogueOntologies_TableTypes_agg {
  count: number;
}

export interface ITables extends IMgTableClass {
  resource: IResources;
  name: string;
  label?: string;
  tableType?: IOntologyNode[];
  unitOfObservation?: IOntologyNode;
  keywords?: IOntologyNode[];
  description?: string;
  numberOfRows?: number;
  mappedTo?: ITableMappings[];
  mappedFrom?: ITableMappings[];
  variables?: IVariables[];
  sinceVersion?: string;
  untilVersion?: string;
}

export interface ITables_agg {
  count: number;
}

export interface ITechnologies extends IMgTableClass {
  name: string;
  description?: string;
  provider?: string;
  device?: string;
  softwareName?: string;
  softwareVersion?: string;
}

export interface ITechnologies_agg {
  count: number;
}

export interface ICatalogueOntologies_Themes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Themes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Themes[];
}

export interface ICatalogueOntologies_Themes_agg {
  count: number;
}

export interface ICatalogueOntologies_Titles extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Titles;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Titles[];
}

export interface ICatalogueOntologies_Titles_agg {
  count: number;
}

export interface ICatalogueOntologies_Units extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Units;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Units[];
}

export interface ICatalogueOntologies_Units_agg {
  count: number;
}

export interface IVariableMappings extends IMgTableClass {
  source: IResources;
  sourceTable: ITables;
  sourceVariables?: IVariables[];
  sourceVariablesOtherTables?: IVariables[];
  target: IResources;
  targetTable: ITables;
  targetVariable: IVariables;
  repeats: string;
  match: IOntologyNode;
  description?: string;
  syntax?: string;
  comments?: string;
}

export interface IVariableMappings_agg {
  count: number;
}

export interface ICatalogueOntologies_VariableRepeatUnits
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_VariableRepeatUnits;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_VariableRepeatUnits[];
}

export interface ICatalogueOntologies_VariableRepeatUnits_agg {
  count: number;
}

export interface IVariableValueMappings extends IMgTableClass {
  resource: IResources;
  id: string;
  source: IVariableValues;
  target: IVariableValues;
  mappingStatus?: IOntologyNode;
  mappingMethod?: IOntologyNode;
  reviewedBy?: string;
  mappingDate?: string;
}

export interface IVariableValueMappings_agg {
  count: number;
}

export interface IVariableValues extends IMgTableClass {
  resource: IResources;
  table: ITables;
  variable: IVariables;
  value: string;
  label: string;
  order?: number;
  isMissing?: boolean;
  ontologyTermURI?: string;
  sinceVersion?: string;
  untilVersion?: string;
}

export interface IVariableValues_agg {
  count: number;
}

export interface IVariables extends IMgTableClass {
  resource: IResources;
  table: ITables;
  name: string;
  useExternalDefinition?: IVariables;
  label?: string;
  description?: string;
  originalLanguageDescription?: string;
  collectionEvent?: ICollectionEvents[];
  format?: IOntologyNode;
  unit?: IOntologyNode;
  ontologyTermURI?: string;
  sinceVersion?: string;
  untilVersion?: string;
  reusedInResources?: IReusedVariables[];
  repeatUnit?: IOntologyNode;
  repeatMin?: number;
  repeatMax?: number;
  exampleValues?: string[];
  permittedValues?: IVariableValues[];
  keywords?: IOntologyNode[];
  generated_keywords?: IOntologyNode[];
  vocabularies?: IOntologyNode[];
  notes?: string;
  mappings?: IVariableMappings[];
}

export interface IVariables_agg {
  count: number;
}

export interface IVersion extends IMgTableClass {}

export interface IVersion_agg {
  count: number;
}

export interface ICatalogueOntologies_Vocabularies extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Vocabularies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Vocabularies[];
}

export interface ICatalogueOntologies_Vocabularies_agg {
  count: number;
}
