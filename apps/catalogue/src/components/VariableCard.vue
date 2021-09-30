<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">{{ variable.name }}</h5>
      <div class="card-text">
        <Property label="label">{{ variable.label }}</Property>
        <Property
          v-if="!tableName"
          :label="
            variable.release.resource.mg_tableclass.includes('Project')
              ? 'Project'
              : 'Databank'
          "
        >
          <RouterLink
            :to="{
              name: 'databank',
              params: {
                databankPid: variable.release.resource.pid,
              },
            }"
          >
            {{ variable.release.resource.pid }}
          </RouterLink>
        </Property>
        <Property v-if="!tableName" label="table">
          <RouterLink
            :to="{
              name: 'table',
              params: {
                databankPid: variable.release.resource.pid,
                tableName: variable.table.name,
              },
            }"
          >
            {{ variable.table.name }}
          </RouterLink>
        </Property>
        <Property label="format">
          {{ variable.format ? variable.format.name : "" }}
        </Property>
        <Property label="unit">
          {{ variable.unit ? variable.unit.name : "" }}
        </Property>
        <Property label="topics">
          {{
            variable.topics ? variable.topics.map((t) => t.name).join(",") : ""
          }}
        </Property>
        <Property label="description">
          {{ variable.description }}
        </Property>
        <Property label="categories">
          <table v-if="variable.categories" class="table table-sm">
            <thead>
              <th scope="col">value</th>
              <th scope="col">label</th>
              <th scope="col">isMissing</th>
            </thead>
            <tr v-for="category in variable.categories">
              <td>{{ category.value }}</td>
              <td>{{ category.label }}</td>
              <td>{{ category.isMissing }}</td>
            </tr>
          </table>
        </Property>
        <Property label="harmonisations">
          <HarmonisationDetails
            v-for="h in variable.harmonisations"
            :key="JSON.stringify(h)"
            :sourceCollection="h.sourceRelease.resource.pid"
            :source-table="h.sourceTable.name"
            :source-version="h.sourceRelease.version"
            :target-resource="variable.release.resource.pid"
            :target-version="variable.release.version"
            :target-table="variable.table.name"
            :target-variable="variable.name"
            :match="variable.match ? variable.match.name : 'unknown'"
          />
        </Property>
      </div>
    </div>
  </div>
</template>

<script>
import HarmonisationDetails from "./HarmonisationDetails";
import Property from "./Property";

export default {
  props: {
    variable: Object,
    tableName: String,
  },
  components: { HarmonisationDetails, Property },
};
</script>
