import { IColumn } from "../../../Interfaces/IColumn";
import { IRow } from "../../../Interfaces/IRow";
import { ITableMetaData } from "../../../Interfaces/ITableMetaData";
import constants from "../../constants.js";
import { deepClone, filterObject } from "../../utils";

const { EMAIL_REGEX, HYPERLINK_REGEX, AUTO_ID, HEADING } = constants;

export function getRowErrors(
  tableMetaData: ITableMetaData,
  rowData: Record<string, any>
) {
  return tableMetaData.columns.reduce((accum, column) => {
    accum[column.name] = getColumnError(column, rowData, tableMetaData);
    return accum;
  }, {} as Record<string, string | undefined>);
}

function getColumnError(
  column: IColumn,
  values: Record<string, any>,
  tableMetaData: ITableMetaData
) {
  const value = values[column.name];
  const type = column.columnType;
  const isInvalidNumber = isInValidNumericValue(type, value);
  // FIXME: this function should also check all array types
  // FIXME: longs are not checked
  const missesValue = value === undefined || value === null || value === "";

  if (column.columnType === AUTO_ID || column.columnType === HEADING) {
    return undefined;
  }
  if (column.required && (missesValue || isInvalidNumber)) {
    return column.name + " is required";
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
    return getColumnValidationError(column.validation, values, tableMetaData);
  }
  if (isRefLinkWithoutOverlap(column, values)) {
    return `value should match your selection in column '${column.refLink}'`;
  }

  return undefined;
}

function isInValidNumericValue(columnType: string, value: number) {
  if (["DECIMAL", "INT"].includes(columnType)) {
    return isNaN(value);
  } else {
    return false;
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
    if (!copy.hasOwnProperty(column.name)) {
      copy[column.name] = null;
    }
  });

  // FIXME: according to the new Function definition the input here is incorrectly typed
  // see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function
  // FIXME: use es2021 instead of es2020 as you need it for replaceAll
  const func = new Function(
    Object.keys(copy),
    `return eval('${expression.replaceAll("'", '"')}');`
  );
  return func(...Object.values(copy));
}

function isRefLinkWithoutOverlap(column: IColumn, values: Record<string, any>) {
  if (!column.refLink) {
    return false;
  }
  const value = values[column.name];
  const refValue = values[column.refLink];

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
  const keyColumnsNames = tableMetaData?.columns
    ?.filter((column: IColumn) => column.key === 1)
    .map((column: IColumn) => column.name);

  return filterObject(rowData, (key) => !keyColumnsNames?.includes(key));
}

export function filterVisibleColumns(
  columns: IColumn[],
  visibleColumns: string[] | null
): IColumn[] {
  if (!visibleColumns) {
    return columns;
  } else {
    return columns.filter((column) => visibleColumns.includes(column.name));
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

export function splitColumnNamesByHeadings(columns: IColumn[]): string[][] {
  return columns.reduce((accum, column) => {
    if (column.columnType === "HEADING") {
      accum.push([column.name]);
    } else {
      if (accum.length === 0) {
        accum.push([] as string[]);
      }
      accum[accum.length - 1].push(column.name);
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
