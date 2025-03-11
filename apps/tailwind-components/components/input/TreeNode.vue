<script setup lang="ts">
import type { ITreeNodeState } from "~/types/types";
import BaseIcon from "../BaseIcon.vue";
import CustomTooltip from "../CustomTooltip.vue";

const props = withDefaults(
  defineProps<{
    id: string;
    nodes: ITreeNodeState[];
    inverted?: boolean;
    isRoot: boolean;
    valid?: boolean;
    invalid?: boolean;
    disabled?: boolean;
  }>(),
  {
    inverted: false,
    isRoot: false,
  }
);
const emit = defineEmits(["toggleSelect", "toggleExpand"]);

function toggleSelect(node: ITreeNodeState) {
  emit("toggleSelect", node);
}

function toggleExpand(node: ITreeNodeState) {
  emit("toggleExpand", node);
}
</script>

<template>
  <ul
    :class="[
      inverted
        ? 'text-search-filter-group-title-inverted'
        : 'text-search-filter-group-title',
    ]"
  >
    <li
      v-for="node in nodes.filter((node2) => node2.visible === true)"
      :key="id + node.name"
      class="mt-2.5 relative"
    >
      <div class="flex items-center">
        <button
          v-if="node.children?.length"
          @click.stop="toggleExpand(node)"
          class="-left-[11px] top-0 rounded-full hover:cursor-pointer h-6 w-6 flex items-center justify-center absolute z-20"
          :class="{
            'text-search-filter-group-toggle-inverted hover:bg-search-filter-group-toggle-inverted':
              inverted,
            'text-search-filter-group-toggle hover:bg-search-filter-group-toggle focus:bg-search-filter-group-toggle':
              !inverted,
          }"
          :aria-expanded="node.expanded"
          :aria-controls="node.name"
        >
          <BaseIcon
            :name="node.expanded ? 'caret-down' : 'caret-right'"
            :width="20"
          />
          <span class="sr-only">expand {{ node.name }}</span>
        </button>
        <template v-if="!isRoot">
          <BaseIcon
            v-if="node.children?.length"
            name="collapsible-list-item-sub"
            :width="20"
            class="text-blue-200 absolute -top-[9px]"
          />
          <BaseIcon
            v-else
            name="collapsible-list-item"
            :width="20"
            class="text-blue-200 absolute -top-[9px]"
          />
        </template>
      </div>
      <div class="flex justify-start items-center ml-4">
        <input
          v-if="node.selectable"
          type="checkbox"
          :indeterminate="node.selected === 'intermediate'"
          :id="id + '-' + node.name + '-input'"
          :name="node.name"
          :checked="node.selected === 'selected'"
          @click.stop="toggleSelect(node)"
          class="sr-only"
        />
        <InputLabel
          :for="id + '-' + node.name + '-input'"
          class="flex justify-center items-start hover:cursor-pointer"
        >
          <InputCheckboxIcon
            v-if="node.selectable"
            :indeterminate="node.selected === 'intermediate'"
            :checked="node.selected === 'selected'"
            class="ml-[-6px]"
            :class="{
              '[&>rect]:stroke-gray-400': inverted,
            }"
          />
          <span class="block text-body-sm leading-normal">{{ node.name }}</span>
        </InputLabel>
        <div class="inline-flex items-center whitespace-nowrap">
          <div class="inline-block pl-1">
            <CustomTooltip
              v-if="node.description"
              label="Read more"
              :hoverColor="inverted ? 'none' : 'white'"
              :content="node.description"
            />
          </div>
        </div>
      </div>
      <TreeNode
        :id="id"
        v-if="node.children?.length && node.expanded"
        class="ml-[31px]"
        :nodes="node.children"
        :isRoot="false"
        :inverted="inverted"
        @toggleSelect="toggleSelect"
        @toggleExpand="toggleExpand"
      />
    </li>
  </ul>
</template>
