import type {
  columnValue,
  IColumn,
  ITableMetaData,
  recordValue,
} from "../../../../../metadata-utils/src/types";
import type { IRow } from "../../../Interfaces/IRow";
import constants from "../../constants.js";
import { deepClone, filterObject } from "../../utils";

const {
  EMAIL_REGEX,
  HYPERLINK_REGEX,
  PERIOD_REGEX,
  UUID_REGEX,
  AUTO_ID,
  HEADING,
  SECTION,
  MIN_INT,
  MIN_NON_NEGATIVE_INT,
  MAX_INT,
  MIN_LONG,
  MAX_LONG,
} = constants;
const BIG_INT_ERROR = `Invalid long: must be value from ${MIN_LONG} to ${MAX_LONG}`;
const INT_ERROR = `Invalid integer: must be value from ${MIN_INT} to ${MAX_INT}`;
const PERIOD_EXPLANATION =
  "must start with a P and should contain at least a Y(year), M(month) or D(day): e.g. 'P1Y3M14D'";
const UUID_EXPLANATION =
  "must use a valid UUID format (rfc9562): e.g. '123e4567-e89b-12d3-a456-426614174000'";
export const NON_NEGATIVE_INT_ERROR = `Invalid non negative integer: must be value from ${MIN_NON_NEGATIVE_INT} to ${MAX_INT}`;

export function getRowErrors(
  tableMetaData: ITableMetaData,
  rowData: Record<string, any>
): Record<string, string> {
  return tableMetaData.columns.reduce(
    (accum: Record<string, string>, column: IColumn) => {
      const error = getColumnError(column, rowData, tableMetaData);
      if (error) {
        accum[column.id] = error;
      }
      return accum;
    },
    {}
  );
}

export function getColumnError(
  column: IColumn,
  rowData: recordValue | undefined,
  tableMetaData: ITableMetaData
): string | undefined {
  const value = rowData?.[column.id];
  const type = column.columnType;
  const missesValue = isMissingValue(value);
  // FIXME: this function should also check all array types

  try {
    if (!isColumnVisible(column, rowData, tableMetaData)) {
      return undefined;
    }
  } catch (error) {
    return error as string;
  }

  if (
    column.columnType === AUTO_ID ||
    column.columnType === HEADING ||
    column.columnType === SECTION
  ) {
    return undefined;
  }

  if (column.required) {
    if (isRequired(column.required)) {
      const isInvalidNumber = isInValidNumericValue(type, value);
      if (missesValue || isInvalidNumber) {
        return column.label + " is required";
      }
    } else {
      const error = getRequiredExpressionError(
        column.required as string,
        rowData,
        tableMetaData
      );
      if (error && missesValue) {
        return error;
      }
    }
  }

  if (value === undefined || (!type.includes("_ARRAY") && missesValue)) {
    return undefined;
  }
  if (type === "EMAIL" && isInvalidEmail(value)) {
    return "Invalid email address";
  }
  if (type === "EMAIL_ARRAY") {
    return readableStringArray(
      getInvalidEmails(value),
      " is an invalid email address",
      " are invalid email addresses"
    );
  }
  if (type === "HYPERLINK" && isInvalidHyperlink(value)) {
    return "Invalid hyperlink";
  }
  if (type === "HYPERLINK_ARRAY") {
    return readableStringArray(
      getInvalidHyperlinks(value),
      " is an invalid hyperlink",
      " are invalid hyperlinks"
    );
  }

  if (type === "PERIOD" && isInvalidPeriod(value)) {
    return "Invalid Period: " + PERIOD_EXPLANATION;
  }
  if (type === "PERIOD_ARRAY") {
    return readableStringArray(
      getInvalidPeriods(value),
      " is an invalid Period: " + PERIOD_EXPLANATION,
      " are invalid Periods: " + PERIOD_EXPLANATION
    );
  }
  if (type === "UUID" && isInvalidUUID(value)) {
    return "Invalid UUID: " + UUID_EXPLANATION;
  }
  if (type === "UUID_ARRAY") {
    return readableStringArray(
      getInvalidUUIDs(value),
      " is an invalid UUID: " + UUID_EXPLANATION,
      " are invalid UUIDs: " + UUID_EXPLANATION
    );
  }
  if (type === "JSON") {
    try {
      if (
        typeof value === "string" &&
        !isJsonObjectOrArray(JSON.parse(value))
      ) {
        return `Root element must be an object or array`;
      }
    } catch {
      return `Please enter valid JSON`;
    }
  }
  if (
    type === "LONG" &&
    value !== null &&
    isInvalidBigInt(value as string | undefined)
  ) {
    return BIG_INT_ERROR;
  }
  if (
    type === "LONG_ARRAY" &&
    Array.isArray(value) &&
    (value as unknown as Array<string>)?.length &&
    (value as string[])?.some(isInvalidBigInt)
  ) {
    return BIG_INT_ERROR;
  }
  if (type === "DECIMAL" && isNaN(parseFloat(value as string))) {
    return "Invalid number";
  }
  if (
    type === "DECIMAL_ARRAY" &&
    (value as unknown as string[])?.some(
      (val) => val && isNaN(parseFloat(val as string))
    )
  ) {
    return "Invalid number";
  }
  if (type === "INT" && isInvalidInt(value as number)) {
    return INT_ERROR;
  }
  if (type === "INT_ARRAY" && (value as number[])?.some(isInvalidInt)) {
    return INT_ERROR;
  }
  if (type === "NON_NEGATIVE_INT" && isInvalidNonNegativeInt(value as number)) {
    return NON_NEGATIVE_INT_ERROR;
  }

  if (type === "NON_NEGATIVE_INT_ARRAY") {
    return readableStringArray(
      getInvalidNonNegativeIntegers(value),
      " is an invalid non negative integer",
      " are invalid non negative integers"
    );
  }

  if (column.validation) {
    return getColumnValidationError(column.validation, rowData, tableMetaData);
  }
  return undefined;
}

