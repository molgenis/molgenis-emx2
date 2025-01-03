import type { GqlResp, IOntologyRespItem } from "~/interfaces/types";

const ONTOLOGIES_SCHEMA_NAME = "CatalogueOntologies";

export function fetchOntology(
  tableId: string,
  variables?: object
): Promise<GqlResp<IOntologyRespItem>> {
  const query = `
    query
    ${tableId}( $filter:${tableId}Filter, $orderby:${tableId}orderby )
    {
      ${tableId}( filter:$filter, limit:100000,  offset:0, orderby:$orderby )
        {
          name
          parent { name }
          definition
        }
      }
  `;

  return fetchGql<IOntologyRespItem>(query, variables, ONTOLOGIES_SCHEMA_NAME);
}
