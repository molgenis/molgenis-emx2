<script setup>
const props = defineProps({
  title: {
    type: String,
  },
  conditions: {
    type: Array,
  },
  search: {
    type: String,
  },
  initialCollapsed: {
    type: Boolean,
    default: true,
  },
  mobileDisplay: {
    type: Boolean,
    default: false,
  },
});

let isCollapsed = ref(props.initialCollapsed);
let selected = ref([]);

const emit = defineEmits(["update:conditions", "update:search"]);

watch(
  () => props.conditions,
  function () {
    selected.value = props.conditions;
  }
);

function toggleCollapseTitle() {
  isCollapsed.value = !isCollapsed.value;
}

function clearSelection() {
  selected.value.splice(0);
  emit("update:conditions", selected);
}

function clearSearch() {
  emit("update:search", "");
}
</script>

<template>
  <hr class="mx-5 border-black opacity-10" />
  <div class="flex items-center gap-1 p-5">
    <div class="inline-flex gap-1 group" @click="toggleCollapseTitle()">
      <h3
        class="font-sans text-body-base font-bold mr-[5px] group-hover:underline group-hover:cursor-pointer"
        :class="`text-search-filter-group-title${
          mobileDisplay ? '-mobile' : ''
        }`">
        {{ title }}
      </h3>
      <span
        :class="{
          'rotate-180': isCollapsed,
          'text-search-filter-group-toggle': !mobileDisplay,
        }"
        class="flex items-center justify-center w-8 h-8 rounded-full group-hover:bg-search-filter-group-toggle group-hover:cursor-pointer">
        <BaseIcon name="caret-up" :width="26" />
      </span>
    </div>
    <div class="text-right grow">
      <span
        v-if="selected.length"
        class="text-body-sm hover:underline hover:cursor-pointer"
        :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
        @click="clearSelection()">
        Remove {{ selected?.length }} selected
      </span>
      <span
        class="text-body-sm hover:underline hover:cursor-pointer"
        :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
        @click="clearSearch()"
        v-else-if="search"
        >Clear</span
      >
    </div>
  </div>

  <div
    v-if="!isCollapsed"
    class="mb-5 ml-5 mr-5"
    :class="`text-search-filter-group-title${mobileDisplay ? '-mobile' : ''}`">
    <slot />
  </div>
</template>