export function readableStringArray(
  strings: any[],
  postErrorSingular?: string,
  postErrorPlural?: string
): string {
  const escapedStrings = strings.map((str) =>
    str.toString().replaceAll("'", "\\'")
  );
  if (escapedStrings.length === 0) {
    return "";
  } else if (escapedStrings.length === 1) {
    return `'${escapedStrings[0]}' ${postErrorSingular}`;
  } else {
    return `'${escapedStrings
      .slice(0, escapedStrings.length - 1)
      .join("', '")}' and '${
      escapedStrings[escapedStrings.length - 1]
    }' ${postErrorPlural}`;
  }
}

export function isInvalidBigInt(value?: string): boolean {
  const isValidRegex = /^-?\d+$/;
  if (!value) {
    return true;
  }
  if (isValidRegex.test(value)) {
    return BigInt(value) > BigInt(MAX_LONG) || BigInt(value) < BigInt(MIN_LONG);
  } else {
    return true;
  }
}

function isInvalidInteger(
  value: number,
  minInt: number,
  maxInt: number
): boolean {
  return isNaN(value) || value < minInt || value > maxInt;
}

function isInvalidInt(value: number): boolean {
  return isInvalidInteger(value, MIN_INT, MAX_INT);
}

export function isInvalidNonNegativeInt(value: number): boolean {
  return isInvalidInteger(value, MIN_NON_NEGATIVE_INT, MAX_INT);
}

export function isMissingValue(value: any): boolean {
  if (Array.isArray(value)) {
    return value.some((element) => isMissingValue(element));
  } else {
    return value === undefined || value === null || value === "";
  }
}

export function isRequired(value: string | boolean | undefined): boolean {
  if (typeof value === "string") {
    if (value.toLowerCase() === "true") {
      return true;
    } else {
      return false;
    }
  } else {
    return value === true;
  }
}

function isInValidNumericValue(columnType: string, value?: columnValue) {
  if (["DECIMAL", "INT"].includes(columnType)) {
    return value === undefined || (typeof value === "number" && isNaN(value));
  } else {
    return false;
  }
}

function getRequiredExpressionError(
  expression: string,
  values: Record<string, any> | undefined,
  tableMetaData: ITableMetaData
): string | undefined {
  try {
    const result = executeExpression(expression, values, tableMetaData);
    if (result === true) {
      return `Field is required when: ${expression}`;
    } else if (result === false || result === undefined) {
      return undefined;
    }
    return result;
  } catch (error) {
    return `Invalid expression '${expression}', reason: ${error}`;
  }
}

function getColumnValidationError(
  validation: string,
  values: recordValue | undefined,
  tableMetaData: ITableMetaData
) {
  try {
    const result = executeExpression(validation, values, tableMetaData);
    if (result === false) {
      return `Applying validation rule returned error: ${validation}`;
    } else if (result === true || result === undefined) {
      return undefined;
    } else {
      return `Applying validation rule returned error: ${result}`;
    }
  } catch (error) {
    return `Invalid validation expression '${validation}', reason: ${error}`;
  }
}

export function executeExpression(
  expression: string,
  values: recordValue | undefined,
  tableMetaData: ITableMetaData
) {
  //make sure all columns have keys to prevent reference errors
  const copy: Record<string, any> = deepClone(values);
  tableMetaData.columns.forEach((column: IColumn) => {
    if (!copy.hasOwnProperty(column.id)) {
      copy[column.id] = null;
    }
  });
  if (!copy.mg_tableclass) {
    copy.mg_tableclass = `${tableMetaData.id}.${tableMetaData.label}`;
  }

  // A simple client for scripts to use to request data.
  // Note: don't overuse this, the API call is blocking.
  let simplePostClient = function (
    query: string,
    variables: object,
    schemaId?: string
  ) {
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open(
      "POST",
      schemaId ? "/" + schemaId + "/graphql" : "graphql",
      false
    );
    xmlHttp.send(
      `{"query":"${query}", "variables":${JSON.stringify(variables)}}`
    );
    return JSON.parse(xmlHttp.responseText).data;
  };

  // FIXME: according to the new Function definition the input here is incorrectly typed
  // see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function
  // FIXME: use es2021 instead of es2020 as you need it for replaceAll
  const func = new Function(
    "simplePostClient",
    //@ts-ignore
    Object.keys(copy),
    "return eval(`" + expression.replaceAll("`", "\\`") + "`)"
  );
  return func(simplePostClient, ...Object.values(copy));
}

