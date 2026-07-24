export interface IInheritSource {
  inheritName?: string | null;
  inheritNames?: string[] | null;
}

export const normalizeInheritNames = (source: IInheritSource): string[] => {
  const candidates =
    source.inheritNames && source.inheritNames.length > 0
      ? source.inheritNames
      : source.inheritName
      ? [source.inheritName]
      : [];
  const seen = new Set<string>();
  const result: string[] = [];
  for (const name of candidates) {
    const trimmed = typeof name === "string" ? name.trim() : "";
    if (trimmed.length > 0 && !seen.has(trimmed)) {
      seen.add(trimmed);
      result.push(trimmed);
    }
  }
  return result;
};

export const getPrimaryInheritName = (
  source: IInheritSource
): string | undefined => {
  return normalizeInheritNames(source)[0];
};

export interface IInheritanceEdgeSource extends IInheritSource {
  name?: string | null;
  tableType?: string | null;
}

export interface IInheritanceEdge {
  parent: string;
  child: string;
  isModule: boolean;
}

export const getInheritanceEdges = (
  source: IInheritanceEdgeSource
): IInheritanceEdge[] => {
  const child = typeof source.name === "string" ? source.name.trim() : "";
  if (child.length === 0) {
    return [];
  }
  const isModule = source.tableType === "MODULE";
  return normalizeInheritNames(source).map((parent) => ({
    parent,
    child,
    isModule,
  }));
};
