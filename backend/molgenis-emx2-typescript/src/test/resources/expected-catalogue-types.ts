// Generated (on: 2024-09-02) from Generator.java for schema: catalogue

export interface IFile {
    id?: string;
    size?: number;
    extension?: string;
    url?: string;
}

export interface ITreeNode {
    name: string;
    children?: ITreeNode[];
    parent?: string;
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
    parent?: IAgeGroups;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IAgeGroups[];
}

export interface IAreasOfInformationCohorts {
    order?: number;
    name: string;
    label?: string;
    parent?: IAreasOfInformationCohorts;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IAreasOfInformationCohorts[];
}

export interface IAreasOfInformationDs {
    order?: number;
    name: string;
    label?: string;
    parent?: IAreasOfInformationDs;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IAreasOfInformationDs[];
}

export interface IBiospecimens {
    order?: number;
    name: string;
    label?: string;
    parent?: IBiospecimens;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IBiospecimens[];
}

export interface ICatalogueTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: ICatalogueTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ICatalogueTypes[];
}

export interface ICatalogues {
    name: string;
    network: IResources;
    type: IOntologyNode;
    description: string;
    publisher?: string;
}

export interface IClinicalStudyTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IClinicalStudyTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IClinicalStudyTypes[];
}

export interface ICohortCollectionTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: ICohortCollectionTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ICohortCollectionTypes[];
}

export interface ICohortDesigns {
    order?: number;
    name: string;
    label?: string;
    parent?: ICohortDesigns;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ICohortDesigns[];
}

export interface ICohortStudyTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: ICohortStudyTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ICohortStudyTypes[];
}

export interface IContributionTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IContributionTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IContributionTypes[];
}

export interface ICountries {
    order?: number;
    name: string;
    label?: string;
    parent?: ICountries;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ICountries[];
}

export interface IDAPInformation {
    order?: number;
    name: string;
    label?: string;
    parent?: IDAPInformation;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IDAPInformation[];
}

export interface IDataAccessConditions {
    order?: number;
    name: string;
    label?: string;
    parent?: IDataAccessConditions;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IDataAccessConditions[];
}

export interface IDataCategories {
    order?: number;
    name: string;
    label?: string;
    parent?: IDataCategories;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IDataCategories[];
}

export interface IDataUseConditions {
    order?: number;
    name: string;
    label?: string;
    parent?: IDataUseConditions;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IDataUseConditions[];
}

export interface IDatasetTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IDatasetTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IDatasetTypes[];
}

export interface IDatasourceTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IDatasourceTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IDatasourceTypes[];
}

export interface IDiseases {
    order?: number;
    name: string;
    label?: string;
    parent?: IDiseases;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IDiseases[];
}

export interface IDocumentTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IDocumentTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IDocumentTypes[];
}

export interface IExternalIdentifierTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IExternalIdentifierTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IExternalIdentifierTypes[];
}

export interface IFormats {
    order?: number;
    name: string;
    label?: string;
    parent?: IFormats;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IFormats[];
}

export interface IFundingTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IFundingTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IFundingTypes[];
}

export interface IICDOMorphologies {
    order?: number;
    name: string;
    label?: string;
    parent?: IICDOMorphologies;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IICDOMorphologies[];
}

export interface IICDOTopologies {
    order?: number;
    name: string;
    label?: string;
    parent?: IICDOTopologies;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IICDOTopologies[];
}

export interface IInclusionCriteria {
    order?: number;
    name: string;
    label?: string;
    parent?: IInclusionCriteria;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IInclusionCriteria[];
}

export interface IInformedConsentRequired {
    order?: number;
    name: string;
    label?: string;
    parent?: IInformedConsentRequired;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IInformedConsentRequired[];
}

export interface IInformedConsentTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IInformedConsentTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IInformedConsentTypes[];
}

export interface IKeywords {
    order?: number;
    name: string;
    label?: string;
    parent?: IKeywords;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IKeywords[];
}

export interface ILanguages {
    order?: number;
    name: string;
    label?: string;
    parent?: ILanguages;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ILanguages[];
}

export interface ILinkageStrategies {
    order?: number;
    name: string;
    label?: string;
    parent?: ILinkageStrategies;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ILinkageStrategies[];
}

export interface IMappedDatasets {
    source: IResources;
    sourceDataset: IResourceDatasets;
    target: IResources;
    targetDataset: IResourceDatasets;
    order?: number;
    description?: string;
    syntax?: string;
}

export interface IMappedResources {
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

export interface IMappedVariables {
    source: IResources;
    sourceDataset: IResourceDatasets;
    sourceVariables?: IVariables[];
    sourceVariablesOtherDatasets?: IVariables[];
    target: IResources;
    targetDataset: IResourceDatasets;
    targetVariable: IVariables;
    repeats: string;
    match: IOntologyNode;
    description?: string;
    syntax?: string;
    comments?: string;
}

export interface IMappingStatus {
    order?: number;
    name: string;
    label?: string;
    parent?: IMappingStatus;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IMappingStatus[];
}

export interface IMedDRA {
    order?: number;
    name: string;
    label?: string;
    parent?: IMedDRA;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IMedDRA[];
}

export interface INetworkTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: INetworkTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: INetworkTypes[];
}

