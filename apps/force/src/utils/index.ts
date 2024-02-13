export function createPalette(labels: Array, palette: Array): Object {
  const colors = labels.map((label, index) => [label, palette[index]]);
  return Object.fromEntries(colors);
}
