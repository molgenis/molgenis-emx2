<template>
  <div v-if="graphqlError">
    <MessageError>{{ graphqlError }}</MessageError>
  </div>
  <div v-else-if="dataset" class="container bg-white">
    <div class="p-2 bg-dark text-white mb-3">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="datasets" class="text-white"> datasets</RouterLink>
        /
      </h6>
    </div>
    <h6 class="d-inline">{{ resourceType }}:&nbsp;</h6>
    <RouterLink
        :to="{
        name: convertToPascalCase(resourceType) + '-details',
        params: { id: resource },
      }"
    >{{ dataset.resource.id }}
    </RouterLink>
    <h1>Dataset: {{ dataset.name }}</h1>
    <p>{{ dataset.description ? dataset.description : "Description: N/A" }}</p>

    <MessageError v-if="graphqlError"> {{ graphqlError }}</MessageError>
    <div v-if="dataset.mappedFrom">
      <h6>The following datasets are mapped to this standard:</h6>
      <ul>
        <li v-for="(m, index) in dataset.mappedFrom" :key="index">
          <RouterLink
            :to="{
              name: 'DatasetMappings-details',
              params: {
                source: m.source.id,
                sourceDataset: m.sourceDataset.name,
                target: m.target.id,
                targetDataset: m.targetDataset.name,
              },
            }"
          >
            <span>{{ m.source.id }} : {{ m.sourceDataset.name }} </span>
          </RouterLink>
        </li>
      </ul>
    </div>
    <h6>
      This dataset is mapped to the following standards:
      <RowButtonAdd
        id="add-mapping"
        table-id="Dataset Mappings"
        :default-value="{
          source: { id: dataset.resource.id },
          sourceDataset: {
            name: dataset.name,
            resource: { id: dataset.resource.id },
          },
        }"
        :visibleColumns="['target', 'target dataset']"
        @close="reload"
      />
    </h6>
    <div v-if="dataset.mappedTo">
      <ul>
        <li v-for="(m, index) in dataset.mappedTo" :key="index">
          <RouterLink
            :to="{
              name: 'DatasetMappings-details',
              params: {
                source: m.source.id,
                sourceDataset: m.sourceDataset.name,
                target: m.target.id,
                targetDataset: m.targetDataset.name,
              },
            }"
          >
            <span>{{ m.target.id }} : {{ m.targetDataset.name }} </span>
          </RouterLink>
        </li>
      </ul>
    </div>
    <div v-else>N/A</div>
    <h6>Variables</h6>
    <TableExplorer
      tableId="Variables"
      :showHeader="false"
      :showFilters="[]"
      :showColumns="['name', 'label', 'format', 'description', 'notes']"
      :showCards="true"
      :filter="{
        dataset: { equals: { resource: { id: resource }, name: name } },
      }"
      :canEdit="canEdit"
      :canManage="canManage"
      @rowClick="openVariable"
    />
  </div>
</template>
<script>
import { request, gql } from "graphql-request";
import { TableExplorer, MessageError, RowButtonAdd } from "molgenis-components";
import { mapActions, mapGetters } from "vuex";

export default {
  components: {
    MessageError,
    TableExplorer,
    RowButtonAdd,
  },
  props: {
    resource: String,
    name: String,
  },
  data() {
    return {
      graphqlError: null,
      dataset: null,
    };
  },
  computed: {
    ...mapGetters(["canEdit", "canManage"]),
    resourceType() {
      if (this.dataset) {
        return this.dataset.resource.mg_tableclass.split('.')[1];
      }
    },
  },
  methods: {
    ...mapActions(["reloadMetadata"]),
    openVariable(row) {
      this.$router.push({
        name: "Variables-details",
        params: {
          resource: this.resource,
          dataset: this.name,
          name: row.name,
        },
      });
    },
    reload() {
      request(
        "graphql",
        gql`
          query Datasets($resource: String, $name: String) {
            Datasets(
              filter: {
                resource: { id: { equals: [$resource] } }
                name: { equals: [$name] }
              }
            ) {
              name
              resource {
                id
                mg_tableclass
              }
              mappedFrom {
                source {
                  id
                }
                sourceDataset {
                  name
                }
                target {
                  id
                }
                targetDataset {
                  name
                }
              }
              mappedTo {
                source {
                  id
                }
                sourceDataset {
                  name
                }
                target {
                  id
                }
                targetDataset {
                  name
                }
              }
            }
          }
        `,
        {
          resource: this.resource,
          name: this.name,
        }
      )
        .then((data) => {
          this.dataset = data.Datasets[0];
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
    convertToPascalCase(string) {
      if (!string) return string;
      const words = string.trim().split(/\s+/);
      let result = "";
      words.forEach((word) => {
        result += word.charAt(0).toUpperCase();
        if (word.length > 1) {
          result += word.slice(1);
        }
      });
      return result;
    }
  },
  created() {
    this.reload();
  },
};
</script>
