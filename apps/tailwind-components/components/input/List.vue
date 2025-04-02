<script setup lang="ts">
import type { INode } from "../../types/types";
import CustomTooltip from "../CustomTooltip.vue";

const props = withDefaults(
  defineProps<{
    nodes: INode[];
    modelValue: string[];
    inverted?: boolean;
  }>(),
  {
    isMultiSelect: true,
    inverted: false,
  }
);

const emit = defineEmits(["update:modelValue"]);

function toggleSelect(node: INode) {
  if (props.modelValue.includes(node.name)) {
    // remove node(s) from selected nodes and emit new model
    const newSelection = props.modelValue.filter((n) => n !== node.name);
    emit("update:modelValue", newSelection);
  } else {
    let newSelection = [...props.modelValue, node.name];
    // clone model and add new before emitting
    emit("update:modelValue", newSelection);
  }
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
    <li v-for="node in nodes" :key="node.name" class="mb-2.5 relative">
      <div class="flex items-start ml-4">
        <div class="flex items-center">
          <input
            type="checkbox"
            :id="node.name"
            :name="node.name"
            :checked="modelValue.includes(node.name)"
            @click.stop="toggleSelect(node)"
            class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 accent-yellow-500 indeterminate:accent-yellow-500 border border-checkbox hover:cursor-pointer"
          />
        </div>
        <label :for="node.name" class="hover:cursor-pointer text-body-sm group">
          <span class="group-hover:underline">{{ node.name }}</span>
        </label>
        <div class="inline-flex items-center whitespace-nowrap">
          <!--
          <span
            v-if="node?.result?.count"
            class="inline-block mr-2 text-blue-200 group-hover:underline decoration-blue-200 fill-black"
            hoverColor="white"
            >&nbsp;- {{ node.result.count }}
          </span>
          -->
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
    </li>
  </ul>
</template>
