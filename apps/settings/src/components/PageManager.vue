<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    <h5 class="card-title">Manage pages</h5>
    <p v-if="!pages.length && loading">No pages available</p>
    <table v-else class="table caption-top">
      <thead>
        <tr>
          <th>Page</th>
          <th>View</th>
          <th>Edit</th>
        </tr>
      </thead>
      <tr v-for="(page, index) in pages.sort()" :key="index">
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
      <ButtonAction
        v-if="newPage && !nameError"
        @click="savePageSetting"
        class="ml-4"
      >
        Create new
      </ButtonAction>
    </form>
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
import { request } from "graphql-request";

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
      pages: [],
    };
  },
  methods: {
    async savePageSetting() {
      const initialContent = JSON.stringify({
        html: `<h1>${this.newPage}</h1>`,
        css: "",
        javascript: "",
      });

      const response = await request(
        "graphql",
        `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){status message}}`,
        {
          settings: {
            key: `page.${this.newPage}`,
            value: initialContent,
          },
        }
      );
      if (Object.hasOwn(response, "change")) {
        if (response.change.status === "SUCCESS") {
          this.success = response.change.message;
          this.pages.push(this.newPage);
          this.newPage = "";
        } else {
          this.graphqlError = response;
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
  },
  async created() {
    const query = `query {_settings { key }}`;
    const response = await request("graphql", query);
    if (response._settings) {
      const pageList = response._settings
        .filter((setting) => setting.key.startsWith("page."))
        .map((setting) => setting.key.replace("page.", "").trim());
      this.pages = pageList;
    } else {
      this.graphqlError = this.response;
    }
  },
};
</script>
