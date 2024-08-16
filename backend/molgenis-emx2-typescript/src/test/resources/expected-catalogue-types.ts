// Generated (on: 2024-08-16T14:43:24.827769) from Generator.java for schema: GeneratorTestCatalogue

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

export interface IATC {
    order :number;
    name :string;
    label :string;
    parent :IATC;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IATC[];
}

export interface IAgeGroups {
    order :number;
    name :string;
    label :string;
    parent :IAgeGroups;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IAgeGroups[];
}

export interface IAllVariables {
    resource :IExtendedResources;
    dataset :IDatasets;
    name :string;
    label :string;
    collectionEvent :ICollectionEvents;
    sinceVersion :string;
    untilVersion :string;
    networkVariables :INetworkVariables[];
    mappings :IVariableMappings[];
}

export interface IAreasOfInformationCohorts {
    order :number;
    name :string;
    label :string;
    parent :IAreasOfInformationCohorts;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IAreasOfInformationCohorts[];
}

export interface IAreasOfInformationDs {
    order :number;
    name :string;
    label :string;
    parent :IAreasOfInformationDs;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IAreasOfInformationDs[];
}

export interface IBiospecimens {
    order :number;
    name :string;
    label :string;
    parent :IBiospecimens;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IBiospecimens[];
}

export interface ICatalogueTypes {
    order :number;
    name :string;
    label :string;
    parent :ICatalogueTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ICatalogueTypes[];
}

export interface ICatalogues {
    network :INetworks;
    type :IOntologyNode;
    publisher :IOntologyNode;
}

export interface ICohortDesigns {
    order :number;
    name :string;
    label :string;
    parent :ICohortDesigns;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ICohortDesigns[];
}

export interface ICohorts {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    localName :string;
    keywords :string;
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    externalIdentifiers :IExternalIdentifiers[];
    contactEmail :string;
    contacts :IContacts[];
    type :IOntologyNode[];
    typeOther :string;
    design :IOntologyNode;
    designDescription :string;
    designSchematic :IFile;
    collectionType :IOntologyNode[];
    logo :IFile;
    numberOfParticipants :number;
    numberOfParticipantsWithSamples :number;
    countries :IOntologyNode[];
    regions :IOntologyNode[];
    populationAgeGroups :IOntologyNode[];
    inclusionCriteria :IOntologyNode[];
    otherInclusionCriteria :string;
    startYear :number;
    endYear :number;
    populationDisease :IOntologyNode[];
    populationOncologyTopology :IOntologyNode[];
    populationOncologyMorphology :IOntologyNode[];
    subcohorts :ISubcohorts[];
    datasets :IDatasets[];
    collectionEvents :ICollectionEvents[];
    prelinked :boolean;
    linkagePossibilityDescription :string;
    releaseType :IOntologyNode;
    releaseDescription :string;
    linkageOptions :string;
    dataHolder :IOrganisations;
    dAPs :IDAPs[];
    dataAccessConditions :IOntologyNode[];
    dataUseConditions :IOntologyNode[];
    dataAccessConditionsDescription :string;
    dataAccessFee :boolean;
    designPaper :IPublications[];
    publications :IPublications[];
    informedConsentType :IOntologyNode;
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
    supplementaryInformation :string;
    studies :IStudies[];
    networks :INetworks[];
}

export interface ICollectionEvents {
    resource :IExtendedResources;
    name :string;
    description :string;
    subcohorts :ISubcohorts[];
    startYear :IOntologyNode;
    startMonth :IOntologyNode;
    endYear :IOntologyNode;
    endMonth :IOntologyNode;
    ageGroups :IOntologyNode[];
    numberOfParticipants :number;
    areasOfInformation :IOntologyNode[];
    dataCategories :IOntologyNode[];
    sampleCategories :IOntologyNode[];
    standardizedTools :IOntologyNode[];
    standardizedToolsOther :string;
    coreVariables :string[];
    supplementaryInformation :string;
}

export interface ICollectionTypes {
    order :number;
    name :string;
    label :string;
    parent :ICollectionTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ICollectionTypes[];
}

