<template>
  <div @keyup.ctrl.f="format">
    <div ref="filter-editor" class="filter-editor" @keyup="dirty = true"></div>

    <button class="btn btn-info mt-3" :disabled="!dirty" @click="apply">
      Apply changes to {{ title }}
    </button>
    <button class="btn btn-danger ml-3 mt-3" @click="deleteFilter">
      Delete {{ title }}
    </button>
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
    filterIndex: {
      type: Number,
      required: true,
    },
    title: {
      type: String,
      required: true,
    },
  },
  methods: {
    format() {
      if (this.filterEditor && this.filterEditor.getAction) {
        this.filterEditor.getAction("editor.action.formatDocument").run();
      }
    },
    apply() {
      const newConfig = this.configObject;
      newConfig.filterFacets[this.filterIndex] = JSON.parse(
        toRaw(this.filterEditor).getValue()
      );
      this.$emit("filterUpdate", JSON.stringify(newConfig));
    },
    deleteFilter() {
      this.$emit("delete");
    },
  },
  computed: {
    configObject() {
      return JSON.parse(this.config);
    },
    filter() {
      return JSON.stringify(this.configObject.filterFacets[this.filterIndex]);
    },
  },
  data() {
    return {
      dirty: false,
      filterEditor: {},
    };
  },
  destroyed() {
    this.dirty = false;
    this.filterEditor.dispose();
  },
  watch: {
    config() {
      this.dirty = false;
      if (this.filterEditor.dispose) {
        this.filterEditor.getModel().setValue(this.filter);
        this.format();
      }
    },
  },
  mounted() {
    this.filterEditor = monaco.editor.create(this.$refs["filter-editor"], {
      automaticLayout: true,
      value: this.filter,
      language: "json",
    });

    const formatTimer = setTimeout(() => {
      this.format();
      clearTimeout(formatTimer);
    }, 400);
  },
};
</script>

<style scoped>
.filter-editor {
  height: 100%;
  width: 100%;
}
</style>
