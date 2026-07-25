import { readdirSync } from "node:fs";
import { join, relative } from "node:path";

export function filesUnder(directory, base = directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = join(directory, entry.name);
    return entry.isDirectory()
      ? filesUnder(entryPath, base)
      : [relative(base, entryPath).split("\\").join("/")];
  });
}
