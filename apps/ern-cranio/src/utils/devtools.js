import { randomInt,sum } from "d3";
import { asDataObject } from "../../../molgenis-viz/src/utils/utils";

export function randomPercentages({labels=[], asObject=false}) {
  let result = [];
  for (let i=0; i<labels.length; i++) {
    const currentSum = sum(result, row => row.value);
    const record = {label: labels[i]}
    if (i === 0) {
      record.value = randomInt(1,100)();
    } else if (i===length) {
      record.value = 100 - currentSum;  
    } else {
      record.value = randomInt(1, 100 - currentSum)();
    }
    result.push(record)
  }
  const sorted = result.sort((a, b) => {
    return a.value <= b.value
  });
  
  return asObject ? asDataObject(sorted, 'label','value') : sorted;
}

export function randomGroupDataset(groups, categories, minValue, maxValue) {
  return categories.map(group => {
    return groups.map(category => {
      return {
        group: group,
        category: category,
        value: randomInt(minValue, maxValue)()
      }
    })
  })
  .flat()
}
