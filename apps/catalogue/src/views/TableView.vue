<template>
  <div v-if="error">
    <MessageError>{{ error }}</MessageError>
  </div>
  <div v-else-if="table" class="container bg-white">
    <div class="p-2 bg-dark text-white mb-3">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="/list/Tables" class="text-white"> tables</RouterLink>
        /
      </h6>
    </div>

    <h4>Table:</h4>
    <h1>{{ table.name }}</h1>
    <p>{{ table.description ? table.description : "Description: N/A" }}</p>
    <MessageError v-if="error"> {{ error }}</MessageError>
    <div class="row">
      <div class="col">
        <h6>Topics</h6>
        <OntologyTerms :terms="table.topics" color="dark" />
      </div>
      <div class="col">
        <h6>{{ resourceType }}</h6>
        <RouterLink
          :to="{
            name: resourceType.toLowerCase(),
            params: { acronym: acronym },
          }"
          >{{ table.release.resource.acronym }}
        </RouterLink>
        <h6>Release</h6>
        <RouterLink
          :to="{
            name: 'release',
            params: { acronym: acronym, version: version },
          }"
        >
          {{ table.release.version }}
        </RouterLink>
      </div>
    </div>
    <nav>
      <div class="nav nav-tabs">
        <a
          class="nav-item nav-link"
          :class="{ 'active grey': tab == 'Variables' }"
          @click="tab = 'Variables'"
          ><h6>Variables</h6></a
        >
        <a
          class="nav-item nav-link border"
          :class="{ 'active grey': tab == 'Mappings' }"
          @click="tab = 'Mappings'"
          ><h6>Mappings</h6></a
        >
      </div>
    </nav>
    <TableExplorer
      v-if="tab == 'Variables'"
      table="Variables"
      :showHeader="false"
      :filter="{
        table: { name: { equals: name } },
        release: {
          version: { equals: version },
          resource: { acronym: { equals: acronym } },
        },
      }"
      @click="openVariable"
    />
    <div v-else>
      <table v-if="table.mappings" class="table table-sm table-bordered">
        <thead>
          <tr>
            <th>from</th>
            <th>fromVersion</th>
            <th>fromTable</th>
            <th>description</th>
            <th>syntax</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in table.mappings">
            <td>
              <RouterLink
                :to="{
                  name: getType(
                    m.fromRelease.resource.mg_tableclass
                  ).toLowerCase(),
                  params: { acronym: m.fromRelease.resource.acronym },
                }"
                >{{ m.fromRelease.resource.acronym }}
              </RouterLink>
            </td>
            <td>
              <RouterLink
                :to="{
                  name: 'release',
                  params: {
                    acronym: m.fromRelease.resource.acronym,
                    version: m.fromRelease.version,
                  },
                }"
              >
                {{ m.fromRelease.version }}
              </RouterLink>
            </td>
            <td>
              <RouterLink
                :to="{
                  name: 'table',
                  params: {
                    acronym: m.fromRelease.resource.acronym,
                    version: m.fromRelease.version,
                    name: m.fromTable.name,
                  },
                }"
              >
                {{ m.fromTable.name }}
              </RouterLink>
            </td>
            <td>{{ m.description }}</td>
            <td>
              <pre>{{ m.syntax }}</pre>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else>N/A</p>
    </div>
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError, TableExplorer } from "@mswertz/emx2-styleguide";
import VariablesList from "../components/VariablesList";
import Property from "../components/Property";
import OntologyTerms from "../components/OntologyTerms";

export default {
  components: {
    OntologyTerms,
    VariablesList,
    Property,
    MessageError,
    TableExplorer,
  },
  props: {
    acronym: String,
    version: String,
    name: String,
  },
  data() {
    return {
      error: null,
      table: null,
      tab: "Variables",
    };
  },
  computed: {
    resourceType() {
      if (this.table.release) {
        return this.table.release.resource.mg_tableclass
          .split(".")[1]
          .slice(0, -1);
      }
    },
  },
  methods: {
    getType(mg_tableclass) {
      return mg_tableclass.split(".")[1].slice(0, -1);
    },
    openVariable(row) {
      this.$router.push({
        name: "variable",
        params: {
          acronym: this.acronym,
          version: this.version,
          table: this.name,
          name: row.name,
        },
      });
    },
    reload() {
      request(
        "graphql",
        `query Tables($acronym:String,$version:String,$name:String){Tables(filter:{release:{version:{equals:[$version]},resource:{acronym:{equals:[$acronym]}}},name:{equals:[$name]}})
        {name,release{version,resource{acronym,name,mg_tableclass}},topics{name,ontologyTermURI,definition}, description,label,topics{name}
        mappings{description,syntax,fromRelease{resource{acronym,mg_tableclass}version}fromTable{name}}}}`,
        {
          acronym: this.acronym,
          version: this.version,
          name: this.name,
        }
      )
        .then((data) => {
          this.table = data.Tables[0];
        })
        .catch((error) => {
          if (error.response) this.error = error.response.errors[0].message;
          else this.error = error;
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
