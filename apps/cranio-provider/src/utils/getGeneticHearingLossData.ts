import { getDashboardChart } from "./getDashboardData";
import type { ICharts, IChartData } from "../types/schema";

export async function getGeneticLossData(url: string, type: string) {
  const hearingLossTypeLeft = (
    (await getDashboardChart(url, "ghl-type-of-hearing-loss-left")) as ICharts[]
  )[0] as ICharts;

  const hearingLossTypeRight = (
    (await getDashboardChart(
      url,
      "ghl-type-of-hearing-loss-right"
    )) as ICharts[]
  )[0] as ICharts;

  const severityChart = (
    (await getDashboardChart(
      url,
      "severity-of-hearing-loss-by-ear"
    )) as ICharts[]
  )[0] as ICharts;

  const onsetChart = (
    (await getDashboardChart(url, "age-of-hearing-loss-onset")) as ICharts[]
  )[0] as ICharts;

  const dxTypeChart = (
    (await getDashboardChart(url, "genetic-diagnosis-type")) as ICharts[]
  )[0] as ICharts;

  const dxGenes = (
    (await getDashboardChart(url, "genes")) as ICharts[]
  )[0] as ICharts;

  const etiologyChart = (
    (await getDashboardChart(url, "etiology")) as ICharts[]
  )[0] as ICharts;

  const classificationChart = (
    (await getDashboardChart(url, "syndromic-classification")) as ICharts[]
  )[0] as ICharts;

  const rehabilitationChart = (
    (await getDashboardChart(url, "ghl-rehabilitation-type")) as ICharts[]
  )[0] as ICharts;

  const data = {
    hearingLossTypeLeft: hearingLossTypeLeft,
    hearingLossTypeRight: hearingLossTypeRight,
    severity: severityChart,
    ageOfOnset: onsetChart,
    diagnosisTypes: dxTypeChart,
    diagnosisGenes: dxGenes,
    etiology: etiologyChart,
    syndromicClassification: classificationChart,
    rehabilitationChart: rehabilitationChart,
  };

  Object.keys(data).forEach((key) => {
    // @ts-ignore
    data[key].dataPoints = data[key].dataPoints.map((row: IChartData) => {
      return Object.assign(row, { dataPointSecondaryCategory: type });
    });
  });

  return data;
}
