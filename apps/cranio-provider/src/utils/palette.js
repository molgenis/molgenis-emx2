import { schemeTableau10 } from "d3";

function generateColors(labels) {
  const colors = labels.map((label,index) => [label, schemeTableau10[index]])
  return Object.fromEntries(colors);
}

export default generateColors