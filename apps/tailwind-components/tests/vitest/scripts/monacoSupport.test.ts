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
});
