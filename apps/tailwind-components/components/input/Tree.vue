<script setup lang="ts">
import type { ITreeNode } from "../../types/types";
import BaseIcon from "../BaseIcon.vue";
import CustomTooltip from "../CustomTooltip.vue";

const props = withDefaults(
  defineProps<{
    nodes: ITreeNode[];
    modelValue: string[];
    isMultiSelect?: boolean;
    expandSelected?: boolean;
    isRoot?: boolean;
    inverted?: boolean;
  }>(),
  {
    isMultiSelect: true,
    expandSelected: false,
    isRoot: true,
    inverted: false,
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
    // remove node(s) from selected nodes and emit new model
    let deSelectionList = [node.name];
    if (props.expandSelected) {
      deSelectionList = deSelectionList.concat(expandSelection(node));
    }
    const newSelection = props.modelValue.filter(
      (n) => !deSelectionList.includes(n)
    );
    emit("update:modelValue", newSelection);
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
  <ul
    :class="[
      inverted
        ? 'text-search-filter-group-title-inverted'
        : 'text-search-filter-group-title',
    ]"
  >
    <li v-for="node in nodes" :key="node.name" class="mt-2.5 relative">
      <div class="flex items-center">
        <button
          v-if="node.children?.length"
          @click.stop="toggleExpand(node.name)"
          class="-left-[11px] top-0 rounded-full hover:cursor-pointer h-6 w-6 flex items-center justify-center absolute z-20"
          :class="[
            inverted
              ? 'text-search-filter-group-toggle-inverted hover:bg-search-filter-group-toggle-inverted'
              : 'text-search-filter-group-toggle hover:bg-search-filter-group-toggle',
          ]"
          :aria-expanded="expandedNodes.includes(node.name)"
          :aria-controls="node.name"
        >
          <BaseIcon
            :name="
              expandedNodes.includes(node.name) ? 'caret-down' : 'caret-right'
            "
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
          type="checkbox"
          :indeterminate="
            node.children?.some((c) => modelValue.includes(c.name)) &&
            !node.children?.every((c) => modelValue.includes(c.name))
          "
          :id="node.name"
          :name="node.name"
          :checked="modelValue.includes(node.name)"
          @click.stop="toggleSelect(node)"
          class="sr-only"
        />
        <InputLabel
          :for="node.name"
          class="flex justify-center items-start hover:cursor-pointer"
        >
          <InputCheckboxIcon
            :indeterminate="
              node.children?.some((c) => modelValue.includes(c.name)) &&
              !node.children?.every((c) => modelValue.includes(c.name))
            "
            :checked="
              modelValue.includes(node.name) &&
              node.children?.every((c) => modelValue.includes(c.name))
            "
            class="w-[20px] ml-[-6px]"
            :class="{
              '[&>rect]:stroke-gray-400': inverted,
            }"
          />
          <span class="block w-[calc(100%-20px)] text-body-sm leading-normal">{{
            node.name
          }}</span>
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
      <Tree
        v-if="node.children?.length && expandedNodes.includes(node.name)"
        class="ml-[31px]"
        :nodes="node.children"
        :modelValue="modelValue"
        :expandSelected="expandSelected"
        :isRoot="false"
        @update:modelValue="handleChildSelect($event, node)"
        :inverted="inverted"
      />
    </li>
  </ul>
</template>
