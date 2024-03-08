<script setup lang="ts">
import type { ITreeNode } from "../../types/types";
import TreeChild from "./treeComponents/TreeChild.vue";
import BaseIcon from "../BaseIcon.vue";
import CustomTooltip from "../CustomTooltip.vue";

const props = withDefaults(
  defineProps<{
    rootNodes: ITreeNode[];
    modelValue?: ITreeNode[];
    isMultiSelect?: boolean;
    mobileDisplay?: boolean;
  }>(),
  {
    isMultiSelect: true,
    mobileDisplay: false,
  }
);

const emit = defineEmits(["update:modelValue"]);

const nodes = ref(props.rootNodes);

function toggleExpand(index: number) {
  nodes.value[index].expanded = !nodes.value[index].expanded;
}

function toggleSelect(index: number) {
  nodes.value[index].selected = true;
  props.modelValue?.push(nodes.value[index]);
}

function toggleDeselect(index: number) {
  nodes.value[index].selected = false;
  props.modelValue?.splice(
    props.modelValue.findIndex((n) => n.name === nodes.value[index].name),
    1
  );
}

function handleChildSelect(child: ITreeNode) {
  props.modelValue?.push(child);
  console.log("child selected", child.name);
}

function handleChildDeselect(child: ITreeNode) {
  props.modelValue?.splice(
    props.modelValue.findIndex((n) => n.name === child.name),
    1
  );
  console.log("child de-selected", child.name);
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
            class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 text-search-filter-group-checkbox border border-checkbox hover:cursor-pointer"
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
          @select="handleChildSelect"
          @deselect="handleChildDeselect"
        />
      </ul>
    </li>
  </ul>
</template>
