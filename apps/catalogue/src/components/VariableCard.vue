<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">{{ variable.name }}</h5>
      <div class="card-text">
        <Property label="label">{{ variable.label }}</Property>
        <Property v-if="!datasetName" label="Collection">
          <RouterLink
            :to="{
              name: 'collection',
              params: {
                collectionAcronym: variable.dataset.collection.acronym,
              },
            }"
          >
            {{ variable.dataset.collection.acronym }}
          </RouterLink>
        </Property>
        <Property v-if="!datasetName" label="dataset">
          <RouterLink
            :to="{
              name: 'dataset',
              params: {
                collectionAcronym: variable.dataset.collection.acronym,
                datasetName: variable.dataset.name,
              },
            }"
          >
            {{ variable.dataset.name }}
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
            :sourceCollection="h.sourceDataset.collection.acronym"
            :source-dataset="h.sourceDataset.name"
            :target-collection="variable.dataset.collection.acronym"
            :target-dataset="variable.dataset.name"
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
    datasetName: String,
  },
  components: { HarmonisationDetails, Property },
};
</script>
