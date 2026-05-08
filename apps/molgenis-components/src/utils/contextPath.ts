export function getContextPath(): string {
  if (typeof window === "undefined") return "";

  const injected = (window as any).__molgenisContextPath;
  if (injected !== undefined) return injected as string;

  // Fallback for local dev (Vite serve): no injection, no context path
  const path = window.location.pathname;
  const appsIdx = path.indexOf("/apps/");
  if (appsIdx >= 0) return path.substring(0, appsIdx);

  return "";
}
