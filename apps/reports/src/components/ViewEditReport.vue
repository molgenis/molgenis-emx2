<template>
  <Spinner v-if="!session" />
  <MessageWarning
    v-else-if="
      !session ||
      !session.roles ||
      !['Viewer'].some((r) => session.roles.includes(r))
    "
  >
    Schema doesn't exist or you don't have permission to view. Might you need to
    login?
  </MessageWarning>
  <div v-else>
    <router-link to="/">&lt; back to report list</router-link>
    <div v-if="edit">
      <h2>
        Edit report: {{ id }}<IconAction icon="eye" @click="edit = false" />
      </h2>
      <InputString id="reportName" v-model="name" label="name" />
      <InputText id="reportSql" v-model="sql" label="sql" />
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <ButtonAction @click="save">Save</ButtonAction>
      <div class="mt-2">
        <label><b>Result:</b></label>
      </div>
    </div>
    <h2 v-else>
      Report: {{ name
      }}<IconAction v-if="canEdit" icon="pencil-alt" @click="edit = true" />
    </h2>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div v-if="rows && rows.length > 0">
      <Pagination
        v-if="count"
        v-model="page"
        :limit="limit"
        :count="count"
      /><IconAction icon="download" @click="download(id)" />
      <TableSimple
        :columns="columns"
        :rows="rows"
        class="bg-white"
        :key="JSON.stringify(this.rows)"
      />
    </div>
    <div v-else>No results found.</div>
  </div>
</template>

<script>
import {
  Client,
  TableSimple,
  ButtonAction,
  InputText,
  InputString,
  MessageError,
  MessageSuccess,
  Pagination,
  IconAction,
  Spinner,
  MessageWarning,
} from "molgenis-components";
import { request } from "graphql-request";

export default {
  name: "EditQuery",
  components: {
    TableSimple,
    ButtonAction,
    InputText,
    MessageError,
    MessageSuccess,
    InputString,
    Pagination,
    IconAction,
    Spinner,
    MessageWarning,
  },
  props: {
    session: Object,
    id: String,
    limit: { type: Number, default: 5 },
  },
  data() {
    return {
      rows: undefined,
      count: null,
      sql: 'select * from "Pet"',
      name: null,
      error: null,
      success: null,
      page: 1,
      edit: false,
    };
  },
  computed: {
    columns() {
      if (this.rows) {
        const names = [];
        this.rows.forEach((row) => {
          Object.keys(row).forEach((key) => {
            if (names.indexOf(key) === -1) {
              names.push(key);
            }
          });
        });
        return names;
      }
    },
    canEdit() {
      return this.session?.roles?.includes("Manager");
    },
  },
  methods: {
    async run() {
      this.error = null;
      const offset = this.limit * (this.page - 1);
      const result = await request(
        "graphql",
        `{_reports(id:${this.id},limit:${this.limit},offset:${offset}){data,count}}`
      ).catch((error) => {
        this.error = error;
      });
      this.rows = JSON.parse(result._reports.data);
      this.count = result._reports.count;
    },
    async save() {
      this.succes = null;
      this.error = null;
      const reports = await this.client.fetchSettingValue("reports");
      reports[this.id].sql = this.sql;
      reports[this.id].name = this.name;
      this.client
        .saveSetting("reports", reports)
        .then((res) => {
          this.success = "Saved report " + this.id + " and refreshed query";
          this.run();
        })
        .catch((error) => (this.error = error));
    },
    async reload() {
      const reports = await this.client.fetchSettingValue("reports");
      if (reports[this.id]) {
        this.sql = reports[this.id].sql;
        this.name = reports[this.id].name;
      } else {
        this.error = "report not found";
      }
      this.run();
    },
    download(id) {
      window.open("../api/reports/zip?id=" + id, "_blank");
    },
  },
  watch: {
    page() {
      this.run();
    },
  },
  mounted() {
    this.client = Client.newClient();
    this.reload();
  },
};
</script>
