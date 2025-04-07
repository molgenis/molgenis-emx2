// Generated (on: 2025-04-05T22:58:47.077180) from Generator.java for schema: catalogue

export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
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

export interface IAgeGroups {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IAgeGroups;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IAgeGroups[];
}

export interface IAgeGroups_agg {
  count: number;
}

export interface IAgent {
  name: string;
  logo?: string;
  url?: string;
  mbox?: string;
}

export interface IAgent_agg {
  count: number;
}

export interface IAreasOfInformationCohorts {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IAreasOfInformationCohorts;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IAreasOfInformationCohorts[];
}

export interface IAreasOfInformationCohorts_agg {
  count: number;
}

export interface IAreasOfInformationDs {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IAreasOfInformationDs;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IAreasOfInformationDs[];
}

export interface IAreasOfInformationDs_agg {
  count: number;
}

export interface IBiospecimens {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IBiospecimens;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IBiospecimens[];
}

export interface IBiospecimens_agg {
  count: number;
}

export interface IClinicalStudyTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IClinicalStudyTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IClinicalStudyTypes[];
}

export interface IClinicalStudyTypes_agg {
  count: number;
}

export interface ICohortCollectionTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICohortCollectionTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICohortCollectionTypes[];
}

export interface ICohortCollectionTypes_agg {
  count: number;
}

export interface ICohortDesigns {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICohortDesigns;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICohortDesigns[];
}

export interface ICohortDesigns_agg {
  count: number;
}

export interface ICohortStudyTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICohortStudyTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICohortStudyTypes[];
}

export interface ICohortStudyTypes_agg {
  count: number;
}

export interface ICollectionEvents {
  resource: IResources;
  name: string;
  description?: string;
  subpopulations?: ISubpopulations[];
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
  publisher?: IOrganisations;
  creator?: IOrganisations[];
  license?: string;
  issued?: string;
  modified?: string;
  theme?: string[];
}

export interface ICollectionEvents_agg {
  count: number;
}

export interface IContacts {
  resource: IResources;
  role?: IOntologyNode[];
  roleDescription?: string;
  firstName: string;
  lastName: string;
  displayName: string;
  prefix?: string;
  initials?: string;
  title?: IOntologyNode;
  organisation?: IOrganisations;
  email?: string;
  orcid?: string;
  homepage?: string;
  photo?: IFile;
  expertise?: string;
}

export interface IContacts_agg {
  count: number;
}

export interface IContainer {
  id: string;
  title?: string[];
  membershipResource?: string;
  hasMemberRelation?: string;
  contains?: IResources[];
}

export interface IContainer_agg {
  count: number;
}

export interface IContributionTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IContributionTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IContributionTypes[];
}

export interface IContributionTypes_agg {
  count: number;
}

export interface ICountries {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICountries;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICountries[];
}

export interface ICountries_agg {
  count: number;
}

export interface IDataAccessConditions {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataAccessConditions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataAccessConditions[];
}

export interface IDataAccessConditions_agg {
  count: number;
}

export interface IDataCategories {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataCategories;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataCategories[];
}

export interface IDataCategories_agg {
  count: number;
}

export interface IDataUseConditions {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataUseConditions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataUseConditions[];
}

export interface IDataUseConditions_agg {
  count: number;
}

export interface IDatasetMappings {
  source: IResources;
  sourceDataset: IDatasets;
  target: IResources;
  targetDataset: IDatasets;
  order?: number;
  description?: string;
  syntax?: string;
}

export interface IDatasetMappings_agg {
  count: number;
}

export interface IDatasetTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDatasetTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDatasetTypes[];
}

export interface IDatasetTypes_agg {
  count: number;
}

export interface IDatasets {
  resource: IResources;
  name: string;
  label?: string;
  datasetType?: IOntologyNode[];
  unitOfObservation?: IOntologyNode;
  keywords?: IOntologyNode[];
  description?: string;
  numberOfRows?: number;
  mappedTo?: IDatasetMappings[];
  mappedFrom?: IDatasetMappings[];
  sinceVersion?: string;
  untilVersion?: string;
}

export interface IDatasets_agg {
  count: number;
}

export interface IDatasourceTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDatasourceTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDatasourceTypes[];
}

export interface IDatasourceTypes_agg {
  count: number;
}

export interface IDiseases {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDiseases;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDiseases[];
}

export interface IDiseases_agg {
  count: number;
}

export interface IDocumentTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDocumentTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDocumentTypes[];
}

export interface IDocumentTypes_agg {
  count: number;
}