export interface IContacts {
    resource :IResources;
    role :IOntologyNode[];
    roleDescription :string;
    firstName :string;
    lastName :string;
    prefix :string;
    initials :string;
    title :IOntologyNode;
    organisation :IOrganisations;
    email :string;
    orcid :string;
    homepage :string;
    photo :IFile;
    expertise :string;
}

export interface IContributionTypes {
    order :number;
    name :string;
    label :string;
    parent :IContributionTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IContributionTypes[];
}

export interface ICountries {
    order :number;
    name :string;
    label :string;
    parent :ICountries;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ICountries[];
}

export interface IDAPInformation {
    order :number;
    name :string;
    label :string;
    parent :IDAPInformation;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IDAPInformation[];
}

export interface IDAPs {
    organisation :IOrganisations;
    resource :IResources;
    isDataAccessProvider :IOntologyNode[];
    reasonAccessOther :string;
    populationSubsetOther :string;
    processTime :number;
}

export interface IDataAccessConditions {
    order :number;
    name :string;
    label :string;
    parent :IDataAccessConditions;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IDataAccessConditions[];
}

export interface IDataCategories {
    order :number;
    name :string;
    label :string;
    parent :IDataCategories;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IDataCategories[];
}

export interface IDataResources {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    localName :string;
    keywords :string;
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    externalIdentifiers :IExternalIdentifiers[];
    contacts :IContacts[];
    logo :IFile;
    numberOfParticipants :number;
    numberOfParticipantsWithSamples :number;
    countries :IOntologyNode[];
    regions :IOntologyNode[];
    populationAgeGroups :IOntologyNode[];
    populationDisease :IOntologyNode[];
    populationOncologyTopology :IOntologyNode[];
    populationOncologyMorphology :IOntologyNode[];
    datasets :IDatasets[];
    prelinked :boolean;
    linkagePossibilityDescription :string;
    dataHolder :IOrganisations;
    dAPs :IDAPs[];
    designPaper :IPublications[];
    publications :IPublications[];
    informedConsentType :IOntologyNode;
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
    supplementaryInformation :string;
}

export interface IDataSources {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    localName :string;
    type :IOntologyNode[];
    typeOther :string;
    keywords :string;
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    dataCollectionDescription :string;
    dateEstablished :string;
    startDataCollection :string;
    endDataCollection :string;
    timeSpanDescription :string;
    externalIdentifiers :IExternalIdentifiers[];
    contacts :IContacts[];
    logo :IFile;
    numberOfParticipants :number;
    numberOfParticipantsWithSamples :number;
    countries :IOntologyNode[];
    regions :IOntologyNode[];
    populationAgeGroups :IOntologyNode[];
    populationEntry :IOntologyNode[];
    populationEntryOther :string;
    populationExit :IOntologyNode[];
    populationExitOther :string;
    populationDisease :IOntologyNode[];
    populationOncologyTopology :IOntologyNode[];
    populationOncologyMorphology :IOntologyNode[];
    populationCoverage :string;
    populationNotCovered :string;
    quantantitativeInformation :IQuantitativeInformation[];
    datasets :IDatasets[];
    mappingsToDataModels :IDatasetMappings[];
    areasOfInformation :IOntologyNode[];
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
    diseaseDetails :IOntologyNode[];
    diseaseDetailsOther :string;
    biospecimenCollected :IOntologyNode[];
    languages :IOntologyNode[];
    recordTrigger :string;
    prelinked :boolean;
    linkageDescription :string;
    linkagePossibility :boolean;
    linkagePossibilityDescription :string;
    linkedResources :ILinkedResources[];
    dataHolder :IOrganisations;
    dAPs :IDAPs[];
    informedConsent :IOntologyNode;
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
    refreshPeriod :IOntologyNode[];
    dateLastRefresh :string;
    qualification :boolean;
    qualificationsDescription :string;
    accessForValidation :boolean;
    qualityValidationFrequency :string;
    qualityValidationMethods :string;
    correctionMethods :string;
    qualityValidationResults :string;
    cdms :IMappings[];
    cdmsOther :string;
    designPaper :IPublications[];
    publications :IPublications[];
    informedConsentType :IOntologyNode;
    fundingSources :IOntologyNode[];
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
    supplementaryInformation :string;
    networks :INetworks[];
    studies :IStudies[];
}

