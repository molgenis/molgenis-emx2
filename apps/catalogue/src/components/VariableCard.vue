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
          ">
          <RouterLink
            :to="{
              name: 'databank',
              params: {
                databankid: variable.release.resource.id,
              },
            }">
            {{ variable.release.resource.id }}
          </RouterLink>
        </Property>
        <Property v-if="!tableName" label="table">
          <RouterLink
            :to="{
              name: 'table',
              params: {
                resourceId: variable.resource.id,
                tableName: variable.dataset.name,
              },
            }">
            {{ variable.dataset.name }}
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
            variable.topics ? variable.topics.map(t => t.name).join(",") : ""
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
            :sourceCollection="h.resource.id"
            :source-dataset="h.sourceDataset.name"
            :target-resource="variable.resource.id"
            :target-dataset="variable.dataset.name"
            :target-variable="variable.name"
            :match="variable.match ? variable.match.name : 'unknown'" />
        </Property>
      </div>
    </div>
  </div>
</template>

<script>
import HarmonisationDetails from "./HarmonisationDetails.vue";
import Property from "./Property.vue";

export default {
  props: {
    variable: Object,
    tableName: String,
  },
  components: { HarmonisationDetails, Property },
};
</script>
