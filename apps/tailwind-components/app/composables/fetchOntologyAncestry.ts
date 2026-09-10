import type { IColumn, IRow } from "../../../metadata-utils/src/types";
import type { IOntologyTerm, IOntologyTreeItem } from "../../types/types";
import { resolveOntologyAncestry } from "../utils/resolveOntologyAncestry";
import fetchGraphql from "./fetchGraphql";

const resolved = new Map<string, Map<string, IOntologyTerm>>();
const inflight = new Map<string, Promise<Map<string, IOntologyTerm>>>();

export default async function fetchOntologyAncestry(
  schemaId: string,
  tableId: string,
  termNames: string[]
): Promise<Map<string, IOntologyTerm>> {
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
}

export async function withOntologyAncestry(
  row: IRow,
  columns: IColumn[]
): Promise<IRow> {
  const resolvedRow = { ...row };
  await Promise.all(
    columns.map(async ({ id, columnType, refSchemaId, refTableId }) => {
      const value = row[id];
      if (
        !["ONTOLOGY", "ONTOLOGY_ARRAY"].includes(columnType) ||
        !value ||
        !refSchemaId ||
        !refTableId
      ) {
        return;
      }
      const candidates: unknown[] = Array.isArray(value) ? value : [value];
      const terms = candidates.filter(isOntologyTreeItem);
      try {
        const termsByName = await fetchOntologyAncestry(
          refSchemaId,
          refTableId,
          terms.map((term) => term.name)
        );
        const withAncestors = resolveOntologyAncestry(terms, termsByName);
        resolvedRow[id] = Array.isArray(value)
          ? withAncestors
          : withAncestors[0];
      } catch (err) {
        console.error("Failed to resolve ontology ancestry", err);
      }
    })
  );
  return resolvedRow;
}

function isOntologyTreeItem(value: unknown): value is IOntologyTreeItem {
  return (
    typeof value === "object" &&
    value !== null &&
    "name" in value &&
    typeof value.name === "string"
  );
}

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
