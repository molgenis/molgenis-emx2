import type { columnValue } from "../../../metadata-utils/src/types";

/**
 * Builds the `filter` query param of a csv/excel table download.
 *
 * Selected rows are matched on their primary key (the `key === 1` columns)
 * using the graphql key filter `{ equals: [key, ...] }`; refs in a composite
 * key are passed as nested objects. When both a filter and a row selection are
 * given, a row must match both.
 *
 * @returns undefined when there is nothing to filter on
 */
export function buildDownloadFilter(
  filter?: Record<string, unknown>,
  rowKeys?: Record<string, columnValue>[]
): Record<string, unknown> | undefined {
  const hasFilter = !!filter && Object.keys(filter).length > 0;
  const rowFilter = rowKeys?.length ? { equals: rowKeys } : undefined;

  if (hasFilter && rowFilter) {
    return { _and: [filter, rowFilter] };
  }
  return hasFilter ? filter : rowFilter;
}
