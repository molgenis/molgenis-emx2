// Generated (on: 2026-07-14T10:28:13.953871) from Generator.java for schema: UiDashboard

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

export interface IChartData extends IMgTableClass {
  id: string;
  name?: string;
  value?: number;
  valueLabel?: string;
  series?: string;
  primaryCategory?: string;
  secondaryCategory?: string;
  primaryCategoryLabel?: string;
  secondaryCategoryLabel?: string;
  timeValue?: string;
  timeUnit?: IOntologyNode;
  color?: string;
  description?: string;
  sortOrder?: number;
  includedInChart?: ICharts;
}

export interface IChartData_agg {
  count: number;
}

export interface IChartPalette extends IMgTableClass {
  key: string;
  color: string;
  includedInChart?: ICharts;
}

export interface IChartPalette_agg {
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
  colorPalette?: IChartPalette[];
  topMargin?: number;
  rightMargin?: number;
  bottomMargin?: number;
  leftMargin?: number;
  legendPosition?: IOntologyNode;
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

export interface ILegendPositions extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ILegendPositions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ILegendPositions[];
}

export interface ILegendPositions_agg {
  count: number;
}

export interface ITimeIntervals extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ITimeIntervals;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ITimeIntervals[];
}

export interface ITimeIntervals_agg {
  count: number;
}