export interface IDocumentation {
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

export interface IEndpoint {
  id: string;
  type: string[];
  name: string[];
  version?: string;
  description?: string;
  publisher: IAgent[];
  language?: string[];
  license: string;
  conformsTo: string;
  rights?: string[];
  accessRights?: string[];
  contact?: IAgent;
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

export interface IExternalIdentifierTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IExternalIdentifierTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IExternalIdentifierTypes[];
}

export interface IExternalIdentifierTypes_agg {
  count: number;
}

export interface IExternalIdentifiers {
  resource: IResources;
  identifier: string;
  externalIdentifierType?: IOntologyNode;
  externalIdentifierTypeOther?: string;
}

export interface IExternalIdentifiers_agg {
  count: number;
}

export interface IFormats {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IFormats;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IFormats[];
}

export interface IFormats_agg {
  count: number;
}

export interface IFundingTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IFundingTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IFundingTypes[];
}

export interface IFundingTypes_agg {
  count: number;
}

export interface IICDOMorphologies {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IICDOMorphologies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IICDOMorphologies[];
}

export interface IICDOMorphologies_agg {
  count: number;
}

export interface IICDOTopologies {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IICDOTopologies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IICDOTopologies[];
}

export interface IICDOTopologies_agg {
  count: number;
}

export interface IInclusionExclusionCriteria {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IInclusionExclusionCriteria;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IInclusionExclusionCriteria[];
}

export interface IInclusionExclusionCriteria_agg {
  count: number;
}

export interface IInformedConsentRequired {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IInformedConsentRequired;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IInformedConsentRequired[];
}

export interface IInformedConsentRequired_agg {
  count: number;
}

export interface IInformedConsentTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IInformedConsentTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IInformedConsentTypes[];
}

export interface IInformedConsentTypes_agg {
  count: number;
}

export interface IInternalIdentifierTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IInternalIdentifierTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IInternalIdentifierTypes[];
}

export interface IInternalIdentifierTypes_agg {
  count: number;
}

export interface IInternalIdentifiers {
  resource: IResources;
  identifier: string;
  internalIdentifierType?: IOntologyNode;
  internalIdentifierTypeOther?: string;
}

export interface IInternalIdentifiers_agg {
  count: number;
}

export interface IKeywords {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IKeywords;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IKeywords[];
}

export interface IKeywords_agg {
  count: number;
}

export interface ILanguages {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ILanguages;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ILanguages[];
}

export interface ILanguages_agg {
  count: number;
}

export interface ILinkageStrategies {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ILinkageStrategies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ILinkageStrategies[];
}

export interface ILinkageStrategies_agg {
  count: number;
}

export interface ILinkages {
  resource: IResources;
  linkedResource: IResources;
  linkageStrategy?: IOntologyNode;
  linkageVariable?: string;
  linkageVariableUnique?: boolean;
  linkageCompleteness?: string;
  preLinked?: boolean;
}

export interface ILinkages_agg {
  count: number;
}

export interface IMappingStatus {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IMappingStatus;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IMappingStatus[];
}

export interface IMappingStatus_agg {
  count: number;
}

export interface IMedDRA {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IMedDRA;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IMedDRA[];
}

export interface IMedDRA_agg {
  count: number;
}

export interface INetworkTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: INetworkTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: INetworkTypes[];
}

export interface INetworkTypes_agg {
  count: number;
}

export interface IObservationTargets {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IObservationTargets;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IObservationTargets[];
}

export interface IObservationTargets_agg {
  count: number;
}

export interface IOrganisationRoles {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IOrganisationRoles;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IOrganisationRoles[];
}

export interface IOrganisationRoles_agg {
  count: number;
}

export interface IOrganisations {
  resource: IResources;
  id: string;
  pid?: string;
  name: string;
  acronym?: string;
  logo?: IFile;
  country?: IOntologyNode[];
  website?: string;
  role?: IOntologyNode[];
  isLeadOrganisation?: boolean;
}

export interface IOrganisations_agg {
  count: number;
}

export interface IPopulationEntry {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IPopulationEntry;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IPopulationEntry[];
}

export interface IPopulationEntry_agg {
  count: number;
}

export interface IPopulationExit {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IPopulationExit;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IPopulationExit[];
}

export interface IPopulationExit_agg {
  count: number;
}

export interface IPopulationOfInterest {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IPopulationOfInterest;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IPopulationOfInterest[];
}

export interface IPopulationOfInterest_agg {
  count: number;
}

export interface IPublications {
  resource: IResources;
  doi: string;
  title: string;
  isDesignPublication?: boolean;
  reference?: string;
}

export interface IPublications_agg {
  count: number;
}

