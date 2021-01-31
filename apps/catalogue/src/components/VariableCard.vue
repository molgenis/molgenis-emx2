<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">{{ variable.name }}</h5>
      <div class="card-text">
        <Property label="label">{{ variable.label }}</Property>
        <Property
          v-if="!tableName"
          :label="
            variable.table.collection.mg_tableclass.includes('Consort')
              ? 'Consortium'
              : 'Databank'
          "
        >
          <RouterLink
            :to="{
              name: 'databank',
              params: {
                databankAcronym: variable.table.collection.acronym,
              },
            }"
          >
            {{ variable.table.collection.acronym }}
          </RouterLink>
        </Property>
        <Property v-if="!tableName" label="table">
          <RouterLink
            :to="{
              name: 'table',
              params: {
                databankAcronym: variable.table.collection.acronym,
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
        <Property label="description">
          {{ variable.description }}
        </Property>
        <Property label="categories">
          <table v-if="variable.categories" class="table table-sm">
            <thead>
              <th>value</th>
              <th>label</th>
              <th>isMissing</th>
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
            :sourceCollection="h.sourceTable.collection.acronym"
            :source-table="h.sourceTable.name"
            :target-collection="variable.table.collection.acronym"
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
