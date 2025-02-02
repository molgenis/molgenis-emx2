import type { IColumn, ITableMetaData} from "../../metadata-utils/src/types";
import {deepClone, filterObject} from "~/utils/utils";
import constants from "~/utils/constants";


const { EMAIL_REGEX, HYPERLINK_REGEX, PERIOD_REGEX, AUTO_ID, HEADING } =
  constants;

export function getRowErrors(
  schemaId: string,
  tableMetaData: ITableMetaData,
  rowData: Record<string, any>
): Record<string, string> {
  return tableMetaData.columns.reduce(
    (accum: Record<string, string>, column: IColumn) => {
      const error = getColumnError(column, rowData, schemaId, tableMetaData);
      if (error) {
        accum[column.id] = error;
      }
      return accum;
    },
    {}
  );
}

function getColumnError(
  column: IColumn,
  rowData: Record<string, any>,
  schemaId: string,
  tableMetaData: ITableMetaData
): string | undefined {
  const value = rowData[column.id];
  const type = column.columnType;
  const missesValue = isMissingValue(value);
  // FIXME: this function should also check all array types
  // FIXME: longs are not checked

  try {
    if (!isColumnVisible(column, rowData, tableMetaData)) {
      return undefined;
    }
  } catch (error) {
    return error as string;
  }

  if (column.columnType === AUTO_ID || column.columnType === HEADING) {
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
        schemaId,
        tableMetaData
      );
      if (error && missesValue) {
        return error;
      }
    }
  }

  if (missesValue) {
    return undefined;
  }
  if (type === "EMAIL" && !isValidEmail(value)) {
    return "Invalid email address";
  }
  if (type === "EMAIL_ARRAY" && containsInvalidEmail(value)) {
    return "Invalid email address";
  }
  if (type === "HYPERLINK" && !isValidHyperlink(value)) {
    return "Invalid hyperlink";
  }
  if (type === "HYPERLINK_ARRAY" && containsInvalidHyperlink(value)) {
    return "Invalid hyperlink";
  }
  if (type === "PERIOD" && !isValidPeriod(value)) {
    return "Invalid Period: should start with a P and should contain at least a Y(year), M(month) or D(day): e.g. 'P1Y3M14D'";
  }
  if (type === "PERIOD_ARRAY" && containsInvalidPeriod(value)) {
    return "Invalid Period: should start with a P and should contain at least a Y(year), M(month) or D(day): e.g. 'P1Y3M14D'";
  }
  if (type === "JSON") {
    try {
      if (!isJsonObjectOrArray(JSON.parse(value))) {
        return `Root element must be an object or array`;
      }
    } catch {
      return `Please enter valid JSON`;
    }
  }
  if (column.validation) {
    return getColumnValidationError(column.validation, rowData, schemaId, tableMetaData);
  }

  return undefined;
}

export function isMissingValue(value: any): boolean {
  if (Array.isArray(value)) {
    return value.length === 0 || value.some((element) => isMissingValue(element));
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
    return value;
  }
}

function isInValidNumericValue(columnType: string, value: number) {
  if (["DECIMAL", "INT"].includes(columnType)) {
    return isNaN(value);
  } else {
    return false;
  }
}

function getRequiredExpressionError(
  expression: string,
  values: Record<string, any>,
  schemaId: string,
  tableMetaData: ITableMetaData
): string | undefined {
  try {
    const result = executeExpression(expression, values, schemaId, tableMetaData);
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

/** validation error is when result is string | false */
export function getColumnValidationError(
  validation: string,
  values: Record<string, any>,
  schemaId: string,
  tableMetaData: ITableMetaData
) {
  try {
    const result = executeExpression(validation, values, schemaId, tableMetaData);
    if(result) {
      return `${result}`;
    } else {
      return "";
    }
  } catch (error) {
    return `Invalid validation expression '${validation}', reason: ${error}`;
  }
}

export function executeExpression(
  expression: string,
  values: Record<string, any>,
  schemaId: string,
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
    copy.mg_tableclass = `${schemaId}.${tableMetaData.label}`;
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

function isValidHyperlink(value: any) {
  return HYPERLINK_REGEX.test(String(value).toLowerCase());
}

function containsInvalidHyperlink(hyperlinks: any) {
  return hyperlinks.find((hyperlink: any) => !isValidHyperlink(hyperlink));
}

function isValidEmail(value: any) {
  return EMAIL_REGEX.test(String(value).toLowerCase());
}

function containsInvalidEmail(emails: any) {
  return emails.find((email: any) => !isValidEmail(email));
}

function isValidPeriod(value: any) {
  return PERIOD_REGEX.test(String(value));
}

function containsInvalidPeriod(periods: any) {
  return periods.find((period: any) => !isValidPeriod(period));
}

export function isJsonObjectOrArray(parsedJson: any) {
  if (typeof parsedJson === "object" && parsedJson !== null) {
    return true;
  }
  return false;
}

export function removeKeyColumns(tableMetaData: ITableMetaData, rowData: columnValueObject) {
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
  values: Record<string, any>,
  schemaId: string,
  tableMetadata: ITableMetaData
): boolean {
  const expression = column.visible;
  if (expression) {
    try {
      return executeExpression(expression, values, schemaId, tableMetadata);
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
      accum[accum.length - 1].push(column.id);
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
