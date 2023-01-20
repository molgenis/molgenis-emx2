<template>
  <div>
    {{ page }}
    <router-link :to="'/' + page">view page</router-link>
    <h1>{{ title }}</h1>
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <QuillEditor
        v-model:content="draft"
        toolbar="full"
        class="bg-white"
        contentType="html"
      />
      <div class="mt-2 float-right">
        <ButtonAction @click="savePage">Save '{{ page }}'</ButtonAction>
      </div>
    </div>
  </div>
</template>

<script>
import {
  ButtonAction,
  ButtonAlt,
  MessageError,
  MessageSuccess,
  Spinner,
} from "molgenis-components";
import { request } from "graphql-request";
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";

export default {
  components: {
    ButtonAction,
    ButtonAlt,
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
