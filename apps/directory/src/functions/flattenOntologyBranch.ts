import { IOntologyItem } from "../interfaces/interfaces";

/** Uses basic non-recursive depth-first search algorithm*/
export default function flattenOntologyBranch(branch: IOntologyItem) {
  let stack = [branch];
  let result: IOntologyItem[] = [];
  while (stack.length) {
    const current = stack.pop();
    if (current?.children) {
      stack.push(...current.children);
    }
    if (current) {
      result.push(current);
    }
  }
  return result;
}
