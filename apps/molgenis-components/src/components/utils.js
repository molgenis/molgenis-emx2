import constants from "./constants";

const { CODE_0, CODE_9, CODE_BACKSPACE, CODE_DELETE } = constants;

export function isNumericKey(event) {
  const keyCode = event.which ? event.which : event.keyCode;
  return (
    (keyCode >= CODE_0 && keyCode <= CODE_9) ||
    keyCode === CODE_BACKSPACE ||
    keyCode === CODE_DELETE
  );
}

export function flattenObject(object) {
  if (typeof object === "object") {
    let result = "";
    Object.keys(object).forEach((key) => {
      if (object[key] === null) {
        return;
      }
      if (typeof object[key] === "object") {
        result += this.flattenObject(object[key]);
      } else {
        result += " " + object[key];
      }
    });
    return result;
  } else {
    return object;
  }
}

export function getPrimaryKey(row, tableMetaData) {
  //we only have pkey when the record has been saved
  if (!row["mg_insertedOn"] || !tableMetaData) {
    return null;
  } else {
    return tableMetaData.columns?.reduce((accum, column) => {
      if (column.key === 1 && row[column.id]) {
        accum[column.id] = row[column.id];
      }
      return accum;
    }, {});
  }
}

export function deepClone(original) {
  // node js may not have structuredClone function, then fallback to deep clone via JSON
  return typeof structuredClone === "function"
    ? structuredClone(original)
    : JSON.parse(JSON.stringify(original));
}

export function filterObject(object, filter) {
  return Object.keys(object).reduce((accum, key) => {
    if (filter(key)) {
      accum[key] = object[key];
    }
    return accum;
  }, {});
}
