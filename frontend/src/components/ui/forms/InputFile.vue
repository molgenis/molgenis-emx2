<template>
  <form-group v-bind="$props">
    <!-- hidden input-->
    <div class="input-group">
      <input
        :id="id"
        ref="file"
        style="display: none;"
        type="file"
        @change="handleFileUpload"
      >
      <input
        class="form-control active"
        :class="{ 'is-invalid': errorMessage }"
        :placeholder="filename"
        @click="$refs.file.click()"
        @keydown.prevent
      >
      <div class="input-group-append">
        <button
          class="btn bg-transparent"
          :class="{
            'text-primary': !errorMessage,
            'text-danger': errorMessage,
          }"
          data-toggle
          style="margin-left: -40px; z-index: 100;"
          title="Toggle"
          type="button"
          @click="clearInput"
        >
          <i class="fa fa-times">
            <span aria-hidden="true" class="sr-only">Clear</span>
          </i>
        </button>
      </div>
      <div class="input-group-append">
        <button
          class="btn"
          :class="{
            'btn-outline-primary': !errorMessage,
            'btn-outline-danger': errorMessage,
          }"
          data-toggle
          title="Toggle"
          type="button"
          @click="$refs.file.click()"
        >
          Browse
        </button>
      </div>
    </div>
    <div v-for="val in valueArray" :key="JSON.stringify(val)">
      <a v-if="val && val.url" :href="val.url">
        Previous value: {{ name }}.{{ val.extension }}
      </a>
    </div>
    <br>
  </form-group>
</template>

<script>
import _baseInput from './_baseInput.vue'

export default {
  extends: _baseInput,
  emits: ['update:modelValue'],
  computed: {
    filename() {
      if (this.value) return this.value.name
      return null
    },
  },
  methods: {
    clearInput() {
      this.$refs.file.value = ''
      this.value = null
      this.$emit('update:modelValue', this.value)
    },
    handleFileUpload() {
      this.value = this.$refs.file.files[0]
      this.$emit('update:modelValue', this.value)
    },
  },
}
</script>

<style scoped>
.form-control.is-invalid {
  background-image: none;
}
</style>

