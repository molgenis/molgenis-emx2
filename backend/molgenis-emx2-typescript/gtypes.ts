export interface IPublications {
  doi :any;
  title :string;
  authors :string[];
  year :number;
  journal :string;
  volume :number;
  number :number;
  pagination :string;
  publisher :string;
  school :string;
  abstract :string;
  resources :any;
}

export interface IResources {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  website :any;
  description :string;
  contacts :any;
}

export interface IVersion {
}

export interface IOrganisations {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  type :any;
  typeOther :string;
  institution :string;
  institutionAcronym :string;
  email :string;
  logo :any;
  address :string;
  expertise :string;
  country :any;
  features :any;
  role :any;
  leadingResources :any;
  additionalResources :any;
  website :any;
  description :string;
  contacts :any;
}

export interface IContacts {
  resource :IResources;
  role :any;
  roleDescription :string;
  firstName :string;
  lastName :string;
  prefix :string;
  initials :string;
  title :any;
  organisation :IOrganisations;
  email :string;
  orcid :string;
  homepage :string;
  photo :any;
  expertise :string;
}

export interface IExtendedResources {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  externalIdentifiers :any;
  contacts :any;
  logo :any;
  countries :any;
  datasets :any;
  publications :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
}

export interface IDataResources {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  localName :string;
  keywords :string;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  externalIdentifiers :any;
  contacts :any;
  logo :any;
  numberOfParticipants :number;
  numberOfParticipantsWithSamples :number;
  countries :any;
  regions :any;
  populationAgeGroups :any;
  populationDisease :any;
  populationOncologyTopology :any;
  populationOncologyMorphology :any;
  datasets :any;
  prelinked :boolean;
  linkagePossibilityDescription :string;
  dataHolder :IOrganisations;
  dAPs :any;
  designPaper :any;
  publications :any;
  informedConsentType :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
  supplementaryInformation :string;
}

export interface IDatasets {
  resource :IExtendedResources;
  name :string;
  label :string;
  unitOfObservation :any;
  keywords :any;
  description :string;
  numberOfRows :number;
  mappedTo :any;
  mappedFrom :any;
  sinceVersion :string;
  untilVersion :string;
}

export interface IExternalIdentifiers {
  resource :IExtendedResources;
  identifier :string;
  externalIdentifierType :any;
  externalIdentifierTypeOther :string;
}

export interface IMappings {
  source :IExtendedResources;
  sourceVersion :string;
  target :IModels;
  targetVersion :string;
  cdmsOther :string;
  mappingStatus :any;
  eTLFrequency :number;
  eTLSpecificationUrl :any;
  eTLSpecificationDocument :any;
}

