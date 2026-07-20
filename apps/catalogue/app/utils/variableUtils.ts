import type { IVariables } from "../../interfaces/catalogue";

export const getKey = (variable: IVariables) => {
  return {
    name: variable.name,
    resource: {
      id: variable.resource.id,
    },
    table: {
      name: variable.table.name,
      resource: {
        id: variable.table.resource.id,
      },
    },
  };
};
