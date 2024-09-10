import type { IVariableBase } from "~/interfaces/types";

export const getKey = (variable: IVariableBase) => {
  return {
    name: variable.name,
    collection: {
      id: variable.collection.id,
    },
    dataset: {
      name: variable.dataset.name,
      collection: {
        id: variable.dataset.collection.id,
      },
    },
  };
};
