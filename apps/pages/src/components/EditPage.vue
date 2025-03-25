<template>
  <div>
    {{ content }}
    <router-link :to="'/' + page">view page</router-link>
    <div class="d-flex">
      <div class="flex-grow-1">
        <h1>{{ title }}</h1>
      </div>
      <div class="mt-2 mb-4 d-flex justify-content-end gap-2">
        <ButtonAction @click="savePage" class="ml-2">Save changes</ButtonAction>
      </div>
    </div>
    <Spinner v-if="loading" />
    <div class="container-fluid" v-else>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <div class="row">
        <div class="col-7">
          <div class="position-relative shadow">
            <div
              class="px-2 size-7 sticky-top d-flex justify-content-start border bg-white"
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
            <div
              ref="html"
              @keyup="(event) => onCodeInput(event.target.value, 'html')"
            />
          </div>
          <div class="position-relative mt-4 shadow">
            <div
              class="px-2 size-7 sticky-top d-flex justify-content-start border bg-white"
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
            <div
              ref="css"
              @keyup="(event) => onCodeInput(event.target.value, 'css')"
            />
          </div>
          <div class="position-relative mt-4 shadow">
            <div
              class="px-2 size-7 sticky-top d-flex justify-content-start border bg-white"
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
            <div
              ref="javascript"
              @keyup="(event) => onCodeInput(event.target.value, 'javascript')"
            />
          </div>
        </div>
        <div class="position-relative col-5 bg-light shadow">
          <div ref="pagePreview" class="sticky-top pt-2 h-100"></div>
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
} from "molgenis-components";
import { request } from "graphql-request";
import * as monaco from "monaco-editor";

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
  },
  data() {
    return {
      initialLoadComplete: false,
      graphqlError: null,
      success: null,
      loading: false,
      html: {},
      css: {},
      javascript: {},
      content: {
        html: "",
        css: "",
        javascript: "",
      },
    };
  },
  props: {
    page: String,
    session: Object,
  },
  computed: {
    title() {
      if (
        this.session &&
        this.session.settings &&
        this.session.settings["page." + this.page]
      ) {
        return "Edit page '" + this.page + "'";
      } else {
        return "Create new page '" + this.page + "'";
      }
    },
    contentJSON() {
      return JSON.stringify(this.content);
    },
  },
  methods: {
    generatePreview() {
      if (this.content) {
        this.$refs.pagePreview.replaceChildren();

        const parser = new DOMParser();

        if (this.content.html) {
          const doc = parser.parseFromString(this.content.html, "text/html");
          Array.from(doc.body.children).forEach((elem) => {
            this.$refs.pagePreview.appendChild(elem);
          });
        }

        if (this.content.css) {
          const styleElem = document.createElement("style");
          styleElem.textContent = this.content.css;
          this.$refs.pagePreview.appendChild(styleElem);
        }

        if (this.content.javascript) {
          const scriptElem = document.createElement("script");
          scriptElem.setAttribute("type", "text/javascript");
          scriptElem.textContent = this.content.javascript;
          this.$refs.pagePreview.appendChild(scriptElem);
        }
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
          height: 350,
        },
        suggest: {
          insertMode: "insert",
        },
      });

      this.formatEditor(editor);
    },

    formatEditor(editor) {
      if (this[editor] && this[editor].getAction) {
        this[editor].getAction("editor.action.formatDocument").run();
      }
    },

    createAllEditors() {
      editorLanguages.forEach((editor) => this.initEditor(editor));
    },

    onCodeInput(content, editor) {
      this.content[editor] = content;
      this.generatePreview();
    },

    savePage() {
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        "graphql",
        `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){message}}`,
        {
          settings: {
            key: "page." + this.page,
            value: this.contentJSON,
          },
        }
      )
        .then((data) => {
          this.success = data.change.message;
          this.session.settings["page." + this.page] = this.contentJSON;
        })
        .catch((graphqlError) => {
          this.graphqlError = graphqlError.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    reload() {
      if (
        this.session &&
        this.session.settings &&
        this.session.settings["page." + this.page]
      ) {
        const doc = this.session.settings["page." + this.page];
        this.content = doc;
      }
    },
  },
  destroyed() {
    editorLanguages.forEach((editor) => this[editor].dispose());
  },
  watch: {
    session: {
      deep: true,
      handler() {
        this.reload();
      },
    },
    content: {
      handler(newContent) {
        if (newContent) {
          this.createAllEditors();
          this.generatePreview();
        }
      },
    },
  },
  mounted() {
    this.reload();
  },
};
</script>
