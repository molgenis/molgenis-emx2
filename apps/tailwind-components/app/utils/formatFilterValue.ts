import type { IFilterValue } from "../../types/filters";
import { nonEmptyRanges } from "./filterUtils";

export function extractDisplayValue(obj: Record<string, unknown>): string {
  if (obj.name) return String(obj.name);
  if (obj.label) return String(obj.label);
  const firstValue = Object.values(obj)[0];
  return String(firstValue);
}

export function formatFilterValue(
  filterValue: IFilterValue,
  optionLabels: Record<string, string>
): {
  displayValue: string;
  values: string[];
} {
  const { operator, value } = filterValue;

  switch (operator) {
    case "between": {
      const ranges = nonEmptyRanges(value).map(formatRange);
      if (ranges.length > 1)
        return { displayValue: `${ranges.length}`, values: ranges };
      return { displayValue: ranges[0] ?? "", values: [] };
    }

    case "notNull":
      return { displayValue: "has value", values: [] };

    case "isNull":
      return { displayValue: "is empty", values: [] };

    case "like":
    case "equals":
    default:
      if (Array.isArray(value)) {
        const formatted = value.map((v) => resolveLabel(v, optionLabels));
        if (value.length > 1) {
          return { displayValue: `${value.length}`, values: formatted };
        }
        return { displayValue: formatted[0] || "", values: [] };
      }
      if (typeof value === "object" && value !== null) {
        return {
          displayValue: resolveLabel(value, optionLabels),
          values: [],
        };
      }
      return {
        displayValue: resolveLabel(value, optionLabels),
        values: [],
      };
  }
}

function resolveLabel(
  value: unknown,
  optionLabels: Record<string, string>
): string {
  if (typeof value === "object" && value !== null) {
    const jsonKey = JSON.stringify(value);
    if (optionLabels[jsonKey] !== undefined) return optionLabels[jsonKey]!;
    return extractDisplayValue(value as Record<string, unknown>);
  }
  const strKey = String(value);
  return optionLabels[strKey] ?? strKey;
}

function formatRange([min, max]: [unknown, unknown]): string {
  const hasMin = min != null && min !== "";
  const hasMax = max != null && max !== "";
  if (hasMin && hasMax) return `${min} - ${max}`;
  return hasMin ? `≥ ${min}` : `≤ ${max}`;
}
