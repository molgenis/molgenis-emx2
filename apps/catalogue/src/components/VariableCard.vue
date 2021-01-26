<template>
  <div class="card">
    <div class="card-body">
      <h5 class="card-title">{{ variable.name }}</h5>
      <dl class="card-text">
        <dt>label</dt>
        <dd>{{ variable.label }}</dd>
        <div v-if="!datasetName">
          <dt>Collection</dt>
          <dd>
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
          </dd>
          <dt>dataset</dt>
          <dd>
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
          </dd>
        </div>
        <dt>format</dt>
        <dd>
          {{ variable.format ? variable.format.name : "N/A" }}
        </dd>
        <dt>unit</dt>
        <dd>{{ variable.unit ? variable.unit.name : "N/A" }}</dd>
        <span v-if="variable.description">
          <dt>description</dt>
          <dd>
            {{ variable.description }}
          </dd>
        </span>
        <span v-if="variable.harmonisations">
          <dt>harmo</dt>
          <dd class="p-1">
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
          </dd>
        </span>
      </dl>
    </div>
  </div>
</template>

<script>
import HarmonisationDetails from "./HarmonisationDetails";

export default {
  props: {
    variable: Object,
    datasetName: String,
  },
  components: { HarmonisationDetails },
};
</script>
