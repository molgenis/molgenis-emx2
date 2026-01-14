// Generated (on: 2026-01-14T00:37:29.969663) from Generator.java for schema: catalogue-demo

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

export interface ICatalogueOntologies_AccessRights {
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

export interface ICatalogueOntologies_AgeGroups {
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

export interface ICatalogueOntologies_AgentTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_AgentTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_AgentTypes[];
}

export interface ICatalogueOntologies_AgentTypes_agg {
  count: number;
}

export interface IAgents {
  resource: IResources;
  id: string;
  type: IOntologyNode;
  name?: string;
  organisation?: ICatalogueOntologies_Organisations;
  otherOrganisation?: string;
  department?: string;
  website?: string;
  email?: string;
  logo?: IFile;
  role?: IOntologyNode[];
  organisationName?: string;
  organisationPid?: string;
  organisationWebsite?: string;
}

export interface IAgents_agg {
  count: number;
}

export interface ICatalogueOntologies_AreasOfInformationCohorts {
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

export interface ICatalogueOntologies_AreasOfInformationDs {
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

export interface ICatalogueOntologies_Biospecimens {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Biospecimens;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Biospecimens[];
}

export interface ICatalogueOntologies_Biospecimens_agg {
  count: number;
}

export interface ICatalogueOntologies_CatalogueTypes {
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

export interface ICatalogueOntologies_ClinicalStudyTypes {
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

export interface ICatalogueOntologies_CohortCollectionTypes {
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

export interface ICatalogueOntologies_CohortDesigns {
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

export interface ICatalogueOntologies_CohortStudyTypes {
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

export interface ICollectionEvents {
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
  publisher?: IOrganisations;
  creator?: IOrganisations[];
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

export interface ICatalogueOntologies_ContributionTypes {
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

export interface ICatalogueOntologies_Countries {
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

export interface ICatalogueOntologies_DataAccessConditions {
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

export interface ICatalogueOntologies_DataCategories {
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

export interface ICatalogueOntologies_DataUseConditions {
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

export interface ICatalogueOntologies_DatasetTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DatasetTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DatasetTypes[];
}

export interface ICatalogueOntologies_DatasetTypes_agg {
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

export interface ICatalogueOntologies_DatasourceTypes {
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

export interface ICatalogueOntologies_Diseases {
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

export interface ICatalogueOntologies_DocumentTypes {
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
  publisher: IAgents[];
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

export interface ICatalogueOntologies_ExternalIdentifierTypes {
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

export interface IExternalIdentifiers {
  resource: IResources;
  identifier: string;
  externalIdentifierType?: IOntologyNode;
  externalIdentifierTypeOther?: string;
}

export interface IExternalIdentifiers_agg {
  count: number;
}

export interface ICatalogueOntologies_Formats {
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

export interface ICatalogueOntologies_FundingTypes {
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

export interface ICatalogueOntologies_ICDOMorphologies {
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

export interface ICatalogueOntologies_ICDOTopologies {
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

export interface ICatalogueOntologies_InclusionExclusionCriteria {
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

export interface ICatalogueOntologies_InformedConsentRequired {
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

export interface ICatalogueOntologies_InformedConsentTypes {
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

export interface ICatalogueOntologies_InternalIdentifierTypes {
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

export interface IInternalIdentifiers {
  resource: IResources;
  identifier: string;
  internalIdentifierType?: IOntologyNode;
  internalIdentifierTypeOther?: string;
}

export interface IInternalIdentifiers_agg {
  count: number;
}

export interface ICatalogueOntologies_Keywords {
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

export interface ICatalogueOntologies_Languages {
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

export interface ICatalogueOntologies_Legislations {
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

export interface ICatalogueOntologies_LinkageStrategies {
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

export interface ICatalogueOntologies_MappingStatus {
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

export interface ICatalogueOntologies_MedDRA {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_MedDRA;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_MedDRA[];
}

export interface ICatalogueOntologies_MedDRA_agg {
  count: number;
}

export interface ICatalogueOntologies_NetworkTypes {
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

export interface ICatalogueOntologies_ObservationTargets {
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

export interface ICatalogueOntologies_OrganisationRoles {
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

export interface IOrganisations {
  resource: IResources;
  id: string;
  type: IOntologyNode;
  name?: string;
  organisation?: ICatalogueOntologies_Organisations;
  otherOrganisation?: string;
  department?: string;
  website?: string;
  email?: string;
  logo?: IFile;
  role?: IOntologyNode[];
  organisationName?: string;
  organisationPid?: string;
  organisationWebsite?: string;
  isLeadOrganisation?: boolean;
}

export interface IOrganisations_agg {
  count: number;
}

export interface ICatalogueOntologies_Organisations {
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
  children?: ICatalogueOntologies_Organisations[];
}

export interface ICatalogueOntologies_Organisations_agg {
  count: number;
}

export interface ICatalogueOntologies_PopulationEntry {
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

export interface ICatalogueOntologies_PopulationExit {
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

export interface ICatalogueOntologies_PopulationOfInterest {
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

export interface IProfiles {
  dataCatalogueFlat?: string;
}

export interface IProfiles_agg {
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

export interface ICatalogueOntologies_RefreshPeriods {
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

export interface ICatalogueOntologies_Regions {
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

export interface ICatalogueOntologies_ReleaseTypes {
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

export interface ICatalogueOntologies_ResourceTypes {
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

export interface IResources {
  rdfType?: string;
  fdpEndpoint?: IEndpoint;
  ldpMembershipRelation?: string;
  hricore?: boolean;
  id: string;
  pid?: string;
  name: string;
  localName?: string;
  acronym?: string;
  type: IOntologyNode[];
  typeOther?: string;
  catalogueType?: IOntologyNode;
  mainCatalogue?: boolean;
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
  dataResources?: IResources[];
  partOfNetworks?: IResources[];
  numberOfParticipants?: number;
  numberOfParticipantsWithSamples?: number;
  underlyingPopulation?: string;
  populationOfInterest?: IOntologyNode[];
  populationOfInterestOther?: string;
  countries?: IOntologyNode[];
  regions?: IOntologyNode[];
  populationAgeGroups?: IOntologyNode[];
  ageMin?: number;
  ageMax?: number;
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
  organisationsInvolved?: IOrganisations[];
  publisher?: IOrganisations;
  creator?: IOrganisations[];
  peopleInvolved?: IContacts[];
  contactPoint?: IContacts;
  childNetworks?: IResources[];
  parentNetworks?: IResources[];
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
  accessRights?: IOntologyNode;
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
  provenanceStatement?: string;
  documentation?: IDocumentation[];
  supplementaryInformation?: string;
  theme?: IOntologyNode[];
  applicableLegislation?: IOntologyNode[];
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

export interface ICatalogueOntologies_SampleCategories {
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

export interface ICatalogueOntologies_SampleTypes {
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

export interface ISamplesets {
  resource: IResources;
  name: string;
  sampleTypes?: IOntologyNode[];
}

export interface ISamplesets_agg {
  count: number;
}

export interface ICatalogueOntologies_StandardizedTools {
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

export interface ICatalogueOntologies_StatusDetails {
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

export interface ICatalogueOntologies_StudyFunding {
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

export interface ICatalogueOntologies_StudyStatus {
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
  publisher?: IOrganisations;
  creator?: IOrganisations[];
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

export interface ICatalogueOntologies_Themes {
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

export interface ICatalogueOntologies_Titles {
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

export interface ICatalogueOntologies_Units {
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

export interface ICatalogueOntologies_VariableRepeatUnits {
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

export interface ICatalogueOntologies_Vocabularies {
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