export interface IRWEResources {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  localName :string;
  type :any;
  typeOther :string;
  keywords :string;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  dataCollectionDescription :string;
  dateEstablished :string;
  startDataCollection :string;
  endDataCollection :string;
  timeSpanDescription :string;
  externalIdentifiers :any;
  contacts :any;
  logo :any;
  numberOfParticipants :number;
  numberOfParticipantsWithSamples :number;
  countries :any;
  regions :any;
  populationAgeGroups :any;
  populationEntry :any;
  populationEntryOther :string;
  populationExit :any;
  populationExitOther :string;
  populationDisease :any;
  populationOncologyTopology :any;
  populationOncologyMorphology :any;
  populationCoverage :string;
  populationNotCovered :string;
  quantantitativeInformation :any;
  datasets :any;
  mappingsToDataModels :any;
  areasOfInformation :any;
  qualityOfLifeOther :string;
  causeOfDeathCodeOther :string;
  indicationVocabularyOther :string;
  geneticDataVocabularyOther :string;
  careSettingOther :string;
  medicinalProductVocabularyOther :string;
  prescriptionsVocabularyOther :string;
  dispensingsVocabularyOther :string;
  proceduresVocabularyOther :string;
  biomarkerDataVocabularyOther :string;
  diagnosisMedicalEventVocabularyOther :string;
  diseaseDetails :any;
  diseaseDetailsOther :string;
  biospecimenCollected :any;
  languages :any;
  recordTrigger :string;
  prelinked :boolean;
  linkageDescription :string;
  linkagePossibility :boolean;
  linkagePossibilityDescription :string;
  linkedResources :any;
  dataHolder :IOrganisations;
  dAPs :any;
  informedConsent :any;
  informedConsentOther :string;
  accessIdentifiableData :string;
  accessIdentifiableDataRoute :string;
  accessSubjectDetails :boolean;
  accessSubjectDetailsRoute :string;
  auditPossible :boolean;
  standardOperatingProcedures :boolean;
  biospecimenAccess :boolean;
  biospecimenAccessConditions :string;
  governanceDetails :string;
  approvalForPublication :boolean;
  preservation :boolean;
  preservationDuration :number;
  refreshPeriod :any;
  dateLastRefresh :string;
  qualification :boolean;
  qualificationsDescription :string;
  accessForValidation :boolean;
  qualityValidationFrequency :string;
  qualityValidationMethods :string;
  correctionMethods :string;
  qualityValidationResults :string;
  cdms :any;
  cdmsOther :string;
  designPaper :any;
  publications :any;
  informedConsentType :any;
  fundingSources :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
  supplementaryInformation :string;
  networks :any;
  studies :any;
}

export interface ISubcohortCounts {
  subcohort :ISubcohorts;
  ageGroup :any;
  nTotal :number;
  nFemale :number;
  nMale :number;
}

export interface IAllVariables {
  resource :IExtendedResources;
  dataset :IDatasets;
  name :string;
  label :string;
  collectionEvent :ICollectionEvents;
  sinceVersion :string;
  untilVersion :string;
  networkVariables :any;
  mappings :any;
}

export interface IModels {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  externalIdentifiers :any;
  releaseFrequency :number;
  contacts :any;
  logo :any;
  countries :any;
  datasets :any;
  publications :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
}

export interface IQuantitativeInformation {
  resource :IExtendedResources;
  ageGroup :any;
  populationSize :number;
  activeSize :number;
  noIndividualsWithSamples :number;
  meanObservationYears :number;
  meanYearsActive :number;
  medianAge :number;
  proportionFemale :number;
}

export interface ISubcohorts {
  resource :IExtendedResources;
  name :string;
  description :string;
  numberOfParticipants :number;
  counts :any;
  inclusionStart :number;
  inclusionEnd :number;
  ageGroups :any;
  mainMedicalCondition :any;
  comorbidity :any;
  countries :any;
  regions :any;
  inclusionCriteria :string;
  supplementaryInformation :string;
}

export interface ICollectionEvents {
  resource :IExtendedResources;
  name :string;
  description :string;
  subcohorts :any;
  startYear :any;
  startMonth :any;
  endYear :any;
  endMonth :any;
  ageGroups :any;
  numberOfParticipants :number;
  areasOfInformation :any;
  dataCategories :any;
  sampleCategories :any;
  standardizedTools :any;
  standardizedToolsOther :string;
  coreVariables :string[];
  supplementaryInformation :string;
}

export interface ICohorts {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  localName :string;
  keywords :string;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  externalIdentifiers :any;
  contactEmail :string;
  contacts :any;
  type :any;
  typeOther :string;
  design :any;
  designDescription :string;
  designSchematic :any;
  collectionType :any;
  logo :any;
  numberOfParticipants :number;
  numberOfParticipantsWithSamples :number;
  countries :any;
  regions :any;
  populationAgeGroups :any;
  inclusionCriteria :any;
  otherInclusionCriteria :string;
  startYear :number;
  endYear :number;
  populationDisease :any;
  populationOncologyTopology :any;
  populationOncologyMorphology :any;
  subcohorts :any;
  datasets :any;
  collectionEvents :any;
  prelinked :boolean;
  linkagePossibilityDescription :string;
  releaseType :any;
  releaseDescription :string;
  linkageOptions :string;
  dataHolder :IOrganisations;
  dAPs :any;
  dataAccessConditions :any;
  dataUseConditions :any;
  dataAccessConditionsDescription :string;
  dataAccessFee :boolean;
  designPaper :any;
  publications :any;
  informedConsentType :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
  supplementaryInformation :string;
  studies :any;
  networks :any;
}

