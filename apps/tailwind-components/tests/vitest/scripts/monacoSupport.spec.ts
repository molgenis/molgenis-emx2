import { describe, expect, test } from "vitest";
import {
  codeEditorMarkersOf,
  monacoSupportFailure,
} from "../../../scripts/check-monaco-support.ts";

const codeEditorSource = `<template>
  <div class="flex justify-between items-center gap-5 text-title p-2.5 px-7.5">
    <Button
      class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
      label="Format code"
    />
    <MonacoEditor :lang="lang" />
  </div>
</template>`;

const codeEditorMarkers: [string, string] = [
  "flex justify-between items-center gap-5 text-title p-2.5 px-7.5",
  "hover:bg-button-secondary-hover focus:bg-button-secondary-hover",
];

describe("monaco support detector:", () => {
  test("markers are the long literals of the component, short ones are too common to identify it", () => {
    expect(codeEditorMarkersOf(codeEditorSource)).toEqual(codeEditorMarkers);
  });

  test("the editor is undetectable when no single built file carries all its markers", () => {
    const failure = monacoSupportFailure({
      consumerDirectory: "/apps/example",
      builtJavascriptFiles: [
        `monaco-diff-editor ${codeEditorMarkers[0]}`,
        `${codeEditorMarkers[1]} unrelated chunk`,
      ],
      codeEditorMarkers,
      missingMonacoWorkers: [],
    });

    expect(failure).toEqual(
      "no CodeEditor reference found — this check cannot detect the editor"
    );
  });

  test("the editor is detected when one built file carries all its markers", () => {
    const failure = monacoSupportFailure({
      consumerDirectory: "/apps/example",
      builtJavascriptFiles: [
        `${codeEditorMarkers.join(" ")} compiled component`,
        "monaco-diff-editor",
      ],
      codeEditorMarkers,
      missingMonacoWorkers: [],
    });

    expect(failure).toEqual(null);
  });

  test("an unbuilt app is reported as unbuilt, not as an app without the editor", () => {
    const failure = monacoSupportFailure({
      consumerDirectory: "/apps/example",
      builtJavascriptFiles: [],
      codeEditorMarkers,
      missingMonacoWorkers: [],
    });

    expect(failure).toEqual(
      "no javascript found in /apps/example/.output — build first"
    );
  });

  test("a component that no longer yields two markers is reported instead of silently passing", () => {
    const failure = monacoSupportFailure({
      consumerDirectory: "/apps/example",
      builtJavascriptFiles: [`${codeEditorMarkers[0]} monaco-diff-editor`],
      codeEditorMarkers: [codeEditorMarkers[0]],
      missingMonacoWorkers: [],
    });

    expect(failure).toEqual(
      "CodeEditor.vue yields fewer than two markers — this check cannot detect the editor"
    );
  });

  test("the editor without the monaco runtime names the module to register", () => {
    const failure = monacoSupportFailure({
      consumerDirectory: "/apps/example",
      builtJavascriptFiles: [`${codeEditorMarkers.join(" ")} compiled`],
      codeEditorMarkers,
      missingMonacoWorkers: [],
    });

    expect(failure).toEqual(
      "CodeEditor is bundled but the monaco runtime is not — register nuxt-monaco-editor in this app"
    );
  });

  test("unserved web workers are named one by one", () => {
    const failure = monacoSupportFailure({
      consumerDirectory: "/apps/example",
      builtJavascriptFiles: [
        `${codeEditorMarkers.join(" ")} monaco-diff-editor`,
      ],
      codeEditorMarkers,
      missingMonacoWorkers: [
        "editor/editor.worker.js",
        "language/css/css.worker.js",
      ],
    });

    expect(failure).toEqual(
      "monaco web workers are not served: editor/editor.worker.js, language/css/css.worker.js — serve monaco-editor/esm from nitro.publicAssets at _nuxt/nuxt-monaco-editor"
    );
  });
});
