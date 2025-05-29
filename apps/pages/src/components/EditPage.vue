<template>
  <div>
    <router-link :to="'/' + page">view page</router-link>
    <div class="d-flex">
      <div class="flex-grow-1">
        <h1>{{ title }}</h1>
      </div>
      <div class="mt-2 mb-4 d-flex justify-content-end gap-2">
        <ButtonAction @click="savePageSettings" class="ml-2"
          >Save changes</ButtonAction
        >
      </div>
    </div>
    <div class="container-fluid">
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <div class="row">
        <div class="col-7">
          <div class="position-relative shadow rounded">
            <div
              class="px-2 size-7 sticky-top d-flex justify-content-start bg-white"
            >
              <h2 class="flex-grow-1 h6 m-0 p-2">HTML</h2>
              <div class="d-flex">
                <IconAction
                  icon="file-code"
                  tooltip="Format code"
                  @click="formatEditor('html')"
                />
              </div>
            </div>
            <div ref="html" />
          </div>
          <div class="position-relative mt-4 shadow rounded">
            <div
              class="px-2 size-7 sticky-top d-flex justify-content-start bg-white"
            >
              <h2 class="flex-grow-1 h6 m-0 p-2">CSS</h2>
              <div class="d-flex">
                <IconAction
                  icon="file-code"
                  tooltip="Format code"
                  @click="formatEditor('css')"
                />
              </div>
            </div>
            <div ref="css" />
          </div>
          <div class="position-relative mt-4 shadow rounded">
            <div
              class="px-2 size-7 sticky-top d-flex justify-content-start bg-white"
            >
              <h2 class="flex-grow-1 h6 m-0 p-2">JS</h2>
              <div class="d-flex">
                <IconAction
                  icon="file-code"
                  tooltip="Format code"
                  @click="formatEditor('javascript')"
                />
              </div>
            </div>
            <div ref="javascript" />
          </div>
          <div class="position-relative mt-4 shadow rounded">
            <div
              class="px-2 size-7 sticky-top d-flex justify-content-start bg-white"
            >
              <h2 class="flex-grow-1 h6 m-0 p-2">Resources</h2>
            </div>
            <Spinner v-if="loading" />
            <form class="p-4" v-else>
              <legend class="h4">Add external dependencies</legend>
              <div class="">
                <label class="h6">Add URLs to CSS dependencies</label>
                <ArrayInput
                  id="css-urls"
                  columnType="HYPERLINK_ARRAY"
                  v-model="content.dependencies.css"
                />
              </div>
              <fieldset>
                <legend class="h6">Add URLs to JavaScript dependencies</legend>
                <template
                  v-if="content.dependencies.javascript.length"
                  v-for="(dependency, index) in content.dependencies.javascript"
                >
                  <div class="d-flex">
                    <ExternalDependency
                      class="flex-fill mr-4"
                      :url="dependency.url"
                      :defer="dependency.defer"
                      @update:modelValue="
                        (value) => updateJsDependency(value, index)
                      "
                    />
                    <IconAction
                      icon="trash"
                      tooltip="Remove dependency"
                      @click="removeJsDependency(dependency, index)"
                    />
                  </div>
                </template>
                <ButtonOutline @click="addJsDependency">
                  Add dependency
                </ButtonOutline>
              </fieldset>
            </form>
          </div>
        </div>
        <div class="position-relative col-5 p-0 bg-light shadow">
          <div class="sticky-top top-0">
            <div class="size-7 bg-white p-2">
              <h2 class="h6 m-0 ml-2">Preview</h2>
            </div>
            <div ref="pagePreview" class="px-4 py-2 h-100"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import ExternalDependency from "./ExternalDependency.vue";

import {
  InputText,
  IconAction,
  ButtonAction,
  ButtonOutline,
  MessageError,
  MessageSuccess,
  Spinner,
  ArrayInput,
  InputBoolean,
} from "molgenis-components";
import { request } from "graphql-request";
import * as monaco from "monaco-editor";

import { toRaw } from "vue";
import { getPageSetting } from "../utils/getPageSetting";
import { generateHtmlPreview } from "../utils/generateHtmlPreview";
import { newPageContentObject } from "../utils/newPageContentObject";

const editorLanguages = ["html", "css", "javascript"];

