import type { IChartPalette, IChartData } from "../../../types/cms";

import { scaleOrdinal, schemeBlues } from "d3";
const d3 = { scaleOrdinal, schemeBlues };

interface ChartColorPaletteGeneratorProps {
  data: IChartData[];
  axisDomains: string[];
  isGroupedChart?: boolean;
  colorPalette?: IChartPalette[];
  fillColor?: string;
  fallbackColor?: string;
}

interface ChartColorPaletteGeneratorReturn {
  generator: any;
  legendData?: Record<string, string>;
}

export function ColorPaletteGenerator({
  data,
  axisDomains,
  isGroupedChart = false,
  colorPalette,
  fillColor = "#014f9e",
  fallbackColor = "#ccc",
}: ChartColorPaletteGeneratorProps): ChartColorPaletteGeneratorReturn {
  let domains: string[] = axisDomains;
  let colors: string[];
  let legendData: Record<string, string> = {};

  const hasRowLevelColors = data.find((row) => row.fillColor);

  if (colorPalette) {
    const palette = transformColorPalette(colorPalette);
    domains = palette.domains;
    colors = palette.colors;
  } else if (!colorPalette && isGroupedChart) {
    const length = domains.length === 2 ? 3 : domains.length;
    colors = d3.schemeBlues[length as number] as string[];
  } else if (!colorPalette && hasRowLevelColors) {
    const dataRowColors = data
      .map((row) => {
        return { xValue: row.xValue, fillColor: row.fillColor || fillColor };
      })
      .sort((curr, next) => {
        return curr.xValue.localeCompare(next.xValue);
      });
    domains = dataRowColors.map((row) => row.xValue);
    colors = dataRowColors.map((row) => row.fillColor);
  } else {
    colors = Array(domains.length).fill(fillColor);
  }

  if (isGroupedChart) {
    legendData = Object.fromEntries(
      domains.map((item, index) => [item, colors[index] || fallbackColor])
    );
  }

  const generator = d3
    .scaleOrdinal()
    .domain(domains)
    .range(colors)
    .unknown(fallbackColor);

  return { generator, legendData };
}

function transformColorPalette(palette: IChartPalette[]) {
  const sorted = palette.sort((curr, next) =>
    curr.value.localeCompare(next.value)
  );
  const domains = sorted.map((row) => row.value);
  const colors = sorted.map((row) => row.color);
  return { domains, colors };
}
