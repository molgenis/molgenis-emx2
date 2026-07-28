// In practice these markers are CodeEditor.vue's long tailwind class strings:
// reformatting them changes the markers and fails the tailwind-components and ui builds.
const literalLongEnoughToIdentifyTheComponent = /"([^"\n]{40,})"/g;
const monacoRuntimeMarker = "monaco-diff-editor";

export const monacoWorkerFiles = [
  "editor/editor.worker.js",
  "language/css/css.worker.js",
  "language/html/html.worker.js",
  "language/json/json.worker.js",
  "language/typescript/ts.worker.js",
];

export function codeEditorMarkersOf(componentSource) {
  return [
    ...componentSource.matchAll(literalLongEnoughToIdentifyTheComponent),
  ].map(([, literal]) => literal);
}

export function monacoSupportFailure({
  consumerDirectory,
  builtJavascriptFiles,
  codeEditorMarkers,
  missingMonacoWorkers,
}) {
  if (builtJavascriptFiles.length === 0) {
    return `no javascript found in ${consumerDirectory}/.output — build first`;
  }
  if (codeEditorMarkers.length < 2) {
    return "CodeEditor.vue yields fewer than two markers — this check cannot detect the editor";
  }
  if (
    !builtJavascriptFiles.some((javascript) =>
      codeEditorMarkers.every((marker) => javascript.includes(marker))
    )
  ) {
    return "no CodeEditor reference found — this check cannot detect the editor";
  }
  if (
    !builtJavascriptFiles.some((javascript) =>
      javascript.includes(monacoRuntimeMarker)
    )
  ) {
    return "CodeEditor is bundled but the monaco runtime is not — register nuxt-monaco-editor in this app";
  }
  if (missingMonacoWorkers.length > 0) {
    return `monaco web workers are not served: ${missingMonacoWorkers.join(
      ", "
    )} — serve monaco-editor/esm from nitro.publicAssets at _nuxt/nuxt-monaco-editor`;
  }
  return null;
}
