export function formatDate(val: string | null | undefined): string {
  if (!val) return "-";
  try {
    return new Date(val).toLocaleString();
  } catch {
    return val;
  }
}
