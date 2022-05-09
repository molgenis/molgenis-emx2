import constants from "../../constants";
const { CODE_0, CODE_9, CODE_BACKSPACE, CODE_DELETE } = constants;

export function isNumericKey(event) {
  const keyCode = event.which ? event.which : event.keyCode;
  return (
    (keyCode >= CODE_0 && keyCode <= CODE_9) ||
    keyCode === CODE_BACKSPACE ||
    keyCode === CODE_DELETE
  );
}
