<script setup lang="ts">
import { ref, reactive } from "vue";
import type { ITreeNode } from "../../../types/types";
import BaseIcon from "../../BaseIcon.vue";

const props = withDefaults(
  defineProps<{
    nodes: ITreeNode[];
    visible?: boolean;
  }>(),
  { visible: true }
);

const emit = defineEmits(["select", "deselect"]);

function toggleExpand(node: ITreeNode) {
  node.expanded = !node.expanded;
}

function toggleSelect(node: ITreeNode) {
  //if selecting then also expand
  //if deselection we keep it open
  if (node.selected == "complete") {
    emit("deselect", node.name);
  } else {
    emit("select", node.name);
  }
}
</script>

<template>
  <div>
    <li v-for="node in nodes" class="mt-2.5 relative">
      <span class="flex items-center">
        <span
          v-if="node.children?.length"
          @click="toggleExpand(node)"
          class="-left-[11px] top-0 text-search-filter-group-toggle rounded-full hover:bg-search-filter-group-toggle hover:cursor-pointer h-6 w-6 flex items-center justify-center absolute z-20"
        >
          <BaseIcon
            :name="node.expanded ? 'caret-down' : 'caret-up'"
            :width="20"
          />
        </span>
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
      </span>
      <div class="flex items-start ml-3">
        <div class="flex items-center">
          <input
            type="checkbox"
            :id="node.name"
            :name="node.name"
            @click.stop="toggleSelect(node)"
            :checked="
              node.selected === 'complete' || node.selected === 'partial'
            "
            class="w-5 h-5 rounded-3px ml-2.5 mr-2.5 mt-0.5 text-search-filter-group-checkbox border border-checkbox"
          />
        </div>
        <label :for="node.name" class="hover:cursor-pointer text-body-sm group">
          <span class="group-hover:underline">{{ node.name }}</span>
          <div class="inline-flex items-center whitespace-nowrap">
            <div class="inline-block">
              <CustomTooltip
                v-if="node.description"
                label="Description"
                hoverColor="white"
                :content="node.description"
              />
            </div>
          </div>
        </label>
      </div>
      <ul
        v-if="node.children"
        :class="{ hidden: !node.expanded }"
        class="ml-[31px]"
      >
        <TreeChild
          :nodes="node.children"
          @select="$emit('select', $event)"
          @deselect="$emit('deselect', $event)"
        />
      </ul>
    </li>
  </div>
</template>
