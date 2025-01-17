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
const expandSelected = ref(true);
const emitSelectedChildren = ref(true);
</script>

<template>
  <div class="flex flex-row gap-2 mb-4">
    <div class="basis-3/5">
      <InputTree
        :nodes="nodes"
        v-model="selectedNodesNames"
        :expandSelected="expandSelected"
        class="p-4"
        :class="inverted ? 'bg-white' : 'bg-sidebar-gradient'"
        :inverted="inverted"
        :emitSelectedChildren="emitSelectedChildren"
      />
    </div>

    <div class="basis-2/5 p-2">
      <fieldset class="border border-gray-900 mb-2">
        <legend class="m-2 px-2">Props</legend>
        <div class="mb-2">
          <input
            id="tree-expand-selected"
            class="ml-2 hover:cursor-pointer"
            type="checkbox"
            v-model="expandSelected"
          />
          <label class="ml-1 hover:cursor-pointer" for="tree-expand-selected">
            expandSelected
          </label>
        </div>
        <div class="mb-2">
          <input
            id="tree-inverted"
            class="ml-2 hover:cursor-pointer"
            type="checkbox"
            v-model="inverted"
          />
          <label class="ml-1 hover:cursor-pointer" for="tree-inverted">
            inverted
          </label>
        </div>
        <div class="mb-2">
          <input
            id="tree-emit-children"
            class="ml-2 hover:cursor-pointer"
            type="checkbox"
            v-model="emitSelectedChildren"
          />
          <label class="ml-1 hover:cursor-pointer" for="tree-inverted">
            emitSelectedChildren
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
