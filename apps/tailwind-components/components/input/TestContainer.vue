<script setup lang="ts">
const placeholder = ref("");
const state = ref('neutral');
</script>

<template>
  <div class="flex">
  <div class="flex-1 "><slot :error="state == 'error'" :placeholder="placeholder" :valid="state == 'valid'" :disabled="state == 'disabled'"></slot></div>
    <div class="flex-1 bg-white ml-2">
      <h2>Standard input test settings: </h2>
      Placeholder: <input type="text"  v-model="placeholder"/>
      <br/>
      state is neutral:  <input type="radio" value="neutral" v-model="state" />
      <br/>
       state has error:  <input type="radio" value="error" v-model="state" />
      <br/>
      state is valid:  <input type="radio" value="valid" v-model="state" />
      <br/>
      state is disabled:  <input type="radio" value="disabled"  v-model="state" />
    </div>
  </div>
</template>