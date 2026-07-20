import { describe, expect, it } from "vitest";
import { resourceToCartItem, variableToCartItem } from "./cartItem";
import type { IResources, IVariables } from "~~/interfaces/catalogue";

describe("resourceToCartItem", () => {
  it("should convert a resource to a cart item", () => {
    const resource = { id: "res1", name: "Resource 1" } as IResources;
    expect(resourceToCartItem(resource)).toEqual({
      id: "resource:res1",
      label: "res1",
      type: "resource",
      data: resource,
    });
  });
});

describe("variableToCartItem", () => {
  it("should convert a variable to a cart item with a composite id", () => {
    const variable = {
      name: "height",
      label: "Height",
      resource: { id: "network1" },
      table: { name: "core", resource: { id: "cohort1" } },
    } as IVariables;

    expect(variableToCartItem(variable)).toEqual({
      id: "variable:network1:cohort1:core:height",
      label: "network1: core.height",
      type: "variable",
      data: variable,
    });
  });
});
