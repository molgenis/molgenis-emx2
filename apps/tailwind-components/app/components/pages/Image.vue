<script setup lang="ts">
import { ref } from "vue";
import type { IImages } from "../../../types/cms";
import ComponentActions from "./ComponentActions.vue";

const props = withDefaults(defineProps<IImages & { isEditable?: boolean }>(), {
  isEditable: false,
  imageIsCentered: false,
});
const emit = defineEmits(["edit", "delete"]);
const showMenu = ref<boolean>(true);

const src = ref<string>();
if (props.image?.url) {
  src.value = props.image.url.replace("Components", "Images");
}

let style = "";
if (props.width) {
  style = style + `width: ${props.width};`;
}

if (props.height) {
  style = style + `height: ${props.height};`;
}
</script>

<template>
  <div
    class="w-full"
    @mouseenter="showMenu = true"
    @mouseleave="showMenu = false"
  >
    <div
      class="relative"
      :class="{
        'm-auto': imageIsCentered,
      }"
      :style="style"
    >
      <img :id="id" :src="src" :alt="alt" />
      <ComponentActions
        v-if="isEditable && showMenu"
        name="Image"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        class="right-2 top-2 !left-auto"
      />
    </div>
  </div>
</template>
