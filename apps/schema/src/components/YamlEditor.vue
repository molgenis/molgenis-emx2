<template>
  <div class="row">
    <div class="col-8">
      <div ref="editor" class="editor"></div>
      <a href="#" @click.prevent="loadExample"
        >load example to show all features</a
      ><br />
      N.B. we have designed using yamld documents so we can easily split this in
      seperate files
    </div>
    <div class="col-4">
      <Spinner v-if="submitting" />
      <ButtonAction v-else @click="handleSubmit">Submit</ButtonAction>
      <MessageError v-if="error || warnings?.length">{{
        error ? error : "there are warnings:"
      }}</MessageError>
      <MessageSuccess v-else-if="success">{{ success }}</MessageSuccess>
      <div v-if="warnings?.length">
        <table class="table table-bordered">
          <caption>
            Error list
          </caption>
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
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import * as monaco from "monaco-editor";
import { configureMonacoYaml } from "monaco-yaml";
//bit of a hack for vue+vite
import YamlWorker from "./yaml.worker.js?worker";
import schema from "../../../../molgenis-emx2.schema.json";
import example from "../../../../molgenis-emx2.schema.example.yaml?raw";
import YAML from "yaml";
import { editor, MarkerSeverity } from "monaco-editor";
import IMarker = editor.IMarker;
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
  Spinner,
} from "molgenis-components";

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
const error = ref();
const success = ref();
const submitting = ref(false);

try {
  const response = await fetch(`../api/yaml`);
  const source = await response.text();
  code.value = source;
} catch (e) {
  error.value = e;
  code.value = example;
}

json.value = YAML.parseAllDocuments(code.value);

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
    result.push({
      line: marker.startLineNumber,
      message: marker.message,
    });
  }
  warnings.value = result;
});

const emx2YamlModel = monaco.editor.createModel(
  code.value,
  "yaml",
  monaco.Uri.parse("file:///emx2.yaml")
);

onMounted(() => {
  const instance = monaco.editor.create(editor.value, {
    model: emx2YamlModel,
    quickSuggestions: { other: true, strings: true, comments: true },
  });
  instance.onDidChangeModelContent((event) => {
    console.log("updated");
    code.value = instance.getValue();
    error.value = null;
    success.value = null;
    json.value = YAML.parseAllDocuments(code.value);
    //we expect to use this json to render a preview of the documentation later
  });
});

function loadExample() {
  monaco.editor.getModels()[0].setValue(example);
}

async function handleSubmit() {
  try {
    submitting.value = true;
    success.value = null;
    error.value = null;
    const response = await fetch("api/yaml", {
      method: "POST",
      body: code.value,
    });
    if (response.ok) {
      success.value = "Submission success";
    } else {
      error.value =
        "Submit failed. " + (await response.json())["errors"][0]["message"];
    }
    submitting.value = false;
  } catch (e) {
    error.value = e.message;
    submitting.value = false;
  }
}
</script>

<style scoped>
.editor {
  margin: 0 auto;
  border: 1px solid black;
  height: 65vh;
  width: 100%;
}
</style>
