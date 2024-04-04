/**
 * Create the GraphQL structure for a column that has the class ontology type
 * @param name name of the column to query
 * @param type the column type (e.g., int, string, ontology, etc.)
 * @returns graphql string with primary ontology fields
 */
export function createColumnQuery(name: string, type: string) {
  return type === "ONTOLOGY"
    ? `${name} {
        order
        name
        label
        codesystem
        code
        ontologyTermURI
        definition
      }
    `
    : name;
}

export interface VariablesIF {
  column?: string;
  columnType?: string;
}

export function createQuery({ table, x, y, group }: VariablesIF) {
  const xQuery = createColumnQuery(x.column, x.columnType);
  const yQuery = createColumnQuery(y.column, y.columnType);
  const groupQuery = createColumnQuery(group.column, group.columnType);

  const queries = [xQuery, groupQuery, yQuery].filter((value: string) => value);
  const columnQuerystring = queries.join("\n     ");
  const query = `query {
    ${table} {
      ${columnQuerystring}
    }
  }`;

  return query;
}
