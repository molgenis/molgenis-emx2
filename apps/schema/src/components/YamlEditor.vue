<template>
  <div class="w-50">
    <div ref="editor" class="editor"></div>
    <div v-if="warnings?.length">
      There are errors/warnings:
      <table class="table table-bordered">
        <thead>
          <th>line</th>
          <th>message</th>
        </thead>
        <tr v-for="warning in warnings">
          <td>{{ warning.line }}</td>
          <td>{{ warning.message }}</td>
        </tr>
      </table>
    </div>
    <pre>
JSON representation:
{{ json }}
    </pre>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import * as monaco from "monaco-editor";
import { configureMonacoYaml } from "monaco-yaml";
//bit of a hack for vue+vite
import YamlWorker from "./yaml.worker.js?worker";
import { JSONSchema7 } from "json-schema";
import schema from "../../../../molgenis-emx2.schema.json";
import example from "../../../../molgenis-emx2.schema.example.yaml?raw";
import YAML from "yaml";
import { editor, MarkerSeverity } from "monaco-editor";
import IMarker = editor.IMarker;

const s: JSONSchema7 = schema;
console.log(schema);

window.MonacoEnvironment = {
  getWorker(moduleId, label) {
    switch (label) {
      // Handle other cases
      case "yaml":
        return new YamlWorker();
      default:
        throw new Error(`Unknown label ${label}`);
    }
  },
};

const editor = ref();
const code = ref();
const json = ref();
const warnings = ref();

code.value = example;
json.value = YAML.parseAllDocuments(code.value);

const emx2YamlModel = monaco.editor.createModel(
  code.value,
  "yaml",
  monaco.Uri.parse("file:///emx2.yaml")
);

configureMonacoYaml(monaco, {
  enableSchemaRequest: true,
  schemas: [
    {
      // If YAML file is opened matching this glob
      fileMatch: ["emx2.yaml"],
      // The following schema will be applied
      schema: schema,
      // And the URI will be linked to as the source.
      uri: "http://example.com/schema-name.json",
    },
  ],
});

monaco.editor.onDidChangeMarkers(([resource]) => {
  const markers = monaco.editor.getModelMarkers({ resource });
  const result = [];
  for (const marker: IMarker of markers) {
    if (marker.severity === MarkerSeverity.Hint) {
      continue;
    }
    console.log(marker);
    result.push({
      line: marker.startLineNumber,
      message: marker.message,
    });
  }
  warnings.value = result;
});

onMounted(() => {
  const instance = monaco.editor.create(editor.value, {
    model: emx2YamlModel,
    quickSuggestions: { other: true, strings: true, comments: true },
  });
  instance.onDidChangeModelContent((event) => {
    code.value = instance.getValue();
    json.value = YAML.parseAllDocuments(code.value);
  });
});
</script>

<style scoped>
.editor {
  margin: 0 auto;
  border: 1px solid black;
  height: 65vh;
  width: 100%;
}
</style>
