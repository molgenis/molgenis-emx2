<template>
  <div>
    <TreeComponent
      facetIdentifier="demo"
      v-model="selectedNodesNames"
      :options="options"
      :parentSelected="false"
      style="border: 1px solid red"
    />

    <div>
      <h3>Selected nodes:</h3>
      <ul>
        <li v-for="name in selectedNodesNames" :key="name">{{ name }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import TreeComponent from "../components/filters/base/TreeComponent.vue";
import TreeBranchComponent from "../components/filters/base/TreeBranchComponent.vue";
import type { IOntologyItem } from "../types/ontologyItem";
import { ref } from "vue";

const selectedNodesNames: Ref<string[]> = ref(["0.0.0"]);

const options: IOntologyItem[] = [
  {
    name: "0",
    label: "0",

    parent: [],
    children: [
      {
        name: "0.0",
        label: "0.0",
        parent: [{ name: "0" }],
        children: [
          {
            name: "0.0.0",
            label: "0.0.0",
            parent: [{ name: "0.0" }],
          },
        ],
      },
      {
        name: "0.1",
        label: "0.1",
        parent: [{ name: "0" }],
      },
    ],
  },
];
</script>