export default {
  components: {
    InputText,
    ButtonAction,
    ButtonOutline,
    MessageError,
    MessageSuccess,
    Spinner,
    IconAction,
    ArrayInput,
    InputBoolean,
    ExternalDependency,
  },
  data() {
    return {
      graphqlError: null,
      success: null,
      loading: true,
      html: {},
      css: {},
      javascript: {},
      content: newPageContentObject(),
    };
  },
  props: {
    page: String,
    session: Object,
  },
  computed: {
    pageSettingKey() {
      return "page." + this.page;
    },
    title() {
      if (
        this.session &&
        this.session.settings &&
        this.session.settings["page." + this.page]
      ) {
        return "'" + this.page + "' page editor";
      } else {
        return "Create new page '" + this.page + "'";
      }
    },
    contentJSON() {
      return JSON.stringify(this.content);
    },
  },
  methods: {
    async savePageSettings() {
      const response = await request(
        "graphql",
        `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){status message}}`,
        {
          settings: {
            key: this.pageSettingKey,
            value: this.contentJSON,
          },
        }
      );
      if (Object.hasOwn(response, "change")) {
        if (response.change.status === "SUCCESS") {
          this.success = response.change.message;
        }
      } else {
        this.graphqlError = response;
      }
    },
    initEditor(editor) {
      const editorRef = this.$refs[editor];
      this[editor] = monaco.editor.create(editorRef, {
        automaticLayout: true,
        value: this.content[editor],
        language: editor,
        theme: "vs-dark",
        automaticLayout: true,
        formatOnPaste: true,
        autoIndent: "brackets",
        autoClosingBrackets: true,
        dimension: {
          height: 310,
        },
        suggest: {
          insertMode: "insert",
        },
      });
    },

    formatEditor(editor) {
      if (this[editor] && this[editor].getAction) {
        this[editor].getAction("editor.action.formatDocument").run();
      }
    },

    createAllEditors() {
      editorLanguages.forEach((editor) => this.initEditor(editor));
    },

    formatAllEditors() {
      editorLanguages.forEach((editor) => this.formatEditor(editor));
    },

    setContentFromEditor(editor, key) {
      editor.getModel().onDidChangeContent(() => {
        this.content[key] = toRaw(editor).getValue();
      });
    },

    addJsDependency() {
      this.content.dependencies.javascript.push({ url: null, defer: false });
    },

    removeJsDependency(dependency, index) {
      if (dependency && dependency.url !== null) {
        this.content.dependencies.javascript =
          this.content.dependencies.javascript.filter((row) => {
            return row.url !== dependency.url;
          });
      } else {
        this.content.dependencies.javascript =
          this.content.dependencies.javascript.splice(index, 1);
      }
    },

    updateJsDependency(dependency, index) {
      this.content.dependencies.javascript[index] = dependency;
    },
  },
  destroyed() {
    editorLanguages.forEach((editor) => this[editor].dispose());
  },
  watch: {
    content: {
      deep: true,
      handler(newContent) {
        if (newContent) {
          generateHtmlPreview(this, this.content, "pagePreview");
        }
      },
    },
    html: {
      deep: true,
      handler(editor) {
        this.setContentFromEditor(editor, "html");
      },
    },
    css: {
      deep: true,
      handler(editor) {
        this.setContentFromEditor(editor, "css");
      },
    },
    javascript: {
      deep: true,
      handler(editor) {
        this.setContentFromEditor(editor, "javascript");
      },
    },
  },
  mounted() {
    Promise.resolve(getPageSetting(this.pageSettingKey))
      .then((data) => {
        if (
          data &&
          (Object.keys(data).includes("html") ||
            Object.keys(data).includes("css") ||
            Object.keys(data).includes("javascript") ||
            Object.keys(data).includes("options"))
        ) {
          this.content = data;
        } else if (data && typeof data === "string") {
          const contentObj = newPageContentObject();
          contentObj.html = data;
          this.content = contentObj;
        } else {
          this.content = newPageContentObject();
        }
        this.loading = false;
      })
      .then(() => {
        this.createAllEditors();
        this.formatAllEditors();
        generateHtmlPreview(this, this.content, "pagePreview");
      })
      .catch((err) => (this.graphqlError = err));
  },
};
</script>
