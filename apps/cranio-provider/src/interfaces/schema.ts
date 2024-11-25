// Generated (on: 2024-11-19T14:49:53.211482) from Generator.java for schema: dashboard

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

export interface IChartData {
  dataPointId: string;
  dataPointName?: string;
  dataPointValue?: number;
  dataPointValueLabel?: string;
  dataPointSeries?: string;
  dataPointPrimaryCategory?: string;
  dataPointSecondaryCategory?: string;
  dataPointTime?: string;
  dataPointTimeUnit?: string;
  dataPointColor?: string;
  dataPointDescription?: string;
  dataPointOrder?: number;
  includedInChart?: ICharts;
}

export interface IChartTypes {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IChartTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IChartTypes[];
}

export interface ICharts {
  chartId: string;
  chartType?: IOntologyNode;
  chartTitle?: string;
  chartSubtitle?: string;
  xAxisLabel?: string;
  xAxisMinValue?: number;
  xAxisMaxValue?: number;
  xAxisTicks?: number[];
  yAxisLabel?: string;
  yAxisMinValue?: number;
  yAxisMaxValue?: number;
  yAxisTicks?: number[];
  topMargin?: number;
  rightMargin?: number;
  bottomMargin?: number;
  leftMargin?: number;
  legendPosition?: string;
  dataPoints?: IChartData[];
  dashboardPage?: IDashboardPages;
}

export interface IDashboardPages {
  name: string;
  description?: string;
  charts?: ICharts[];
}

export interface IDataproviders {
  providerIdentifier: string;
  organisation?: IOrganisations;
  hasSubmittedData?: boolean;
}

export interface IOrganisations {
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
  city?: string;
  country?: string;
  latitude?: number;
  longitude?: number;
  organisationType?: string;
  providerInformation: IDataproviders[];
  image?: IFile;
  hasSchema?: boolean;
  schemaName?: string;
}

export interface IOrganisationsResponse {
  Organisations: IOrganisations[];
}

export interface IDashboardPagesResponse {
  DashboardPages: IDashboardPages[];
}

export interface IChartsResponse {
  Charts: ICharts[];
}
