// Generated (on: 2026-09-29T11:19:53.008800) from Generator.java for schema: cms

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

export interface IBlockOrders extends IMgTableClass {
  id: string;
  configurablePage?: any;
  block?: any;
  order?: number;
}

export interface IBlockOrders_agg {
  count: number;
}

export interface IBlocks extends IMgTableClass {
  enableFullScreenWidth?: boolean;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  id: string;
  columns?: number;
  applyShadedBackground?: boolean;
  title?: string;
  subtitle?: string;
  backgroundImage?: any;
  titleIsCentered?: boolean;
  pageHeaderHeight?: IOntologyNode;
}

export interface IBlocks_agg {
  count: number;
}

export interface IChartData extends IMgTableClass {
  id: string;
  primaryGroupValue?: string;
  primaryGroupLabel?: string;
  fillColor?: string;
  strokeColor?: string;
  sortOrder?: number;
  displayedInChart?: ICharts;
  locationName?: string;
  alternateId?: string;
  city?: string;
  country?: string;
  continent?: string;
  latitude?: number;
  longitude?: number;
  website?: string;
  tooltipContent?: string;
  xValue: string;
  xLabel?: string;
  yValue: string;
  yLabel?: string;
  secondaryGroupValue?: string;
  secondaryGroupLabel?: string;
}

export interface IChartData_agg {
  count: number;
}

export interface IChartPalette extends IMgTableClass {
  value: string;
  color: string;
  usedInChart?: ICharts;
}

export interface IChartPalette_agg {
  count: number;
}

export interface ICharts extends IMgTableClass {
  chartType?: IOntologyNode;
  chartTitle?: string;
  chartDescription?: string;
  chartData?: IChartData[];
  legendIsEnabled?: boolean;
  legendPosition?: IOntologyNode;
  legendIsHorizontal?: boolean;
  legendMarkerType?: IOntologyNode;
  fillColor?: string;
  strokeColor?: string;
  hoverFillColor?: string;
  hoverStrokeColor?: string;
  colorPalette?: IChartPalette[];
  chartWidth?: number;
  chartHeight?: number;
  topMargin?: number;
  rightMargin?: number;
  bottomMargin?: number;
  leftMargin?: number;
  enableAnimations?: boolean;
  enableHoverEvents?: boolean;
  enableClickEvents?: boolean;
  inBlock?: any;
  id: string;
  x?: string;
  y?: string;
  primaryGroupValues?: string;
  secondaryGroupValues?: string;
  xAxisTitle?: string;
  xAxisMinValue?: number;
  xAxisMaxValue?: number;
  xAxisTicks?: string[];
  breakXAxisTickLabelsAt?: string;
  yAxisTitle?: string;
  yAxisMinValue?: number;
  yAxisMaxValue?: number;
  yAxisTicks?: string[];
  breakYAxisTickLabelsAt?: string;
}

export interface ICharts_agg {
  count: number;
}

export interface ICmsPageHeaderHeights extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ICmsPageHeaderHeights;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ICmsPageHeaderHeights[];
}

export interface ICmsPageHeaderHeights_agg {
  count: number;
}

export interface IComponentOrders extends IMgTableClass {
  id: string;
  block?: any;
  component?: any;
  order?: number;
}

export interface IComponentOrders_agg {
  count: number;
}