export interface IDataUseConditions {
    order :number;
    name :string;
    label :string;
    parent :IDataUseConditions;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IDataUseConditions[];
}

export interface IDatabanks {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    localName :string;
    type :IOntologyNode[];
    typeOther :string;
    keywords :string;
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    dataCollectionDescription :string;
    dateEstablished :string;
    startDataCollection :string;
    endDataCollection :string;
    timeSpanDescription :string;
    externalIdentifiers :IExternalIdentifiers[];
    contacts :IContacts[];
    logo :IFile;
    numberOfParticipants :number;
    numberOfParticipantsWithSamples :number;
    underlyingPopulation :string;
    countries :IOntologyNode[];
    regions :IOntologyNode[];
    populationAgeGroups :IOntologyNode[];
    populationEntry :IOntologyNode[];
    populationEntryOther :string;
    populationExit :IOntologyNode[];
    populationExitOther :string;
    populationDisease :IOntologyNode[];
    populationOncologyTopology :IOntologyNode[];
    populationOncologyMorphology :IOntologyNode[];
    populationCoverage :string;
    populationNotCovered :string;
    quantantitativeInformation :IQuantitativeInformation[];
    datasets :IDatasets[];
    mappingsToDataModels :IDatasetMappings[];
    areasOfInformation :IOntologyNode[];
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
    diseaseDetails :IOntologyNode[];
    diseaseDetailsOther :string;
    biospecimenCollected :IOntologyNode[];
    languages :IOntologyNode[];
    recordTrigger :string;
    unitOfObservation :string;
    multipleEntries :boolean;
    hasIdentifier :boolean;
    identifierDescription :string;
    prelinked :boolean;
    linkageDescription :string;
    linkagePossibility :boolean;
    linkagePossibilityDescription :string;
    linkedResources :ILinkedResources[];
    dataHolder :IOrganisations;
    dAPs :IDAPs[];
    reasonSustained :string;
    informedConsent :IOntologyNode;
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
    refreshPeriod :IOntologyNode[];
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
    cdms :IMappings[];
    cdmsOther :string;
    eTLStandardVocabularies :IOntologyNode[];
    eTLStandardVocabulariesOther :string;
    designPaper :IPublications[];
    publications :IPublications[];
    informedConsentType :IOntologyNode;
    fundingSources :IOntologyNode[];
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
    supplementaryInformation :string;
    networks :INetworks[];
    studies :IStudies[];
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

export interface IDatasets {
    resource :IExtendedResources;
    name :string;
    label :string;
    unitOfObservation :IOntologyNode;
    keywords :IOntologyNode[];
    description :string;
    numberOfRows :number;
    mappedTo :IDatasetMappings[];
    mappedFrom :IDatasetMappings[];
    sinceVersion :string;
    untilVersion :string;
}

export interface IDatasourceTypes {
    order :number;
    name :string;
    label :string;
    parent :IDatasourceTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IDatasourceTypes[];
}

export interface IDiseases {
    order :number;
    name :string;
    label :string;
    parent :IDiseases;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IDiseases[];
}

export interface IDocumentTypes {
    order :number;
    name :string;
    label :string;
    parent :IDocumentTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IDocumentTypes[];
}

export interface IDocumentation {
    resource :IExtendedResources;
    name :string;
    type :IOntologyNode;
    description :string;
    url :string;
    file :IFile;
}

export interface IExtendedResources {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    externalIdentifiers :IExternalIdentifiers[];
    contacts :IContacts[];
    logo :IFile;
    countries :IOntologyNode[];
    datasets :IDatasets[];
    publications :IPublications[];
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
}

export interface IExternalIdentifierTypes {
    order :number;
    name :string;
    label :string;
    parent :IExternalIdentifierTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IExternalIdentifierTypes[];
}

export interface IExternalIdentifiers {
    resource :IExtendedResources;
    identifier :string;
    externalIdentifierType :IOntologyNode;
    externalIdentifierTypeOther :string;
}

export interface IFormats {
    order :number;
    name :string;
    label :string;
    parent :IFormats;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IFormats[];
}

export interface IFundingTypes {
    order :number;
    name :string;
    label :string;
    parent :IFundingTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IFundingTypes[];
}

export interface IICDOMorphologies {
    order :number;
    name :string;
    label :string;
    parent :IICDOMorphologies;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IICDOMorphologies[];
}

export interface IICDOTopologies {
    order :number;
    name :string;
    label :string;
    parent :IICDOTopologies;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IICDOTopologies[];
}

export interface IINN {
    order :number;
    name :string;
    label :string;
    parent :IINN;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IINN[];
}

export interface IInclusionCriteria {
    order :number;
    name :string;
    label :string;
    parent :IInclusionCriteria;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IInclusionCriteria[];
}

export interface IInformedConsentTypes {
    order :number;
    name :string;
    label :string;
    parent :IInformedConsentTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IInformedConsentTypes[];
}

export interface IInformedConsents {
    order :number;
    name :string;
    label :string;
    parent :IInformedConsents;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IInformedConsents[];
}

export interface IKeywords {
    order :number;
    name :string;
    label :string;
    parent :IKeywords;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IKeywords[];
}

export interface ILanguages {
    order :number;
    name :string;
    label :string;
    parent :ILanguages;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ILanguages[];
}

export interface ILinkageStrategies {
    order :number;
    name :string;
    label :string;
    parent :ILinkageStrategies;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ILinkageStrategies[];
}

export interface ILinkedResources {
    mainResource :IRWEResources;
    linkedResource :IRWEResources;
    otherLinkedResource :string;
    linkageStrategy :IOntologyNode;
    linkageVariable :string;
    linkageVariableUnique :boolean;
    linkageCompleteness :string;
    preLinked :boolean;
}

export interface IMappingStatus {
    order :number;
    name :string;
    label :string;
    parent :IMappingStatus;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IMappingStatus[];
}

export interface IMappings {
    source :IExtendedResources;
    sourceVersion :string;
    target :IModels;
    targetVersion :string;
    cdmsOther :string;
    mappingStatus :IOntologyNode;
    eTLFrequency :number;
    eTLSpecificationUrl :string;
    eTLSpecificationDocument :IFile;
}

export interface IMedDRA {
    order :number;
    name :string;
    label :string;
    parent :IMedDRA;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IMedDRA[];
}

export interface IMedicineBrandNames {
    order :number;
    name :string;
    label :string;
    parent :IMedicineBrandNames;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IMedicineBrandNames[];
}

export interface IModels {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    externalIdentifiers :IExternalIdentifiers[];
    releaseFrequency :number;
    contacts :IContacts[];
    logo :IFile;
    countries :IOntologyNode[];
    datasets :IDatasets[];
    publications :IPublications[];
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
}

export interface IMonths {
    order :number;
    name :string;
    label :string;
    parent :IMonths;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IMonths[];
}

export interface INetworkFeatures {
    order :number;
    name :string;
    label :string;
    parent :INetworkFeatures;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :INetworkFeatures[];
}

export interface INetworkTypes {
    order :number;
    name :string;
    label :string;
    parent :INetworkTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :INetworkTypes[];
}

export interface INetworkVariables {
    network :INetworks;
    variable :IAllVariables;
}

export interface INetworks {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    type :IOntologyNode[];
    features :IOntologyNode[];
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    externalIdentifiers :IExternalIdentifiers[];
    contacts :IContacts[];
    logo :IFile;
    countries :IOntologyNode[];
    startYear :number;
    endYear :number;
    datasets :IDatasets[];
    publications :IPublications[];
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
    dataSources :IDataSources[];
    databanks :IDatabanks[];
    cohorts :ICohorts[];
    models :IModels[];
    studies :IStudies[];
    networks :INetworks[];
    partOfNetworks :INetworks[];
}

export interface IObservationTargets {
    order :number;
    name :string;
    label :string;
    parent :IObservationTargets;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IObservationTargets[];
}

export interface IOrganisationFeatures {
    order :number;
    name :string;
    label :string;
    parent :IOrganisationFeatures;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IOrganisationFeatures[];
}

export interface IOrganisationRoles {
    order :number;
    name :string;
    label :string;
    parent :IOrganisationRoles;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IOrganisationRoles[];
}

export interface IOrganisationTypes {
    order :number;
    name :string;
    label :string;
    parent :IOrganisationTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IOrganisationTypes[];
}

export interface IOrganisations {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    type :IOntologyNode[];
    typeOther :string;
    institution :string;
    institutionAcronym :string;
    email :string;
    logo :IFile;
    address :string;
    expertise :string;
    country :IOntologyNode[];
    features :IOntologyNode[];
    role :IOntologyNode[];
    leadingResources :IExtendedResources[];
    additionalResources :IExtendedResources[];
    website :string;
    description :string;
    contacts :IContacts[];
}

export interface IPopulationEntry {
    order :number;
    name :string;
    label :string;
    parent :IPopulationEntry;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IPopulationEntry[];
}

export interface IPopulationExit {
    order :number;
    name :string;
    label :string;
    parent :IPopulationExit;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IPopulationExit[];
}

export interface IPopulationOfInterest {
    order :number;
    name :string;
    label :string;
    parent :IPopulationOfInterest;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IPopulationOfInterest[];
}

export interface IPublications {
    doi :string;
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
    resources :IExtendedResources[];
}

export interface IQuantitativeInformation {
    resource :IExtendedResources;
    ageGroup :IOntologyNode;
    populationSize :number;
    activeSize :number;
    noIndividualsWithSamples :number;
    meanObservationYears :number;
    meanYearsActive :number;
    medianAge :number;
    proportionFemale :number;
}

export interface IRWEResources {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    localName :string;
    type :IOntologyNode[];
    typeOther :string;
    keywords :string;
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    dataCollectionDescription :string;
    dateEstablished :string;
    startDataCollection :string;
    endDataCollection :string;
    timeSpanDescription :string;
    externalIdentifiers :IExternalIdentifiers[];
    contacts :IContacts[];
    logo :IFile;
    numberOfParticipants :number;
    numberOfParticipantsWithSamples :number;
    countries :IOntologyNode[];
    regions :IOntologyNode[];
    populationAgeGroups :IOntologyNode[];
    populationEntry :IOntologyNode[];
    populationEntryOther :string;
    populationExit :IOntologyNode[];
    populationExitOther :string;
    populationDisease :IOntologyNode[];
    populationOncologyTopology :IOntologyNode[];
    populationOncologyMorphology :IOntologyNode[];
    populationCoverage :string;
    populationNotCovered :string;
    quantantitativeInformation :IQuantitativeInformation[];
    datasets :IDatasets[];
    mappingsToDataModels :IDatasetMappings[];
    areasOfInformation :IOntologyNode[];
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
    diseaseDetails :IOntologyNode[];
    diseaseDetailsOther :string;
    biospecimenCollected :IOntologyNode[];
    languages :IOntologyNode[];
    recordTrigger :string;
    prelinked :boolean;
    linkageDescription :string;
    linkagePossibility :boolean;
    linkagePossibilityDescription :string;
    linkedResources :ILinkedResources[];
    dataHolder :IOrganisations;
    dAPs :IDAPs[];
    informedConsent :IOntologyNode;
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
    refreshPeriod :IOntologyNode[];
    dateLastRefresh :string;
    qualification :boolean;
    qualificationsDescription :string;
    accessForValidation :boolean;
    qualityValidationFrequency :string;
    qualityValidationMethods :string;
    correctionMethods :string;
    qualityValidationResults :string;
    cdms :IMappings[];
    cdmsOther :string;
    designPaper :IPublications[];
    publications :IPublications[];
    informedConsentType :IOntologyNode;
    fundingSources :IOntologyNode[];
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
    supplementaryInformation :string;
    networks :INetworks[];
    studies :IStudies[];
}

export interface IRefreshPeriods {
    order :number;
    name :string;
    label :string;
    parent :IRefreshPeriods;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IRefreshPeriods[];
}

export interface IRegions {
    order :number;
    name :string;
    label :string;
    parent :IRegions;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IRegions[];
}

export interface IReleaseTypes {
    order :number;
    name :string;
    label :string;
    parent :IReleaseTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IReleaseTypes[];
}

export interface IRepeatedVariables {
    resource :IExtendedResources;
    dataset :IDatasets;
    name :string;
    label :string;
    collectionEvent :ICollectionEvents;
    sinceVersion :string;
    untilVersion :string;
    networkVariables :INetworkVariables[];
    mappings :IVariableMappings[];
    isRepeatOf :IVariables;
}

export interface IResourceTypes {
    order :number;
    name :string;
    label :string;
    parent :IResourceTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IResourceTypes[];
}

export interface IResources {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    website :string;
    description :string;
    contacts :IContacts[];
}

export interface ISampleCategories {
    order :number;
    name :string;
    label :string;
    parent :ISampleCategories;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ISampleCategories[];
}

export interface IStandardizedTools {
    order :number;
    name :string;
    label :string;
    parent :IStandardizedTools;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStandardizedTools[];
}

export interface IStatus {
    order :number;
    name :string;
    label :string;
    parent :IStatus;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStatus[];
}

export interface IStatusDetails {
    order :number;
    name :string;
    label :string;
    parent :IStatusDetails;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStatusDetails[];
}

export interface IStudies {
    id :string;
    pid :string;
    acronym :string;
    name :string;
    type :IOntologyNode;
    typeOther :string;
    website :string;
    leadOrganisation :IOrganisations[];
    additionalOrganisations :IOrganisations[];
    description :string;
    externalIdentifiers :IExternalIdentifiers[];
    status :IOntologyNode;
    contacts :IContacts[];
    logo :IFile;
    countries :IOntologyNode[];
    datasets :IDatasets[];
    publications :IPublications[];
    fundingScheme :IOntologyNode[];
    fundingStatement :string;
    acknowledgements :string;
    documentation :IDocumentation[];
    networks :INetworks[];
    networksOther :string;
    studyRequirements :IOntologyNode[];
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
    dataSources :IDataSources[];
    dataSourcesOther :string;
    databanks :IDatabanks[];
    databanksOther :string;
    cohorts :ICohorts[];
    cdms :IMappings[];
    studyFeatures :IOntologyNode[];
    dataCharacterisationDetails :string;
    dataSourceTypes :IOntologyNode[];
    dataSourceTypesOther :string;
    qualityMarks :IOntologyNode[];
    numberOfDataSources :string;
    medicinesStudiedINNCodes :IOntologyNode[];
    medicinesStudiedATCCodes :IOntologyNode[];
    medicinesStudiesBrands :IOntologyNode[];
    medicinesStudiedOther :string;
    medicalConditionsStudied :IOntologyNode[];
    medicalConditionsStudiedOther :string;
    dataExtractionDate :string;
    studySetting :string;
    analysisPlan :string;
    populationDescription :string;
    numberOfSubjects :number;
    ageGroups :IOntologyNode[];
    objectives :string;
    interventions :string;
    comparators :string;
    outcomes :string;
    studyDesign :string;
    results :string;
    topic :IOntologyNode[];
    topicOther :string;
    trialRegulatoryScope :IOntologyNode[];
    studyDesignClassification :IOntologyNode[];
    studyDesignClassificationOther :string;
    studyScope :IOntologyNode[];
    studyScopeOther :string;
    populationOfInterest :IOntologyNode[];
    populationOfInterestOther :string;
}

export interface IStudyDatasourceTypes {
    order :number;
    name :string;
    label :string;
    parent :IStudyDatasourceTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyDatasourceTypes[];
}

export interface IStudyDesignClassification {
    order :number;
    name :string;
    label :string;
    parent :IStudyDesignClassification;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyDesignClassification[];
}

export interface IStudyFeatures {
    order :number;
    name :string;
    label :string;
    parent :IStudyFeatures;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyFeatures[];
}

export interface IStudyFunding {
    order :number;
    name :string;
    label :string;
    parent :IStudyFunding;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyFunding[];
}

export interface IStudyQualityMarks {
    order :number;
    name :string;
    label :string;
    parent :IStudyQualityMarks;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyQualityMarks[];
}

export interface IStudyRequirements {
    order :number;
    name :string;
    label :string;
    parent :IStudyRequirements;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyRequirements[];
}

export interface IStudyScopes {
    order :number;
    name :string;
    label :string;
    parent :IStudyScopes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyScopes[];
}

export interface IStudyStatus {
    order :number;
    name :string;
    label :string;
    parent :IStudyStatus;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyStatus[];
}

export interface IStudyTopics {
    order :number;
    name :string;
    label :string;
    parent :IStudyTopics;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyTopics[];
}

export interface IStudyTrialRegulatoryScopes {
    order :number;
    name :string;
    label :string;
    parent :IStudyTrialRegulatoryScopes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyTrialRegulatoryScopes[];
}

export interface IStudyTypes {
    order :number;
    name :string;
    label :string;
    parent :IStudyTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IStudyTypes[];
}

export interface ISubcohortCounts {
    subcohort :ISubcohorts;
    ageGroup :IOntologyNode;
    nTotal :number;
    nFemale :number;
    nMale :number;
}

export interface ISubcohorts {
    resource :IExtendedResources;
    name :string;
    description :string;
    numberOfParticipants :number;
    counts :ISubcohortCounts[];
    inclusionStart :number;
    inclusionEnd :number;
    ageGroups :IOntologyNode[];
    mainMedicalCondition :IOntologyNode[];
    comorbidity :IOntologyNode[];
    countries :IOntologyNode[];
    regions :IOntologyNode[];
    inclusionCriteria :string;
    supplementaryInformation :string;
}

export interface ISubmissionTypes {
    order :number;
    name :string;
    label :string;
    parent :ISubmissionTypes;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ISubmissionTypes[];
}

export interface ISubmissions {
    submissionDate :string;
    submitterName :string;
    resources :IExtendedResources[];
    submitterEmail :string;
    submitterOrganisation :IOrganisations;
    submitterRole :IOntologyNode;
    submitterRoleOther :string;
    submissionType :IOntologyNode;
    submissionDescription :string;
    responsiblePersons :string;
    acceptanceDate :string;
}

export interface ISubmitterRoles {
    order :number;
    name :string;
    label :string;
    parent :ISubmitterRoles;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ISubmitterRoles[];
}

export interface ITitles {
    order :number;
    name :string;
    label :string;
    parent :ITitles;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :ITitles[];
}

export interface IUnits {
    order :number;
    name :string;
    label :string;
    parent :IUnits;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IUnits[];
}

export interface IVariableMappings {
    source :IExtendedResources;
    sourceDataset :IDatasets;
    sourceVariables :IAllVariables[];
    sourceVariablesOtherDatasets :IAllVariables[];
    target :IExtendedResources;
    targetDataset :IDatasets;
    targetVariable :IAllVariables;
    match :IOntologyNode;
    status :IOntologyNode;
    description :string;
    syntax :string;
    comments :string;
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

export interface IVariables {
    resource :IExtendedResources;
    dataset :IDatasets;
    name :string;
    label :string;
    collectionEvent :ICollectionEvents;
    sinceVersion :string;
    untilVersion :string;
    networkVariables :INetworkVariables[];
    format :IOntologyNode;
    unit :IOntologyNode;
    references :IAllVariables;
    mandatory :boolean;
    description :string;
    order :number;
    exampleValues :string[];
    permittedValues :IVariableValues[];
    keywords :IOntologyNode[];
    repeats :IRepeatedVariables[];
    vocabularies :IOntologyNode[];
    notes :string;
    mappings :IVariableMappings[];
}

export interface IVersion {
}

export interface IVocabularies {
    order :number;
    name :string;
    label :string;
    parent :IVocabularies;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IVocabularies[];
}

export interface IYears {
    order :number;
    name :string;
    label :string;
    parent :IYears;
    codesystem :string;
    code :string;
    ontologyTermURI :string;
    definition :string;
    children :IYears[];
}


