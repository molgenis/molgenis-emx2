<script setup lang="ts">
import { type Ref, ref } from "vue";
import type { INode } from "../../types/types";

function generateListData(length: number) {
  const nodes = [];
  for (let i = 0; i < length; i++) {
    const name = `Node ${i}`;
    const node: INode = {
      name,
      description: `Description for ${name}`,
    };

    nodes.push(node);
  }
  return nodes;
}

const length = 7;
const nodes = generateListData(length);

const selectedNodesNames: Ref<string[]> = ref([]);

const clearSelection = () => {
  selectedNodesNames.value = [];
};

const deselect = (selectedNodeName: string) => {
  selectedNodesNames.value = selectedNodesNames.value.filter(
    (n) => n !== selectedNodeName
  );
};

const inverted = ref(false);
</script>

<template>
  <div class="flex mb-4">
    <div class="flex-1">
      <InputList
        :nodes="nodes"
        v-model="selectedNodesNames"
        class="p-4 text-current"
        :class="inverted ? 'bg-white' : 'bg-sidebar-gradient'"
        :inverted="inverted"
      />
    </div>

    <div class="h-12 ml-4 mt-2">
      <fieldset class="border border-gray-900 mb-2">
        <legend class="m-2 px-2">Props</legend>

        <div class="mb-2">
          <input
            id="tree-inverted"
            class="ml-2 hover:cursor-pointer"
            type="checkbox"
            v-model="inverted"
          />
          <label class="ml-1 hover:cursor-pointer" for="tree-inverted">
            inverted colors
          </label>
        </div>
      </fieldset>

      <div class="mb-2">
        <button
          @click="clearSelection"
          class="bg-orange-500 hover:bg-white py-2 px-4 rounded border border-gray-900"
        >
          Clear selection
        </button>
      </div>
      <hr />
      <div class="my-2">
        Number off selected nodes: {{ selectedNodesNames.length }}
      </div>
      <div>
        <h3 class="font-bold">Selected nodes:</h3>
        <ul>
          <li v-for="selectedNodeName in selectedNodesNames">
            {{ selectedNodeName }}
            <button
              @click="deselect(selectedNodeName)"
              class="bg-gray-400 hover:bg-gray-600 text-white py-0 px-1 mx-1 rounded"
            >
              x
            </button>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
