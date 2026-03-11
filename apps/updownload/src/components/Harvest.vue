<template>
  <Molgenis v-model="session">
    <div class="bg-white container" :key="JSON.stringify(session)">
      <h1>DCAT Harvest (beta)</h1>
      <router-link to="/">← Back to Import</router-link>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <div v-if="taskId" class="pb-3">
        <h4>Harvest progress:</h4>
        <ul class="fa-ul">
          <Task :taskId="taskId" @taskUpdated="taskUpdated" />
        </ul>
        <ButtonAction v-if="taskDone" @click="done">Done</ButtonAction>
      </div>
      <div v-else class="pb-3">
        <MessageWarning
          v-if="
            !session ||
            !session.roles ||
            !['Manager', 'Editor', 'Owner'].some((r) =>
              session.roles.includes(r)
            )
          "
        >
          You don't have permission to harvest data. Might you need to login?
        </MessageWarning>
        <div v-else>
          <div class="mt-3">
            <h4>Harvest from URL</h4>
            <p>Enter the URL of a DCAT catalog (FAIR Data Point or similar).</p>
            <form class="form-inline" @submit.prevent>
              <input
                v-model="catalogUrl"
                type="text"
                class="form-control mr-2"
                placeholder="https://example.org/catalog"
                aria-label="DCAT catalog URL"
              />
              <ButtonAction @click="harvestUrl" v-if="catalogUrl">
                Harvest
              </ButtonAction>
              <Spinner v-if="loading" />
            </form>
          </div>
          <div class="mt-4">
            <h4>Harvest from file</h4>
            <p>Upload a Turtle (.ttl) file containing a DCAT catalog.</p>
            <form class="form-inline">
              <InputFile v-model="file" accept=".ttl" />
              <ButtonAction @click="harvestFile" v-if="file">
                Upload & Harvest
              </ButtonAction>
              <Spinner v-if="loading" />
            </form>
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
  Spinner,
  MessageError,
  MessageSuccess,
  MessageWarning,
  Molgenis,
  Task,
} from "molgenis-components";
import { request } from "graphql-request";

export default {
  components: {
    ButtonAction,
    Spinner,
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
      catalogUrl: "",
      file: null,
      error: null,
      success: null,
      loading: false,
      taskId: null,
      taskDone: false,
    };
  },
  methods: {
    loadSchema() {
      request("graphql", "{_schema{id}}")
        .then((data) => {
          this.schema = data._schema.id;
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        });
    },
    harvestUrl() {
      this.error = null;
      this.success = null;
      this.loading = true;
      this.taskDone = false;
      fetch(`/${this.schema}/api/harvest/dcat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url: this.catalogUrl }),
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
              this.error = error.errors?.[0]?.message ?? "Harvest failed";
            });
          }
        })
        .catch((error) => {
          this.error = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    harvestFile() {
      this.error = null;
      this.success = null;
      this.loading = true;
      this.taskDone = false;
      const formData = new FormData();
      formData.append("file", this.file);
      fetch(`/${this.schema}/api/harvest/dcat`, {
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
              this.error = error.errors?.[0]?.message ?? "Harvest failed";
            });
          }
        })
        .catch((error) => {
          this.error = error;
        })
        .finally(() => {
          this.file = null;
          this.loading = false;
        });
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
