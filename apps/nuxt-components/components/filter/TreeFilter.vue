<script setup lang="ts">
import type { ITreeNode } from "../../types/types";
import TreeChild from "./treeComponents/TreeChild.vue";
import BaseIcon from "../BaseIcon.vue";
import CustomTooltip from "../CustomTooltip.vue";

const props = withDefaults(
  defineProps<{
    rootNodes?: ITreeNode[];
    selectedNodes: ITreeNode[];
    isMultiSelect?: boolean;
    mobileDisplay?: boolean;
  }>(),
  {
    isMultiSelect: true,
    mobileDisplay: false,
  }
);

function toggleExpand(node: ITreeNode) {
  node.expanded = !node.expanded;
}

function toggleSelect(node: ITreeNode) {
  console.log("toggleSelect", node);
}

function toggleDeselect(node: ITreeNode) {
  console.log("toggleDeselect", node);
}
</script>

<template>
  <ul class="text-search-filter-group-title">
    <li v-for="rootNode in rootNodes" :key="rootNode.name" class="mb-2.5">
      <div class="flex items-start">
        <span
          v-if="rootNode.children?.length"
          @click="toggleExpand(rootNode)"
          class="flex items-center justify-center w-6 h-6 rounded-full hover:bg-search-filter-group-toggle hover:cursor-pointer"
          :class="{
            'rotate-180': !rootNode.expanded,
            'text-search-filter-group-toggle-mobile': mobileDisplay,
            'text-search-filter-group-toggle': !mobileDisplay,
          }"
        >
          <BaseIcon name="caret-up" :width="20" />
        </span>
        <span
          v-else
          class="flex items-center justify-center w-6 h-6 rounded-full"
          :class="`text-search-filter-group-toggle${
            mobileDisplay ? '-mobile' : ''
          }`"
        >
        </span>
        <div class="flex items-center">
          <input
            type="checkbox"
            :id="rootNode.name"
            :name="rootNode.name"
            :checked="rootNode.selected"
            @click.stop="toggleSelect(rootNode)"
            
            class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 text-search-filter-group-checkbox border border-checkbox hover:cursor-pointer"
          />
        </div>
        <label
          :for="rootNode.name"
          class="hover:cursor-pointer text-body-sm group"
        >
          <span class="group-hover:underline">{{ rootNode.name }}</span>
        </label>
        <div class="inline-flex items-center whitespace-nowrap">
          <div class="inline-block pl-1">
            <CustomTooltip
              v-if="rootNode.description"
              label="Read more"
              hoverColor="white"
              :content="rootNode.description"
            />
          </div>
        </div>
      </div>

      <ul
        class="ml-10 mr-4"
        :class="{ hidden: !rootNode.expanded }"
        v-if="rootNode.children"
      >
        <TreeChild
          :nodes="rootNode.children"
          @select="toggleSelect"
          @deselect="toggleDeselect"
        />
      </ul>
    </li>
  </ul>
</template>
