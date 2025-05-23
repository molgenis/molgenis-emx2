<script setup lang="ts">
import { type Ref, ref } from "vue";
import type { ITreeNode } from "../../types/types";

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
  <h1 class="text-heading-2xl pb-4">InputTree</h1>
  <div class="flex flex-row gap-2 mb-4">
    <div class="basis-3/5">
      <InputTree
        id="tree-story-input"
        :nodes="nodes"
        v-model="selectedNodesNames"
        :expandSelected="expandSelected"
        class="p-4"
        :class="inverted ? 'bg-white' : 'bg-sidebar-gradient'"
        :inverted="inverted"
        :emitSelectedChildren="emitSelectedChildren"
      />
    </div>

    <div class="basis-2/5 p-2 text-title">
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
          <label class="ml-1 hover:cursor-pointer" for="tree-emit-children">
            emitSelectedChildren
          </label>
        </div>
      </fieldset>

      <div class="mb-2">
        <Button @click="clearSelection" type="outline" size="small">
          Clear selection
        </Button>
      </div>
      <hr />
      <div class="my-2">
        Number off selected nodes: {{ selectedNodesNames.length }}
      </div>
      <div>
        <h2 class="font-bold">Selected nodes:</h2>
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
