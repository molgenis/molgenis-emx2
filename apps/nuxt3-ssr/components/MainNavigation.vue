<script setup>
const props = defineProps({
  maximumButtonShown: {
    type: Number,
    default: 3,
  },
  navigation: {
    type: Array,
    required: true,
  },
  showMoreButton: {
    type: Boolean,
    default: true,
  },
});
const mainButtons = props.navigation.slice(0, props.maximumButtonShown);
const subButtons = props.navigation.slice(props.maximumButtonShown);
const active = "underline";
</script>

<template>
  <nav class="flex items-center xl:justify-center gap-6 justify-between">
    <a
      v-for="button in mainButtons"
      :href="button.link"
      class="
        flex
        items-center
        gap-1
        tracking-widest
        text-white
        transition-colors
        border border-b-0 border-transparent
        font-display
        text-heading-xl
        hover:underline
      "
    >
      {{ button.label }}
    </a>

    <VMenu
      placement="bottom-end"
      :distance="-1"
      v-if="subButtons.length > 0 && showMoreButton"
    >
      <button
        class="
          flex
          translate-y-1
          items-center
          gap-1
          tracking-widest
          text-white
          border border-b-0
          -ml-4
          -mt-3
          pl-4
          pt-3
          pr-2
          pb-2
          border-transparent
          rounded-t-3px
          transition-colors
          duration-300
          font-display
          text-heading-xl
          hover:border-white
        "
      >
        More
        <BaseIcon name="caret-down" />
      </button>

      <template #popper>
        <ol
          class="
            flex flex-col
            gap-1.5
            bg-white
            text-body-base
            rounded-3px rounded-tr-none
            shadow-xl
            p-6
          "
        >
          <li v-for="button in subButtons">
            <a
              :href="button.link"
              class="
                font-bold
                text-blue-500
                transition-colors
                hover:text-blue-700
              "
              >{{ button.label }}</a
            >
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
}
</style>
