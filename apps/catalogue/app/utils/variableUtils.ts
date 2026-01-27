import type { IVariables } from "../../interfaces/catalogue";

export const getKey = (variable: IVariables) => {
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
