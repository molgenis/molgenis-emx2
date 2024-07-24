<template>
  <div @keyup.ctrl.f="format">
    <div>
      <div ref="editorDiv" class="editor" @keyup="setDirty()"></div>
    </div>

    <input
      type="file"
      id="file-selector"
      accept=".json"
      @change="processUpload"
    />

    <div class="row px-5 py-5">
      <div>
        <button class="btn btn-primary mr-3 save-button" @click="save">
          Save changes
        </button>
        <button class="btn btn-dark mr-3" @click="cancel">Cancel</button>

        <button class="btn btn-outline-dark mr-3" @click="download">
          Download config
        </button>
        <button class="btn btn-outline-dark" @click="upload">
          Upload config
        </button>
      </div>
      <small class="ml-auto">To format your file press ctrl + f</small>
    </div>
  </div>
</template>

<script setup lang="ts">
import * as monaco from "monaco-editor";
import { onMounted, onUnmounted, ref, toRaw } from "vue";

const { config } = defineProps<{ config: string }>();

const editor = ref<any>({});
const editorDiv = ref<HTMLElement>();
const dirty = ref(false);
const uploadedAppConfig = ref("");

const emit = defineEmits(["dirty", "save", "cancel", "diff"]);

onMounted(() => {
  editor.value = monaco.editor.create(editorDiv.value as HTMLElement, {
    automaticLayout: true,
    value: config,
    language: "json",
  });

  const formatTimer = setTimeout(() => {
    format();
    clearTimeout(formatTimer);
  }, 500);
});

onUnmounted(() => {
  editor.value.dispose();
});

function setDirty() {
  dirty.value = true;
  emit("dirty");
}

function format() {
  editor.value.getAction("editor.action.formatDocument").run();
}

function save() {
  /** doesn't work with proxy, so that is where toRaw comes in */
  const changesToSave = toRaw(editor.value).getValue();
  emit("save", changesToSave);
}

function cancel() {
  emit("cancel");
}

function download() {
  const file = new Blob([toRaw(editor.value).getValue()], {
    type: "json",
  });
  const aElement = document.createElement("a");
  const url = URL.createObjectURL(file);
  aElement.href = url;
  aElement.download = `${window.location.host}-config.json`;
  document.body.appendChild(aElement);
  aElement.click();
  setTimeout(() => {
    document.body.removeChild(aElement);
    window.URL.revokeObjectURL(url);
  }, 0);
}

function upload() {
  const fileInput = document.getElementById("file-selector");
  fileInput?.click();
}

async function processUpload(event: Record<string, any>) {
  const reader = new FileReader();
  reader.addEventListener("load", (event: ProgressEvent<FileReader>) => {
    uploadedAppConfig.value = atob(event.target?.result?.split(",")[1]);

    emit("diff", {
      currentAppConfig: toRaw(editor.value).getValue(),
      uploadedAppConfig: uploadedAppConfig.value,
    });
  });
  reader.readAsDataURL(event.target.files[0]);
}
</script>

<style scoped>
.save-button {
  width: 14rem;
}

#file-selector {
  display: none;
}
.editor {
  margin: 0 auto;
  border: 1px solid black;
  height: 65vh;
  width: 100%;
}
</style>
