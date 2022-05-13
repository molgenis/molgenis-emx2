import constants from "../constants";
import _ from "lodash";

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
    return _.reduce(
      object,
      (accum, value) => {
        if (value === null) {
          return accum;
        }
        if (typeof value === "object") {
          accum += this.flattenObject(value);
        } else {
          accum += " " + value;
        }
        return accum;
      },
      ""
    );
  } else {
    return object;
  }
}
