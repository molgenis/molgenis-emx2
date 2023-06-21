<template>
  <Molgenis v-model="session">
    <div class="bg-white container" :key="JSON.stringify(session)">
      <h1>Up/Download for {{ schema }}</h1>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <div v-if="taskId" class="pb-3">
        <h4>Progress of current upload:</h4>
        <ul class="fa-ul">
          <Task :taskId="taskId" @taskUpdated="taskUpdated" />
        </ul>
        <ButtonAction v-if="taskDone" @click="done"> Done </ButtonAction>
      </div>
      <div v-else class="pb-3">
        <h4>Upload</h4>
        <MessageWarning
          v-if="
            !session ||
            !session.roles ||
            !['Manager', 'Editor', 'Owner'].some((r) =>
              session.roles.includes(r)
            )
          "
        >
          You don't have permission to upload data. Might you need to login?
        </MessageWarning>
        <div v-else>
          <p>
            Import and export data (tables) and metadata (schema, settings) in
            bulk.
          </p>
          <div>
            <p>
              Import data by uploading files in excel, zip, json or yaml format.
            </p>
            <form class="form-inline">
              <InputFile v-model="file" />
              <ButtonAction @click="upload" v-if="file != undefined">
                Import
              </ButtonAction>
            </form>
            <br />
          </div>
        </div>
        <div
          v-if="
            session &&
            session.roles &&
            ['Manager', 'Editor', 'Viewer', 'Aggregator', 'Owner'].some((r) =>
              session.roles.includes(r)
            )
          "
        ></div>
        <h4>Download</h4>
        <MessageWarning
          v-if="!session || !session.roles || session.roles.length == 0"
        >
          You don't have permission to download data. Might you need to login?
        </MessageWarning>
        <div v-else>
          <p>Export data by downloading various file formats:</p>
          <div>
            <p>
              Export schema as <a :href="'../api/csv'">csv</a> /
              <a :href="'../api/json'">json</a> /
              <a :href="'../api/yaml'">yaml</a>
            </p>
            <p>
              Export all data as
              <a :href="'../api/excel'">excel</a> /
              <a :href="'../api/zip'">csv.zip</a> /
              <a :href="'../api/ttl'">ttl</a> /
              <a :href="'../api/jsonld'">jsonld</a>
            </p>
            <div v-if="visibleTables?.length > 0" :key="tablesHash">
              Export specific tables:
              <ul>
                <li v-for="table in visibleTables" :key="table.name">
                  {{ table.name }}:
                  <a :href="'../api/csv/' + table.name">csv</a> /
                  <a :href="'../api/excel/' + table.name">excel</a>
                </li>
              </ul>
            </div>
            <p>
              Note to programmers: the GET endpoints above also accept http POST
              command for updates, and DELETE commands for deletions.
            </p>
          </div>
        </div>
      </div>
    </div>
  </Molgenis>
</template>

<script>
import {
  ButtonAction,
  InputFile,
  MessageError,
  MessageSuccess,
  MessageWarning,
  Molgenis,
  Task,
} from "molgenis-components";
import { request } from "graphql-request";

/** Data import tool */
export default {
  components: {
    ButtonAction,
    InputFile,
    MessageError,
    MessageSuccess,
    MessageWarning,
    Molgenis,
    Task,
  },
  data: function () {
    return {
      session: null,
      schema: null,
      tables: [],
      file: null,
      error: null,
      success: null,
      loading: false,
      taskId: null,
      taskDone: false,
    };
  },
  computed: {
    visibleTables() {
      if (this.session?.roles.includes("Viewer")) {
        return this.tables;
      } else {
        return this.tables.filter((t) => t.tableType === "ONTOLOGIES");
      }
    },
    tablesHash() {
      if (this.tables) {
        return this.tables.map((table) => table.name).join("-");
      } else {
        return null;
      }
    },
  },
  methods: {
    loadSchema() {
      this.loading = true;
      request("graphql", "{_schema{name,tables{name,tableType}}}")
        .then((data) => {
          this.schema = data._schema.name;
          this.tables = data._schema.tables;
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    upload() {
      this.error = null;
      this.success = null;
      this.loading = true;
      this.taskDone = false;
      const splitFileName = this.file.name.split(".");
      const fileName = splitFileName[0];
      const type = splitFileName[splitFileName.length - 1];
      if (["csv", "json", "yaml"].includes(type)) {
        const reader = new FileReader();
        reader.readAsText(this.file);
        reader.onload = () => {
          const url = `/${this.schema}/api/${type}`;
          const options = {
            method: "POST",
            body: reader.result,
            headers: { fileName: fileName },
          };
          fetch(url, options)
            .then((response) => {
              if (response.ok) {
                response.text().then((successText) => {
                  this.success = successText;
                  this.error = null;
                });
              } else {
                response.json().then((error) => {
                  this.success = null;
                  this.error = error.errors[0].message;
                });
              }
            })
            .catch((error) => {
              this.error = error;
            })
            .finally(() => {
              this.file = null;
              this.loading = false;
              this.loadSchema();
            });
        };
      } else if (["xlsx", "zip"].includes(type)) {
        let formData = new FormData();
        formData.append("file", this.file);
        let url =
          "/" +
          this.schema +
          "/api/" +
          (type == "xlsx" ? "excel" : "zip") +
          "?async=true";
        fetch(url, {
          method: "POST",
          body: formData,
        })
          .then((response) => {
            if (response.ok) {
              response.json().then((task) => {
                this.taskId = task.id;
                this.error = null;
              });
            } else {
              response.json().then((error) => {
                this.success = null;
                this.error = error.errors[0].message;
              });
            }
          })
          .catch((error) => {
            this.error = error;
          })
          .finally(() => {
            this.file = null;
            this.loading = false;
            this.loadSchema();
          });
      } else {
        this.error = "File extension " + type + " not supported";
      }
    },
    taskUpdated(task) {
      if (["COMPLETED", "ERROR"].includes(task.status)) {
        this.taskDone = true;
      }
    },
    done() {
      this.taskId = null;
    },
  },
  watch: {
    session() {
      if (this.session && this.session.roles) {
        this.loadSchema();
      }
    },
  },
};
</script>
