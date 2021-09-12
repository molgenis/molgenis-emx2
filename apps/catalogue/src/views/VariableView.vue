<template>
  <div class="container bg-white">
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div v-if="variable">
      <div class="p-2 bg-dark text-white mb-3">
        <h6>
          <RouterLink to="/" class="text-white"> home</RouterLink>
          /
          <RouterLink to="/list/Variables" class="text-white">
            variables
          </RouterLink>
          /
        </h6>
      </div>
      <h6 class="d-inline">{{ resourceType }}&nbsp;</h6>
      <RouterLink
        :to="{
          name: resourceType + '-details',
          params: {
            pid: variable.release.resource.pid,
          },
        }"
        >{{ variable.release.resource.pid }}
      </RouterLink>
      /
      <h6 class="d-inline">Release</h6>
      <RouterLink
        :to="{
          name: 'Releases-details',
          params: {
            pid: variable.release.resource.pid,
            version: variable.release.version,
          },
        }"
      >
        {{ variable.release.version }}
      </RouterLink>
      /
      <h6 class="d-inline">Table name</h6>
      <RouterLink
        :to="{
          name: 'Tables-details',
          params: {
            pid: variable.release.resource.pid,
            name: variable.table.name,
            version: variable.release.version,
          },
        }"
      >
        {{ variable.table.name }}
      </RouterLink>

      <h1>Variable: {{ variable.name }}</h1>
      <div class="row">
        <div class="col">
          <h6>Variable name</h6>
          <p>{{ variable.name }}</p>
          <h6>Variable type</h6>
          <OntologyTerms :terms="[variable.format]" color="dark" />
          <h6>Variable keywords</h6>
          <OntologyTerms :terms="variable.keywords" color="dark" />
          <h6>Variable unit</h6>
          <OntologyTerms :terms="[variable.unit]" color="dark" />
          <h6>Mandatory</h6>
          <p>{{ variable.mandatory ? "YES" : "NO" }}</p>
          <h6>Variable Description</h6>
          <p>{{ variable.description ? variable.description : "N/A" }}</p>
          <h6>Vocabularies</h6>
          <OntologyTerms :terms="variable.vocabularies" color="dark" />

          <h6>Permitted values</h6>
          <table
            v-if="variable.permittedValues"
            class="table table-sm table-bordered m-2"
          >
            <thead>
              <th>value</th>
              <th>label</th>
              <th>isMissing</th>
            </thead>
            <tbody>
              <tr v-for="v in variable.permittedValues">
                <td>{{ v.value }}</td>
                <td>{{ v.label }}</td>
                <td>
                  {{ v.isMissing ? "YES" : "NO" }}
                </td>
              </tr>
            </tbody>
          </table>
          <p v-else>N/A</p>
          <p v-else>N/A</p>
          <h6>Example values</h6>
          <ul v-if="variable.exampleValues">
            <li v-for="v in variable.exampleValues" :key="v">'{{ v }}'</li>
          </ul>
          <p v-else>N/A</p>
          <h6>This variable is repeated as:</h6>
          <table
            v-if="variable.repeats"
            class="table table-sm table-bordered m-2"
          >
            <thead>
              <th>table</th>
              <th>name</th>
              <th>collectionEvent</th>
            </thead>
            <tbody>
              <tr v-for="v in variable.repeats">
                <td>{{ v.table.name }}</td>
                <td>{{ v.name }}</td>
                <td>
                  {{ v.collectionEvent ? v.collectionEvent.name : "N/A" }}
                </td>
              </tr>
            </tbody>
          </table>
          <p v-else>N/A</p>
        </div>
        <div class="col"></div>
      </div>

      <h6>Mappings</h6>
      <table v-if="variable.mappings" class="table table-sm table-bordered">
        <thead>
          <tr>
            <th>fromTable</th>
            <th>fromVariables</th>
            <th>match</th>
            <th>description</th>
            <th>syntax</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in variable.mappings">
            <td>
              <RouterLink
                :to="{
                  name: 'tablemapping',
                  params: {
                    fromPid: m.fromRelease.resource.pid,
                    fromVersion: m.fromRelease.version,
                    fromTable: m.fromTable.name,
                    toPid: variable.release.resource.pid,
                    toVersion: variable.release.version,
                    toTable: variable.table.name,
                  },
                }"
              >
                {{ getType(m.fromRelease.resource.mg_tableclass) }}:
                {{ m.fromRelease.resource.pid }}
                {{ m.fromRelease.version }}, table: {{ m.fromTable.name }}
              </RouterLink>
            </td>

            <td>
              <div v-for="v in m.fromVariable" :key="v.name">
                <RouterLink
                  :to="{
                    name: 'tablemapping',
                    params: {
                      fromPid: m.fromRelease.resource.pid,
                      version: m.fromRelease.version,
                      table: m.fromTable.name,
                      name: v.name,
                    },
                  }"
                  >{{ v.name }}
                </RouterLink>
              </div>
            </td>
            <td>{{ m.match ? m.match.name : "" }}</td>
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
import {
  MessageError,
  TableExplorer,
  TableSimple,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import OntologyTerms from "../components/OntologyTerms";

export default {
  components: {
    OntologyTerms,
    MessageError,
    TableExplorer,
    TableSimple,
  },
  props: {
    pid: String,
    version: String,
    table: String,
    name: String,
  },
  data() {
    return {
      graphqlError: null,
      variable: null,
    };
  },
  computed: {
    resourceType() {
      if (this.variable.release) {
        return this.getType(this.variable.release.resource.mg_tableclass);
      }
    },
  },
  methods: {
    getType(mg_tableclass) {
      return mg_tableclass.split(".")[1];
    },
    reload() {
      request(
        "graphql",
        `query Variables($pid:String,$version:String,$table:String,$name:String){Variables(filter:{release:{version:{equals:[$version]},resource:{pid:{equals:[$pid]}}},table:{name:{equals:[$table]}},name:{equals:[$name]}})
        {name,table{name},repeats{name,table{name},collectionEvent{name}},format{name},vocabularies{name,definition,ontologyTermURI},mandatory,unit{name,definition,ontologyTermURI},exampleValues,permittedValues{value,label,isMissing},release{version,resource{pid,name,mg_tableclass}},description,label,keywords{name,ontologyTermURI,definition}
                mappings{description,syntax,match{name}fromTable{name}fromVariable{name}fromRelease{resource{pid,mg_tableclass}version}}}}`,
        {
          pid: this.pid,
          version: this.version,
          table: this.table,
          name: this.name,
        }
      )
        .then((data) => {
          this.variable = data.Variables[0];
        })
        .catch((error) => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
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
