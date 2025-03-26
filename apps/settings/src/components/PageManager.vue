<template>
  <div>
    <h5 class="card-title">Manage pages</h5>
    <table class="table caption-top">
      <caption class="h5" style="caption-side: top">
        Available pages
      </caption>
      <thead>
        <tr>
          <th>Page</th>
          <th>View</th>
          <th>Edit</th>
        </tr>
      </thead>
      <tr v-for="(page, index) in pages" :key="index">
        <td>{{ page }}</td>
        <td>
          <router-link :to="'../pages/#/' + page">view</router-link>
        </td>
        <td>
          <router-link :to="'../pages/#/' + page + '/edit'">edit</router-link>
        </td>
      </tr>
    </table>
    <form class="form-inline">
      <legend class="h5">Add a new page</legend>
      <InputString
        id="page-title"
        label="Page name"
        v-model="newPage"
        :errorMessage="nameError"
        class="d-flex align-items-center"
        style="gap: 12px"
      />
      <ButtonAction v-if="newPage && !nameError" @click="savePage" class="ml-4">
        Create new
      </ButtonAction>
    </form>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
  </div>
</template>

<script>
import {
  ButtonAction,
  IconAction,
  InputString,
  MessageError,
  MessageSuccess,
} from "molgenis-components";

export default {
  components: {
    IconAction,
    ButtonAction,
    InputString,
  },
  props: {
    session: Object,
  },
  data() {
    return {
      success: null,
      graphqlError: null,
      newPage: null,
    };
  },
  methods: {
    async savePageSettings() {
      const response = await request(
        "graphql",
        `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){status message}}`,
        {
          settings: {
            key: `page.${this.page}`,
            value: JSON.stringify({
              html: `<h1>${this.newPage}</h1>`,
              css: "",
              javascript: "",
            }),
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
    openPageEdit(page) {
      window.open("../pages/#/" + page + "/edit", "_self");
    },
  },
  computed: {
    nameError() {
      if (this.pages.includes(this.newPage)) {
        return "Page name already exists";
      } else {
        return undefined;
      }
    },
    pages() {
      if (this.session && this.session.settings) {
        return Object.keys(this.session.settings)
          .filter((key) => key.startsWith("page."))
          .map((key) => key.substring(5));
      }
      return [];
    },
  },
};
</script>
