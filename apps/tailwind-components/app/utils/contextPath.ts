export function getContextPath(): string {
  if (typeof window === "undefined") return "";
  return (window as any).__molgenisContextPath ?? "";
}
