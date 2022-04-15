<template>
  <demo-item id="input-ontology" label="Input ontology">
    <div>
      <InputOntology
        id="input-ontology-1"
        v-model="value"
        label="My ontology select"
        description="please choose your options in tree below"
        :options="[
          { name: 'pet' },
          { name: 'cat', parent: { name: 'pet' } },
          { name: 'dog', parent: { name: 'pet' } },
          { name: 'cattle' },
          { name: 'cow', parent: { name: 'cattle' } },
        ]"
        :isMultiSelect="true"
      />
      <div>You selected: {{ value }}</div>
    </div>

    <div class="mt-3">
      <InputOntology
        id="input-ontology-2"
        v-model="value"
        label="My ontology select expanded"
        :showExpanded="true"
        description="please choose your options in tree below"
        :options="[
          { name: 'pet' },
          { name: 'cat', parent: { name: 'pet' } },
          { name: 'dog', parent: { name: 'pet' } },
          { name: 'cattle' },
          { name: 'cow', parent: { name: 'cattle' } },
        ]"
        :isMultiSelect="true"
      />
      <div>You selected: {{ value }}</div>
    </div>

    <div class="mt-3">
      <InputOntology
        id="input-ontology-3"
        label="Ontology select with backend data"
        description="please choose your options in tree below"
        v-model="value"
        :isMultiSelect="true"
        ontologyTableName="Tag"
        graphqlURL="/pet store/graphql"
      />
    </div>
  </demo-item>
</template>
<script>
export default {
  data: function () {
    return {
      value: null,
    };
  },
};
</script>