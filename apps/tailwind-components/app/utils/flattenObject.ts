export function flattenObject(object: Record<string, unknown>): string {
  if (typeof object === "object") {
    let result = "";
    Object.keys(object).forEach((key) => {
      if (object[key] === null) {
        return;
      }
      if (typeof object[key] === "object") {
        result += flattenObject(object[key] as Record<string, unknown>);
      } else {
        result += " " + object[key];
      }
    });
    return result;
  } else {
    return String(object);
  }
}
