<template>
    <div class="container bg-white">
        <div class="p-2 bg-dark text-white mb-3">
            <h6>
                <RouterLink class="text-white" to="/">
                    home
                </RouterLink>
                /
                <RouterLink class="text-white" to="/list/TableMappings">
                    tablemappings
                </RouterLink>
                /
            </h6>
        </div>
        <h4>Table mapping</h4>
        <MessageError v-if="graphqlError">
            {{ graphqlError }}
        </MessageError>
        <table class="table table-bordered">
            <tr>
                <td><h6>Target table:&nbsp;</h6></td>
                <td colspan="4">
                    <RouterLink
                        :to="{
                            name: 'table',
                            params: { acronym: toAcronym, version: toVersion, name: toTable },
                        }"
                    >
                        {{ toTable }}
                    </RouterLink>
                    within release
                    <RouterLink
                        :to="{
                            name: 'release',
                            params: { acronym: toAcronym, version: toVersion },
                        }"
                    >
                        {{ toAcronym }}
                        {{ toVersion }}
                    </RouterLink>
                </td>
            </tr>
            <tr>
                <td><h6>Origin table:&nbsp;</h6></td>
                <td colspan="4">
                    <RouterLink
                        :to="{
                            name: 'table',
                            params: {
                                acronym: fromAcronym,
                                version: fromVersion,
                                name: fromTable,
                            },
                        }"
                    >
                        {{ fromTable }}
                    </RouterLink>
                    within release
                    <RouterLink
                        :to="{
                            name: 'release',
                            params: { acronym: fromAcronym, version: fromVersion },
                        }"
                    >
                        {{ fromAcronym }}
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
            <tr v-for="m in variablemappings">
                <td v-if="m.toVariable">
                    <RouterLink
                        :to="{
                            name: 'variable',
                            params: {
                                acronym: toAcronym,
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
                    <RouterLink
                        v-if="m.fromVariable"
                        :to="{
                            name: 'variable',
                            params: {
                                acronym: fromAcronym,
                                version: fromVersion,
                                table: fromTable,
                                name: m.fromVariable.name,
                            },
                        }"
                    >
                        {{ m.fromVariable.name }}
                    </RouterLink>
                </td>
                <td>{{ m.description }}</td>
                <td>{{ m.syntax }}</td>
                <td />
            </tr>
        </table>
        <br>
    </div>
</template>

<script>
import { request } from "graphql-request";

import { MessageError } from "@/components/ui/index.js";
export default {
  components: {
    MessageError,
  },
  props: {
    fromAcronym: String,
    fromVersion: String,
    fromTable: String,
    toAcronym: String,
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
  created() {
    this.reload();
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
        `
query TableMappings($fromAcronym:String,$fromVersion:String,$fromTable:String,$toAcronym:String,$toVersion:String,$toTable:String)
{
  TableMappings(filter:{
  fromRelease:{version:{equals:[$fromVersion]},resource:{acronym:{equals:[$fromAcronym]}}},fromTable:{name:{equals:[$fromTable]}},
  toRelease:{version:{equals:[$toVersion]},resource:{acronym:{equals:[$toAcronym]}}},toTable:{name:{equals:[$toTable]}}
  })
  {
    description
  },
  VariableMappings(filter:{
  fromRelease:{version:{equals:[$fromVersion]},resource:{acronym:{equals:[$fromAcronym]}}},fromTable:{name:{equals:[$fromTable]}},
  toRelease:{version:{equals:[$toVersion]},resource:{acronym:{equals:[$toAcronym]}}},toTable:{name:{equals:[$toTable]}}
  })
  {
    description,fromVariable{name},toVariable{name},syntax
  }
}`,
        {
          fromAcronym: this.fromAcronym,
          fromVersion: this.fromVersion,
          fromTable: this.fromTable,
          toAcronym: this.toAcronym,
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
};
</script>
