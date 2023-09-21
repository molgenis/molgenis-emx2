<template>
  <div @keyup.ctrl.f="format">
    <div>
      <div ref="editor" class="editor" @keyup="dirty = true"></div>
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

<script>
import * as monaco from "monaco-editor";
import { toRaw } from "vue";

export default {
  props: {
    config: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      editor: {},
      dirty: false,
    };
  },
  watch: {
    dirty(newValue) {
      this.$emit("dirty", newValue);
    },
  },
  methods: {
    format() {
      this.editor.getAction("editor.action.formatDocument").run();
    },
    save() {
      /** doesnt work with proxy, so that is where toRaw comes in */
      const changesToSave = toRaw(this.editor).getValue();
      this.$emit("save", changesToSave);
    },
    cancel() {
      this.$emit("cancel");
    },
    download() {
      const file = new Blob([toRaw(this.editor).getValue()], {
        type: "json",
      });
      const a = document.createElement("a");
      const url = URL.createObjectURL(file);
      a.href = url;
      a.download = `${window.location.host}-config.json`;
      document.body.appendChild(a);
      a.click();
      setTimeout(function () {
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      }, 0);
    },
    upload() {
      const fileInput = document.getElementById("file-selector");
      fileInput.click();
    },
    async processUpload(event) {
      const reader = new FileReader();
      reader.addEventListener("load", (event) => {
        this.uploadedAppConfig = atob(event.target.result.split(",")[1]);

        this.$emit("diff", {
          currentAppConfig: toRaw(this.editor).getValue(),
          uploadedAppConfig: this.uploadedAppConfig,
        });
      });
      reader.readAsDataURL(event.target.files[0]);
    },
  },
  mounted() {
    this.editor = monaco.editor.create(this.$refs.editor, {
      automaticLayout: true,
      value: this.config,
      language: "json",
    });

    const formatTimer = setTimeout(() => {
      this.format();
      clearTimeout(formatTimer);
    }, 500);
  },
  destroyed() {
    this.editor.dispose();
  },
};
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
