// Generated (on: 2026-03-12T13:14:59.424968) from Generator.java for schema: Dashboard

export interface IMgTableClass {
  mg_tableclass: string;
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

export interface IChartData extends IMgTableClass {
  dataPointId: string;
  dataPointName?: string;
  dataPointValue?: number;
  dataPointValueLabel?: string;
  dataPointSeries?: string;
  dataPointPrimaryCategory?: string;
  dataPointSecondaryCategory?: string;
  dataPointPrimaryCategoryLabel?: string;
  dataPointSecondaryCategoryLabel?: string;
  dataPointTime?: string;
  dataPointTimeUnit?: string;
  dataPointColor?: string;
  dataPointDescription?: string;
  dataPointOrder?: number;
  includedInChart?: ICharts;
}

export interface IChartData_agg {
  count: number;
}

export interface IChartTypes extends IMgTableClass {
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

export interface IChartTypes_agg {
  count: number;
}

export interface ICharts extends IMgTableClass {
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

export interface ICharts_agg {
  count: number;
}

export interface IDashboardPages extends IMgTableClass {
  name: string;
  description?: string;
  charts?: ICharts[];
}

export interface IDashboardPages_agg {
  count: number;
}
