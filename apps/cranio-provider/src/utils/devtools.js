import { randomInt } from "d3";

export function randomGroupDataset(groups, categories, minValue, maxValue) {
  return categories
    .map((group) => {
      return groups.map((category) => {
        return {
          group: group,
          category: category,
          value: randomInt(minValue, maxValue)(),
        };
      });
    })
    .flat();
}

export function seq(start, stop, by) {
  return Array.from(
    { length: (stop - start) / by + 1 },
    (_, i) => start + i * by
  );
}
