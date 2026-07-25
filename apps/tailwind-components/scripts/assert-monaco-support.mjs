import { existsSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { filesUnder } from "./built-files.mjs";
import {
  codeEditorMarkersOf,
  monacoSupportFailure,
  monacoWorkerFiles,
} from "./monaco-support.mjs";

const consumerDirectory = resolve(process.argv[2] ?? ".");
const builtDirectories = [
  join(consumerDirectory, ".output/public"),
  join(consumerDirectory, ".output/server"),
].filter(existsSync);
const codeEditorFile = fileURLToPath(
  new URL("../app/components/editor/CodeEditor.vue", import.meta.url)
);
const monacoWorkerDirectory = join(
  consumerDirectory,
  ".output/public/_nuxt/nuxt-monaco-editor/vs"
);

const failure = monacoSupportFailure({
  consumerDirectory,
  builtJavascriptFiles: readBuiltJavascriptFiles(),
  codeEditorMarkers: codeEditorMarkersOf(readFileSync(codeEditorFile, "utf8")),
  missingMonacoWorkers: monacoWorkerFiles.filter(
    (file) => !existsSync(join(monacoWorkerDirectory, file))
  ),
});

if (failure) {
  console.error(`assert-monaco-support: ${failure}`);
  process.exit(1);
}
console.log(
  "assert-monaco-support: CodeEditor bundled with its monaco runtime and web workers"
);

function readBuiltJavascriptFiles() {
  return builtDirectories.flatMap((directory) =>
    filesUnder(directory)
      .filter((file) => /\.(js|mjs)$/.test(file))
      .filter((file) => !file.includes("nuxt-monaco-editor/"))
      .map((file) => readFileSync(join(directory, file), "utf8"))
  );
}
