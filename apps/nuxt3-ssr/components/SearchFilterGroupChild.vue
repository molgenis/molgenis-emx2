<script setup>
import SearchFilterGroupChild from "./SearchFilterGroupChild.vue";
import BaseIcon from "./BaseIcon.vue";
import Tooltip from "./Tooltip.vue";
import { ref } from "vue";

defineProps({
  title: {
    type: String,
  },
  visible: {
    type: Boolean,
    default: true,
  },
  data: {
    type: Object,
  },
});

let collapsed = ref(true);
const toggleCollapse = () => {
  collapsed.value = !collapsed.value;
};
</script>

<template>
  <li v-for="child in data" :key="child.code" class="mt-2.5 relative">
    <span class="flex items-center">
      <span
        v-if="child.children"
        @click="toggleCollapse()"
        :class="{ 'rotate-180': collapsed }"
        class="-left-[11px] top-0 text-white rounded-full hover:bg-blue-800 hover:cursor-pointer h-6 w-6 flex items-center justify-center absolute z-20"
      >
        <BaseIcon name="caret-up" width="20" />
      </span>
      <BaseIcon
        v-if="child.children"
        name="collapsible-list-item-sub"
        width="20"
        class="text-blue-200 absolute -top-[9px]"
      />
      <BaseIcon
        v-else
        name="collapsible-list-item"
        width="20"
        class="text-blue-200 absolute -top-[9px]"
      />
    </span>
    <div class="flex items-start ml-3">
      <div class="flex items-center">
        <input
          type="checkbox"
          :id="child.code"
          :name="child.code"
          class="w-5 h-5 rounded-[3px] ml-2.5 mr-2.5 mt-0.5 text-yellow-500 border-0"
        />
      </div>
      <label :for="child.code" class="hover:cursor-pointer text-body-sm group">
        <span class="group-hover:underline">{{ child.name }}</span>
        <div class="whitespace-nowrap inline-flex items-center">
          <span
            class="text-blue-200 inline-block mr-2 group-hover:underline decoration-blue-200"
          >
            &nbsp;- 34
          </span>
          <div class="inline-block">
            <Tooltip
              v-if="child.mg_insertedOn"
              label="Lees meer"
              hoverColor="white"
              :content="child.mg_insertedOn"
            />
          </div>
        </div>
      </label>
    </div>
    <ul v-if="child.children" :class="{ hidden: collapsed }" class="ml-[31px]">
      <SearchFilterGroupChild :data="child.children" />
    </ul>
  </li>
</template>