export interface IDataSources {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  localName :string;
  type :any;
  typeOther :string;
  keywords :string;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  dataCollectionDescription :string;
  dateEstablished :string;
  startDataCollection :string;
  endDataCollection :string;
  timeSpanDescription :string;
  externalIdentifiers :any;
  contacts :any;
  logo :any;
  numberOfParticipants :number;
  numberOfParticipantsWithSamples :number;
  countries :any;
  regions :any;
  populationAgeGroups :any;
  populationEntry :any;
  populationEntryOther :string;
  populationExit :any;
  populationExitOther :string;
  populationDisease :any;
  populationOncologyTopology :any;
  populationOncologyMorphology :any;
  populationCoverage :string;
  populationNotCovered :string;
  quantantitativeInformation :any;
  datasets :any;
  mappingsToDataModels :any;
  areasOfInformation :any;
  qualityOfLifeOther :string;
  causeOfDeathCodeOther :string;
  indicationVocabularyOther :string;
  geneticDataVocabularyOther :string;
  careSettingOther :string;
  medicinalProductVocabularyOther :string;
  prescriptionsVocabularyOther :string;
  dispensingsVocabularyOther :string;
  proceduresVocabularyOther :string;
  biomarkerDataVocabularyOther :string;
  diagnosisMedicalEventVocabularyOther :string;
  diseaseDetails :any;
  diseaseDetailsOther :string;
  biospecimenCollected :any;
  languages :any;
  recordTrigger :string;
  prelinked :boolean;
  linkageDescription :string;
  linkagePossibility :boolean;
  linkagePossibilityDescription :string;
  linkedResources :any;
  dataHolder :IOrganisations;
  dAPs :any;
  informedConsent :any;
  informedConsentOther :string;
  accessIdentifiableData :string;
  accessIdentifiableDataRoute :string;
  accessSubjectDetails :boolean;
  accessSubjectDetailsRoute :string;
  auditPossible :boolean;
  standardOperatingProcedures :boolean;
  biospecimenAccess :boolean;
  biospecimenAccessConditions :string;
  governanceDetails :string;
  approvalForPublication :boolean;
  preservation :boolean;
  preservationDuration :number;
  refreshPeriod :any;
  dateLastRefresh :string;
  qualification :boolean;
  qualificationsDescription :string;
  accessForValidation :boolean;
  qualityValidationFrequency :string;
  qualityValidationMethods :string;
  correctionMethods :string;
  qualityValidationResults :string;
  cdms :any;
  cdmsOther :string;
  designPaper :any;
  publications :any;
  informedConsentType :any;
  fundingSources :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
  supplementaryInformation :string;
  networks :any;
  studies :any;
}

export interface IDatasetMappings {
  source :IExtendedResources;
  sourceDataset :IDatasets;
  target :IExtendedResources;
  targetDataset :IDatasets;
  order :number;
  description :string;
  syntax :string;
}

export interface ILinkedResources {
  mainResource :IRWEResources;
  linkedResource :IRWEResources;
  otherLinkedResource :string;
  linkageStrategy :any;
  linkageVariable :string;
  linkageVariableUnique :boolean;
  linkageCompleteness :string;
  preLinked :boolean;
}

export interface ISubmissions {
  submissionDate :string;
  submitterName :string;
  resources :any;
  submitterEmail :string;
  submitterOrganisation :IOrganisations;
  submitterRole :any;
  submitterRoleOther :string;
  submissionType :any;
  submissionDescription :string;
  responsiblePersons :string;
  acceptanceDate :string;
}

