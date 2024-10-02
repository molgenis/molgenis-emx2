import type {
  IDocumentation,
  IFile,
  INode,
} from "../../tailwind-components/types/types";
import type { ISubpopulations } from "./catalogue";
export interface IResource {
  id: string;
  pid: string;
  name: string;
  acronym?: string;
  description?: string;
  website?: string;
  logo?: IUrlObject;
  contactEmail?: string;
  organisationsInvolved?: IOrganisation[];
  institution?: {
    acronym: string;
  };
  type: INameObject[];
  typeOther?: string;
  cohortType: INameObject[];
  networkType: INameObject[];
  clinicalStudyType: INameObject[];
  rWDType: INameObject[];
  keywords?: string;
  externalIdentifiers?: [
    {
      identifier: string;
      externalIdentifierType: INameObject;
    }
  ];
  dateEstablished?: string;
  startDataCollection?: string;
  endDataCollection?: string;
  license?: string;
  populationAgeGroups?: IOntologyNode[];
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
  dataCollectionType?: {
    definition: string;
    name: string;
  }[];
  dataCollectionDescription?: string;
  reasonSustained?: string;
  unitOfObservation?: string;
  recordTrigger?: string;
  designPaper?: {
    title: string;
    doi: string;
  }[];
  inclusionCriteria?: IOntologyNode[];
  otherInclusionCriteria?: string;
  collectionEvents: ICollectionEvent[];
  collectionEvents_agg: { count: number };
  peopleInvolved: IContributor[];
  networks: INetwork[];
  publications: IPublication[];
  releaseDescription?: string;
  linkageOptions?: string;
  dataAccessConditionsDescription?: string;
  dataAccessConditions?: { name: string }[];
  dataUseConditions?: IOntologyNode[];
  dataAccessFee?: boolean;
  prelinked?: boolean;
  releaseType?: boolean;
  fundingStatement?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
  datasets: { name: string }[];
  populationOncologyTopology?: IOntologyNode[];
  populationOncologyMorphology?: IOntologyNode[];
  subpopulations: ISubpopulations[];
  subpopulations_agg: { count: number };
  partOfResources: IResource[];
}

export interface IPublication {
  doi: string;
  title?: string;
  authors?: string[];
  year?: number;
  journal?: string;
  volume?: number;
  number?: number;
  pagination?: number;
  publisher?: string;
  school?: string;
  abstract?: string;
  isDesignPublication: boolean;
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
  mg_tableclass?: string;
}

export interface IVariableDetails {
  unit?: IOntologyNode;
  format?: IOntologyNode;
  repeatUnit: IOntologyItem;
  repeatMin: number;
  repeatMax: number;
}

export interface IVariableMappings {
  mappings?: IMapping[];
}

export type IVariable = IVariableBase & IVariableDetails;
export type IVariableWithMappings = IVariable & IVariableMappings;

export interface IOrganisation extends IPartner {
  email: string;
  type: {
    name: string;
  };
  institution: any;
  institutionAcronym: string;
  typeOther: string;
  address: string;
  expertise: string;
  country: {
    name: string;
  }[];
  isLeadOrganisation: boolean;
  role: IOntologyNode[];
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
  role?: IOntologyNode[];
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
  subpopulations: INameObject[];
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
  order?: number;
}

export interface IFormField {
  name: string;
  label: string;
  fieldValue: string; // value is taken by vue reactivity
  inputType: "string" | "textarea" | "select";
  hasError?: boolean;
  message?: string;
  placeholder?: string;
}

export interface ISelectFormField extends IFormField {
  inputType: "select";
  options: string[] | number[];
}

export interface IContactFormData {
  recipientsFilter: string;
  subject: string;
  body: string;
}

export type INotificationType =
  | "light"
  | "dark"
  | "success"
  | "error"
  | "warning"
  | "info";

export interface IMapping {
  syntax: string;
  description: string;
  match: {
    name: HarmonisationStatus;
  };
  source: {
    id: string;
    name: string;
    mg_tableclass: string;
  };
  repeats: string;
  sourceDataset: {
    resource: {
      id: string;
    };
    name: string;
  };
  sourceVariables: IVariableBase[] | IVariable[];
  sourceVariablesOtherDatasets: IVariableBase[] | IVariable[];
  targetVariable: IVariableBase | IVariable;
}

export type HarmonisationStatus =
  | "unmapped"
  | "partial"
  | "complete"
  | "available";