export interface IObservationTargets {
    order?: number;
    name: string;
    label?: string;
    parent?: IObservationTargets;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IObservationTargets[];
}

export interface IOrganisationRoles {
    order?: number;
    name: string;
    label?: string;
    parent?: IOrganisationRoles;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IOrganisationRoles[];
}

export interface IOrganisations {
    order?: number;
    name: string;
    label?: string;
    parent?: IOrganisations;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IOrganisations[];
}

export interface IPopulationEntry {
    order?: number;
    name: string;
    label?: string;
    parent?: IPopulationEntry;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IPopulationEntry[];
}

export interface IPopulationExit {
    order?: number;
    name: string;
    label?: string;
    parent?: IPopulationExit;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IPopulationExit[];
}

export interface IPopulationOfInterest {
    order?: number;
    name: string;
    label?: string;
    parent?: IPopulationOfInterest;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IPopulationOfInterest[];
}

export interface IRefreshPeriods {
    order?: number;
    name: string;
    label?: string;
    parent?: IRefreshPeriods;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IRefreshPeriods[];
}

export interface IRegions {
    order?: number;
    name: string;
    label?: string;
    parent?: IRegions;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IRegions[];
}

export interface IReleaseTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: IReleaseTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IReleaseTypes[];
}

export interface IResourceDAPs {
    resource: IResources;
    organisation: IOntologyNode;
    isDataAccessProvider?: IOntologyNode[];
    reasonAccessOther?: string;
    populationSubsetOther?: string;
    processTime?: number;
}

export interface IResourceCohortCounts {
    resource: IResources;
    cohort: IResourceCohorts;
    ageGroup: IOntologyNode;
    nTotal?: number;
    nFemale?: number;
    nMale?: number;
}

export interface IResourceCohorts {
    resource: IResources;
    name: string;
    description?: string;
    numberOfParticipants?: number;
    counts?: IResourceCohortCounts[];
    inclusionStart?: number;
    inclusionEnd?: number;
    ageGroups?: IOntologyNode[];
    mainMedicalCondition?: IOntologyNode[];
    comorbidity?: IOntologyNode[];
    countries?: IOntologyNode[];
    regions?: IOntologyNode[];
    inclusionCriteria?: string;
}

export interface IResourceCollectionEvents {
    resource: IResources;
    name: string;
    description?: string;
    cohorts?: IResourceCohorts[];
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
}