export interface IComponents extends IMgTableClass {
  inBlock?: any;
  id: string;
  displayName?: string;
  image?: IFile;
  alt?: string;
  width?: string;
  height?: string;
  imageIsCentered?: boolean;
  orderedItems?: string[];
  text?: string;
  level?: number;
  headingIsCentered?: boolean;
  headingIsHidden?: boolean;
  paragraphIsCentered?: boolean;
  title?: string;
  description?: string;
  url: string;
  urlLabel?: string;
  urlIsExternal?: boolean;
  unorderedItems?: string[];
  chartType?: IOntologyNode;
  chartTitle?: string;
  chartDescription?: string;
  chartData?: IChartData[];
  legendIsEnabled?: boolean;
  legendPosition?: IOntologyNode;
  legendIsHorizontal?: boolean;
  legendMarkerType?: IOntologyNode;
  fillColor?: string;
  strokeColor?: string;
  hoverFillColor?: string;
  hoverStrokeColor?: string;
  colorPalette?: IChartPalette[];
  chartWidth?: number;
  chartHeight?: number;
  topMargin?: number;
  rightMargin?: number;
  bottomMargin?: number;
  leftMargin?: number;
  enableAnimations?: boolean;
  enableHoverEvents?: boolean;
  enableClickEvents?: boolean;
  x?: string;
  y?: string;
  primaryGroupValues?: string;
  secondaryGroupValues?: string;
  xAxisTitle?: string;
  xAxisMinValue?: number;
  xAxisMaxValue?: number;
  xAxisTicks?: string[];
  breakXAxisTickLabelsAt?: string;
  yAxisTitle?: string;
  yAxisMinValue?: number;
  yAxisMaxValue?: number;
  yAxisTicks?: string[];
  breakYAxisTickLabelsAt?: string;
}

export interface IComponents_agg {
  count: number;
}

export interface IConfigurablePages extends IMgTableClass {
  name: string;
  description?: string;
  blocks?: IBlocks[];
  blockOrder?: IBlockOrders[];
}

export interface IConfigurablePages_agg {
  count: number;
}

export interface IContainers extends IMgTableClass {
  name: string;
  description?: string;
  html?: string;
  css?: string;
  javascript?: string;
  dependencies?: IDependencies[];
  enableBaseStyles?: boolean;
  enableButtonStyles?: boolean;
  enableFullScreen?: boolean;
  blocks?: IBlocks[];
  blockOrder?: IBlockOrders[];
}

export interface IContainers_agg {
  count: number;
}

export interface IDataVizChartTypes extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataVizChartTypes;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataVizChartTypes[];
}

export interface IDataVizChartTypes_agg {
  count: number;
}

export interface IDataVizLegendMarkers extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataVizLegendMarkers;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataVizLegendMarkers[];
}

export interface IDataVizLegendMarkers_agg {
  count: number;
}

export interface IDataVizLegendPositions extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IDataVizLegendPositions;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IDataVizLegendPositions[];
}

export interface IDataVizLegendPositions_agg {
  count: number;
}

export interface IDependencies extends IMgTableClass {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
  async?: boolean;
  defer?: boolean;
}

export interface IDependencies_agg {
  count: number;
}

export interface IDependenciesCSS extends IMgTableClass {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
}

export interface IDependenciesCSS_agg {
  count: number;
}

export interface IDependenciesJS extends IMgTableClass {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
  async?: boolean;
  defer?: boolean;
}

export interface IDependenciesJS_agg {
  count: number;
}

export interface IDeveloperPages extends IMgTableClass {
  name: string;
  description?: string;
  html?: string;
  css?: string;
  javascript?: string;
  dependencies?: IDependencies[];
  enableBaseStyles?: boolean;
  enableButtonStyles?: boolean;
  enableFullScreen?: boolean;
}

export interface IDeveloperPages_agg {
  count: number;
}

export interface IHeaders extends IMgTableClass {
  title?: string;
  subtitle?: string;
  backgroundImage?: any;
  titleIsCentered?: boolean;
  pageHeaderHeight?: IOntologyNode;
  enableFullScreenWidth?: boolean;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  id: string;
}

export interface IHeaders_agg {
  count: number;
}

export interface IHeadings extends IMgTableClass {
  text?: string;
  level?: number;
  headingIsCentered?: boolean;
  headingIsHidden?: boolean;
  inBlock?: any;
  id: string;
}

export interface IHeadings_agg {
  count: number;
}

export interface IImages extends IMgTableClass {
  displayName?: string;
  image?: IFile;
  alt?: string;
  width?: string;
  height?: string;
  imageIsCentered?: boolean;
  inBlock?: any;
  id: string;
}

export interface IImages_agg {
  count: number;
}

