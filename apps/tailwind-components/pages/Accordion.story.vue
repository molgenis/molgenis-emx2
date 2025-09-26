<template>
  <InputTestContainer show-label v-slot="{ label }">
    <Accordion :label="label || 'Accordion component'" class="max-w-[500px]">
      <p class="text-title-contrast">
        This is an accordion component that can be used to hide and show
        content. The label can be changed and the content can be hidden by
        default.
      </p>
    </Accordion>
  </InputTestContainer>
</template>
