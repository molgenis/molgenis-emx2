<template>
  <form-group v-bind="$props">
    <!-- hidden input-->
    <div class="input-group">
      <input :id="id" ref="file" type="file" style="display:none" @change="handleFileUpload" />
      <input
        class="form-control active"
        :class="{'is-invalid':error}"
        :placeholder="filename"
        @click="$refs.file.click()"
        @keydown.prevent
      />
      <div class="input-group-append">
        <button
          class="btn"
          :class="{'btn-outline-primary':!error,'btn-outline-danger':error }"
          type="button"
          title="Toggle"
          data-toggle
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
          :class="{'btn-outline-primary':!error,'btn-outline-danger':error }"
          type="button"
          title="Toggle"
          data-toggle
          @click="$refs.file.click()"
        >Browse</button>
      </div>
    </div>
  </form-group>
</template>

<style scoped>
.form-control.is-invalid {
  background-image: none;
}
</style>

<script>
import _baseInput from './_baseInput.vue'

export default {
  extends: _baseInput,
  computed: {
    filename () {
      if (this.value) return this.value.name
      return null
    }
  },
  methods: {
    handleFileUpload () {
      this.value = this.$refs.file.files[0]
    },
    clearInput () {
      alert('clear')
      this.$refs.file.value = ''
      this.value = null
    }
  }
}
</script>

<docs>
Example
```
<template>
  <div>
    <InputFile label="My file input" v-model="check" />
    Selected: {{check}}
  </div>
</template>
<script>
export default {
  data: function() {
    return {
      check: null
    };
  },
  methods: {
    clear() {
      this.check = null;
    }
  }
};
</script>
```

Example with error
```
<InputFile label="My file input" error="Some error" />
```
</docs>
