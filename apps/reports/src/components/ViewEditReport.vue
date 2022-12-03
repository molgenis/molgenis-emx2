<template>
  <div>
    <router-link  to="/">&lt; back to report list</router-link>
    <div v-if="edit">
      <h2>Edit report: {{id}}<IconAction icon="eye" @click="edit = false"/></h2>
      <InputString id="reportName" v-model="name" label="name"/>
      <InputText id="reportSql" v-model="sql" label="sql"/>
      <MessageSuccess v-if="success">{{success}}</MessageSuccess>
        <ButtonAction @click="run">Refresh</ButtonAction>
        <ButtonAction @click="save" class="ml-2">Save</ButtonAction>
      <label><b>Result:</b></label>
    </div>
    <h2 v-else>Report: {{name}}<IconAction v-if="canEdit" icon="pencil-alt" @click="edit = true"/></h2>
    <MessageError v-if="error">{{error}}</MessageError>
    <div v-if="rows">
      <Pagination v-if="count" v-model="page" :limit="limit" :count="count"/><IconAction icon="download" @click="download(id)"/>
      <TableSimple :columns="columns" :rows="rows" class="bg-white" :key="JSON.stringify(this.rows)"/>
    </div>
  </div>
</template>

<script>
import { Client,TableSimple,ButtonAction,InputText,InputString,MessageError,MessageSuccess,Pagination,IconAction } from "molgenis-components";
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
    IconAction
  },
  props: {
    session: Object,
    id: String,
    limit: {type: Number, default: 5}
  },
  data() {
    return {
      rows: [{"test": "a"}],
      count: null,
      sql: "select * from \"Pet\"",
      name: null,
      error: null,
      success:  null,
      page: 1,
      edit: false
    }
  },
  computed: {
    columns() {
      //todo, we would like server to return types
      const names = [];
      this.rows.forEach(row => {
        Object.keys(row).forEach(key => {
          if( names.indexOf(key) === -1 ) {
            names.push(key)
          }
        });
      });
      return names;
    },
    canEdit() {
      return this.session?.roles?.includes("Manager");
    },
  },
  methods: {
    async run() {
      this.success = null;
      this.error = null;
      const offset = this.limit * (this.page - 1);
      const dataSql = this.sql + " OFFSET " + offset + " LIMIT "+this.limit;
      const data = await request("graphql", "query($sql: String){_query(sql:$sql){json}}",{sql: dataSql})
          .catch(error =>{
        this.error = error;
      });
      const countSql = `select count(*) from (${this.sql}) as count`;
      const count = await request("graphql", "query($sql: String){_query(sql:$sql){json}}",{sql: countSql})
          .catch(error =>{
            this.error = error;
          });
      this.rows = JSON.parse(data._query.json);
      this.count = JSON.parse(count._query.json)[0].count;
    },
    async save() {
      this.succes = null;
      this.error = null;
      const reports = await this.client.fetchSettingValue("reports");
      reports[this.id].sql = this.sql;
      reports[this.id].name = this.name;
      await this.client.saveSetting("reports",reports).catch(error => this.error = error);
      this.success = "Saved report "+this.id;
      this.run();
    },
    async reload() {
      const reports = await this.client.fetchSettingValue("reports");
      if(reports[this.id]) {
        this.sql = reports[this.id].sql;
        this.name = reports[this.id].name;
      } else {
        this.error = "report not found";
      }
      this.run();
    },
    download(id) {
      window.open("../api/zip/reports?id="+id, '_blank');
    }
  },
  watch: {
    page() {
      this.run();
    }
  },
  async mounted() {
    this.client = Client.newClient();
    this.reload();
  }
};
</script>