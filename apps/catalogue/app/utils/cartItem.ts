import type { IResources, IVariables } from "../../interfaces/catalogue";
import type { ICartItem } from "../../interfaces/types";
import { getKey } from "./variableUtils";

export function resourceToCartItem(resource: IResources): ICartItem {
  return {
    id: `resource:${resource.id}`,
    label: resource.id,
    type: "resource",
    data: resource,
  };
}

export function variableToCartItem(variable: IVariables): ICartItem {
  const key = getKey(variable);
  return {
    id: `variable:${key.resource.id}:${key.table.resource.id}:${key.table.name}:${key.name}`,
    label: `${variable.resource.id}: ${variable.table.name}.${variable.name}`,
    type: "variable",
    data: variable,
  };
}
