import type { IColumn } from "meta-data-utils";
export interface IResource {
  id: string;
  pid: string;
  acronym: string;
  name: string;
  website: string;
  description: string;
  contacts: IContributor[];
  logo?: IUrlObject;
}
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
  datasets: { name: string }[];
}

export interface IVariableBase {
  name: string;
  resource: {
    id: string;
  };
  dataset: {
    name: string;
    resource: {
      id: string;
    };
  };
  label?: string;
  description?: string;
}

export interface IVariableDetails {
  unit?: IOntologyNode;
  format?: IOntologyNode;
}

export interface IVariableMappings {
  mappings?: IMapping[];
  repeats?: {
    name: string;
    mappings: IMapping[];
  }[];
}

export type IVariable = IVariableBase & IVariableDetails;
export type IVariableWithMappings = IVariable & IVariableMappings;

export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

export interface IDocumentation {
  name: string;
  description: string;
  url: string;
  file: IFile;
}

export interface IPartner {
  id: string;
  acronym: string;
  website: string;
  name: string;
  description: string;
  logo: IUrlObject;
}

export interface IContributor {
  roleDescription: string;
  firstName: string;
  lastName: string;
  prefix?: string;
  initials: string;
  email: string;
  title: INameObject;
  organisation: INameObject;
}

export interface INameObject {
  name: string;
}

export interface IUrlObject {
  url: string;
}

export interface ICollectionEvent {
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

export interface ICollectionEventCategory {
  name: string;
  parent?: INameObject;
  definition?: string;
}

export interface INetwork {
  id: string;
  name: string;
  acronym?: string;
  description?: string;
  logo?: IUrlObject;
  website?: string;
}

export interface ICatalogue {
  network: INetwork;
  type: IOntologyNode;
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
}

interface IBaseFilter {
  title: string;
  initialCollapsed?: boolean;
}

interface ISearchFilter extends IBaseFilter {
  columnType: "_SEARCH";
  search?: string;
}

export interface IFilter extends IBaseFilter {
  columnType: "_SEARCH" | "ONTOLOGY" | "REF_ARRAY";
  refTableId?: string;
  columnId?: string;
  filterTable?: string;
  conditions?: [] | { [key: string]: string }[];
  searchTables?: string[];
  search?: string;
}

export interface IFormField {
  name: string;
  label: string;
  fieldValue: string; // value is taken by vue reactivity
  inputType: "string" | "textarea";
  hasError?: boolean;
  message?: string;
}

export interface IContactFormData {
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

export interface ISectionField {
  meta: IColumn;
  value: any;
}

export interface ISection {
  meta: IColumn;
  fields: ISectionField[];
}
export interface IMapping {
  syntax: string;
  description: string;
  match: {
    name: string;
  };
  source: {
    id: string;
    name: string;
  };
  sourceDataset: {
    resource: {
      id: string;
    };
    name: string;
  };
  sourceVariables: IVariableBase[] | IVariable[];
  targetVariable: IVariableBase[] | IVariable[];
  sourceVariablesOtherDatasets: IVariableBase[] | IVariable[];
}

export type HarmonizationStatus = "unmapped" | "partial" | "complete";

export type HarmonizationIconSize = "small" | "large";
export interface IMgError {
  message: string;
  statusCode: number;
  data: { errors: { message: string }[] };
}

export interface IDefinitionListItem {
  label: string;
  tooltip?: string;
  type?: string;
  content: any;
}
