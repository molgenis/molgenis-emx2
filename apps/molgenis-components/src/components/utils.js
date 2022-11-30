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
  // return typeof structuredClone === "function"
  //   ? structuredClone(original)
  //   :
  //structuredClone doesn't work in vue 3
  return JSON.parse(JSON.stringify(original));
}

export function filterObject(object, filter) {
  return Object.keys(object).reduce((accum, key) => {
    if (filter(key)) {
      accum[key] = object[key];
    }
    return accum;
  }, {});
}

export function flipSign(value) {
  switch (value) {
    case "-":
      return null;
    case null:
      return "-";
    default:
      if (value.toString().charAt(0) === "-") {
        return value.toString().substring(1);
      } else {
        return "-" + value;
      }
  }
}

export function convertToCamelCase(string) {
  const words = string.trim().split(/\s+/);
  let result = "";
  words.forEach((word, index) => {
    if (index === 0) {
      result += word.charAt(0).toLowerCase();
    } else {
      result += word.charAt(0).toUpperCase();
    }
    if (word.length > 1) {
      result += word.slice(1);
    }
  });
  return result;
}

export function convertToPascalCase(string) {
  const words = string.trim().split(/\s+/);
  let result = "";
  words.forEach((word, index) => {
    result += word.charAt(0).toUpperCase();
    if (word.length > 1) {
      result += word.slice(1);
    }
  });
  return result;
}
