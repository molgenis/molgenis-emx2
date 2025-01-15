<template>
  <ButtonBase
    class="flex items-center justify-center border rounded-full uppercase bg-button-filter-well text-button-filter-well hover:brightness-110 focus:brightness-110 text-body-xs py-1 px-2"
  >
    <slot></slot>
    <BaseIcon :width="12" name="cross" class="ml-0.5" />
  </ButtonBase>
</template>
