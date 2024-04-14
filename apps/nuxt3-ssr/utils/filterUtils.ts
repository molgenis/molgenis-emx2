import type { IConditionsFilter, IFilter } from "~/interfaces/types";

export function isConditionFilter(
  filter: IConditionsFilter | IFilter
): filter is IConditionsFilter {
  return (
    (filter as IConditionsFilter).conditions !== undefined &&
    !!(filter as IConditionsFilter).conditions.length
  );
}
