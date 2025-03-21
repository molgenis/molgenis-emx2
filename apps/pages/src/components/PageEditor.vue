<template>
  <div>
    <router-link :to="'/' + page">view page</router-link>
    <div class="d-flex">
      <div class="flex-grow-1">
        <h1>{{ title }}</h1>
      </div>
      <div class="mt-2 mb-4 d-flex justify-content-end gap-2">
        <ButtonOutline @click=""> Format HTML </ButtonOutline>
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
              <h2 class="h4 m-0 p-2 text-white">HTML</h2>
            </div>
            <div
              ref="html"
              @keyup="(event) => onCodeInput(event.target.value, 'html')"
            />
          </div>
          <div class="position-relative mt-4 shadow">
            <div class="bg-dark pl-2 size-7 sticky-top">
              <h2 class="h4 m-0 p-2 text-white">CSS</h2>
            </div>
            <div
              ref="css"
              @keyup="(event) => onCodeInput(event.target.value, 'css')"
            />
          </div>
          <div class="position-relative mt-4 shadow">
            <div class="bg-dark pl-2 size-7 sticky-top">
              <h2 class="h4 m-0 p-2 text-white">JS</h2>
            </div>
            <div
              ref="javascript"
              @keyup="(event) => onCodeInput(event.target.value, 'javscript')"
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
      graphqlError: null,
      success: null,
      loading: false,
      viewHtml: false,
      draft: "<h1>New page</h1><p>Add your contents here</p>",
      dirty: false,
      html: {},
      css: {},
      javascript: {},
      webContent: {
        html: "",
        content: {
          html: `<p class="welcome-message">Hello, world!</p>`,
          css: ".welcome-message { color: blue; }",
          javascript: "",
        },
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
  },
  methods: {
    initEditor(editor) {
      const editorRef = this.$refs[editor];
      this[editor] = monaco.editor.create(editorRef, {
        automaticLayout: true,
        value: this.webContent.content[editor],
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
      this.webContent.content[editor] = content;
      this.setWebContentHtml();
    },

    setWebContentHtml() {
      const content = [];
      if (this.webContent.content.css && this.webContent.content.css !== "") {
        const cssMarkup = [
          "<style type='text/css'>",
          this.webContent.content.css,
          "</style>",
        ].join("");
        content.push(cssMarkup);
      }

      content.push(this.webContent.content.html);

      if (
        this.webContent.content.javascript &&
        this.webContent.content.javascript !== ""
      ) {
        const jsMarkup = [
          "<script type='text/javascript'>",
          this.webContent.content.javascript,
          "</style>",
        ].join("");
        content.push(jsMarkup);
      }

      this.webContent.html = content.join("").trim();
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
            value: this.webContent,
          },
        }
      )
        .then((data) => {
          this.success = data.change.message;
          this.session.settings["page." + this.page] = this.webContent;
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
        this.webContent = this.session.settings["page." + this.page];
      } else {
        return "New page, edit here";
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
  mounted() {
    this.createAllEditors();
    this.setWebContentHtml();
  },
  created() {
    this.reload();
  },
};
</script>
