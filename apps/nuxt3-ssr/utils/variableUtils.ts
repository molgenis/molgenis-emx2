import { IVariableBase } from "~/interfaces/types";

export const getKey = (variable: IVariableBase) => {
  return {
    name: variable.name,
    resource: {
      id: variable.resource.id,
    },
    dataset: {
      name: variable.dataset.name,
      resource: {
        id: variable.dataset.resource.id,
      },
    },
  };
};
