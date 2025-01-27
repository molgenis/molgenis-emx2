<template>
  <div class="flex flex-row bg-invalid p-2 justify-between">
    <div></div>
    <div class="flex flex-row gap-1">
      <BaseIcon name="info" class="text-invalid" />
      <span class="my-auto text-invalid"
        >3 fields require your attention before you can save this cohort</span
      >
    </div>

    <div class="flex flex-row">
      <Button icon="caret-up" size="small"></Button>
      <Button icon="caret-down" size="small"></Button>
    </div>
  </div>
</template>
