import type { IFilterValue } from "../../types/filters";

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
      const [min, max] = value;
      if (min != null && max != null)
        return { displayValue: `${min} - ${max}`, values: [] };
      if (min != null) return { displayValue: `≥ ${min}`, values: [] };
      if (max != null) return { displayValue: `≤ ${max}`, values: [] };
      return { displayValue: "", values: [] };
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
