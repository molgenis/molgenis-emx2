<template>
  <InputTestContainer show-label v-slot="{ label }">
    <div class="grid grid-rows-1 gap-4">
      <Accordion
        :label="label || 'Accordion component'"
        class="max-w-[600px]"
        toggle-icon-size="small"
      >
        <p class="text-title-contrast">
          This is an accordion component that can be used to hide and show
          content. The label can be changed and the content can be hidden by
          default.
        </p>
      </Accordion>
      <Accordion :label="label || 'Accordion component'" class="max-w-[600px]">
        <template #toolbar>
          <Button
            class="hover:bg-button-secondary-hover"
            :icon-only="true"
            icon="edit"
            type="inline"
            label="Edit"
          />
          <Button
            class="hover:bg-button-secondary-hover"
            :icon-only="true"
            icon="trash"
            type="inline"
            label="Delete"
          />
        </template>
        <p class="text-title-contrast">
          Using the slot <code>toolbar</code>, you can insert additional actions
          in the accordion heading. This is useful for components such as
          <code>InputRefBack</code>. Use <code>toggle-icon-size</code> to align
          the appearance of the accordion toggle.
        </p>
      </Accordion>
    </div>
  </InputTestContainer>
</template>
