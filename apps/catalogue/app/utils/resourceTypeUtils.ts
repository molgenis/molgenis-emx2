export interface ResourceType {
  name?: string;
}

export function isCatalogueResource(types: ResourceType[] | undefined): boolean {
  if (!types) return false;
  return types.some((t) => t.name === "Catalogue");
}
