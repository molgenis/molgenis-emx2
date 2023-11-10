<template>
  <div class="container bg-white">
    <div class="p-2 bg-dark text-white mb-3">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="TableMappings" class="text-white">
          datasetmappings
        </RouterLink>
        /
      </h6>
    </div>
    <h4>Dataset mapping</h4>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <table class="table table-bordered">
      <tr>
        <td><h6>Source table:&nbsp;</h6></td>
        <td colspan="5">
          <RouterLink
            :to="{
              name: 'Datasets-details',
              params: {
                resource: source,
                name: sourceDataset,
              },
            }"
            >{{ sourceDataset }}
          </RouterLink>
        </td>
      </tr>
      <tr>
        <td><h6>Source table:&nbsp;</h6></td>
        <td colspan="5">
          <RouterLink
            :to="{
              name: 'Datasets-details',
              params: { resource: target, name: targetDataset },
            }"
            >{{ targetDataset }}
          </RouterLink>
        </td>
      </tr>

      <tr>
        <td colspan="6">
          <h6>Description:</h6>
          {{
            datasetmappings && datasetmappings.description
              ? datasetmappings.description
              : "N/A"
          }}
          <RowButtonEdit
            v-if="datasetmappings"
            id="row-button-edit-dataset"
            tableId="DatasetMappings"
            :pkey="pkeyDataset(datasetmappings)"
            :visibleColumns="['description']"
            @close="handleModalClose"
          />
          <br />
          <br />
          <a :href="downloadURLcsv">Download ETL as csv</a><br />
          <a :href="downloadURLexcel">Download ETL as Excel</a><br />
        </td>
      </tr>
      <tr>
        <th v-if="canEdit">#</th>
        <th><h6>Target column</h6></th>
        <th><h6>Source column</h6></th>
        <th><h6>Description</h6></th>
        <th><h6>Syntax</h6></th>
        <th><h6>Notes</h6></th>
      </tr>
      <tr v-for="(m, index) in variablemappings">
        <td v-if="canEdit">
          <div class="d-flex flex-row">
            <RowButtonEdit
              id="'row-button-edit' + index"
              tableId="VariableMappings"
              :pkey="pkey(m)"
              :visibleColumns="[
                'description',
                'syntax',
                'comments',
                'source variables',
              ]"
              @close="handleModalClose"
            />
          </div>
        </td>
        <td v-if="m.targetVariable">
          <RouterLink
            :to="{
              name: 'Variables-details',
              params: {
                resource: target,
                dataset: targetDataset,
                name: m.targetVariable.name,
              },
            }"
          >
            {{ m.targetVariable.name }}
          </RouterLink>
        </td>
        <td>
          <div v-if="m.sourceVariables">
            <RouterLink
              v-for="v in m.sourceVariables"
              :key="v.name"
              :to="{
                name: 'Variables-details',
                params: {
                  resource: source,
                  dataset: sourceDataset,
                  name: v.name,
                },
              }"
            >
              {{ sourceDataset }}.{{ v.name }}
            </RouterLink>
          </div>
          <div v-if="m.sourceVariablesOtherDataset">
            <RouterLink
              v-for="v in m.sourceVariablesOtherDataset"
              :key="v.name"
              :to="{
                name: 'Variables-details',
                params: {
                  resource: source,
                  dataset: v.dataset.name,
                  name: v.name,
                },
              }"
            >
              {{ v.dataset.name }}.{{ v.name }}
            </RouterLink>
          </div>
        </td>
        <td>{{ m.description }}</td>
        <td>{{ m.syntax }}</td>
        <td></td>
      </tr>
    </table>
    <br />

    <!--{{ tablemapping }} -->

    <!-- {{ variablemappings }} -->
  </div>
</template>

<script>
import { request, gql } from "graphql-request";
import { MessageError, RowButtonEdit, EditModal } from "molgenis-components";
import { mapActions, mapGetters } from "vuex";

