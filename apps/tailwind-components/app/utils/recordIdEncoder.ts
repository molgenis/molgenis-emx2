function flattenToParams(
  obj: Record<string, any>,
  params: URLSearchParams,
  prefix: string
): void {
  for (const [key, value] of Object.entries(obj)) {
    const paramKey = prefix ? `${prefix}.${key}` : key;
    if (value !== null && typeof value === "object" && !Array.isArray(value)) {
      flattenToParams(value, params, paramKey);
    } else if (value !== null && value !== undefined) {
      params.append(paramKey, String(value));
    }
  }
}

function unflattenParams(params: URLSearchParams): Record<string, any> {
  const result: Record<string, any> = {};
  for (const [key, value] of params.entries()) {
    const parts = key.split(".");
    let current: Record<string, any> = result;
    for (let i = 0; i < parts.length - 1; i++) {
      const part = parts[i]!;
      if (!(part in current)) {
        current[part] = {};
      }
      current = current[part] as Record<string, any>;
    }
    const lastPart = parts[parts.length - 1]!;
    current[lastPart] = value;
  }
  return result;
}

export function encodeRecordId(pk: Record<string, any>): string {
  const params = new URLSearchParams();
  flattenToParams(pk, params, "");
  return params.toString();
}

export function decodeRecordId(queryString: string): Record<string, any> {
  const params = new URLSearchParams(queryString);
  return unflattenParams(params);
}
