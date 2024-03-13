<script setup lang="ts">
import type { ITreeNode } from "~/types/types";

function generateTreeData(width: number, depth: number, parentName?: string) {
  const nodes = [];
  for (let i = 0; i < width; i++) {
    const name = parentName ? parentName + `.${i}` : `Node ${i}`;
    const node: ITreeNode = {
      name,
      selected: false,
      expanded: false,
      children: depth > 0 ? generateTreeData(width, depth - 1, name) : [],
    };

    nodes.push(node);
  }
  return nodes;
}

const width = 2;
const depth = 3;
const rootNodes = generateTreeData(width, depth);

const selectedNodes: Ref<ITreeNode[]> = ref([]);

const clearSelection = () => {
  selectedNodes.value = [];
};

const deselect = (node: ITreeNode) => {
  selectedNodes.value = selectedNodes.value.filter((n) => n !== node);
};
</script>

<template>
  <div class="flex mb-4">
    <div class="flex-1 h-12">
      <InputTree
        :rootNodes="rootNodes"
        v-model="selectedNodes"
        :expandSelected="true"
        class="bg-blue-500 p-4"
      />
    </div>

    <div class="h-12 p-4">
      <div>
        Number off selected nodes: {{ selectedNodes.length }}
        <button
          @click="clearSelection"
          class="bg-blue-500 hover:bg-blue-700 text-white py-2 px-4 rounded"
        >
          Clear selection
        </button>
      </div>
      <div>
        Selected nodes:
        <ul>
          <li v-for="node in selectedNodes">
            {{ node.name }}
            <button
              @click="deselect(node)"
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
