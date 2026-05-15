/**
 * Returns the Vite `base` option for a given app.
 * In dev (vite serve), base is "/" so assets resolve correctly.
 * In build, base is prefixed with VITE_BASE_PATH for context-path deployments.
 */
export function viteBase(appName, command) {
  return command === "serve"
    ? "/"
    : (process.env.VITE_BASE_PATH ?? "") + `/apps/${appName}/`;
}
