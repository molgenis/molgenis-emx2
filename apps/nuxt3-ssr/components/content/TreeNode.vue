<script setup lang="ts">
import type { IOntologyItem } from "meta-data-utils";

const { node, collapseAll } = withDefaults(
  defineProps<{
    node: IOntologyItem;
    isRootNode?: boolean;
    collapseAll?: boolean;
  }>(),
  {
    collapseAll: true,
    isRootNode: false,
  }
);

let collapsed = ref(collapseAll);
const toggleCollapse = () => {
  collapsed.value = !collapsed.value;
};
</script>

<template>
  <li class="my-[5px]">
    <div class="flex gap-1 items-start">
      <span
        v-if="node.children?.length"
        @click="toggleCollapse()"
        class="text-blue-500 mr-1 mt-0.5 rounded-full hover:bg-blue-50 hover:cursor-pointer p-0.5"
        :class="{ 'rotate-180': collapsed }"
      >
        <BaseIcon name="caret-up" :width="20" />
      </span>
      <span
        v-else
        class="relative"
        style="top: -0.35rem"
        :class="{ 'mr-2': isRootNode }"
      >
        <BaseIcon
          name="collapsible-list-item"
          :width="20"
          class="text-gray-400"
          :class="{ invisible: isRootNode }"
        />
      </span>

      <div>
        <span
          @click="toggleCollapse()"
          :class="{ 'cursor-pointer hover:underline': node.children?.length }"
        >
          {{ node.name }}
        </span>
        <div class="whitespace-nowrap inline-flex items-center">
          <!-- maybe later -->
          <!-- <span v-if="node.children?.length" class="text-gray-400 inline-block ml-1">- {{ node.children.length }}</span> -->
          <div v-if="node.definition" class="inline-block ml-1">
            <CustomTooltip label="Read more" :content="node.definition" />
          </div>
        </div>
      </div>
    </div>

    <ul
      v-if="node.children?.length"
      class="break-inside-avoid"
      :class="{ hidden: collapsed }"
    >
      <TreeNode
        class="pt-1 pl-8"
        v-for="child in node.children"
        :node="child"
      ></TreeNode>
    </ul>
  </li>
</template>
