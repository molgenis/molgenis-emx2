export interface Charts {
  id: string;
  title: string;
  description?: string;
  width?: number;
  height?: number;
}

export type LegendPosition = "top" | "right" | "bottom" | "left";

export interface ChartContext {
  title: string;
  description?: string;
}

export interface PieCharts extends Charts {
  chartData: Record<string, number>;
  showValues?: boolean;
  showValuesAsPercentages?: boolean;
  asDonutChart?: boolean;
  pieChartIsCentered?: boolean;
  hoverEventsAreEnabled?: boolean;
  clickEventsAreEnabled?: boolean;
  chartLegendIsEnabled?: boolean;
  legendIsStacked?: boolean;
  legendPosition?: LegendPosition;
  legendClickEventsAreEnabled?: boolean;
  legendHoverEventsAreEnabled?: boolean;
}