export interface IResourceContacts {
    resource: IResources;
    role?: IOntologyNode[];
    roleDescription?: string;
    firstName: string;
    lastName: string;
    displayName: string;
    prefix?: string;
    initials?: string;
    title?: IOntologyNode;
    organisation?: IResourceOrganisations;
    email?: string;
    orcid?: string;
    homepage?: string;
    photo?: IFile;
    expertise?: string;
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

export interface IResourceDatasets {
    resource: IResources;
    name: string;
    label?: string;
    datasetType?: IOntologyNode[];
    unitOfObservation?: IOntologyNode;
    keywords?: IOntologyNode[];
    description?: string;
    numberOfRows?: number;
    mappedTo?: IMappedDatasets[];
    mappedFrom?: IMappedDatasets[];
    sinceVersion?: string;
    untilVersion?: string;
}

export interface IResourceDocumentation {
    resource: IResources;
    name: string;
    type?: IOntologyNode;
    description?: string;
    url?: string;
    file?: IFile;
}

export interface IResourceExternalIdentifiers {
    resource: IResources;
    identifier: string;
    externalIdentifierType?: IOntologyNode;
    externalIdentifierTypeOther?: string;
}

export interface IResourceLinkages {
    resource: IResources;
    linkedResource: IResources;
    otherLinkedResource?: string;
    linkageStrategy?: IOntologyNode;
    linkageVariable?: string;
    linkageVariableUnique?: boolean;
    linkageCompleteness?: string;
    preLinked?: boolean;
}

export interface IResourceOrganisations {
    resource: IResources;
    id: string;
    pid?: string;
    name: string;
    acronym?: string;
    logo?: IFile;
    country?: IOntologyNode[];
    website?: string;
    role?: IOntologyNode[];
}

export interface IResourcePublications {
    resource: IResources;
    doi: string;
    title: string;
    isDesignPublication?: boolean;
    reference?: string;
}

export interface IResourceSamplesets {
    resource: IResources;
    name: string;
    sampleTypes?: IOntologyNode[];
}

export interface IResourceTypesFLAT {
    order?: number;
    name: string;
    label?: string;
    parent?: IResourceTypesFLAT;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IResourceTypesFLAT[];
}

export interface IResources {
    id: string;
    pid?: string;
    name: string;
    localName?: string;
    acronym?: string;
    type?: IOntologyNode[];
    typeOther?: string;
    cohortType?: IOntologyNode[];
    clinicalStudyType?: IOntologyNode[];
    rWDType?: IOntologyNode[];
    networkType?: IOntologyNode[];
    website?: string;
    description?: string;
    keywords?: string;
    externalIdentifiers?: IResourceExternalIdentifiers[];
    dateEstablished?: string;
    startDataCollection?: string;
    endDataCollection?: string;
    contactEmail?: string;
    logo?: IFile;
    status?: IOntologyNode;
    license?: string;
    designType?: IOntologyNode;
    designDescription?: string;
    designSchematic?: IFile;
    dataCollectionType?: IOntologyNode[];
    dataCollectionDescription?: string;
    reasonSustained?: string;
    unitOfObservation?: string;
    recordTrigger?: string;
    cohorts?: IResourceCohorts[];
    collectionEvents?: IResourceCollectionEvents[];
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
    peopleInvolved?: IResourceContacts[];
    organisationsInvolved?: IResourceOrganisations[];
    networksInvolved?: IResources[];
    datasets?: IResourceDatasets[];
    samplesets?: IResourceSamplesets[];
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
    linkedResources?: IResourceLinkages[];
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
    dAPs?: IResourceDAPs[];
    releaseType?: IOntologyNode;
    releaseDescription?: string;
    numberOfRecords?: number;
    releaseFrequency?: number;
    refreshTime?: number;
    lagTime?: number;
    refreshPeriod?: IOntologyNode[];
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
    mappingsToCommonDataModels?: IMappedDatasets[];
    commonDataModelsOther?: string;
    eTLStandardVocabularies?: IOntologyNode[];
    eTLStandardVocabulariesOther?: string;
    publications?: IResourcePublications[];
    fundingSources?: IOntologyNode[];
    fundingScheme?: IOntologyNode[];
    fundingStatement?: string;
    citationRequirements?: string;
    acknowledgements?: string;
    documentation?: IResourceDocumentation[];
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
}

export interface ISampleCategories {
    order?: number;
    name: string;
    label?: string;
    parent?: ISampleCategories;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ISampleCategories[];
}

export interface ISampleTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: ISampleTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ISampleTypes[];
}

export interface IStandardizedTools {
    order?: number;
    name: string;
    label?: string;
    parent?: IStandardizedTools;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IStandardizedTools[];
}

export interface IStatusDetails {
    order?: number;
    name: string;
    label?: string;
    parent?: IStatusDetails;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IStatusDetails[];
}

export interface IStudyFunding {
    order?: number;
    name: string;
    label?: string;
    parent?: IStudyFunding;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IStudyFunding[];
}

export interface IStudyStatus {
    order?: number;
    name: string;
    label?: string;
    parent?: IStudyStatus;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IStudyStatus[];
}

export interface ISubmissionTypes {
    order?: number;
    name: string;
    label?: string;
    parent?: ISubmissionTypes;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ISubmissionTypes[];
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

export interface ISubmitterRoles {
    order?: number;
    name: string;
    label?: string;
    parent?: ISubmitterRoles;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ISubmitterRoles[];
}

export interface ITitles {
    order?: number;
    name: string;
    label?: string;
    parent?: ITitles;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: ITitles[];
}

export interface IUnits {
    order?: number;
    name: string;
    label?: string;
    parent?: IUnits;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IUnits[];
}

export interface IVariableRepeatUnits {
    order?: number;
    name: string;
    label?: string;
    parent?: IVariableRepeatUnits;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IVariableRepeatUnits[];
}

export interface IVariableValues {
    resource: IResources;
    dataset: IResourceDatasets;
    name: IVariables;
    value: string;
    label: string;
    order?: number;
    isMissing?: boolean;
    ontologyTermURI?: string;
    sinceVersion?: string;
    untilVersion?: string;
}

export interface IVariables {
    resource: IResources;
    dataset: IResourceDatasets;
    name: string;
    useExternalDefinition?: IVariables;
    label?: string;
    description?: string;
    collectionEvent?: IResourceCollectionEvents[];
    format?: IOntologyNode;
    unit?: IOntologyNode;
    sinceVersion?: string;
    untilVersion?: string;
    repeatUnit?: IOntologyNode;
    repeatMin?: number;
    repeatMax?: number;
    exampleValues?: string[];
    permittedValues?: IVariableValues[];
    keywords?: IOntologyNode[];
    vocabularies?: IOntologyNode[];
    notes?: string;
    mappings?: IMappedVariables[];
}

export interface IVersion {
}

export interface IVocabularies {
    order?: number;
    name: string;
    label?: string;
    parent?: IVocabularies;
    codesystem?: string;
    code?: string;
    ontologyTermURI?: string;
    definition?: string;
    children?: IVocabularies[];
}


