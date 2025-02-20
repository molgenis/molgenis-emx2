import type { IColumn, ITableMetaData } from "../../metadata-utils/src/types";
import constants from "../../molgenis-components/src/components/constants";
import {
  isColumnVisible,
  isJsonObjectOrArray,
  isMissingValue,
} from "../../molgenis-components/src/components/forms/formUtils/formUtils";
import {
  deepClone,
  getBigIntError,
} from "../../molgenis-components/src/components/utils";

const {
  AUTO_ID,
  EMAIL_REGEX,
  HEADING,
  HYPERLINK_REGEX,
  MAX_INT,
  MIN_INT,
  PERIOD_REGEX,
} = constants;

export function getColumnError(
  column: IColumn,
  rowData: Record<string, any>,
  tableMetadata: ITableMetaData
): string | undefined {
  const value = rowData[column.id];
  const type = column.columnType;
  // FIXME: this function should also check all array types

  try {
    if (!isColumnVisible(column, rowData, tableMetadata)) {
      return undefined;
    }
  } catch (error) {
    return error as string;
  }

  if (column.columnType === AUTO_ID || column.columnType === HEADING) {
    return undefined;
  }

  const missesValue = isMissingValue(value);

  if (column.required && column.required !== "false") {
    const requiredError = getRequiredError(
      column,
      value,
      rowData,
      tableMetadata
    );
    if (requiredError) {
      return requiredError;
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
  if (type === "LONG" && getBigIntError(value)) {
    return getBigIntError(value);
  }
  if (type === "DECIMAL" && isNaN(parseFloat(value))) {
    return "Invalid number";
  }
  if (type === "INT") {
    const intError = getIntError(value as number);
    if (intError) {
      return intError;
    }
  }
  if (column.validation) {
    return getColumnValidationError(column.validation, rowData, tableMetadata);
  }

  return undefined;
}

function getIntError(value: number) {
  if (isNaN(value)) {
    return "Invalid number";
  }
  if (value < MIN_INT || value > MAX_INT) {
    return `Invalid value: must be value from ${MIN_INT} to ${MAX_INT}`;
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

export function executeExpression(
  expression: string,
  values: Record<string, any>,
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
  const simplePostClient = function (
    query: string,
    variables: object,
    schemaId?: string
  ) {
    const xmlHttp = new XMLHttpRequest();
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

function getRequiredError(
  column: IColumn,
  value: any,
  rowData: Record<string, any>,
  tableMetadata: ITableMetaData
) {
  const missesValue = isMissingValue(value);
  if (column.required === "true") {
    if (missesValue) {
      return column.label + " is required";
    }
  } else {
    const error = getRequiredExpressionError(
      column.required as string,
      rowData,
      tableMetadata
    );
    if (error && missesValue) {
      return error;
    }
  }
}
