<template>
  <form v-on:submit.prevent="$emit('submit')"><slot /></form>
</template>

<script setup lang="ts"></script>