export interface IVariables {
  resource :IExtendedResources;
  dataset :IDatasets;
  name :string;
  label :string;
  collectionEvent :ICollectionEvents;
  sinceVersion :string;
  untilVersion :string;
  networkVariables :any;
  format :any;
  unit :any;
  references :IAllVariables;
  mandatory :boolean;
  description :string;
  order :number;
  exampleValues :string[];
  permittedValues :any;
  keywords :any;
  repeats :any;
  vocabularies :any;
  notes :string;
  mappings :any;
}

export interface IDAPs {
  organisation :IOrganisations;
  resource :IResources;
  isDataAccessProvider :any;
  reasonAccessOther :string;
  populationSubsetOther :string;
  processTime :number;
}

export interface IDocumentation {
  resource :IExtendedResources;
  name :string;
  type :any;
  description :string;
  url :any;
  file :any;
}

export interface IRepeatedVariables {
  resource :IExtendedResources;
  dataset :IDatasets;
  name :string;
  label :string;
  collectionEvent :ICollectionEvents;
  sinceVersion :string;
  untilVersion :string;
  networkVariables :any;
  mappings :any;
  isRepeatOf :IVariables;
}

export interface IVariableMappings {
  source :IExtendedResources;
  sourceDataset :IDatasets;
  sourceVariables :any;
  sourceVariablesOtherDatasets :any;
  target :IExtendedResources;
  targetDataset :IDatasets;
  targetVariable :IAllVariables;
  match :any;
  status :any;
  description :string;
  syntax :string;
  comments :string;
}

export interface IDatabanks {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  localName :string;
  type :any;
  typeOther :string;
  keywords :string;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  dataCollectionDescription :string;
  dateEstablished :string;
  startDataCollection :string;
  endDataCollection :string;
  timeSpanDescription :string;
  externalIdentifiers :any;
  contacts :any;
  logo :any;
  numberOfParticipants :number;
  numberOfParticipantsWithSamples :number;
  underlyingPopulation :string;
  countries :any;
  regions :any;
  populationAgeGroups :any;
  populationEntry :any;
  populationEntryOther :string;
  populationExit :any;
  populationExitOther :string;
  populationDisease :any;
  populationOncologyTopology :any;
  populationOncologyMorphology :any;
  populationCoverage :string;
  populationNotCovered :string;
  quantantitativeInformation :any;
  datasets :any;
  mappingsToDataModels :any;
  areasOfInformation :any;
  qualityOfLifeOther :string;
  causeOfDeathCodeOther :string;
  indicationVocabularyOther :string;
  geneticDataVocabularyOther :string;
  careSettingOther :string;
  medicinalProductVocabularyOther :string;
  prescriptionsVocabularyOther :string;
  dispensingsVocabularyOther :string;
  proceduresVocabularyOther :string;
  biomarkerDataVocabularyOther :string;
  diagnosisMedicalEventVocabularyOther :string;
  dataDictionaryAvailable :boolean;
  diseaseDetails :any;
  diseaseDetailsOther :string;
  biospecimenCollected :any;
  languages :any;
  recordTrigger :string;
  unitOfObservation :string;
  multipleEntries :boolean;
  hasIdentifier :boolean;
  identifierDescription :string;
  prelinked :boolean;
  linkageDescription :string;
  linkagePossibility :boolean;
  linkagePossibilityDescription :string;
  linkedResources :any;
  dataHolder :IOrganisations;
  dAPs :any;
  reasonSustained :string;
  informedConsent :any;
  informedConsentOther :string;
  accessIdentifiableData :string;
  accessIdentifiableDataRoute :string;
  accessSubjectDetails :boolean;
  accessSubjectDetailsRoute :string;
  auditPossible :boolean;
  accessThirdParty :boolean;
  accessThirdPartyConditions :string;
  accessNonEU :boolean;
  accessNonEUConditions :string;
  standardOperatingProcedures :boolean;
  biospecimenAccess :boolean;
  biospecimenAccessConditions :string;
  governanceDetails :string;
  approvalForPublication :boolean;
  refresh :number;
  lagTime :number;
  preservation :boolean;
  preservationDuration :number;
  refreshPeriod :any;
  dateLastRefresh :string;
  qualification :boolean;
  qualificationsDescription :string;
  numberOfRecords :number;
  completeness :string;
  completenessOverTime :string;
  completenessResults :string;
  qualityDescription :string;
  qualityOverTime :string;
  accessForValidation :boolean;
  qualityValidationFrequency :string;
  qualityValidationMethods :string;
  correctionMethods :string;
  qualityValidationResults :string;
  cdms :any;
  cdmsOther :string;
  eTLStandardVocabularies :any;
  eTLStandardVocabulariesOther :string;
  designPaper :any;
  publications :any;
  informedConsentType :any;
  fundingSources :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
  supplementaryInformation :string;
  networks :any;
  studies :any;
}

