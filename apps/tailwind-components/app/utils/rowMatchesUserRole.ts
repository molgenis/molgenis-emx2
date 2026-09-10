import type { IRow } from "../../../metadata-utils/src/types";

/**
 * A row is under row level security when it carries mg_roles. The active user
 * may act on it when one of their roles is listed there.
 */
export function rowMatchesUserRole(row: IRow, userRoles: string[]): boolean {
  const mgRoles: string[] = (row?.mg_roles ?? []) as string[];
  return userRoles.some((role) => mgRoles.includes(role));
}