function isInvalidHyperlink(value: any) {
  return value && !HYPERLINK_REGEX.test(value);
}

function getInvalidHyperlinks(hyperlinks: any): string[] {
  return hyperlinks?.filter((hyperlink: string) =>
    isInvalidHyperlink(hyperlink)
  );
}

function getInvalidNonNegativeIntegers(numbers: any): number[] {
  return numbers?.filter((number: number) => isInvalidNonNegativeInt(number));
}

function isInvalidEmail(value: any) {
  return value && !EMAIL_REGEX.test(value);
}

function getInvalidEmails(emails: any): string[] {
  return emails.filter((email: any) => isInvalidEmail(email));
}

function isInvalidPeriod(value: any) {
  if (value === null || value === undefined || value === "") {
    return false;
  }
  return !PERIOD_REGEX.test(value);
}

function getInvalidPeriods(periods: any): string[] {
  return periods?.filter((period: any) => isInvalidPeriod(period));
}

function isInvalidUUID(value: any) {
  if (value === null || value === undefined || value === "") {
    return false;
  }
  return !UUID_REGEX.test(value);
}

function getInvalidUUIDs(uuids: any) {
  return uuids?.filter((uuid: any) => isInvalidUUID(uuid));
}

export function isJsonObjectOrArray(parsedJson: any) {
  return typeof parsedJson === "object" && parsedJson !== null;
}

export function removeKeyColumns(tableMetaData: ITableMetaData, rowData: IRow) {
  const keyColumnsIds = tableMetaData?.columns
    ?.filter((column: IColumn) => column.key === 1)
    .map((column: IColumn) => column.id);

  return filterObject(rowData, (key) => !keyColumnsIds?.includes(key));
}

export function filterVisibleColumns(
  columns: IColumn[],
  visibleColumns?: string[]
): IColumn[] {
  if (!visibleColumns) {
    return columns;
  } else {
    return columns.filter((column) => visibleColumns.includes(column.id));
  }
}

export function isColumnVisible(
  column: IColumn,
  values: recordValue | undefined,
  tableMetadata: ITableMetaData
): boolean {
  const expression = column.visible;
  if (expression !== undefined) {
    try {
      return executeExpression(expression, values || {}, tableMetadata);
    } catch (error) {
      throw `Invalid visibility expression, reason: ${error}`;
    }
  } else {
    return true;
  }
}

export function splitColumnIdsByHeadings(columns: IColumn[]): string[][] {
  return columns.reduce((accum, column) => {
    if (column.columnType === "HEADING") {
      accum.push([column.id]);
    } else {
      if (accum.length === 0) {
        accum.push([] as string[]);
      }
      accum[accum.length - 1]?.push(column.id);
    }
    return accum;
  }, [] as string[][]);
}

export function getChapterStyle(
  page: string[],
  errors: Record<string, string | undefined>
): { color: "red" } | {} {
  const fieldsWithErrors = page.filter((fieldsInPage: string) =>
    Boolean(errors[fieldsInPage])
  );
  return fieldsWithErrors.length ? { color: "red" } : {};
}

export function getSaveDisabledMessage(
  rowErrors: Record<string, string | undefined>
) {
  const numberOfErrors = Object.values(rowErrors).filter(
    (value) => value
  ).length;
  return numberOfErrors > 0
    ? `There are ${numberOfErrors} error(s) preventing saving`
    : "";
}
export function buildGraphqlFilter(
  defaultFilter: any,
  columns: IColumn[],
  errorCallback: (error: string) => void
) {
  let filter = deepClone(defaultFilter);
  if (columns) {
    columns.forEach((col) => {
      const conditions = col.conditions
        ? col.conditions.filter(
            (condition: string) => condition !== "" && condition !== undefined
          )
        : [];
      if (conditions.length) {
        if (
          col.columnType.startsWith("STRING") ||
          col.columnType.startsWith("TEXT") ||
          col.columnType.startsWith("JSON")
        ) {
          filter[col.id] = { like: conditions };
        } else if (col.columnType.startsWith("BOOL")) {
          filter[col.id] = { equals: conditions };
        } else if (
          col.columnType.startsWith("REF") ||
          col.columnType.startsWith("ONTOLOGY")
        ) {
          filter[col.id] = { equals: conditions };
        } else if (
          ["DECIMAL", "DECIMAL_ARRAY", "INT", "INT_ARRAY"].includes(
            col.columnType
          )
        ) {
          filter[col.id] = {
            between: conditions.flat().map((value) => parseFloat(value)),
          };
        } else if (
          [
            "LONG",
            "LONG_ARRAY",
            "DATE",
            "DATE_ARRAY",
            "DATETIME",
            "DATETIME_ARRAY",
          ].includes(col.columnType)
        ) {
          filter[col.id] = {
            between: conditions.flat(),
          };
        } else {
          errorCallback(
            `filter unsupported for column type ${col.columnType} (please report a bug)`
          );
        }
      }
    });
  }
  return filter;
}