export interface IRefreshPeriods {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IRefreshPeriods;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IRefreshPeriods[];
}

export interface IRefreshPeriods_agg {
  count: number;
}

export interface IRegions {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IRegions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IRegions[];
}

export interface IRegions_agg {
  count: number;
}

export interface IReleaseTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IReleaseTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IReleaseTypes[];
}

export interface IReleaseTypes_agg {
  count: number;
}

export interface IResourceCounts {
  resource: IResources;
  ageGroup: IOntologyNode;
  populationSize?: number;
  activeSize?: number;
  noIndividualsWithSamples?: number;
  meanObservationYears?: number;
  meanYearsActive?: number;
  medianAge?: number;
  proportionFemale?: number;
}

export interface IResourceCounts_agg {
  count: number;
}

export interface IResourceMappings {
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

export interface IResourceTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IResourceTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IResourceTypes[];
}

export interface IResourceTypes_agg {
  count: number;
}

export interface IResources {
  dcatType?: string;
  id: string;
  pid?: string;
  name: string;
  localName?: string;
  acronym?: string;
  type: IOntologyNode[];
  typeOther?: string;
  cohortType?: IOntologyNode[];
  clinicalStudyType?: IOntologyNode[];
  rWDType?: IOntologyNode[];
  networkType?: IOntologyNode[];
  website?: string;
  description?: string;
  keywords?: string[];
  internalIdentifiers?: IInternalIdentifiers[];
  externalIdentifiers?: IExternalIdentifiers[];
  startYear?: number;
  endYear?: number;
  timeSpanDescription?: string;
  contactEmail?: string;
  logo?: IFile;
  status?: IOntologyNode;
  license?: string;
  conformsTo?: string;
  hasMemberRelation?: string;
  issued?: string;
  modified?: string;
  design?: IOntologyNode;
  designDescription?: string;
  designSchematic?: IFile;
  dataCollectionType?: IOntologyNode[];
  dataCollectionDescription?: string;
  reasonSustained?: string;
  recordTrigger?: string;
  unitOfObservation?: string;
  subpopulations?: ISubpopulations[];
  collectionEvents?: ICollectionEvents[];
  resources?: IResources[];
  partOfResources?: IResources[];
  numberOfParticipants?: number;
  numberOfParticipantsWithSamples?: number;
  underlyingPopulation?: string;
  populationOfInterest?: IOntologyNode[];
  populationOfInterestOther?: string;
  countries?: IOntologyNode[];
  regions?: IOntologyNode[];
  populationAgeGroups?: IOntologyNode[];
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
  counts?: IResourceCounts[];
  peopleInvolved?: IContacts[];
  contactPoint?: IContacts;
  organisationsInvolved?: IOrganisations[];
  publisher?: IOrganisations[];
  creator?: IOrganisations[];
  networksInvolved?: IResources[];
  datasets?: IDatasets[];
  samplesets?: ISamplesets[];
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
  diseaseDetails?: IOntologyNode[];
  biospecimenCollected?: IOntologyNode[];
  languages?: IOntologyNode[];
  multipleEntries?: boolean;
  hasIdentifier?: boolean;
  identifierDescription?: string;
  prelinked?: boolean;
  linkageOptions?: string;
  linkagePossibility?: boolean;
  linkedResources?: ILinkages[];
  informedConsentType?: IOntologyNode;
  informedConsentRequired?: IOntologyNode;
  informedConsentOther?: string;
  dataAccessConditions?: IOntologyNode[];
  dataUseConditions?: IOntologyNode[];
  dataAccessConditionsDescription?: string;
  dataAccessFee?: boolean;
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
  numberOfRecords?: number;
  releaseFrequency?: number;
  refreshTime?: number;
  lagTime?: number;
  refreshPeriod?: IOntologyNode[];
  dateLastRefresh?: string;
  preservation?: boolean;
  preservationDuration?: number;
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
  mappingsToCommonDataModels?: IDatasetMappings[];
  commonDataModelsOther?: string;
  eTLStandardVocabularies?: IOntologyNode[];
  eTLStandardVocabulariesOther?: string;
  publications?: IPublications[];
  fundingSources?: IOntologyNode[];
  fundingScheme?: IOntologyNode[];
  fundingStatement?: string;
  citationRequirements?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
  supplementaryInformation?: string;
  collectionStartPlanned?: string;
  collectionStartActual?: string;
  analysisStartPlanned?: string;
  analysisStartActual?: string;
  dataSources?: IResources[];
  medicalConditionsStudied?: IOntologyNode[];
  dataExtractionDate?: string;
  analysisPlan?: string;
  objectives?: string;
  results?: string;
  theme?: string[];
}

