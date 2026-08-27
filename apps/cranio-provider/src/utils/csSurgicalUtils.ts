import type { IChartData } from "../../../metadata-utils/src/viz/UiDashboard";
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
      return row.primaryCategory === diagnosis;
    })
    .sort((a: IChartData, b: IChartData) => {
      return (
        (a.timeValue as unknown as number) - (b.timeValue as unknown as number)
      );
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
      value: row.primaryCategory,
      label: row.primaryCategoryLabel || row.primaryCategory,
    };
  }) as IValueLabel[];

  return [
    ...new Map(diagnoses.map((row) => [row["label"], row])).values(),
  ].sort((a: IValueLabel, b: IValueLabel) => {
    return a.label.localeCompare(b.label);
  });
}