export type HarmonisationIconSize = "small" | "large";
export interface IMgError {
  message: string;
  statusCode: number;
  data: { errors: { message: string }[] };
}

export type DefinitionListItemType = "ONTOLOGY" | "LINK" | "MAPPED";

export interface IDefinitionListItem {
  label: string;
  tooltip?: string;
  type?: DefinitionListItemType;
  content: any;
}
export interface IOntologyItem {
  order?: number;
  name: string;
  label?: string;
  parent?: IOntologyItem;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IOntologyItem[];
}

export interface IOntologyParentTreeItem
  extends Omit<IOntologyItem, "children"> {}

// generic emx2 graphql api response type, pass in query structure as T
export interface GqlResp<T> {
  data: Record<string, T[]>;
}
export interface IOntologyRespItem {
  name: string;
  definition?: string;
  code?: string;
  order?: number;
  parent: {
    name: string;
  };
}

export type ButtonType =
  | "primary"
  | "secondary"
  | "tertiary"
  | "outline"
  | "disabled"
  | "filterWell";
export type ButtonSize = "tiny" | "small" | "medium" | "large";
export type ButtonIconPosition = "left" | "right";

export interface IOntologyChildTreeItem extends Omit<IOntologyItem, "parent"> {}

export interface IManifest {
  ImplementationVersion: string;
  SpecificationVersion: string;
  DatabaseVersion: string;
}

export interface IManifestResponse {
  data: {
    _manifest: IManifest;
  };
}

export type IFilter = ISearchFilter | IOntologyFilter | IRefArrayFilter;

interface IAbstractFilter {
  id: string;
  search?: string;
  config:
    | ISearchFilterConfig
    | IOntologyFilterConfig
    | IRefArrayFilterDefaultConfig
    | IRefArrayFilterCustomConfig;
}
export interface ISearchFilter extends IAbstractFilter {
  search: string;
  config: ISearchFilterConfig;
}

export interface IFilterConfig {
  label: string;
  initialCollapsed?: boolean;
  filterTable?: string;
}

export interface ISearchFilterConfig extends IFilterConfig {
  type: "SEARCH";
  searchTables?: string[];
}

export interface IOntologyFilterConfig extends IFilterConfig {
  type: "ONTOLOGY";
  ontologyTableId: string;
  ontologySchema: string;
  columnId: string;
  refFields?: filterRefField;
}

export interface IRefArrayFilterAbstractConfig extends IFilterConfig {
  type: "REF_ARRAY";
  refTableId: string;
  refSchema?: string;
  refFields?: filterRefField;
  // optional function to build the filter bases on the selected options
  // if empty the defualt builder will be used
  buildFilterFunction?: Function;
}

export interface IRefArrayFilterDefaultConfig
  extends IRefArrayFilterAbstractConfig {
  columnId: string;
}

export interface IRefArrayFilterCustomConfig
  extends IRefArrayFilterAbstractConfig {
  // optional function to build the filter bases on the selected options
  // if empty the defualt builder will be used
  buildFilterFunction?: Function;
}

type filterRefField = {
  [key: string]: string;
};

export type IFilterCondition = {
  [id: string]: IFilterCondition | string;
};

export interface IOntologyFilter extends IAbstractFilter {
  conditions: IFilterCondition[];
  config: IOntologyFilterConfig;
}

export type IConditionsFilter = IOntologyFilter | IRefArrayFilter;

export interface optionsFetchFn {
  (): Promise<INode[]>;
}

export interface IRefArrayFilter extends IAbstractFilter {
  conditions: IFilterCondition[];
  config: IRefArrayFilterCustomConfig | IRefArrayFilterDefaultConfig;
  options?: INode[] | optionsFetchFn;
}

export interface IPathCondition {
  id: string;
  search?: string;
  conditions?: IFilterCondition[];
}

export interface IPathSearchCondition extends IPathCondition {
  search: string;
}

export interface IPathConditionsCondition extends IPathCondition {
  conditions: IFilterCondition[];
}

export type activeTabType = "detailed" | "compact";

export interface IOrganization {
  id: string;
  name?: string;
  email?: string;
  description?: string;
  website?: string;
  acronym?: string;
  type?: {
    name: string;
  };
  institution?: any;
  institutionAcronym?: string;
  typeOther?: string;
  address?: string;
  expertise?: string;
  country?: {
    name: string;
  };
  logo?: IUrlObject;
}

export type linkTarget = "_self" | "_blank" | "_parent" | "_top";
