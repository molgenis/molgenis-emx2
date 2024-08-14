<template>
  <div @keyup.ctrl.alt.f="format">
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
      <small class="ml-auto">
        To format your file press <kbd>ctrl</kbd> + <kbd>alt</kbd> +
        <kbd>f</kbd>
      </small>
    </div>
  </div>
</template>

<script setup lang="ts">
import { editor } from "monaco-editor";
import { onMounted, ref, toRaw } from "vue";

const { config } = defineProps<{ config: string }>();

const localEditor = ref<any>({});
const editorDiv = ref<HTMLElement>();
const dirty = ref(false);
const uploadedAppConfig = ref("");

const emit = defineEmits(["dirty", "save", "cancel", "diff"]);

onMounted(() => {
  localEditor.value = editor.create(editorDiv.value as HTMLElement, {
    automaticLayout: true,
    value: config,
    language: "json",
  });

  const formatTimer = setTimeout(() => {
    format();
    clearTimeout(formatTimer);
  }, 500);
});

function setDirty() {
  dirty.value = true;
  emit("dirty");
}

function format() {
  localEditor.value.getAction("editor.action.formatDocument").run();
}

function save() {
  /** doesn't work with proxy, so that is where toRaw comes in */
  const changesToSave = toRaw(localEditor.value).getValue();
  emit("save", changesToSave);
}

function cancel() {
  emit("cancel");
}

function download() {
  const file = new Blob([toRaw(localEditor.value).getValue()], {
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
    if (typeof event.target?.result === "string") {
      uploadedAppConfig.value = atob(event.target?.result?.split(",")[1]);
    } else {
      uploadedAppConfig.value = "";
    }
    emit("diff", {
      currentAppConfig: toRaw(localEditor.value).getValue(),
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
