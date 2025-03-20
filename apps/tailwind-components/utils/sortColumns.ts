export function sortColumns<T extends { position?: number }>(
  columns: T[]
): T[] {
  return columns.sort(compareColumns);
}

function compareColumns<T extends { position?: number }>(
  col1: T,
  col2: T
): number {
  return (col1.position ?? 0) - (col2.position ?? 0);
}
