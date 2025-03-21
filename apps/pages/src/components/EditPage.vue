<template>
  <div>
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
            <div class="bg-dark pl-2 size-7 sticky-top">
              <h2 class="h6 m-0 p-2 text-white">HTML</h2>
            </div>
            <div
              ref="html"
              @keyup="(event) => onCodeInput(event.target.value, 'html')"
            />
          </div>
          <div class="position-relative mt-4 shadow">
            <div class="bg-dark pl-2 size-7 sticky-top">
              <h2 class="h6 m-0 p-2 text-white">CSS</h2>
            </div>
            <div
              ref="css"
              @keyup="(event) => onCodeInput(event.target.value, 'css')"
            />
          </div>
          <div class="position-relative mt-4 shadow">
            <div class="bg-dark pl-2 size-7 sticky-top">
              <h2 class="h6 m-0 p-2 text-white">JS</h2>
            </div>
            <div
              ref="javascript"
              @keyup="(event) => onCodeInput(event.target.value, 'javascript')"
            />
          </div>
        </div>
        <div class="position-relative col-5 bg-light shadow">
          <div v-html="webContent.html" class="sticky-top pt-2"></div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import {
  InputText,
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
      )
        return "Edit page '" + this.page + "'";
      else return "Create new page '" + this.page + "'";
    },
    webContent() {
      return {
        html: this.setHtmlString(),
        content: this.content,
      };
    },
    webContentJson() {
      return JSON.stringify(this.webContent);
    },
  },
  methods: {
    initEditor(editor) {
      const editorRef = this.$refs[editor];
      this[editor] = monaco.editor.create(editorRef, {
        automaticLayout: true,
        value: this.content[editor],
        language: editor,
        theme: "vs-dark",
        autoClosingBrackets: true,
        dimension: {
          height: 350,
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
    },

    setHtmlString() {
      const doc = [];
      if (this.content.css) {
        const cssMarkup = [
          "<style type='text/css'>",
          this.content.css,
          "</style>",
        ].join("");
        doc.push(cssMarkup);
      }

      doc.push(this.content.html);

      if (this.content.javascript) {
        const jsMarkup = [
          "<script type='text/javascript'>",
          this.content.javascript,
          "</style>",
        ].join("");
        doc.push(jsMarkup);
      }
      return doc.join("").trim();
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
            value: this.webContentJson,
          },
        }
      )
        .then((data) => {
          this.success = data.change.message;
          this.session.settings["page." + this.page] = this.webContentJson;
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
        this.content = doc.content;
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
  },
  created() {
    this.reload();
  },
  mounted() {
    setTimeout(() => {
      this.createAllEditors();
    }, 25);
  },
};
</script>
