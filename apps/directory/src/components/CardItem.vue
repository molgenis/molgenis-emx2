<template>
  <div class="card-item mx-1">
    <div class="ml-1 pt-2 d-flex">
      <slot name="title"></slot>
    </div>

    <slot></slot>
  </div>
</template>

<style>
.card-item {
  word-break: break-word;
}

.card-item th {
  width: 25%;
}
</style>
