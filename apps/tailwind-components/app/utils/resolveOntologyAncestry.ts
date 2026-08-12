import type { IOntologyTreeItem } from "./buildOntologyTree";

export interface IOntologyTerm {
  name: string;
  label?: string;
  definition?: string;
  order?: number;
  parent?: { name: string } | null;
}

export const resolveOntologyAncestry = (
  values: IOntologyTreeItem[],
  termsByName: Map<string, IOntologyTerm>
): IOntologyTreeItem[] =>
  values.map(
    (value) => linkAncestors(value.name, termsByName, new Set()) ?? value
  );

const linkAncestors = (
  termName: string,
  termsByName: Map<string, IOntologyTerm>,
  visitedNames: Set<string>
): IOntologyTreeItem | undefined => {
  if (visitedNames.has(termName)) {
    return undefined;
  }
  visitedNames.add(termName);

  const term = termsByName.get(termName);
  if (!term) {
    return undefined;
  }

  const parentName = term.parent?.name;
  const parent = parentName
    ? linkAncestors(parentName, termsByName, visitedNames)
    : undefined;

  return {
    name: term.name,
    label: term.label,
    definition: term.definition,
    order: term.order,
    parent,
  };
};
