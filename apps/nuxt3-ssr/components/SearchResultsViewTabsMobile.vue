<template>
  <div class="flex justify-between">
    <SideModal :fullScreen="false">
      <template #button>
        <Button
          type="primary"
          size="small"
          label="filters"
          icon="filter"
          iconPosition="left"
        ></Button>
      </template>

      <ContentBlockModal title="Filters">
        <slot></slot>
      </ContentBlockModal>

      <template #footer="{ hide }">
        <Button
          type="secondary"
          size="small"
          label="View results"
          iconPosition="left"
          @click="hide()"
        ></Button>
      </template>
    </SideModal>
  </div>
</template>
