<template>
  <div v-if="graphqlError">
    <MessageError>
      {{ graphqlError }}<br />
      sourceCollection: {{ this.sourceCollection }}<br />
      sourceTable: {{ this.sourceTable }}<br />
      targetVariable: {{ this.targetVariable }}<br />
      targetResource:
      {{ this.targetResource }}<br />
      targetTable:
      {{ this.targetTable }}
    </MessageError>
  </div>
  <div v-else>
    <a v-if="!compact" href="#" @click="show = true">
      {{ sourceCollection }}@{{ sourceVersion.version }}
    </a>
    <i
      v-if="match == 'complete'"
      class="fa fa-check-circle text-success"
      @click="show = true"
    />
    <i
      v-else-if="match == 'partial'"
      class="fa fa-check-circle text-warning"
      @click="show = true"
    />
    <i
      v-else-if="match == 'nza'"
      class="fa fa-times text-primary"
      @click="show = true"
    />
    <i v-else class="fa fa-question text-primary" />
    <LayoutModal
      v-if="show"
      @close="show = false"
      title="Harmonisation details"
    >
      <template v-slot:body>
        <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
        <div class="row">
          <div class="col">
            <h5>Harmonised variable</h5>
            <dl>
              <dt>Consortium</dt>
              <dd>{{ targetResource }}</dd>
              <dt>Version</dt>
              <dd>{{ targetRelease.version }}</dd>
              <dt>Table</dt>
              <dd>{{ targetTable }}</dd>
              <dt>Variable</dt>
              <dd>{{ targetVariable }}</dd>
            </dl>
          </div>
          <div class="col">
            <h5>Source variable(s)</h5>
            <dl>
              <dt>Databank</dt>
              <dd>{{ sourceCollection }}</dd>
              <dt>Version</dt>
              <dd>{{ sourceRelease.version }}</dd>
              <dt>Table</dt>
              <dd>{{ sourceTable }}</dd>
              <dt>Variable</dt>
              <dd>
                {{
                  harmonisation.sourceVariables
                    ? harmonisation.sourceVariables.map((v) => v.name).join(",")
                    : "N/A"
                }}
              </dd>
            </dl>
          </div>
        </div>
        <h5>Description</h5>
        <p>{{ harmonisation.description }}</p>
        <h5>Syntax</h5>
        <div class="border" style="background-color: #eee">
          <pre>
          {{ harmonisation.syntax }}
        </pre
          >
        </div>
        <span
          v-if="
            harmonisation.targetVariable.table &&
            harmonisation.targetVariable.table.harmonisations
          "
        >
          <h5>Table harmonisation</h5>
          <div>
            <table>
              <thead>
                <tr>
                  <th scope="col">Source table</th>
                  <th scope="col">Description of mapping</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="h in harmonisation.targetVariable.table.harmonisations"
                >
                  <td>{{ h.sourceTable.name }}</td>
                  <td>{{ h.description }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </span>
      </template>
      <template v-slot:footer>
        <ButtonAction @click="show = false">Close</ButtonAction>
      </template>
    </LayoutModal>
  </div>
</template>

<style scoped>
dt {
  float: left;
  clear: left;
  width: 200px;
  font-weight: bold;
}

dd {
  margin: 0 0 0 110px;
  padding: 0 0 0.5em 0;
}
</style>

<script>
import {
  ButtonAction,
  ButtonAlt,
  LayoutModal,
  MessageError,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: { LayoutModal, ButtonAlt, MessageError, ButtonAction },
  props: {
    sourceCollection: String,
    sourceTable: String,
    sourceVersion: String,
    targetVariable: String,
    targetResource: String,
    targetTable: String,
    targetVersion: String,
    match: String,
    compact: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      show: false,
      harmonisation: {},
      graphqlError: null,
    };
  },
  methods: {
    reload() {
      let filter = {
        sourceRelease: {
          resource: { pid: { equals: this.sourceCollection } },
          version: { equals: this.sourceVersion },
        },
        sourceTable: {
          name: { equals: this.sourceTable },
        },
        targetRelease: {
          resource: { pid: { equals: this.targetResource } },
          version: {
            equals: this.targetVersion,
          },
        },
        targetVariable: {
          name: { equals: this.targetVariable },
          table: {
            name: { equals: this.targetTable },
          },
        },
      };
      request(
        "graphql",
        `query VariableHarmonisations($filter:VariableHarmonisationsFilter){VariableHarmonisations(filter:$filter){match{name},targetVariable{name,table{harmonisations{sourceTable{name}description}}},sourceVariables{name,description,format{name}},syntax,description}}`,
        {
          filter: filter,
        }
      )
        .then((data) => {
          this.harmonisation = data.VariableHarmonisations[0];
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  watch: {
    show() {
      this.reload();
    },
  },
};
</script>
