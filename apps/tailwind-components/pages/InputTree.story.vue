<script setup lang="ts">
import type { ITreeNode } from "~/types/types";

function generateTreeData(width: number, depth: number, parentName?: string) {
  const nodes = [];
  for (let i = 0; i < width; i++) {
    const name = parentName ? parentName + `.${i}` : `Node ${i}`;
    const node: ITreeNode = {
      name,
      children: depth > 0 ? generateTreeData(width, depth - 1, name) : [],
      description: `Description for ${name}`,
    };

    nodes.push(node);
  }
  return nodes;
}

const width = 2;
const depth = 3;
const nodes = generateTreeData(width, depth);

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
    <div class="flex-1 h-12">
      <InputTree
        :nodes="nodes"
        v-model="selectedNodesNames"
        :expandSelected="true"
        class="p-4"
        :class="inverted ? 'bg-white' : 'bg-blue-500'"
        :inverted="inverted"
      />
    </div>

    <div class="h-12 p-4">
      <div>
        <input id="inverted" type="checkbox" v-model="inverted" />
        <label class="ml-1" for="inverted">inverted colors</label>
      </div>
      <div>
        <button
          @click="clearSelection"
          class="bg-blue-500 hover:bg-blue-700 text-white py-2 px-4 rounded"
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
