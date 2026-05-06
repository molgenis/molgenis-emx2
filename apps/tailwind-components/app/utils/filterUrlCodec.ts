import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue } from "../../types/filters";
import { RANGE_TYPES } from "./filterTypes";

const REF_TYPES = new Set([
  "REF",
  "REF_ARRAY",
  "REFBACK",
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
  "SELECT",
  "MULTISELECT",
  "RADIO",
  "CHECKBOX",
]);

const MULTI_VALUE_SEPARATOR = "|";
const RESERVED_PREFIX = "mg_";
const SEARCH_PARAM = "mg_search";
const LIKE_SUFFIX = "~like";

function extractStringKey(v: unknown, depth = 0): string {
  if (v === undefined) return "";
  if (depth > 10) return String(v);
  if (typeof v === "string") return v;
  if (typeof v !== "object" || v === null) return String(v);
  const obj = v as Record<string, unknown>;
  if (typeof obj.name === "string") return obj.name;
  const values = Object.values(obj);
  if (values.length === 0) return "";
  return extractStringKey(values[0], depth + 1);
}

function extractRefField(value: IFilterValue): string {
  const val = value.value;
  if (
    Array.isArray(val) &&
    val.length &&
    typeof val[0] === "object" &&
    val[0] !== null
  ) {
    return Object.keys(val[0])[0] ?? "name";
  }
  if (typeof val === "object" && val !== null) {
    return Object.keys(val)[0] ?? "name";
  }
  return "name";
}

export function serializeFilterValue(value: IFilterValue): string | null {
  const { operator, value: val } = value;
  if (val === null || val === undefined) return null;

  switch (operator) {
    case "between": {
      const [min, max] = val;
      const minStr = min ?? "";
      const maxStr = max ?? "";
      if (minStr === "" && maxStr === "") return null;
      return `${minStr}..${maxStr}`;
    }

    case "notNull":
      return "!null";

    case "isNull":
      return "null";

    case "like":
      return String(val);

    case "equals":
    default:
      if (Array.isArray(val)) {
        if (val.length && typeof val[0] === "object") {
          const keys = val.map((v) => extractStringKey(v));
          return keys.join(MULTI_VALUE_SEPARATOR);
        }
        return val.join(MULTI_VALUE_SEPARATOR);
      }
      if (typeof val === "object" && val !== null) {
        return extractStringKey(val);
      }
      return String(val);
  }
}

export function parseFilterValue(
  urlValue: string,
  column: IColumn,
  refField: string | null = null
): IFilterValue | null {
  if (!urlValue) return null;

  if (urlValue === "null") {
    return { operator: "isNull", value: true };
  }
  if (urlValue === "!null") {
    return { operator: "notNull", value: true };
  }

  const columnType = column.columnType;

  const MULTI_VALUE_TYPES = new Set([
    "ONTOLOGY",
    "ONTOLOGY_ARRAY",
    "BOOL",
    "RADIO",
    "CHECKBOX",
  ]);

  if (MULTI_VALUE_TYPES.has(columnType)) {
    if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
      return {
        operator: "equals",
        value: urlValue.split(MULTI_VALUE_SEPARATOR),
      };
    }
    return { operator: "equals", value: [urlValue] };
  }

  if (REF_TYPES.has(columnType)) {
    const field = refField ?? "name";
    if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
      const values = urlValue.split(MULTI_VALUE_SEPARATOR);
      return { operator: "equals", value: values.map((v) => ({ [field]: v })) };
    }
    const refValue = { [field]: urlValue };
    return { operator: "equals", value: [refValue] };
  }

  if (RANGE_TYPES.has(columnType)) {
    if (urlValue.includes("..")) {
      const [minStr = "", maxStr = ""] = urlValue.split("..");
      const isDateType = columnType.includes("DATE");
      return {
        operator: "between",
        value: [
          minStr === "" ? null : isDateType ? minStr : Number(minStr),
          maxStr === "" ? null : isDateType ? maxStr : Number(maxStr),
        ],
      };
    }
    const isDateType = columnType.includes("DATE");
    return {
      operator: "equals",
      value: isDateType ? urlValue : Number(urlValue),
    };
  }

  const STRING_TYPES = [
    "STRING",
    "TEXT",
    "EMAIL",
    "HYPERLINK",
    "AUTO_ID",
    "JSON",
    "STRING_ARRAY",
    "TEXT_ARRAY",
    "EMAIL_ARRAY",
    "HYPERLINK_ARRAY",
  ];

  if (columnType === "UUID" || columnType === "UUID_ARRAY") {
    return { operator: "equals", value: urlValue };
  }

  if (STRING_TYPES.includes(columnType)) {
    return { operator: "like", value: urlValue };
  }

  if (urlValue.includes(MULTI_VALUE_SEPARATOR)) {
    return { operator: "equals", value: urlValue.split(MULTI_VALUE_SEPARATOR) };
  }

  return { operator: "like", value: urlValue };
}

