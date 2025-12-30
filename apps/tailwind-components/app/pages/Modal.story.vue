<script setup lang="ts">
import { ref } from "vue";

const visible = ref(false);
let sectionCount = ref(0);
</script>

<template>
  <button
    @click="visible = true"
    class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
  >
    Open modal 🙈
  </button>
  <Modal v-model:visible="visible" title="Title" subtitle="Subtitle">
    <p class="p-8">
      Good resolutions are useless attempts to interfere with scientific laws.
      Their origin is pure vanity. Their result is absolutely nil. They give us,
      now and then, some of those luxurious sterile emotions that have a certain
      charm for the weak. That is all that can be said for them. They are simply
      cheques that men draw on a bank where they have no account.
    </p>
    <div class="flex flex-wrap gap-5 px-8">
      <button
        @click="sectionCount++"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover"
      >
        add content
      </button>
      <button
        @click="sectionCount--"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover"
      >
        remove content
      </button>
    </div>
    <p v-for="index in sectionCount" class="p-8">
      Good resolutions are useless attempts to interfere with scientific laws.
      Their origin is pure vanity. Their result is absolutely nil. They give us,
      now and then, some of those luxurious sterile emotions that have a certain
      charm for the weak. That is all that can be said for them. They are simply
      cheques that men draw on a bank where they have no account.
    </p>
  </Modal>
</template>
