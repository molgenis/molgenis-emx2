<template>
  <Molgenis v-model="session">
    <div class="bg-white container" :key="JSON.stringify(session)">
      <h1>Up/Download for {{ schema }}</h1>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <div v-if="taskUrl">
        <h4>Progress of current upload:</h4>
        <ul class="fa-ul">
          <Task :task="task" />
        </ul>
        <ButtonAction
          v-if="task.status == 'COMPLETED' || task.status == 'ERROR'"
          @click="done"
        >
          Done
        </ButtonAction>
      </div>
      <div v-else class="mb-2">
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
            ['Manager', 'Editor', 'Viewer', 'Owner'].some((r) =>
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
            <div v-if="tables" :key="tablesHash">
              Export specific tables:
              <ul>
                <li v-for="table in tables" :key="table.name">
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
  Spinner,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import Task from "./Task";

/** Data import tool */
export default {
  components: {
    ButtonAction,
    InputFile,
    MessageError,
    MessageSuccess,
    MessageWarning,
    Spinner,
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
      taskUrl: null,
      task: null,
    };
  },
  computed: {
    tablesHash() {
      if (this.tables) {
        this.tables.map((t) => t.name).join("-");
      }
      return null;
    },
  },
  methods: {
    done() {
      this.task = null;
      this.taskUrl = null;
    },
    startMonitorTask() {
      if (!this.task || !["COMPLETED", "ERROR"].includes(this.task.status)) {
        setTimeout(this.monitorTask, 500);
      } else {
        if (this.task.status == "ERROR") {
          this.error = this.task.status.description;
          this.success = null;
        } else {
          this.error = null;
          this.success = this.task.status.description;
        }
      }
    },
    monitorTask() {
      fetch(this.taskUrl)
        .then((response) => {
          if (response.ok) {
            response.json().then((task) => {
              this.task = task;
              this.startMonitorTask();
            });
          } else {
            response.text().then((error) => {
              this.error = error;
              this.startMonitorTask();
            });
          }
        })
        .catch((error) => {
          this.error = error;
          this.startMonitorTask();
        });
    },
    loadSchema() {
      this.loading = true;
      request("graphql", "{_schema{name,tables{name}}}")
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
      //upload file contents
      let type = this.file.name.split(".").pop();
      if (["csv", "json", "yaml"].includes(type)) {
        let reader = new FileReader();
        reader.readAsText(this.file);
        let url = "/" + this.schema + "/api/" + type;
        let _this = this;
        reader.onload = function () {
          fetch(url, { method: "POST", body: reader.result })
            .then((response) => {
              response.text().then((successText) => {
                _this.success = successText;
                _this.error = null;
              });
            })
            .catch((error) => {
              error.text().then((errorText) => {
                _this.success = null;
                _this.error = "Failed: " + errorText;
              });
            })
            .finally(() => {
              _this.file = null;
              _this.loading = false;
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
                this.taskUrl = task.url;
                this.error = null;
                this.monitorTask();
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

<docs>
Example
```
<Import schema="pet store"/>

```
</docs>
