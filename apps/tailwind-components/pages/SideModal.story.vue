<script setup lang="ts">
const showSidePanel = ref(false);
const fullScreen = ref(false);
const slideInRight = ref(true);
const buttonAlignment = ref<"left" | "center" | "right">("right");

function toggle() {
  showSidePanel.value = !showSidePanel.value;
}

let sectionCount = ref(0);
</script>
<template>
  <div class="p-12">
    <button
      @click="toggle"
      class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
    >
      Open side modal 🙈
    </button>

    <fieldset class="border border-gray-900 mb-2">
      <legend class="m-2 px-2">Props</legend>
      <div class="mb-2 pr-2">
        <input
          id="side-modal-full-screen"
          class="ml-2 hover:cursor-pointer"
          type="checkbox"
          v-model="fullScreen"
        />
        <label class="ml-1 hover:cursor-pointer" for="side-modal-full-screen">
          full-screen
        </label>
      </div>
      <div class="mb-2">
        <input
          id="side-modal-slide-in-right"
          class="ml-2 hover:cursor-pointer"
          type="checkbox"
          v-model="slideInRight"
        />
        <label
          class="ml-1 hover:cursor-pointer"
          for="side-modal-slide-in-right"
        >
          slide-in-right
        </label>
      </div>
      <div class="mb-2">
        <select
          class="ml-2 hover:cursor-pointer"
          id="side-modal-button-alignment"
          v-model="buttonAlignment"
        >
          <option value="left">left</option>
          <option value="center">center</option>
          <option value="right">right</option>
        </select>
        <label
          class="ml-1 hover:cursor-pointer"
          for="side-modal-button-alignment"
        >
          buttonAlignment
        </label>
      </div>
    </fieldset>
  </div>
  {{ fullScreen }}
  <SideModal
    :show="showSidePanel"
    :fullScreen="fullScreen"
    :slideInRight="slideInRight"
    :buttonAlignment="buttonAlignment"
    @close="showSidePanel = false"
  >
    <ContentBlockModal title="Side modal">
      <p>
        Good resolutions are useless attempts to interfere with scientific laws.
        Their origin is pure vanity. Their result is absolutely nil. They give
        us, now and then, some of those luxurious sterile emotions that have a
        certain charm for the weak. That is all that can be said for them. They
        are simply cheques that men draw on a bank where they have no account.
      </p>
    </ContentBlockModal>
  </SideModal>
</template>
