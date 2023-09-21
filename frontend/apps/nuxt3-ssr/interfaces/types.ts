export interface ICohort {
  id: string;
  name: string;
  acronym?: string;
  description?: string;
  website?: string;
  logo?: IUrlObject;
  contactEmail?: string;
  institution?: {
    acronym: string;
  };
  type: INameObject[];
  collectionType: INameObject[];
  populationAgeGroups?: IOntologyNode[];
  startYear: number;
  endYear: number;
  countries: {
    name: string;
    order: number;
  }[];
  regions: {
    name: string;
    order: number;
  }[];
  numberOfParticipants: number;
  numberOfParticipantsWithSamples?: number;
  designDescription: string;
  designSchematic: IFile;
  design: {
    definition: string;
    name: string;
  };
  designPaper?: {
    title: string;
    doi: string;
  }[];
  inclusionCriteria?: IOntologyNode[];
  otherInclusionCriteria?: string;
  collectionEvents: ICollectionEvent[];
  additionalOrganisations: IPartner[];
  contacts: IContributor[];
  networks: INetwork[];
  releaseDescription?: string;
  linkageOptions?: string;
  dataAccessConditionsDescription?: string;
  dataAccessConditions?: { name: string }[];
  fundingStatement?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
}

interface IVariable {
  name: string;
  label: string;
  description?: string;
  unit?: IOntologyNode;
  format?: IOntologyNode;
  nRepeats?: number;
}

interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

interface IDocumentation {
  name: string;
  description: string;
  url: string;
  file: IFile;
}

interface IPartner {
  id: string;
  acronym: string;
  website: string;
  name: string;
  description: string;
  logo: IUrlObject;
}

interface IContributor {
  roleDescription: string;
  firstName: string;
  lastName: string;
  prefix?: string;
  initials: string;
  email: string;
  title: INameObject;
  organisation: INameObject;
}

interface INameObject {
  name: string;
}

interface IUrlObject {
  url: string;
}

interface ICollectionEvent {
  name: string;
  description: string;
  startYear: INameObject;
  endYear: number;
  numberOfParticipants: number;
  ageGroups: INameObject[];
  definition: string;
  dataCategories: ICollectionEventCategory[];
  sampleCategories: ICollectionEventCategory[];
  areasOfInformation: ICollectionEventCategory[];
  standardizedTools: ICollectionEventCategory[];
  standardizedToolsOther: string;
  subcohorts: INameObject[];
  coreVariables: string[];
}

interface ICollectionEventCategory {
  name: string;
  parent?: INameObject;
  definition?: string;
}

interface ICollectionEventCategorySet {
  name: string;
  children?: ICollectionEventCategorySet[];
  definition?: string;
}

interface INetwork {
  id: string;
  name: string;
  acronym?: string;
  description?: string;
  logo?: IUrlObject;
  website?: string;
}

interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: string;
}

interface IOntologyNode extends ITreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
}

interface ISetting {
  key: string;
  value: string;
}

interface IBaseFilter {
  title: string;
  initialCollapsed?: boolean;
}

interface ISearchFilter extends IBaseFilter {
  columnType: "_SEARCH";
  search?: string;
}

interface IFilter extends IBaseFilter {
  columnType: "_SEARCH" | "ONTOLOGY" | "REF_ARRAY";
  refTable?: string;
  columnName?: string;
  filterTable?: string;
  conditions?: [] | { [key: string]: string }[];
  searchTables?: string[];
  search?: string;
}

interface IFormField {
  name: string;
  label: string;
  fieldValue: string; // value is taken by vue reactivity
  inputType: "string" | "textarea";
  hasError?: boolean;
  message?: string;
}

interface IContactFormData {
  recipientsFilter: string;
  subject: string;
  body: string;
}

export enum INotificationType {
  light,
  dark,
  success,
  error,
  warning,
  info,
}