export interface IResources_agg {
  count: number;
}

export interface IReusedVariables {
  resource: IResources;
  variable: IVariables;
}

export interface IReusedVariables_agg {
  count: number;
}

export interface ISampleCategories {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ISampleCategories;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ISampleCategories[];
}

export interface ISampleCategories_agg {
  count: number;
}

export interface ISampleTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ISampleTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ISampleTypes[];
}

export interface ISampleTypes_agg {
  count: number;
}

export interface ISamplesets {
  resource: IResources;
  name: string;
  sampleTypes?: IOntologyNode[];
}

export interface ISamplesets_agg {
  count: number;
}

export interface IStandardizedTools {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IStandardizedTools;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IStandardizedTools[];
}

export interface IStandardizedTools_agg {
  count: number;
}

export interface IStatusDetails {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IStatusDetails;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IStatusDetails[];
}

export interface IStatusDetails_agg {
  count: number;
}

export interface IStudyFunding {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IStudyFunding;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IStudyFunding[];
}

export interface IStudyFunding_agg {
  count: number;
}

export interface IStudyStatus {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IStudyStatus;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IStudyStatus[];
}

export interface IStudyStatus_agg {
  count: number;
}

export interface ISubmissionTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ISubmissionTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ISubmissionTypes[];
}

export interface ISubmissionTypes_agg {
  count: number;
}

export interface ISubmissions {
  submissionDate: string;
  submitterName: string;
  resources: IResources[];
  submitterEmail: string;
  submitterOrganisation?: string;
  submitterRole?: IOntologyNode;
  submitterRoleOther?: string;
  submissionType?: IOntologyNode;
  submissionDescription?: string;
  responsiblePersons?: string;
  acceptanceDate?: string;
}

export interface ISubmissions_agg {
  count: number;
}

export interface ISubmitterRoles {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ISubmitterRoles;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ISubmitterRoles[];
}

export interface ISubmitterRoles_agg {
  count: number;
}

export interface ISubpopulationCounts {
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

export interface ISubpopulations {
  resource: IResources;
  name: string;
  description?: string;
  numberOfParticipants?: number;
  counts?: ISubpopulationCounts[];
  inclusionStart?: number;
  inclusionEnd?: number;
  ageGroups?: IOntologyNode[];
  mainMedicalCondition?: IOntologyNode[];
  comorbidity?: IOntologyNode[];
  countries?: IOntologyNode[];
  regions?: IOntologyNode[];
  inclusionCriteria?: IOntologyNode[];
  otherInclusionCriteria?: string;
  exclusionCriteria?: IOntologyNode[];
  otherExclusionCriteria?: string;
  contactPoint?: IContacts;
  publisher?: IOrganisations;
  creator?: IOrganisations[];
  license?: string;
  issued?: string;
  modified?: string;
  theme?: string[];
}

export interface ISubpopulations_agg {
  count: number;
}

export interface ITitles {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ITitles;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ITitles[];
}

export interface ITitles_agg {
  count: number;
}

export interface IUnits {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IUnits;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IUnits[];
}

export interface IUnits_agg {
  count: number;
}

export interface IVariableMappings {
  source: IResources;
  sourceDataset: IDatasets;
  sourceVariables?: IVariables[];
  sourceVariablesOtherDatasets?: IVariables[];
  target: IResources;
  targetDataset: IDatasets;
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

export interface IVariableRepeatUnits {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IVariableRepeatUnits;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IVariableRepeatUnits[];
}

export interface IVariableRepeatUnits_agg {
  count: number;
}

export interface IVariableValues {
  resource: IResources;
  dataset: IDatasets;
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

export interface IVariables {
  resource: IResources;
  dataset: IDatasets;
  name: string;
  useExternalDefinition?: IVariables;
  label?: string;
  description?: string;
  collectionEvent?: ICollectionEvents[];
  format?: IOntologyNode;
  unit?: IOntologyNode;
  sinceVersion?: string;
  untilVersion?: string;
  reusedInResources?: IReusedVariables[];
  repeatUnit?: IOntologyNode;
  repeatMin?: number;
  repeatMax?: number;
  exampleValues?: string[];
  permittedValues?: IVariableValues[];
  keywords?: IOntologyNode[];
  vocabularies?: IOntologyNode[];
  notes?: string;
  mappings?: IVariableMappings[];
}

export interface IVariables_agg {
  count: number;
}

export interface IVersion {}

export interface IVersion_agg {
  count: number;
}

export interface IVocabularies {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IVocabularies;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IVocabularies[];
}

export interface IVocabularies_agg {
  count: number;
}
