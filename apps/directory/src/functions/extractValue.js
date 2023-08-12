export function extractValue(item, key) {
  const isArray = Array.isArray(item);

  if (isArray) {
    return item.map((item) => item[key]);
  } else if (typeof item === "object") {
    return item[key];
  } else return item;
}
