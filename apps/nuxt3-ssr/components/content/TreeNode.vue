<script setup lang="ts">

const { node } = defineProps<{
  node: IOntologyNode;
}>()

let collapsed = ref(true);
const toggleCollapse = () => {
  collapsed.value = !collapsed.value;
};

</script>

<template>
  <li class="my-[5px]">

    <div class="flex gap-1 items-start">
      <span v-if="node.children?.length" @click="toggleCollapse()"
        class="text-blue-500 mr-1 mt-0.5 rounded-full hover:bg-blue-50 hover:cursor-pointer p-0.5 "
        :class="{ 'rotate-180': collapsed }">
        <BaseIcon name="caret-up" :width="20" />

      </span>
      <span v-else>
        <BaseIcon name="collapsible-list-item" :width="20" class="text-gray-400" />
      </span>

      <div>
        <span @click="toggleCollapse()" :class="{ 'cursor-pointer': node.children?.length }">
          {{ node.name }}
        </span>
        <div class="whitespace-nowrap inline-flex items-center">
          <span v-if="node.children?.length" class="text-gray-400 inline-block ml-1">- {{ node.children.length }}</span>
          <div v-if="node.description" class="inline-block ml-1">
            <CustomTooltip label="Read more" :content="node.description" />
          </div>
        </div>
      </div>
    </div>

    <ul v-if="node.children?.length" class="break-inside-avoid" :class="{ hidden: collapsed }">
      <TreeNode class="pt-1 pl-8" v-for="child in node.children" :node="child"></TreeNode>
    </ul>
  </li>

</template>
