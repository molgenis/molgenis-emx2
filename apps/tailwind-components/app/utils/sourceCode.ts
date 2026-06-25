export function globKeyToRouteKey(globKey: string): string {
  return globKey.replace(/^\.\.\/pages/, "");
}
