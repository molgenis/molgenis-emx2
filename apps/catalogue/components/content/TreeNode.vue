<script setup lang="ts">
import { ref } from "vue";
import type { IOntologyItem } from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    node: IOntologyItem;
    isRootNode?: boolean;
    collapseAll?: boolean;
    inverted?: boolean;
  }>(),
  {
    collapseAll: true,
    isRootNode: false,
    inverted: false,
  }
);

let collapsed = ref(props.collapseAll);
const toggleCollapse = () => {
  collapsed.value = !collapsed.value;
};
</script>

<template>
  <li class="relative" :class="{ 'mt-2.5': !isRootNode }">
    <div class="flex items-center">
      <span
        v-if="node.children?.length"
        @click="toggleCollapse()"
        class="text-blue-500 mr-1 mt-0.5 rounded-full hover:bg-blue-50 hover:cursor-pointer p-0.5"
        :class="{ 'rotate-180': collapsed, 'ml-[-0.5rem]': isRootNode }"
      >
        <BaseIcon name="caret-up" :width="20" />
      </span>
      <span v-else-if="isRootNode" />
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

      <span
        @click="toggleCollapse()"
        class="flex justify-center items-start hover:cursor-pointer"
        :class="{ 'cursor-pointer hover:underline': node.children?.length }"
      >
        {{ node.name }}
      </span>
      <div class="inline-flex items-center whitespace-nowrap">
        <div v-if="node.definition" class="inline-block ml-1">
          <CustomTooltip
            label="Read more"
            :hoverColor="inverted ? 'none' : 'white'"
            :content="node.definition"
          />
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
