// Generated (on: 2026-07-27T13:44:53.120466) from Generator.java for schema: UiDashboard

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
  timeValue?: string;
  timeUnit?: IOntologyNode;
  primaryCategory?: string;
  secondaryCategory?: string;
  primaryCategoryLabel?: string;
  secondaryCategoryLabel?: string;
  color?: string;
  sortOrder?: number;
  description?: string;
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
  dataPoints?: IVizDatasets[];
  dashboardPage?: IDashboardPages;
}

export interface ICharts_agg {
  count: number;
}

export interface ICatalogueOntologies_Continents extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Continents;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Continents[];
}

export interface ICatalogueOntologies_Continents_agg {
  count: number;
}

export interface ICatalogueOntologies_Countries extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_Countries;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_Countries[];
}

export interface ICatalogueOntologies_Countries_agg {
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

export interface ICatalogueOntologies_DataVizChartTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DataVizChartTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DataVizChartTypes[];
}

export interface ICatalogueOntologies_DataVizChartTypes_agg {
  count: number;
}

export interface ICatalogueOntologies_DataVizLegendPositions
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DataVizLegendPositions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DataVizLegendPositions[];
}

export interface ICatalogueOntologies_DataVizLegendPositions_agg {
  count: number;
}

export interface ICatalogueOntologies_DataVizTimeIntervals
  extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICatalogueOntologies_DataVizTimeIntervals;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICatalogueOntologies_DataVizTimeIntervals[];
}

export interface ICatalogueOntologies_DataVizTimeIntervals_agg {
  count: number;
}

export interface IMapDotDistributionData extends IMgTableClass {
  id: string;
  locationName?: string;
  alternateId?: string;
  city?: string;
  country?: IOntologyNode;
  continent?: IOntologyNode;
  latitude?: number;
  longitude?: number;
  website?: string;
  tooltipContent?: string;
  primaryCategory?: string;
  secondaryCategory?: string;
  primaryCategoryLabel?: string;
  secondaryCategoryLabel?: string;
  color?: string;
  sortOrder?: number;
  description?: string;
  includedInChart?: ICharts;
}

export interface IMapDotDistributionData_agg {
  count: number;
}

export interface IVizDatasets extends IMgTableClass {
  id: string;
  primaryCategory?: string;
  secondaryCategory?: string;
  primaryCategoryLabel?: string;
  secondaryCategoryLabel?: string;
  color?: string;
  sortOrder?: number;
  description?: string;
  includedInChart?: ICharts;
  name?: string;
  value?: number;
  valueLabel?: string;
  series?: string;
  timeValue?: string;
  timeUnit?: IOntologyNode;
  locationName?: string;
  alternateId?: string;
  city?: string;
  country?: IOntologyNode;
  continent?: IOntologyNode;
  latitude?: number;
  longitude?: number;
  website?: string;
  tooltipContent?: string;
}

export interface IVizDatasets_agg {
  count: number;
}
