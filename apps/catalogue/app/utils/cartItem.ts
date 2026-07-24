import type { IResources, IVariables } from "../../interfaces/catalogue";
import type { ICartItem } from "../../interfaces/types";
import { getKey } from "./variableUtils";

export function resourceToCartItem(resource: IResources): ICartItem {
  return {
    id: `resource:${resource.id}`,
    label: resource.id,
    type: "resource",
    // pid/name can be missing when the source page under-fetches;
    // fall back to the id so the payload stays identifiable
    pid: resource.pid ?? resource.id,
    name: resource.name ?? resource.id,
  };
}

export function variableToCartItem(variable: IVariables): ICartItem {
  const key = getKey(variable);
  return {
    id: `variable:${key.resource.id}:${key.table.resource.id}:${key.table.name}:${key.name}`,
    label: `${variable.resource.id}: ${variable.table.name}.${variable.name}`,
    type: "variable",
  };
}

export function cartItemsOfType<T extends ICartItem["type"]>(
  items: ICartItem[],
  type: T
): Extract<ICartItem, { type: T }>[] {
  return items.filter(
    (item): item is Extract<ICartItem, { type: T }> => item.type === type
  );
}
