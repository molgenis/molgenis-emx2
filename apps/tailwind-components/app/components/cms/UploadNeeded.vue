<template>
  <div
    class="w-full flex items-center justify-center p-3.5 text-center gap-2 text-title-contrast py-5 border border-button-tertiary rounded-base mb-2.5 hover:border-button-tertiary-hover"
  >
    <BaseIcon name="Image" :width="21" />
    <span><slot></slot></span>
  </div>
</template>