export function serializeFiltersToUrl(
  filterStates: Map<string, IFilterValue>,
  searchValue: string,
  columns: IColumn[]
): Record<string, string> {
  const params: Record<string, string> = {};

  if (searchValue.trim()) {
    params[SEARCH_PARAM] = searchValue.trim();
  }

  for (const [key, value] of filterStates) {
    const firstSegment = key.split(".")[0];
    const column = columns.find((c) => c.id === firstSegment);
    if (!column) continue;

    const serialized = serializeFilterValue(value);
    if (serialized !== null) {
      if (key.includes(".")) {
        if (value.operator === "like") {
          params[`${key}${LIKE_SUFFIX}`] = serialized;
        } else {
          const val = value.value;
          const isRefLikeValue =
            (val !== null && typeof val === "object" && !Array.isArray(val)) ||
            (Array.isArray(val) &&
              val.length > 0 &&
              typeof val[0] === "object");
          if (isRefLikeValue) {
            const refField = extractRefField(value);
            params[`${key}.${refField}`] = serialized;
          } else {
            params[`${key}.name`] = serialized;
          }
        }
      } else if (
        column.columnType === "RADIO" ||
        column.columnType === "CHECKBOX"
      ) {
        params[key] = serialized;
      } else if (REF_TYPES.has(column.columnType)) {
        const refField = extractRefField(value);
        params[`${key}.${refField}`] = serialized;
      } else {
        params[key] = serialized;
      }
    }
  }

  return params;
}

export function parseFiltersFromUrl(
  query: Record<
    string,
    string | string[] | (string | null)[] | null | undefined
  >,
  columns: IColumn[]
): { filters: Map<string, IFilterValue>; search: string } {
  const filters = new Map<string, IFilterValue>();
  let search = "";

  for (const [key, value] of Object.entries(query)) {
    if (key === SEARCH_PARAM && typeof value === "string") {
      search = value.replace(/\+/g, " ");
      continue;
    }

    if (key.startsWith(RESERVED_PREFIX)) continue;

    const isLikeKey = key.endsWith(LIKE_SUFFIX);
    const resolvedKey = isLikeKey ? key.slice(0, -LIKE_SUFFIX.length) : key;

    if (isLikeKey) {
      if (typeof value !== "string") continue;
      const decodedValue = value.replace(/\+/g, " ");
      const firstSeg = resolvedKey.split(".")[0]!;
      const column = columns.find((c) => c.id === firstSeg);
      if (!column) continue;
      filters.set(resolvedKey, { operator: "like", value: decodedValue });
      continue;
    }

    const segments = resolvedKey.split(".");
    const firstSegment = segments[0]!;

    const column = columns.find((c) => c.id === firstSegment);
    if (!column || typeof value !== "string") continue;
    const decodedValue = value.replace(/\+/g, " ");

    let filterKey: string;
    let refField: string | null;

    if (!REF_TYPES.has(column.columnType) || segments.length === 1) {
      filterKey = resolvedKey;
      refField = null;
    } else if (segments.length === 2) {
      filterKey = firstSegment;
      refField = segments[1]!;
    } else {
      filterKey = segments.slice(0, -1).join(".");
      refField = segments[segments.length - 1]!;
    }

    const filterValue = parseFilterValue(decodedValue, column, refField);
    if (filterValue) {
      filters.set(filterKey, filterValue);
    }
  }

  return { filters, search };
}
