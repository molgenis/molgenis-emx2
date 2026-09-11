// mg_tableclass is "<schemaId>.<tableId>"; a schema id cannot contain a dot but a table id can.
export function parseMgTableclass(
  value: unknown
): { schemaId: string; tableId: string } | undefined {
  if (typeof value !== "string") return undefined;

  const dotIndex = value.indexOf(".");
  if (dotIndex < 0) return undefined;

  const schemaId = value.slice(0, dotIndex);
  const tableId = value.slice(dotIndex + 1);
  if (!schemaId || !tableId) return undefined;

  return { schemaId, tableId };
}
