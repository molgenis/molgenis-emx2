<template>
  <div>
    <router-link :to="'/' + page">view page</router-link>
    <div class="d-flex">
      <div class="flex-grow-1">
        <h1>{{ title }}</h1>
      </div>
      <div class="mt-2 mb-4 d-flex justify-content-end gap-2">
        <ButtonOutline @click="viewHtml = !viewHtml"
          >View {{ viewHtml ? "editor" : "HTML" }}</ButtonOutline
        >
        <ButtonAction @click="savePage" class="ml-2">Save changes</ButtonAction>
      </div>
    </div>
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>

      <div v-if="viewHtml">
        <InputText :id="`${page}-html-editor`" v-model="draft" />
      </div>
      <QuillEditor
        v-else
        v-model:content="draft"
        toolbar="full"
        class="bg-white"
        contentType="html"
      />
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
};
</script>
