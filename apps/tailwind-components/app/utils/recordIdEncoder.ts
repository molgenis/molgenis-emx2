export function encodeRecordId(pk: Record<string, any>): string {
  return btoa(JSON.stringify(pk));
}

export function decodeRecordId(encoded: string): Record<string, any> {
  return JSON.parse(atob(encoded));
}
