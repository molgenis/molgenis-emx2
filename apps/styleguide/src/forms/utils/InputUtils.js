import { CODE_0, CODE_9, CODE_BACKSPACE } from "../../constants";

export function isNumericKey(event) {
  let specialKeys = [];
  specialKeys.push(CODE_BACKSPACE);
  const keyCode = event.which ? event.which : event.keyCode;
  return (
    (keyCode >= CODE_0 && keyCode <= CODE_9) ||
    specialKeys.indexOf(keyCode) !== -1
  );
}
