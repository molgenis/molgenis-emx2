import { schemeTableau10 } from "d3";

/**
 * @name generateColorPalette
 *
 * Generate a series of colors using the selected cranio color scheme.
 *
 * @param labels an array of strings used to generate values
 *
 * @returns object containing one or more key value pairs where each key is a label and the value is a color.
 */
export function generateColorPalette(labels: string[]) {
  const colors = labels.map((label: string, index: number) => {
    return [label, schemeTableau10[index]];
  });
  return Object.fromEntries(colors);
}
