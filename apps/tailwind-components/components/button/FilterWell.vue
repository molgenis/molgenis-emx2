<template>
  <ButtonBase
    class="flex items-center justify-center border rounded-full uppercase bg-blue-50 text-blue-500 border-blue-50 hover:bg-white hover:border-white text-body-xs py-1 px-2"
  >
    <slot></slot>
    <BaseIcon :width="12" name="cross" class="ml-0.5" />
  </ButtonBase>
</template>
