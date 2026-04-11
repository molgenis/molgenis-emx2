import type { IFilterValue } from "../../types/filters";

export function extractDisplayValue(obj: Record<string, unknown>): string {
  if (obj.name) return String(obj.name);
  if (obj.label) return String(obj.label);
  const firstValue = Object.values(obj)[0];
  return String(firstValue);
}

export function formatFilterValue(filterValue: IFilterValue): {
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
        const formatted = value.map((v) => {
          if (typeof v === "object" && v !== null) {
            return extractDisplayValue(v);
          }
          return String(v);
        });
        if (value.length > 1) {
          return { displayValue: `${value.length}`, values: formatted };
        }
        return { displayValue: formatted[0] || "", values: [] };
      }
      if (typeof value === "object" && value !== null) {
        return { displayValue: extractDisplayValue(value), values: [] };
      }
      return { displayValue: String(value), values: [] };
  }
}
