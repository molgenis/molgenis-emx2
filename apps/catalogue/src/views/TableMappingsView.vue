<template>
  <div class="container bg-white">
    <div class="p-2 bg-dark text-white mb-3">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="/list/TableMappings" class="text-white">
          tablemappings
        </RouterLink>
        /
      </h6>
    </div>
    <h4>Table mapping</h4>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <table class="table table-bordered">
      <tr>
        <td><h6>Target table:&nbsp;</h6></td>
        <td colspan="4">
          <RouterLink
            :to="{
              name: 'Tables-details',
              params: { pid: toPid, version: toVersion, name: toTable },
            }"
            >{{ toTable }}
          </RouterLink>
          within release
          <RouterLink
            :to="{
              name: 'Releases-details',
              params: { pid: toPid, version: toVersion },
            }"
            >{{ toPid }}
            {{ toVersion }}
          </RouterLink>
        </td>
      </tr>
      <tr>
        <td><h6>Origin table:&nbsp;</h6></td>
        <td colspan="4">
          <RouterLink
            :to="{
              name: 'Tables-details',
              params: {
                pid: fromPid,
                version: fromVersion,
                name: fromTable,
              },
            }"
            >{{ fromTable }}
          </RouterLink>
          within release
          <RouterLink
            :to="{
              name: 'Releases-details',
              params: { pid: fromPid, version: fromVersion },
            }"
            >{{ fromPid }}
            {{ fromVersion }}
          </RouterLink>
        </td>
      </tr>

      <tr>
        <td colspan="5">
          <h6>Action:</h6>
          {{
            tablemapping && tablemapping.description
              ? tablemapping.description
              : "N/A"
          }}
        </td>
      </tr>
      <tr>
        <th><h6>Target column</h6></th>
        <th><h6>Origin column</h6></th>
        <th><h6>Rule</h6></th>
        <th><h6>Syntax</h6></th>
        <th><h6>Notes</h6></th>
      </tr>
      <tr v-for="(m, index) in variablemappings" :key="index">
        <td v-if="m.toVariable">
          <RouterLink
            :to="{
              name: 'TargetVariables-details',
              params: {
                pid: toPid,
                version: toVersion,
                table: toTable,
                name: m.toVariable.name,
              },
            }"
          >
            {{ m.toVariable.name }}
          </RouterLink>
        </td>
        <td>
          <div v-if="m.fromVariable">
            <RouterLink
              v-for="v in m.fromVariable"
              :key="v.name"
              :to="{
                name: 'SourceVariables-details',
                params: {
                  pid: fromPid,
                  version: fromVersion,
                  table: fromTable,
                  name: m.fromVariable.name,
                },
              }"
            >
              {{ fromTable }}.{{ v.name }}
            </RouterLink>
          </div>
          <div v-if="m.fromVariablesOtherTables">
            <RouterLink
              v-for="v in m.fromVariablesOtherTables"
              :key="v.name"
              :to="{
                name: 'SourceVariables-details',
                params: {
                  pid: fromPid,
                  version: fromVersion,
                  table: v.table.name,
                  name: v.name,
                },
              }"
            >
              {{ v.table.name }}.{{ v.name }}
            </RouterLink>
          </div>
        </td>
        <td>{{ m.description }}</td>
        <td>{{ m.syntax }}</td>
        <td></td>
      </tr>
    </table>
    <br />

    <!--{{ tablemapping }}

    {{ variablemappings }}-->
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
    fromPid: String,
    fromVersion: String,
    fromTable: String,
    toPid: String,
    toVersion: String,
    toTable: String,
  },
  data() {
    return {
      graphqlError: null,
      tablemapping: null,
      variablemappings: null,
    };
  },
  computed: {
    resourceType() {
      if (this.tablemapping.release) {
        return this.tablemapping.release.resource.mg_tableclass
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
          pid: this.pid,
          version: this.version,
          table: this.name,
          name: row.name,
        },
      });
    },
    reload() {
      request(
        "graphql",
        `
query TableMappings($fromPid:String,$fromVersion:String,$fromTable:String,$toPid:String,$toVersion:String,$toTable:String)
{
  TableMappings(filter:{
  fromDataDictionary:{version:{equals:[$fromVersion]},resource:{pid:{equals:[$fromPid]}}},fromTable:{name:{equals:[$fromTable]}},
  toDataDictionary:{version:{equals:[$toVersion]},resource:{pid:{equals:[$toPid]}}},toTable:{name:{equals:[$toTable]}}
  })
  {
    description
  },
  VariableMappings(filter:{
  fromDataDictionary:{version:{equals:[$fromVersion]},resource:{pid:{equals:[$fromPid]}}},fromTable:{name:{equals:[$fromTable]}},
  toDataDictionary:{version:{equals:[$toVersion]},resource:{pid:{equals:[$toPid]}}},toTable:{name:{equals:[$toTable]}}
  })
  {
    description,fromVariable{name},toVariable{name},syntax,fromVariablesOtherTables{table{name},name}
  }
}`,
        {
          fromPid: this.fromPid,
          fromVersion: this.fromVersion,
          fromTable: this.fromTable,
          toPid: this.toPid,
          toVersion: this.toVersion,
          toTable: this.toTable,
        }
      )
        .then((data) => {
          if (data.TableMappings) this.tablemapping = data.TableMappings[0];
          this.variablemappings = data.VariableMappings;
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
