<template>
  <div v-if="graphqlError">
    <MessageError>{{ graphqlError }}</MessageError>
  </div>
  <div v-else-if="dataset" class="container bg-white">
    {{ resource }} {{ name }}
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
        name: resourceType + '-details',
        params: { id: resource },
      }"
      >{{ dataset.resource.id }}
    </RouterLink>
    /
    <h1>Dataset: {{ dataset.name }}</h1>
    <p>{{ dataset.description ? dataset.description : "Description: N/A" }}</p>

    <MessageError v-if="graphqlError"> {{ graphqlError }}</MessageError>
    <h6>Mappings/ETLs</h6>
    <ul v-if="dataset.mappings">
      <li v-for="(m, index) in dataset.mappings" :key="index">
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
          <span> To: {{ m.target.id }} : {{ m.targetDataset.name }} </span>
        </RouterLink>
      </li>
    </ul>
    <p v-else>N/A</p>
    <h6>Variables</h6>
    <TableExplorer
      tableName="Variables"
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
import {
  TableExplorer,
  MessageError,
  convertToPascalCase,
} from "molgenis-components";
import { mapActions, mapGetters } from "vuex";

export default {
  components: {
    MessageError,
    TableExplorer,
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
        return convertToPascalCase(
          this.dataset.resource.mg_tableclass.split(".")[1]
        );
      }
    },
  },
  methods: {
    ...mapActions(["reloadMetadata"]),
    openVariable(row) {
      this.$router.push({
        name: "Variables-details",
        params: {
          id: this.resource,
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
              mappings {
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
  },
  created() {
    this.reload();
  },
};
</script>
