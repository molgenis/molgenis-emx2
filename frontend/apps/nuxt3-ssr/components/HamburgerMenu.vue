<script setup>
defineProps({
  navigation: {
    type: Array,
    required: true,
  },
});
</script>

<template>
  <SideModal :slideInRight="false" :fullScreen="true">
    <nav class="flex flex-col gap-4 px-6 pb-24 pt-19">
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

    <template #button>
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
          class="antialiased tracking-wider translate-y-px font-display text-heading-xl"
          >Menu
        </span>
      </button>
    </template>

    <template #footer>
      <HeaderButtonMobile label="Favorites" icon="star" />
      <HeaderButtonMobile label="Account" icon="user" />
    </template>
  </SideModal>
</template>
