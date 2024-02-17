import type { IColumn, ITableMetaData } from "meta-data-utils";
import { IRow } from "../../../Interfaces/IRow";
import constants from "../../constants.js";
import { deepClone, filterObject } from "../../utils";

const { EMAIL_REGEX, HYPERLINK_REGEX, AUTO_ID, HEADING } = constants;

export function getRowErrors(
  tableMetaData: ITableMetaData,
  rowData: Record<string, any>
) {
  return tableMetaData.columns.reduce((accum, column: IColumn) => {
    accum[column.id] = getColumnError(column, rowData, tableMetaData);
    return accum;
  }, {} as Record<string, string | undefined>);
}

function getColumnError(
  column: IColumn,
  rowData: Record<string, any>,
  tableMetaData: ITableMetaData
) {
  const value = rowData[column.id];
  const type = column.columnType;
  const isInvalidNumber = isInValidNumericValue(type, value);
  // FIXME: this function should also check all array types
  // FIXME: longs are not checked

  const missesValue = isMissingValue(value);

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
    if (column.required.toLowerCase() === "true") {
      if (missesValue || isInvalidNumber) {
        return column.label + " is required";
      }
    } else {
      let error = getRequiredExpressionError(
        column.required,
        rowData,
        tableMetaData
      );
      if (error && missesValue) return error;
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
  if (column.validation) {
    return getColumnValidationError(column.validation, rowData, tableMetaData);
  }
  if (isRefLinkWithoutOverlap(column, rowData)) {
    return `value should match your selection in column '${column.refLinkId}'`;
  }

  return undefined;
}

export function isMissingValue(value: any): boolean {
  if (Array.isArray(value)) {
    return value.some((element) => isMissingValue(element));
  }
  return value === undefined || value === null || value === "";
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
  values: Record<string, any>,
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
  values: Record<string, any>,
  tableMetaData: ITableMetaData
) {
  //make sure all columns have keys to prevent reference errors
  const copy: Record<string, any> = deepClone(values);
  tableMetaData.columns.forEach((column) => {
    if (!copy.hasOwnProperty(column.id)) {
      copy[column.id] = null;
    }
  });

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
    //@ts-ignore
    "return eval(`" + expression.replaceAll("`", "\\`") + "`)"
  );
  return func(simplePostClient, ...Object.values(copy));
}

function isRefLinkWithoutOverlap(column: IColumn, values: Record<string, any>) {
  if (!column.refLinkId) {
    return false;
  }
  const columnRefLink = column.refLinkId;
  const refLinkId = columnRefLink;

  const value = values[column.id];
  const refValue = values[refLinkId];

  if (typeof value === "string" && typeof refValue === "string") {
    return value && refValue && value !== refValue;
  } else {
    //FIXME: empty ref_array => should give 'required' error instead if applicable
    if (Array.isArray(value) && value.length === 0) {
      return false;
    }
    return (
      value &&
      refValue &&
      !JSON.stringify(value).includes(JSON.stringify(refValue))
    );
  }
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

export function removeKeyColumns(tableMetaData: ITableMetaData, rowData: IRow) {
  const keyColumnsIds = tableMetaData?.columns
    ?.filter((column: IColumn) => column.key === 1)
    .map((column: IColumn) => column.id);

  return filterObject(rowData, (key) => !keyColumnsIds?.includes(key));
}

export function filterVisibleColumns(
  columns: IColumn[],
  visibleColumns: string[] | null
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
  tableMetadata: ITableMetaData
): boolean {
  const expression = column.visible;
  if (expression) {
    try {
      return executeExpression(expression, values, tableMetadata);
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
