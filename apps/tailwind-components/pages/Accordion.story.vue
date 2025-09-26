<template>
  <InputTestContainer show-label v-slot="{ label }">
    <Accordion id="story-acordion" :label="label || 'Hello, world!'">
      <p class="text-title-contrast">
        This is an accordion component that can be used to hide and show
        content. The label can be changed and the content can be hidden by
        default.
      </p>
    </Accordion>
  </InputTestContainer>
</template>
