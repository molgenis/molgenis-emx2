import { existsSync, readFileSync, readdirSync } from "node:fs";
import { join, relative } from "node:path";

export function relativeFilePathsUnder(
  directory: string,
  base: string = directory
): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = join(directory, entry.name);
    return entry.isDirectory()
      ? relativeFilePathsUnder(entryPath, base)
      : [relative(base, entryPath).split("\\").join("/")];
  });
}

export function builtJavascriptOf(consumerDirectory: string): string[] {
  return [
    join(consumerDirectory, ".output/public"),
    join(consumerDirectory, ".output/server"),
  ]
    .filter(existsSync)
    .flatMap((directory) =>
      relativeFilePathsUnder(directory)
        .filter((file) => /\.(js|mjs)$/.test(file))
        .filter((file) => !file.includes("nuxt-monaco-editor/"))
        .map((file) => readFileSync(join(directory, file), "utf8"))
    );
}
