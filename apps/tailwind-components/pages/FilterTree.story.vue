<script setup lang="ts">
import type { ITreeNode } from '~/types/types';


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

const selectedNodes = ref([]);

</script>

<template>
  
 
  <div class="flex mb-4">
    <div class="flex-1  h-12"><FilterTree :rootNodes="rootNodes" v-model="selectedNodes" :expandSelected="true" class=" bg-blue-500 p-4" /></div>
    <div class="flex-1  h-12 p-4"> selected nodes: {{ selectedNodes.map((n: ITreeNode) => n.name) }}</div>
  </div>
</template>