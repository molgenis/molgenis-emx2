<script setup lang="ts">
import type { ITreeNode } from "../../types/types";
import TreeChild from "./treeComponents/TreeChild.vue";
import BaseIcon from "../BaseIcon.vue";
import CustomTooltip from "../CustomTooltip.vue";

const props = withDefaults(
  defineProps<{
    rootNodes: ITreeNode[];
    modelValue: ITreeNode[];
    isMultiSelect?: boolean;
    mobileDisplay?: boolean;
    expandSelected?: boolean;
  }>(),
  {
    isMultiSelect: true,
    mobileDisplay: false,
    expandSelected: false,
  }
);

const emit = defineEmits(["update:modelValue"]);

const nodes = ref(props.rootNodes);

function toggleExpand(index: number) {
  nodes.value[index].expanded = !nodes.value[index].expanded;
}

// select all children of a node recursively
function expandSelection(node: ITreeNode) {
  node.children?.forEach((child) => {
    child.selected = true;
    expandSelection(child);
  });
}

// deselect all children of a node recursively
function expandDeselection(node: ITreeNode) {
  node.children?.forEach((child) => {
    child.selected = false;
    expandDeselection(child);
  });
}

function toggleSelect(index: number) {
  nodes.value[index].selected = true;
  if (props.expandSelected) {
    expandSelection(nodes.value[index]);
  }
  updateModelValue(nodes.value);
}

function toggleDeselect(index: number) {
  nodes.value[index].selected = false;
  if (props.expandSelected) {
    expandDeselection(nodes.value[index]);
  }
  updateModelValue(nodes.value);
}

function handleChildSelect(child: ITreeNode, parent: ITreeNode) {
  const rootIndex = nodes.value.findIndex((n) => n.name === parent.name);
  nodes.value[rootIndex].selected = true;
  updateModelValue(nodes.value);
}

function getSelectedChildren(node: ITreeNode): ITreeNode[] {
  let selection: ITreeNode[] = [];
  if (node.children) {
    node.children.forEach((child) => {
      if (child.selected) {
        selection.push(child);
      }
      selection = [...selection, ...getSelectedChildren(child)];
    });
  }

  return selection;
}

function handleChildDeselect(child: ITreeNode, parent: ITreeNode) {
  if (props.expandSelected) {
    if (getSelectedChildren(parent).length === 0) {
      const rootIndex = nodes.value.findIndex((n) => n.name === parent.name);
      nodes.value[rootIndex].selected = false;
    }
  }
  updateModelValue(nodes.value);
}

function updateModelValue(nodes: ITreeNode[]) {
  // inner function to recursively update the model value
  function updateChildSelection(node: ITreeNode) {
    if (node.children) {
      for (let i = 0; i < node.children.length; i++) {
        if (node.children[i].selected) {
          props.modelValue.push(node.children[i]);
        }
        updateChildSelection(node.children[i]);
      }
    }
  }

  // clearout without changing reference
  props.modelValue.splice(0, props.modelValue.length);
  // add all selected
  for (let i = 0; i < nodes.length; i++) {
    if (nodes[i].selected) {
      props.modelValue.push(nodes[i]);
    }
    updateChildSelection(nodes[i]);
  }
}
</script>

<template>
  <ul class="text-search-filter-group-title">
    <li v-for="(node, index) in nodes" :key="node.name" class="mb-2.5">
      <div class="flex items-start">
        <span
          v-if="node.children?.length"
          @click.stop="toggleExpand(index)"
          class="flex items-center justify-center w-6 h-6 rounded-full hover:bg-search-filter-group-toggle hover:cursor-pointer"
          :class="{
            'rotate-180': !node.expanded,
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
            :id="node.name"
            :name="node.name"
            :checked="node.selected"
            @click.stop="
              node.selected ? toggleDeselect(index) : toggleSelect(index)
            "
            class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 accent-yellow-500 border border-checkbox hover:cursor-pointer"
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

      <ul
        class="ml-10 mr-4"
        :class="{ hidden: !node.expanded }"
        v-if="node.children"
      >
        <TreeChild
          :nodes="node.children"
          :parent="node"
          @select="handleChildSelect($event, node)"
          @deselect="handleChildDeselect($event, node)"
          :expandSelected="expandSelected"
        />
      </ul>
    </li>
  </ul>
</template>