export interface IMapDotDistributionData extends IMgTableClass {
  id: string;
  locationName?: string;
  alternateId?: string;
  city?: string;
  country?: string;
  continent?: string;
  latitude?: number;
  longitude?: number;
  website?: string;
  tooltipContent?: string;
  primaryGroupValue?: string;
  primaryGroupLabel?: string;
  fillColor?: string;
  strokeColor?: string;
  sortOrder?: number;
  displayedInChart?: ICharts;
}

export interface IMapDotDistributionData_agg {
  count: number;
}

export interface INavigationCards extends IMgTableClass {
  title?: string;
  description?: string;
  url: string;
  urlLabel?: string;
  urlIsExternal?: boolean;
  inBlock?: any;
  id: string;
}

export interface INavigationCards_agg {
  count: number;
}

export interface IOrderedLists extends IMgTableClass {
  orderedItems?: string[];
  inBlock?: any;
  id: string;
}

export interface IOrderedLists_agg {
  count: number;
}

export interface IParagraphs extends IMgTableClass {
  text?: string;
  paragraphIsCentered?: boolean;
  inBlock?: any;
  id: string;
}

export interface IParagraphs_agg {
  count: number;
}

export interface ISections extends IMgTableClass {
  columns?: number;
  enableFullScreenWidth?: boolean;
  applyShadedBackground?: boolean;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  id: string;
}

export interface ISections_agg {
  count: number;
}

export interface IStatisticalChartData extends IMgTableClass {
  id: string;
  xValue: string;
  xLabel?: string;
  yValue: string;
  yLabel?: string;
  primaryGroupValue?: string;
  primaryGroupLabel?: string;
  secondaryGroupValue?: string;
  secondaryGroupLabel?: string;
  fillColor?: string;
  strokeColor?: string;
  sortOrder?: number;
  displayedInChart?: ICharts;
}

export interface IStatisticalChartData_agg {
  count: number;
}

export interface IStatisticalCharts extends IMgTableClass {
  chartType?: IOntologyNode;
  chartTitle?: string;
  chartDescription?: string;
  chartData?: IChartData[];
  x?: string;
  y?: string;
  primaryGroupValues?: string;
  secondaryGroupValues?: string;
  xAxisTitle?: string;
  xAxisMinValue?: number;
  xAxisMaxValue?: number;
  xAxisTicks?: string[];
  breakXAxisTickLabelsAt?: string;
  yAxisTitle?: string;
  yAxisMinValue?: number;
  yAxisMaxValue?: number;
  yAxisTicks?: string[];
  breakYAxisTickLabelsAt?: string;
  legendIsEnabled?: boolean;
  legendPosition?: IOntologyNode;
  legendIsHorizontal?: boolean;
  legendMarkerType?: IOntologyNode;
  fillColor?: string;
  strokeColor?: string;
  hoverFillColor?: string;
  hoverStrokeColor?: string;
  colorPalette?: IChartPalette[];
  chartWidth?: number;
  chartHeight?: number;
  topMargin?: number;
  rightMargin?: number;
  bottomMargin?: number;
  leftMargin?: number;
  enableAnimations?: boolean;
  enableHoverEvents?: boolean;
  enableClickEvents?: boolean;
  inBlock?: any;
  id: string;
}

export interface IStatisticalCharts_agg {
  count: number;
}

export interface ITextElements extends IMgTableClass {
  text?: string;
  inBlock?: any;
  id: string;
  level?: number;
  headingIsCentered?: boolean;
  headingIsHidden?: boolean;
  paragraphIsCentered?: boolean;
}

export interface ITextElements_agg {
  count: number;
}

export interface IUnorderedLists extends IMgTableClass {
  unorderedItems?: string[];
  inBlock?: any;
  id: string;
}

export interface IUnorderedLists_agg {
  count: number;
}

export interface IWebFetchPriorities extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IWebFetchPriorities;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IWebFetchPriorities[];
}

export interface IWebFetchPriorities_agg {
  count: number;
}
