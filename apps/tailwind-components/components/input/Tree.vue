<script setup lang="ts">
import { normalizeClass } from "vue";
import type { ITreeNode } from "../../types/types";
import BaseIcon from "../BaseIcon.vue";
import CustomTooltip from "../CustomTooltip.vue";

const props = withDefaults(
  defineProps<{
    nodes: ITreeNode[];
    modelValue: string[];
    isMultiSelect?: boolean;
    mobileDisplay?: boolean;
    expandSelected?: boolean;
    isRoot?: boolean;
  }>(),
  {
    isMultiSelect: true,
    mobileDisplay: false,
    expandSelected: false,
    isRoot: true,
  }
);

// expand status is internally controlled by the component
// whereas the selected status is controlled by the parent
const expandedNodes = ref<string[]>([]);

const emit = defineEmits(["update:modelValue"]);

function toggleExpand(nameName: string) {
  const index = expandedNodes.value.indexOf(nameName);
  if (index > -1) {
    expandedNodes.value.splice(index, 1);
  } else {
    expandedNodes.value.push(nameName);
  }
}

function expandSelection(node: ITreeNode) {
  let selection: string[] = [];
  node.children.forEach((child) => {
    selection.push(child.name);
    if (child.children) {
      selection = [...selection, ...expandSelection(child)];
    }
  });

  return selection;
}

function toggleSelect(node: ITreeNode) {
  if (props.modelValue.includes(node.name)) {
    // remove node from selected nodes and emit new model
    emit(
      "update:modelValue",
      props.modelValue.filter((n) => n !== node.name)
    );
  } else {
    let currnetSelection = [...props.modelValue, node.name];
    if (props.expandSelected) {
      currnetSelection = [...currnetSelection, ...expandSelection(node)];
    }
    const deduplicated = [...new Set(currnetSelection)];
    // clone model and add new before emitting
    emit("update:modelValue", deduplicated);
  }
}

function handleChildSelect(selected: string[], parent: ITreeNode) {
  const siblingNames = parent.children.map((n) => n.name);
  const selectParent =
    siblingNames.some((siblingName) => selected.includes(siblingName)) &&
    siblingNames.every((siblingName) => selected.includes(siblingName));
  const updatedSelection = selectParent ? [...selected, parent.name] : selected;

  const deduplicated = [...new Set(updatedSelection)];
  emit("update:modelValue", deduplicated);
}
</script>

<template>
  <ul class="text-search-filter-group-title">
    <li v-for="node in nodes" :key="node.name" class="mt-2.5 relative">
      <span class="flex items-center">
        <span
          v-if="node.children?.length"
          @click.stop="toggleExpand(node.name)"
          class="-left-[11px] top-0 text-search-filter-group-toggle rounded-full hover:bg-search-filter-group-toggle hover:cursor-pointer h-6 w-6 flex items-center justify-center absolute z-20"
        >
          <BaseIcon
            :name="
              expandedNodes.includes(node.name) ? 'caret-down' : 'caret-up'
            "
            :width="20"
          />
        </span>
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
      </span>
      <div class="flex items-start ml-4">
        <div class="flex items-center">
          <input
            type="checkbox"
            :indeterminate="
              node.children?.some((c) => modelValue.includes(c.name)) &&
              !node.children?.every((c) => modelValue.includes(c.name))
            "
            :id="node.name"
            :name="node.name"
            :checked="modelValue.includes(node.name)"
            @click.stop="toggleSelect(node)"
            class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 accent-yellow-500 indeterminate:accent-yellow-500 border border-checkbox hover:cursor-pointer"
          />
        </div>
        <label :for="node.name" class="hover:cursor-pointer text-body-sm group">
          <span class="group-hover:underline whitespace-nowrap">{{
            node.name
          }}</span>
        </label>
        <div class="inline-flex items-center whitespace-nowrap">
          <div class="inline-block pl-1">
            <CustomTooltip
              v-if="node.description"
              label="Read more"
              hoverColor="white"
              :content="node.description"
            />
          </div>
        </div>
      </div>

      <Tree
        v-if="node.children.length"
        v-show="expandedNodes.includes(node.name)"
        class="ml-[31px]"
        :nodes="node.children"
        :modelValue="modelValue"
        :expandSelected="expandSelected"
        :isRoot="false"
        @update:modelValue="handleChildSelect($event, node)"
      />
    </li>
  </ul>
</template>
