<script setup>
const onShow = () => {
  document.body.classList.add("no-scroll");
};

const onHide = () => {
  document.body.classList.remove("no-scroll");
};

defineProps({
  navigation: {
    type: Array,
    required: true,
  },
});
</script>

<template>
  <VDropdown
    :positioning-disabled="true"
    @apply-show="onShow()"
    @apply-hide="onHide()"
  >
    <button class="flex gap-1 text-menu">
      <svg
        class="w-6 h-6"
        x-description="Heroicon name: outline/bars-3"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        stroke-width="1.5"
        stroke="currentColor"
        aria-hidden="true"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"
        ></path>
      </svg>
      <span
        class="
          antialiased
          tracking-wider
          translate-y-px
          font-display
          text-heading-xl
        "
        >Menu</span
      >
    </button>
    <template #popper="{ hide }">
      <div class="rounded-r-50px mt-5 mr-12.5 overflow-hidden antialiased">
        <div class="relative overflow-y-auto bg-white">
          <button @click="hide()" class="absolute top-7 right-8">
            <BaseIcon name="cross" />
          </button>

          <nav class="flex flex-col gap-4 px-6 pt-19 pb-14">
            <a
              v-for="menuItem in navigation"
              :href="menuItem.link"
              class="font-display text-heading-4xl"
              :class="
                menuItem?.highlight === true ? 'text-blue-500' : 'text-blue-800'
              "
            >
              {{ menuItem.label }}
            </a>
          </nav>

          <div class="flex items-center justify-around px-6 bg-blue-500 h-19">
            <HeaderButtonMobile label="Favorites" icon="star" />
            <HeaderButtonMobile label="Account" icon="user" />
          </div>
        </div>
      </div>
    </template>
  </VDropdown>
</template>

<style>
.v-popper--theme-dropdown .v-popper__inner {
  background: none;
  border-radius: 0;
  border: 0;
  box-shadow: none;
}

.v-popper__popper--no-positioning {
  position: fixed;
  z-index: 9999;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
}

.v-popper__popper--no-positioning .v-popper__backdrop {
  display: block;
  background: rgba(0 0 0 / 60%);
}

.v-popper__popper--no-positioning .v-popper__wrapper {
  width: 100%;
  pointer-events: auto;
  transition: transform 0.15s ease-out;
}

.v-popper__popper--no-positioning.v-popper__popper--hidden .v-popper__wrapper {
  transform: translateX(-100%);
}

body.no-scroll {
  overflow: hidden;
}
</style>
