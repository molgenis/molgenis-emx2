<template>
  <div>
    <a v-if="!compact" href="#" @click="show = true">
      {{ sourceCollection }}:{{ sourceDataset }}
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
        <MessageError v-if="error">{{ error }}</MessageError>
        <div class="row">
          <div class="col">
            <h5>Harmonised variable</h5>
            <dl>
              <dt>Collection</dt>
              <dd>{{ targetCollection }}</dd>
              <dt>Dataset</dt>
              <dd>{{ targetDataset }}</dd>
              <dt>Variable</dt>
              <dd>{{ targetVariable }}</dd>
            </dl>
          </div>
          <div class="col">
            <h5>Source variable(s)</h5>
            <dl>
              <dt>Collection</dt>
              <dd>{{ sourceCollection }}</dd>
              <dt>Dataset</dt>
              <dd>{{ sourceDataset }}</dd>
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
        <span v-if="harmonisation.targetVariable.dataset.harmonisations">
          <h5>Dataset harmonisation</h5>
          <div>
            <table>
              <thead>
                <tr>
                  <th>Source dataset</th>
                  <th>Description of mappping</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="h in harmonisation.targetVariable.dataset
                    .harmonisations"
                >
                  <td>{{ h.sourceDataset.name }}</td>
                  <td>{{ h.description }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </span>
        <ShowMore title="debug">
          <pre>
          {{ JSON.stringify(harmonisation) }}

          match = {{ match }}
       </pre
          >
        </ShowMore>
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
  ShowMore,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: { LayoutModal, ButtonAlt, MessageError, ShowMore, ButtonAction },
  props: {
    sourceCollection: String,
    sourceDataset: String,
    targetVariable: String,
    targetCollection: String,
    targetDataset: String,
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
      error: null,
    };
  },
  methods: {
    reload() {
      let filter = {
        sourceDataset: {
          name: { equals: this.sourceDataset },
          collection: { acronym: { equals: this.sourceCollection } },
        },
        targetVariable: {
          name: { equals: this.targetVariable },
          dataset: {
            name: { equals: this.targetDataset },
            collection: { acronym: { equals: this.targetCollection } },
          },
        },
      };
      request(
        "graphql",
        `query VariableHarmonisations($filter:VariableHarmonisationsFilter){VariableHarmonisations(filter:$filter){targetVariable{name,dataset{harmonisations{sourceDataset{name}description}}},sourceVariables{name,description,format{name},valueLabels,missingValues},syntax,description}}`,
        {
          filter: filter,
        }
      )
        .then((data) => {
          this.harmonisation = data.VariableHarmonisations[0];
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
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