export default {
  components: {
    EditModal,
    MessageError,
    RowButtonEdit,
  },
  props: {
    source: String,
    sourceDataset: String,
    target: String,
    targetDataset: String,
  },
  data() {
    return {
      graphqlError: null,
      datasetmappings: null,
      variablemappings: null,
    };
  },
  computed: {
    ...mapGetters(["canEdit"]),
    downloadURLcsv() {
      return (
        "../api/csv/Variable mappings?filter=" +
        JSON.stringify(this.downloadFilter)
      );
    },
    downloadURLexcel() {
      return (
        "../api/excel/Variable mappings?filter=" +
        JSON.stringify(this.downloadFilter)
      );
    },
    downloadFilter() {
      return {
        sourceDataset: {
          equals: [{ resource: { id: this.source }, name: this.sourceDataset }],
        },
        targetDataset: {
          equals: [{ resource: { id: this.target }, name: this.targetDataset }],
        },
      };
    },
    resourceType() {
      if (this.datasetmapping.source) {
        return this.datasetmapping.source.mg_tableclass
          .split(".")[1]
          .slice(0, -1);
      }
    },
    defaultValueMapping() {
      return {
        source: { id: this.source },
        sourceDataset: {
          resource: { id: this.source },
          name: this.sourceDataset,
        },
        target: { id: this.target },
        targetDataset: {
          resource: { id: this.target },
          name: this.targetDataset,
        },
      };
    },
  },
  methods: {
    ...mapActions(["reloadMetadata"]),
    pkeyDataset(datasetmapping) {
      return {
        source: datasetmapping.source,
        sourceDataset: {
          resource: datasetmapping.source,
          name: datasetmapping.sourceDataset.name,
        },
        target: datasetmapping.target,
        targetDataset: {
          resource: datasetmapping.target,
          name: datasetmapping.targetDataset.name,
        },
      };
    },
    pkey(mapping) {
      return {
        source: mapping.source,
        sourceDataset: {
          resource: mapping.source,
          name: mapping.sourceDataset.name,
        },
        target: mapping.target,
        targetDataset: {
          resource: mapping.target,
          name: mapping.targetDataset.name,
        },
        targetVariable: {
          resource: mapping.target,
          dataset: {
            resource: mapping.target,
            name: mapping.targetDataset.name,
          },
          name: mapping.targetVariable.name,
        },
      };
    },
    handleModalClose() {
      this.reload();
    },
    getType(mg_tableclass) {
      return mg_tableclass.split(".")[1].slice(0, -1);
    },
    openVariable(row) {
      this.$router.push({
        name: "variable",
        params: {
          resource: row.resource,
          dataset: row.dataset,
          name: row.name,
        },
      });
    },
    reload() {
      request(
        "graphql",
        gql`
          query MyMappings(
            $source: String
            $sourceDataset: String
            $target: String
            $targetDataset: String
          ) {
            DatasetMappings(
              filter: {
                sourceDataset: {
                  equals: { resource: { id: $source }, name: $sourceDataset }
                }
                targetDataset: {
                  equals: { resource: { id: $target }, name: $targetDataset }
                }
              }
            ) {
              target {
                id
              }
              targetDataset {
                name
              }
              source {
                id
              }
              sourceDataset {
                name
              }
              description
              syntax
            }
            VariableMappings(
              filter: {
                sourceDataset: {
                  equals: { resource: { id: $source }, name: $sourceDataset }
                }
                targetDataset: {
                  equals: { resource: { id: $target }, name: $targetDataset }
                }
              }
            ) {
              source {
                id
              }
              sourceDataset {
                name
              }
              sourceVariables {
                name
              }
              sourceVariablesOtherDatasets {
                dataset {
                  name
                }
                name
              }
              target {
                id
              }
              targetDataset {
                name
              }
              targetVariable {
                name
              }
              description
              syntax
              status {
                name
              }
              match {
                name
              }
            }
          }
        `,
        {
          source: this.source,
          sourceDataset: this.sourceDataset,
          target: this.target,
          targetDataset: this.targetDataset,
        }
      )
        .then((data) => {
          this.datasetmappings = data.DatasetMappings[0];
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
    this.reloadMetadata();
    this.reload();
  },
};
</script>
