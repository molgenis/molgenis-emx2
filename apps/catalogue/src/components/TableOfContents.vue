<template>
  <div class="height:100%; overflow-y: scroll">
    <MessageError v-if="error">{{ error }}</MessageError>
    <ul class="nav flex-column">
      Databanks & tables:
      <li v-for="databank in databanks" class="nav-item border-top">
        <RouterLink
          class="nav-link"
          :class="{
            'font-weight-bold text-secondary':
              databank.acronym === databankAcronym,
          }"
          :to="{
            name: 'databank-description',
            params: { databankAcronym: databank.acronym },
          }"
        >
          {{ databank.acronym }}
        </RouterLink>
        <ul v-if="databankAcronym == databank.acronym" class="nav flex-column">
          <li v-for="table in databank.tables" class="nav-item pl-4">
            <div class="nav-link">
              <RouterLink
                :class="{
                  'font-weight-bold  text-secondary': table.name === tableName,
                }"
                :to="{
                  name: 'table-description',
                  params: {
                    databankAcronym: databank.acronym,
                    tableName: table.name,
                  },
                }"
              >
                {{ table.name }}
              </RouterLink>

              <RouterLink
                class="float-right"
                :class="{
                  'font-weight-bold  text-secondary': table.name === tableName,
                }"
                :to="{
                  name: 'table-harmonisations',
                  params: {
                    databankAcronym: databank.acronym,
                    tableName: table.name,
                  },
                }"
              >
                <span
                  class="ml-2 fa-stack has-badge"
                  :data-count="
                    Array.isArray(table.variables)
                      ? table.variables
                          .map((v) =>
                            v.harmonisations_agg !== undefined
                              ? v.harmonisations_agg.count
                              : 0
                          )
                          .reduce((a, b) => a + b, 0)
                      : 0
                  "
                >
                  <i class="fa fa-check-circle fa-stack-2x"
                /></span>
              </RouterLink>
              <RouterLink
                class="float-right"
                :class="{
                  'font-weight-bold  text-secondary': table.name === tableName,
                }"
                :to="{
                  name: 'table-variables',
                  params: {
                    databankAcronym: databank.acronym,
                    tableName: table.name,
                  },
                }"
              >
                <span
                  class="ml-2 fa-stack has-badge"
                  :data-count="table.variables_agg.count"
                >
                  <i class="fa fa-table fa-stack-2x"></i>
                </span>
              </RouterLink>
            </div>
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>

<style>
.fa-stack[data-count]:after {
  position: absolute;
  right: 0%;
  top: 1%;
  content: attr(data-count);
  font-size: 60%;
  padding: 0.6em;
  border-radius: 999px;
  line-height: 0.75em;
  color: white;
  background: rgba(255, 0, 0, 0.85);
  text-align: center;
  min-width: 2em;
  font-weight: bold;
}
</style>

<script>
import { request } from "graphql-request";
import { MessageError } from "@mswertz/emx2-styleguide";

export default {
  components: {
    MessageError,
  },
  props: {
    databankAcronym: String,
    tableName: String,
  },
  data() {
    return {
      error: null,
      collections: [],
    };
  },
  methods: {
    reload() {
      console.log("databanks reload");
      request(
        "graphql",
        `{Databanks{acronym,name,tables{name,label,variables_agg{count},variables{harmonisations_agg{count}}}}}`
      )
        .then((data) => {
          this.databanks = data.Databanks;
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
};
</script>
