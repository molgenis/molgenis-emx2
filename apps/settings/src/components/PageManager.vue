<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    <h5 class="card-title">Manage pages</h5>
    <table class="table caption-top">
      <thead>
        <tr>
          <th>Page</th>
          <th>View</th>
          <th>Edit</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(page, index) in pages.sort()"
          :key="index"
          v-if="pages.length > 0"
        >
          <th class="font-weight-normal">{{ page }}</th>
          <td>
            <a :href="'../pages/#/' + page">{{ page }}</a>
          </td>
          <td>
            <a :href="'../pages/#/' + page + '/edit'">{{ page }}</a>
          </td>
        </tr>
        <tr v-else>
          <td colspan="3" class="text-center">No pages available</td>
        </tr>
      </tbody>
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
    MessageError,
    MessageSuccess,
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
  async mounted() {
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
