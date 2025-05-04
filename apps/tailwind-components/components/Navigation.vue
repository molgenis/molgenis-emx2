<script setup lang="ts">
import { computed } from "vue";

interface PropType {
  maximumButtonShown?: number;
  invert?: boolean;
  navigation: {
    label: string;
    link: string;
    highlight?: boolean;
  }[];
  showMoreButton?: boolean;
}

const props: PropType = withDefaults(defineProps<PropType>(), {
  maximumButtonShown: 4,
  showMoreButton: true,
  invert: false,
});

const mainButtons = computed(() =>
  props.navigation.slice(0, props.maximumButtonShown)
);
const subButtons = computed(() =>
  props.navigation.slice(props.maximumButtonShown)
);
</script>

<template>
  <nav class="flex items-center justify-between gap-6 xl:justify-center">
    <NuxtLink
      v-for="button in mainButtons"
      :to="button.link"
      class="flex items-center gap-1 tracking-widest transition-colors border border-b-0 border-transparent font-display text-heading-xl hover:underline"
      :class="invert ? 'text-sub-menu' : 'text-menu'"
    >
      {{ button.label }}
    </NuxtLink>

    <VMenu
      placement="bottom-end"
      :distance="-1"
      v-if="subButtons.length > 0 && showMoreButton"
    >
      <button
        class="flex items-center gap-1 pt-3 pb-2 pl-4 pr-2 -mt-3 -ml-4 tracking-widest transition-colors duration-300 translate-y-1 border border-b-0 border-transparent rounded-t-3px font-display text-heading-xl hover:border-white"
        :class="invert ? 'text-sub-menu' : 'text-menu'"
      >
        More
        <BaseIcon name="caret-down" />
      </button>

      <template #popper>
        <ol
          class="flex flex-col gap-1.5 bg-white text-body-base rounded-3px rounded-tr-none shadow-xl p-6"
        >
          <li v-for="button in subButtons">
            <NuxtLink
              :to="button.link"
              class="font-bold transition-colors text-sub-menu hover:text-sub-menu-hover hover:underline"
            >
              {{ button.label }}
            </NuxtLink>
          </li>
        </ol>
      </template>
    </VMenu>
  </nav>
</template>

<style>
.v-popper--theme-menu .v-popper__arrow-container {
  display: none;
}

.v-popper--theme-menu .v-popper__inner {
  border-radius: 0;
  box-shadow: none;
  border: 0;
  background: none;
  padding: 0;
  overflow: unset;
}
</style>
