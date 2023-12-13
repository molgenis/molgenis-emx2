<template>
  <div class="grid grid-cols-10 gap-4">
    <div
      v-for="iconName in icons"
      :key="iconName"
      class="flex flex-col items-center justify-between h-32 py-5 rounded-sm bg-blue-50"
    >
      <span class="text-blue-500">
        <BaseIcon :name="iconName" :width="24" />
      </span>
      <span class="text-sm">{{ iconName }}</span>
    </div>
  </div>
</template>

<script>
import { defineComponent } from "vue";
import BaseIcon from "../BaseIcon.vue";

export default defineComponent({
  name: "IconsOverview",
  components: {
    BaseIcon,
  },
  props: {
    icons: {
      type: Array,
      default: () => [],
    },
  },
});
</script>
