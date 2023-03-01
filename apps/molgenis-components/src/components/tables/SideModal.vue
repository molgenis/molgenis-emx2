<template>
  <div class="side-modal p-3">
    <slot />
  </div>
</template>

<script lang="ts" setup></script>

<style scoped>
.side-modal {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  width: 20rem;
  border: 1px solid red;
}
</style>

<docs>
    <template>
        <SideModal> content </SideModal>
    </template>
</docs>
