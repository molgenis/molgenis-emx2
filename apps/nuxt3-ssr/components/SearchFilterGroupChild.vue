<script setup>
import { ref } from "vue";

const props = defineProps({
  title: {
    type: String,
  },
  visible: {
    type: Boolean,
    default: true,
  },
  items: {
    type: Array,
  },
});

var itemMap = props.items.reduce(function (map, obj) {
  map[obj.name] = obj;
  return map;
}, {});

let terms = reactive(itemMap)

let key = ref(1)
function toggleExpand(term) {
  terms[term.name].expanded = !terms[term.name].expanded;
  key++;
}
</script>

<template>
  <li v-for="child in Object.values(items).sort((a, b) => a.name.localeCompare(b.name))" :key="child.name" class="mt-2.5 relative">
    <span class="flex items-center">
      <span v-if="child.children" @click="toggleExpand(child)" 
    
      class="
          -left-[11px]
          top-0
          text-white
          rounded-full
          hover:bg-blue-800 hover:cursor-pointer
          h-6
          w-6
          flex
          items-center
          justify-center
          absolute
          z-20
        ">
        <BaseIcon name="caret-up" :width="20" />
      </span>
      <BaseIcon v-if="child.children" name="collapsible-list-item-sub" :width="20"
        class="text-blue-200 absolute -top-[9px]" />
      <BaseIcon v-else name="collapsible-list-item" :width="20" class="text-blue-200 absolute -top-[9px]" />
    </span>
    <div class="flex items-start ml-3">
      <div class="flex items-center">
        <input type="checkbox" :id="child.name" :name="child.name" class="
            w-5
            h-5
            rounded-3px
            ml-2.5
            mr-2.5
            mt-0.5
            text-yellow-500
            border-0
          " />
      </div>
      <label :for="child.name" class="hover:cursor-pointer text-body-sm group">
        <span class="group-hover:underline">{{ child.name }}</span>
        <div class="whitespace-nowrap inline-flex items-center">
          <span v-if="child?.children?.length" class="
              text-blue-200
              inline-block
              mr-2
              group-hover:underline
              decoration-blue-200
            ">
            &nbsp;- {{ child.children.length }}
          </span>
          <div class="inline-block">
            <CustomTooltip v-if="child.description" label="Description" hoverColor="white" :content="child.description" />
          </div>
        </div>
      </label>
    </div>
    <ul v-if="child.children" :class="{ hidden: !terms[child.name].expanded }" class="ml-[31px]">
      <SearchFilterGroupChild :items="child.children" />
    </ul>
  </li>
</template>
