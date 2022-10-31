<script setup></script>

<template>
  <div class="min-h-screen bg-base-gradient">
    <slot name="header"></slot>
    <main>
      <slot></slot>
    </main>
    <slot name="footer"></slot>
  </div>
</template>
