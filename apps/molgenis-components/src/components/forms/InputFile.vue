<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
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
        :class="{ 'is-invalid': errorMessage }"
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
    <div v-if="modelValue">
      <a v-if="modelValue && modelValue.url" :href="modelValue.url">
        Previous value: {{ modelValue.id }}.{{ modelValue.extension }}
      </a>
    </div>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";

export default {
  extends: BaseInput,
  components: { FormGroup },
  data() {
    return {
      filename: this.modelValue instanceof File ? this.modelValue?.name : "",
    };
  },
  props: {
    modelValue: {
      default: null,
    },
  },
  methods: {
    handleFileUpload(event) {
      const target = event.target;
      if (target?.files) {
        this.filename = target.files[0]?.name;
        console.log(this.filename);
        this.$emit("update:modelValue", target.files[0]);
      }
    },
    clearInput() {
      this.filename = null;
      this.$emit("update:modelValue", null);
    },
  },
};
</script>

<docs>
<template>
  <div>

    <DemoItem>
      <label for="input-file-empty">Empty file upload</label>
      <InputFile id='input-file-empty' label="My file input" description="Example file upload" v-model="valueEmpty"/>
      Selected: {{ valueEmpty?.name }}
    </DemoItem>

    <DemoItem>
      <label for="input-file-set">File upload with value set</label>
      <InputFile id='input-file-set' label="My file input" description="Example file upload" v-model="valueSet"/>
      Selected: {{ valueSet }}
    </DemoItem>

  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        valueEmpty: null,
        valueSet: {
          id : "3955978b18024de4bad8fcf8e2d49b59",
          size : 107,
          extension : "csv",
          url : "/pet store/api/file/User/picture/3955978b18024de4bad8fcf8e2d49b59"
        }
      };
    }
  };
</script>
</docs>
