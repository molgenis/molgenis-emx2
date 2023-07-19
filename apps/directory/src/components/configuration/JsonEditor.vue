<template>
  <div @keyup.ctrl.f="format">
    <div>
      <div ref="editor" class="editor" @keyup="dirty = true"></div>
    </div>

    <div class="row px-5 py-5">
      <button class="btn btn-primary mr-3 save-button" @click="save">
        Save changes
      </button>
      <button class="btn btn-dark mr-3" @click="cancel">Cancel</button>
    </div>
  </div>
</template>

<script>
import * as monaco from "monaco-editor";

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
  methods: {
    format() {
      this.editor.getAction("editor.action.formatDocument").run();
    },
    save() {
      const changesToSave = this.editor.getValue();
      this.$emit("save", changesToSave);
    },
    cancel() {
      this.$emit("cancel");
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
.editor {
  margin: 0 auto;
  border: 1px solid black;
  height: 65vh;
  width: 100%;
}

.save-button {
  width: 14rem;
}
</style>

<style scoped>
.editor {
  margin: 0 auto;
  border: 1px solid black;
  height: 65vh;
  width: 100%;
}
</style>
