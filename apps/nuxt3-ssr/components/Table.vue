<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <table class="w-full table-auto">
    <thead>
      <slot name="head"></slot>
    </thead>
    <tbody>
      <slot name="body"></slot>
    </tbody>
    <tfoot>
      <slot name="foot"></slot>
    </tfoot>
  </table>
</template>
