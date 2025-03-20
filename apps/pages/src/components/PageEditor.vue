<template>
  <div>
    <router-link :to="'/' + page">view page</router-link>
    <div class="d-flex">
      <div class="flex-grow-1">
        <h1>{{ title }}</h1>
      </div>
      <div class="mt-2 mb-4 d-flex justify-content-end gap-2">
        <ButtonOutline @click="format"> Format HTML </ButtonOutline>
        <ButtonAction @click="savePage" class="ml-2">Save changes</ButtonAction>
      </div>
    </div>
    <Spinner v-if="loading" />
    <div class="container-fluid" v-else>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <div class="row">
        <div class="col-7">
          <div ref="code-editor" class="code-editor" @keyup="onCodeInput" />
        </div>
        <div v-html="draft" class="col-5 border border-primary" />
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
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import * as monaco from "monaco-editor";

export default {
  components: {
    InputText,
    ButtonAction,
    ButtonOutline,
    MessageError,
    MessageSuccess,
    Spinner,
    QuillEditor,
  },
  data() {
    return {
      graphqlError: null,
      success: null,
      loading: false,
      viewHtml: false,
      draft: "<h1>New page</h1><p>Add your contents here</p>",
      dirty: false,
      codeEditor: {},
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
    format() {
      if (this.codeEditor && this.codeEditor.getAction) {
        this.codeEditor.getAction("editor.action.formatDocument").run();
      }
    },

    onCodeInput(event) {
      this.draft = event.target.value;
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
            value: this.draft.trim(),
          },
        }
      )
        .then((data) => {
          this.success = data.change.message;
          this.session.settings["page." + this.page] = this.draft;
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
        this.draft = this.session.settings["page." + this.page];
      } else {
        return "New page, edit here";
      }
    },
  },
  destroyed() {
    this.codeEditor.dispose();
  },
  watch: {
    session: {
      deep: true,
      handler() {
        this.reload();
      },
    },
    draft: {
      handler(newValue, oldValue) {
        if (newValue === oldValue) {
          this.format();
        }
      },
    },
  },
  mounted() {
    const editor = this.$refs["code-editor"];
    this.codeEditor = monaco.editor.create(editor, {
      automaticLayout: true,
      value: this.draft,
      language: "html",
      theme: "vs-dark",
      autoClosingBrackets: true,
      dimension: {
        height: 500,
      },
    });

    const formatTimer = setTimeout(() => {
      this.format();
      clearTimeout(formatTimer);
    }, 250);
  },
  created() {
    this.reload();
  },
};
</script>
