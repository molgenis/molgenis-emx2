<template>
  <FormGroup :id="id" :label="label" :description="description">
    <div class="input-group">
      <input
        :id="id"
        ref="file"
        type="file"
        style="display: none"
        @change="handleFileUpload"
      />
      <input
        class="form-control active"
        :placeholder="filename"
        @click="$refs.file.click()"
        @keydown.prevent
      />
      <div class="input-group-append">
        <button
          class="btn bg-transparent text-primary"
          type="button"
          title="Toggle"
          data-toggle
          @click="clearInput"
          style="margin-left: -40px; z-index: 100"
        >
          <i class="fa fa-times">
            <span aria-hidden="true" class="sr-only">Clear</span>
          </i>
        </button>
      </div>
      <div class="input-group-append">
        <button
          class="btn btn-outline-primary"
          type="button"
          title="Toggle"
          data-toggle
          @click="$refs.file.click()"
        >
          Browse
        </button>
      </div>
    </div>
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";

export default {
  extends: BaseInput,
  components: { FormGroup },
  computed: {
    filename() {
      if (this.value) {
        return this.value.name;
      } else {
        return null;
      }
    },
  },
  methods: {
    handleFileUpload() {
      this.$emit("input", this.$refs.file.files[0]);
    },
    clearInput() {
      this.$refs.file.value = "";
      this.$emit("input", null);
    },
  },
};
</script>

<docs>
<template>
  <DemoItem>
    <InputFile id='input-file' label="My file input" description="Example file upload" v-model="uploadedFile"/>
    Selected: {{ uploadedFile }}
  </DemoItem>
</template>
<script>
  export default {
    data: function () {
      return {
        uploadedFile: null
      };
    }
  };
</script>
</docs>
