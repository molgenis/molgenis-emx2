import type { IChartData } from "../types/schema";
import type { IValueLabel } from "../types";

/**
 * Filter and sort age at surgery datasets for the craniosynostosis dashboard
 * @param data Dataset containing aggregations at age of surgery; array of objects (IChart)
 * @param diagnosis
 *
 * @return filterd age at surgery data by diagnosis
 */
export function filterAgeAtSurgeryData(data: IChartData[], diagnosis: string) {
  return data
    .filter((row: IChartData) => {
      return row.dataPointPrimaryCategory === diagnosis;
    })
    .sort((a: IChartData, b: IChartData) => {
      return (a.dataPointTime as number) - (b.dataPointTime as number);
    });
}

/**
 * Prepare diagnosis filters from aggregated data for the craniosynostosis dashboard.
 * Diagnositic codes will be transformed into a value/label object, and then reduced
 * to unique records only.
 *
 * @param data array of objects containing aggregated diagnostic data at intervention
 *
 * @return unique diagnostic codes and labels
 */
export function prepareDiagnosisFilters(data: IChartData[]): IValueLabel[] {
  const diagnoses = data.map((row: IChartData) => {
    return {
      value: row.dataPointPrimaryCategory,
      label: row.dataPointPrimaryCategoryLabel || row.dataPointPrimaryCategory,
    };
  }) as IValueLabel[];

  return [
    ...new Map(diagnoses.map((row) => [row["label"], row])).values(),
  ].sort((a: IValueLabel, b: IValueLabel) => {
    return a.label.localeCompare(b.label);
  });
}
