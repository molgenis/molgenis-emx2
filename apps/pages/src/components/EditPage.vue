<template>
  <div>
    <router-link :to="'/' + page">view page</router-link>
    <div class="d-flex">
      <div class="flex-grow-1">
        <h1>{{ title }}</h1>
      </div>
      <div class="mt-2 mb-4 d-flex justify-content-end gap-2">
        <ButtonAction @click="savePageSettings" class="ml-2">
          Save changes
        </ButtonAction>
      </div>
    </div>
    <div class="container-fluid">
      <MessageError v-if="graphqlError" class="d-flex align-items-center">
        <button class="button-plain" @click="graphqlError = null">
          <span class="visually-hidden">close error message</span>
          <span class="fas fa-fw fa-times"></span>
        </button>
        <span>{{ graphqlError }}</span>
      </MessageError>
      <MessageSuccess v-if="success" class="d-flex align-items-center">
        <button class="button-plain" @click="success = null">
          <span class="visually-hidden">close message</span>
          <span class="fas fa-fw fa-times"></span>
        </button>
        <span>{{ success }}</span>
      </MessageSuccess>
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
              <fieldset class="mb-4">
                <legend class="h6">Add CSS dependencies</legend>
                <template
                  v-if="content.dependencies.css.length"
                  v-for="(dependency, index) in content.dependencies.css"
                >
                  <div class="d-flex flex-row flex-wrap">
                    <div class="flex-fill mr-4">
                      <label :for="`css-url-${index}`">
                        Enter the URL to css dependency
                      </label>
                      <FormInput
                        :id="`css-url-${index}`"
                        columnType="HYPERLINK"
                        :modelValue="dependency.url"
                        @update:modelValue="
                          (value) =>
                            updateCssDependency(dependency, index, 'url', value)
                        "
                      />
                    </div>
                    <div
                      class="d-flex justify-content-center align-items-center"
                    >
                      <IconAction
                        icon="trash"
                        tooltip="Remove dependency"
                        @click="removeDependency('css', dependency, index)"
                      />
                    </div>
                  </div>
                </template>
                <ButtonOutline @click="addCssDependency">
                  Add CSS
                </ButtonOutline>
              </fieldset>
              <fieldset>
                <legend class="h6">Add JavaScript dependencies</legend>
                <template
                  v-if="content.dependencies.javascript.length"
                  v-for="(dependency, index) in content.dependencies.javascript"
                >
                  <div class="d-flex flex-row flex-wrap">
                    <div class="flex-fill mr-4">
                      <label :for="`javascript-urls-${index}`">
                        Enter the URL to javascript dependency
                      </label>
                      <FormInput
                        :id="`javascript-urls-${index}`"
                        columnType="HYPERLINK"
                        :modelValue="dependency.url"
                        @update:modelValue="
                          (value) =>
                            updateJsDependency(dependency, index, 'url', value)
                        "
                      />
                    </div>
                    <div>
                      <label :for="`javascript-defer-${index}`">
                        Defer dependency?
                      </label>
                      <InputBoolean
                        :id="`javascript-defer-${index}`"
                        :modelValue="dependency.defer"
                        :isClearable="false"
                        @update:modelValue="
                          (value) =>
                            updateJsDependency(
                              dependency,
                              index,
                              'defer',
                              value
                            )
                        "
                      />
                    </div>
                    <div
                      class="d-flex justify-content-center align-items-center"
                    >
                      <IconAction
                        icon="trash"
                        tooltip="Remove dependency"
                        @click="
                          removeDependency('javascript', dependency, index)
                        "
                      />
                    </div>
                  </div>
                </template>
                <ButtonOutline @click="addJsDependency">
                  Add JavaScript
                </ButtonOutline>
              </fieldset>
              <p class="mt-2">
                Note: Removing external dependencies requires a page refresh
              </p>
            </form>
          </div>
        </div>
        <div class="col-5 p-0 bg-light">
          <div class="position-relative shadow rounded">
            <div class="px-2 sticky-top bg-white">
              <h2 class="h6 p-2">Preview</h2>
            </div>
            <div ref="pagePreview" class="px-4 py-2 h-100"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
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
  FormInput,
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
    FormInput,
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
      request(
        "graphql",
        `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){status message}}`,
        {
          settings: {
            key: this.pageSettingKey,
            value: this.contentJSON,
          },
        }
      )
        .then((response) => {
          if (response?.error) {
            throw new Error(response.error[0].message);
          }
          this.success = response.change.message;
        })
        .catch((err) => {
          this.graphqlError = err.response.errors[0].message;
        });
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
        wordWrap: "on",
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

    addCssDependency() {
      this.content.dependencies.css.push({ url: null });
    },

    removeDependency(dependencyType, dependencyToRemove, indexToRemove) {
      if (dependencyToRemove.url) {
        this.content.dependencies[dependencyType] = this.content.dependencies[
          dependencyType
        ].filter((dependency) => {
          return dependency.url !== dependencyToRemove.url;
        });
      } else {
        this.content.dependencies[dependencyType] = this.content.dependencies[
          dependencyType
        ].filter((dependency, index) => {
          if (index !== indexToRemove) {
            return dependency;
          }
        });
      }
    },

    updateCssDependency(dependency, index, key, value) {
      const newDependency = dependency;
      newDependency[key] = value;
      this.content.dependencies.css[index] = newDependency;
    },

    addJsDependency() {
      this.content.dependencies.javascript.push({ url: null, defer: false });
    },

    updateJsDependency(dependency, index, key, value) {
      const newDependency = dependency;
      newDependency[key] = value;
      this.content.dependencies.javascript[index] = newDependency;
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
            Object.keys(data).includes("javascript"))
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

<style lang="css" scoped>
.button-plain {
  background: none;
  border: none;
  color: currentColor;
  cursor: pointer;
}

.visually-hidden {
  position: absolute;
  clip: rect(1px 1px 1px 1px); /* IE6, IE7 */
  clip: rect(1px, 1px, 1px, 1px);
  overflow: hidden;
  height: 1px;
  width: 1px;
  margin: -1px;
  white-space: nowrap;
}
</style>
