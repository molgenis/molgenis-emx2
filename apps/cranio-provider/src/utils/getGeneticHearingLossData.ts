import { getDashboardChart } from "./getDashboardData";
import type { IChartData } from "../types/schema";

export async function getGeneticLossData(url: string, type: string) {
  const hearingLossTypeLeft = (
    await getDashboardChart(url, "ghl-type-of-hearing-loss-left")
  )[0];

  const hearingLossTypeRight = (
    await getDashboardChart(url, "ghl-type-of-hearing-loss-right")
  )[0];

  const severityChart = (
    await getDashboardChart(url, "severity-of-hearing-loss-by-ear")
  )[0];

  const onsetChart = (
    await getDashboardChart(url, "age-of-hearing-loss-onset")
  )[0];

  const dxTypeChart = (
    await getDashboardChart(url, "genetic-diagnosis-type")
  )[0];

  const dxGenes = (await getDashboardChart(url, "genes"))[0];

  const etiologyChart = (await getDashboardChart(url, "etiology"))[0];

  const classificationChart = (
    await getDashboardChart(url, "syndromic-classification")
  )[0];

  const rehabilitationChart = (
    await getDashboardChart(url, "ghl-rehabilitation-type")
  )[0];

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
