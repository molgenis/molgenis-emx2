import type { IOntologyTerm } from "../utils/resolveOntologyAncestry";
import fetchGraphql from "./fetchGraphql";

const resolved = new Map<string, Map<string, IOntologyTerm>>();
const inflight = new Map<string, Promise<Map<string, IOntologyTerm>>>();

export default async (
  schemaId: string,
  tableId: string,
  termNames: string[]
): Promise<Map<string, IOntologyTerm>> => {
  const ontologyKey = `${schemaId}.${tableId}`;
  const termsByName =
    resolved.get(ontologyKey) ?? new Map<string, IOntologyTerm>();
  resolved.set(ontologyKey, termsByName);

  const unresolvedNames = [...new Set(termNames)].filter(
    (name) => !termsByName.has(name)
  );
  if (!unresolvedNames.length) {
    return termsByName;
  }

  const requestKey = `${ontologyKey}.${[...unresolvedNames].sort().join("\0")}`;
  if (!inflight.has(requestKey)) {
    inflight.set(
      requestKey,
      fetchAncestryInto(
        termsByName,
        schemaId,
        tableId,
        unresolvedNames
      ).finally(() => {
        inflight.delete(requestKey);
      })
    );
  }

  return inflight.get(requestKey)!;
};

async function fetchAncestryInto(
  termsByName: Map<string, IOntologyTerm>,
  schemaId: string,
  tableId: string,
  termNames: string[]
): Promise<Map<string, IOntologyTerm>> {
  const query = `query ${tableId}( $filter:${tableId}Filter ) {
        ${tableId}( filter:$filter ) {
          name
          label
          definition
          order
          parent { name }
        }
      }`;

  const data = await fetchGraphql(schemaId, query, {
    filter: { _match_any_including_parents: termNames },
  });

  for (const term of (data?.[tableId] ?? []) as IOntologyTerm[]) {
    termsByName.set(term.name, term);
  }
  return termsByName;
}
