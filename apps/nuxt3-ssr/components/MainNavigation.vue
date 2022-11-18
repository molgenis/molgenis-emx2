<script setup>
defineProps({
  showMoreButton: {
    type: Boolean,
    default: true,
  },
});

// Active class
const active = "underline";
</script>

<template>
  <nav class="flex items-center xl:justify-center gap-6 justify-between">
    <a
      href="#"
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
      Variables
    </a>
    <a
      href="#"
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
      :class="active"
    >
      Cohorts
    </a>
    <a
      href="#"
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
      Networks
    </a>

    <VMenu placement="bottom-end" :distance="-1" v-if="showMoreButton">
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
          <li>
            <a
              href="#"
              class="
                font-bold
                text-blue-500
                transition-colors
                hover:text-blue-700
              "
              >Statistical methods</a
            >
          </li>
          <li>
            <a
              href="#"
              class="
                font-bold
                text-blue-500
                transition-colors
                hover:text-blue-700
              "
              >Tables</a
            >
          </li>
          <li>
            <a
              href="#"
              class="
                font-bold
                text-blue-500
                transition-colors
                hover:text-blue-700
              "
              >Manuals</a
            >
          </li>
          <li>
            <a
              href="#"
              class="
                font-bold
                text-blue-500
                transition-colors
                hover:text-blue-700
              "
              >About</a
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