export interface INetworks {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  type :any;
  features :any;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  externalIdentifiers :any;
  contacts :any;
  logo :any;
  countries :any;
  startYear :number;
  endYear :number;
  datasets :any;
  publications :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
  dataSources :any;
  databanks :any;
  cohorts :any;
  models :any;
  studies :any;
  networks :any;
  partOfNetworks :any;
}

export interface IVariableValues {
  resource :IExtendedResources;
  variable :IVariables;
  value :string;
  label :string;
  order :number;
  isMissing :boolean;
  ontologyTermURI :string;
  sinceVersion :string;
  untilVersion :string;
}

export interface ICatalogues {
  network :INetworks;
  type :any;
  publisher :any;
}

export interface IStudies {
  id :string;
  pid :string;
  acronym :string;
  name :string;
  type :any;
  typeOther :string;
  website :any;
  leadOrganisation :any;
  additionalOrganisations :any;
  description :string;
  externalIdentifiers :any;
  status :any;
  contacts :any;
  logo :any;
  countries :any;
  datasets :any;
  publications :any;
  fundingScheme :any;
  fundingStatement :string;
  acknowledgements :string;
  documentation :any;
  networks :any;
  networksOther :string;
  studyRequirements :any;
  regulatoryProcedureNumber :string;
  dateOfSigningFundingContractPlanned :string;
  dateOfSigningFundingContractActual :string;
  collectionStartPlanned :string;
  collectionStartActual :string;
  analysisStartPlanned :string;
  analysisStartActual :string;
  interimReportPlanned :string;
  interimReportActual :string;
  finalReportPlanned :string;
  finalReportActual :string;
  dataSources :any;
  dataSourcesOther :string;
  databanks :any;
  databanksOther :string;
  cohorts :any;
  cdms :any;
  studyFeatures :any;
  dataCharacterisationDetails :string;
  dataSourceTypes :any;
  dataSourceTypesOther :string;
  qualityMarks :any;
  numberOfDataSources :string;
  medicinesStudiedINNCodes :any;
  medicinesStudiedATCCodes :any;
  medicinesStudiesBrands :any;
  medicinesStudiedOther :string;
  medicalConditionsStudied :any;
  medicalConditionsStudiedOther :string;
  dataExtractionDate :string;
  studySetting :string;
  analysisPlan :string;
  populationDescription :string;
  numberOfSubjects :number;
  ageGroups :any;
  objectives :string;
  interventions :string;
  comparators :string;
  outcomes :string;
  studyDesign :string;
  results :string;
  topic :any;
  topicOther :string;
  trialRegulatoryScope :any;
  studyDesignClassification :any;
  studyDesignClassificationOther :string;
  studyScope :any;
  studyScopeOther :string;
  populationOfInterest :any;
  populationOfInterestOther :string;
}

export interface INetworkVariables {
  network :INetworks;
  variable :IAllVariables;
}


