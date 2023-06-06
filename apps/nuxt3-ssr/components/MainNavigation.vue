<script setup lang="ts">
interface PropType {
  maximumButtonShown?: number;
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
});

const mainButtons = props.navigation.slice(0, props.maximumButtonShown);
const subButtons = props.navigation.slice(props.maximumButtonShown);
const active = "underline";
</script>

<template>
  <nav class="flex items-center justify-between gap-6 xl:justify-center">
    <a
      v-for="button in mainButtons"
      :href="button.link"
      class="flex items-center gap-1 tracking-widest text-menu transition-colors border border-b-0 border-transparent font-display text-heading-xl hover:underline">
      {{ button.label }}
    </a>

    <VMenu
      placement="bottom-end"
      :distance="-1"
      v-if="subButtons.length > 0 && showMoreButton">
      <button
        class="flex items-center gap-1 pt-3 pb-2 pl-4 pr-2 -mt-3 -ml-4 tracking-widest transition-colors duration-300 translate-y-1 border border-b-0 border-transparent text-menu rounded-t-3px font-display text-heading-xl hover:border-white">
        More
        <BaseIcon name="caret-down" />
      </button>

      <template #popper>
        <ol
          class="flex flex-col gap-1.5 bg-white text-body-base rounded-3px rounded-tr-none shadow-xl p-6">
          <li v-for="button in subButtons">
            <a
              :href="button.link"
              class="font-bold transition-colors text-sub-menu hover:text-sub-menu-hover hover:underline">
              {{ button.label }}
            </a>
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
