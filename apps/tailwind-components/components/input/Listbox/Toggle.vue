<template>
  <button
    role="combobox"
    aria-haspopup="listbox"
    class="flex justify-start items-center h-10 w-full text-left pl-10 border-input bg-input text-button-input-toggle"
  >
    <slot></slot>
    <div class="w-[60px] flex flex-col">
      <BaseIcon :width="18" name="caret-up" class="mx-auto -my-1" />
      <BaseIcon :width="18" name="caret-down" class="mx-auto -my-1" />
    </div>
  </button>
</template>
