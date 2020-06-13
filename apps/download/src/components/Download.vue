<template>
  <Molgenis :title="'Download from ' + schema" :menuItems="menuItems">
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      Use buttons below to download CSV, Excel and Zip format:
      <div class="card mb-3">
        <ButtonAction @click="downloadCsv">
          Download Metadata as molgenis.csv
        </ButtonAction>
        <p>
          Download only schema (i.e. metadata that defines all tables and
          columns of schema {{ schema }}) as Csv file.
        </p>
      </div>
      <div class="card mb-3">
        <ButtonAction @click="downloadExcel">
          Download Excel
        </ButtonAction>
        <p>
          Download all data from schema {{ schema }} as Excel file. N.b. can be
          potentially huge. In addition the excel will include a 'molgenis'
          sheet that defines the columns all other sheets.
        </p>
      </div>
      <div class="card mb-3">
        <ButtonAction @click="downloadZip">
          Download Zip file
        </ButtonAction>
        <p>
          Download all data from schema {{ schema }} as Zip file containing csv
          files. N.b. can be potentially huge. In addition the Zip includes a
          'molgenis.csv' file that defines columns of all other files.
        </p>
      </div>
    </div>
  </Molgenis>
</template>

<script>
import {
  ButtonAction,
  ButtonAlt,
  InputFile,
  MessageError,
  MessageSuccess,
  Spinner,
  Molgenis,
  LayoutCard
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

/** Data import tool */
export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputFile,
    MessageError,
    MessageSuccess,
    Spinner,
    Molgenis,
    LayoutCard
  },
  data: function() {
    return {
      schema: null,
      file: null,
      error: null,
      success: null,
      loading: false
    };
  },
  computed: {
    menuItems() {
      return [
        { label: "Tables", href: "../tables/" },
        {
          label: "Schema",
          href: "../schema/"
        },
        {
          label: "Upload",
          href: "../import/"
        },
        {
          label: "Download",
          href: "../download/"
        },
        {
          label: "GraphQL",
          href: "/api/playground.html?schema=/api/graphql/" + this.schema
        },
        {
          label: "Settings",
          href: "../settings/"
        }
      ];
    }
  },
  methods: {
    loadSchema() {
      this.loading = true;
      request("graphql", "{_schema{name}}")
        .then(data => {
          this.schema = data._schema.name;
        })
        .catch(error => {
          this.error = error.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
    downloadExcel() {
      //TODO add permission check
      window.open("/api/excel/" + this.schema);
    },
    downloadZip() {
      //TODO add permission check
      window.open("/api/zip/" + this.schema);
    },
    downloadCsv() {
      //TODO add permission check
      window.open("/api/csv/" + this.schema);
    }
  },
  created() {
    this.loadSchema();
  }
};
</script>

<docs>
    Example
    ```
    <Download schema="pet store"/>

    ```
</docs>
