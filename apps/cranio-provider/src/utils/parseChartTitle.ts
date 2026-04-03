/**
 * @name parseChartTitle
 * @param string string containing a value to replace
 * @param value value to subsititute
 * @param pattern search pattern
 * @returns string
 */
export function parseChartTitle(
  string: string,
  value: string | number,
  pattern: string = "${value}"
) {
  return string.replace(pattern, `${value}`);
}
