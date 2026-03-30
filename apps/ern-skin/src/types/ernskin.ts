// Generated (on: 2026-03-25T10:38:31.372727) from Generator.java for schema: ernskin

export interface IMgTableClass {
  mg_tableclass?: string;
}

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

export interface IComponents extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IComponents;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IComponents[];
}

export interface IComponents_agg {
  count: number;
}

export interface IDataproviders extends IMgTableClass {
  providerIdentifier: string;
  organisation?: IOrganisations;
  hasSubmittedData?: boolean;
}

export interface IDataproviders_agg {
  count: number;
}

export interface IFiles extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IFiles;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IFiles[];
}

export interface IFiles_agg {
  count: number;
}

export interface IHistoryStatistics extends IMgTableClass {
  age_group_5?: string;
  age_group_6?: string;
  enrollment_1?: string;
  enrollment_2?: string;
  enrollment_3?: string;
  enrollment_4?: string;
  enrollment_5?: string;
  enrollment_6?: string;
  enrollment_8?: string;
  age_group_0?: string;
  age_group_2?: string;
  age_group_4?: string;
  age_group_1?: string;
  age_group_3?: string;
  age_group_7?: string;
  enrollment_7?: string;
  enrollment_9?: string;
  enrollment_10?: string;
  highlights_0?: string;
  highlights_1?: string;
  highlights_2?: string;
  sex_intersex?: string;
  sex_female?: string;
  sex_undetermined?: string;
  sex_male?: string;
  aT01?: string;
  aT04?: string;
  bE04?: string;
  bE06?: string;
  bE10?: string;
  cZ03?: string;
  cZ04?: string;
  cZ06?: string;
  cZ08?: string;
  dE03?: string;
  dE19?: string;
  dE20?: string;
  dE24?: string;
  dE25?: string;
  dE26?: string;
  dE27?: string;
  dK03?: string;
  dK04?: string;
  eS08?: string;
  fI01?: string;
  fR09?: string;
  fR15?: string;
  fR22?: string;
  fR28?: string;
  fR31?: string;
  fR34?: string;
  hR01?: string;
  hU01?: string;
  hU03?: string;
  hU04?: string;
  iE01?: string;
  iT04?: string;
  iT07?: string;
  iT109?: string;
  iT11?: string;
  iT14?: string;
  iT20?: string;
  iT27?: string;
  iT33?: string;
  iT34?: string;
  iT39?: string;
  iT53?: string;
  iT58?: string;
  lT04?: string;
  nL06?: string;
  lU01?: string;
  lV01?: string;
  mT02?: string;
  nL01?: string;
  nL02?: string;
  nL03?: string;
  nL13?: string;
  rO02?: string;
  sE05?: string;
  sI02?: string;
  dE38a?: string;
  dE38b?: string;
  date?: string;
  id: string;
}

export interface IHistoryStatistics_agg {
  count: number;
}

export interface IInclusionCriteria extends IMgTableClass {
  id: string;
  name?: string;
  type?: string;
  value?: string;
  label?: string;
}

export interface IInclusionCriteria_agg {
  count: number;
}

export interface IOrganisations extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IOrganisations;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IOrganisations[];
}

export interface IOrganisations_agg {
  count: number;
}

export interface IPublications extends IMgTableClass {
  title: string;
  doi: string;
}

export interface IPublications_agg {
  count: number;
}

export interface IStatistics extends IMgTableClass {
  id: string;
  label?: string;
  value?: number;
  valueOrder?: number;
  component?: IComponents;
  description?: string;
}

export interface IStatistics_agg {
  count: number;
}

export interface IStudies extends IMgTableClass {
  acronym: string;
  title: string;
  condition: string;
  coordinator: string;
  objective: string;
  patients: string;
  dataUsed: string;
  dataErras: string;
  patientData: string;
  dataOther: string;
  registrationNr: string;
  status: string;
  dateStart: string;
  dateEnd: string;
}

export interface IStudies_agg {
  count: number;
}

export interface IUsers extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IUsers;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IUsers[];
}

export interface IUsers_agg {
  count: number;
}
