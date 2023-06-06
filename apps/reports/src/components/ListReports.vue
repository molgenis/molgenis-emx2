<template>
  <Spinner v-if="!session" />
  <MessageWarning
    v-else-if="
      !session ||
      !session.roles ||
      !['Viewer'].some(r => session.roles.includes(r))
    ">
    Schema doesn't exist or you don't have permission to view. Might you need to
    login?
  </MessageWarning>
  <div v-else>
    <h2>Reports</h2>
    <p>Listing of reports</p>
    <MessageWarning v-if="session && !canView">
      You don't have permission to view this data. Might you need to login?
    </MessageWarning>
    <span v-else-if="session">
      <MessageError v-if="error">{{ error }}</MessageError>
      <ButtonAction
        v-if="selection.length > 0"
        class="mb-2"
        @click="downloadSelected"
        >Download selected</ButtonAction
      >
      <ButtonDanger
        v-if="selection.length > 0 && canEdit"
        class="mb-2 ml-2"
        @click="deleteSelected"
        >Delete selected</ButtonDanger
      >
      <TableSimple
        @rowClick="open"
        :columns="['id', 'name']"
        :rows="reportsWithId"
        class="bg-white"
        selectColumn="id"
        v-model="selection">
        <template v-slot:colheader>
          <IconAction v-if="canEdit" icon="plus" @click="add" />
        </template>
        <template v-slot:rowheader="slotProps">
          <IconAction
            v-if="canEdit"
            icon="pencil-alt"
            @click="open(slotProps.row)" />
          <IconAction v-else icon="eye" @click="open(slotProps.row)" />
        </template>
      </TableSimple>
    </span>
  </div>
</template>

<script>
import {
  Client,
  MessageError,
  IconAction,
  IconDanger,
  TableSimple,
  ButtonAction,
  ButtonDanger,
  MessageWarning,
  Spinner,
} from "molgenis-components";

export default {
  components: {
    MessageError,
    IconAction,
    IconDanger,
    TableSimple,
    ButtonAction,
    ButtonDanger,
    MessageWarning,
    Spinner,
  },
  props: {
    session: Object,
    schema: Object,
  },
  data() {
    return {
      reports: [],
      error: null,
      client: null,
      selection: [],
    };
  },
  computed: {
    canEdit() {
      return this.session?.roles?.includes("Manager");
    },
    canView() {
      return this.session?.roles?.includes("Viewer");
    },
    reportsWithId() {
      if (this.reports) {
        let index = 0;
        return this.reports.map(report => {
          report.id = index++;
          return report;
        });
      }
    },
  },
  methods: {
    async reload() {
      const result = await this.client.fetchSettingValue("reports");
      if (result) {
        this.reports = result;
      }
    },
    async add() {
      this.error = null;
      this.reports.push({ name: "new report", sql: "" });
      await this.client
        .saveSetting("reports", this.reports)
        .catch(error => (this.error = error));
      this.reload();
    },
    async deleteSelected() {
      this.error = null;
      this.selection.forEach(id => this.reports.splice(id, 1));
      await this.client
        .saveSetting("reports", this.reports)
        .catch(error => (this.error = error));
      this.reload();
    },
    open(row) {
      this.$router.push({ name: "edit", params: { id: row.id } });
    },
    downloadSelected() {
      window.open(
        `../api/reports/zip?id=${this.selection.join(",")}`,
        "_blank"
      );
    },
  },
  mounted() {
    this.client = Client.newClient();
    this.reload();
  },
};
</script>
