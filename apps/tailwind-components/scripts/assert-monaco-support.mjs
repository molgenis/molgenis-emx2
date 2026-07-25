import { existsSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { filesUnder } from "./built-files.mjs";

const consumerDirectory = resolve(process.argv[2] ?? ".");
const builtDirectories = [
  join(consumerDirectory, ".output/public"),
  join(consumerDirectory, ".output/server"),
].filter(existsSync);
const codeEditorMarker = "editor.action.formatDocument";
const monacoRuntimeMarker = "monaco-diff-editor";
const monacoWorkerDirectory = join(
  consumerDirectory,
  ".output/public/_nuxt/nuxt-monaco-editor/vs"
);
const monacoWorkerFiles = [
  "editor/editor.worker.js",
  "language/css/css.worker.js",
  "language/html/html.worker.js",
  "language/json/json.worker.js",
  "language/typescript/ts.worker.js",
];

const builtJavascript = readBuiltJavascript();
const missingMonacoWorkers = monacoWorkerFiles.filter(
  (file) => !existsSync(join(monacoWorkerDirectory, file))
);

if (builtJavascript.length === 0) {
  fail(`no javascript found in ${consumerDirectory}/.output — build first`);
}
if (!builtJavascript.includes(codeEditorMarker)) {
  fail("no CodeEditor reference found — this check cannot detect the editor");
}
if (!builtJavascript.includes(monacoRuntimeMarker)) {
  fail(
    "CodeEditor is bundled but the monaco runtime is not — register nuxt-monaco-editor in this app"
  );
}
if (missingMonacoWorkers.length > 0) {
  fail(
    `monaco web workers are not served: ${missingMonacoWorkers.join(
      ", "
    )} — serve monaco-editor/esm from nitro.publicAssets at _nuxt/nuxt-monaco-editor`
  );
}
console.log(
  "assert-monaco-support: CodeEditor bundled with its monaco runtime and web workers"
);

function readBuiltJavascript() {
  return builtDirectories
    .flatMap((directory) =>
      filesUnder(directory)
        .filter((file) => /\.(js|mjs)$/.test(file))
        .filter((file) => !file.includes("nuxt-monaco-editor/"))
        .map((file) => readFileSync(join(directory, file), "utf8"))
    )
    .join("\n");
}

function fail(message) {
  console.error(`assert-monaco-support: ${message}`);
  process.exit(1);
}
