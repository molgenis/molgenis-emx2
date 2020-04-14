<template>
  <form-group v-bind="$props">
    <div class="input-group">
      <flat-pickr
        v-model="value"
        style="background: white"
        class="form-control active"
        :class="{ 'is-invalid': error }"
        :config="config"
        :placeholder="placeholder"
      ></flat-pickr>
      <div class="input-group-append">
        <button
          class="btn"
          :class="{
            'btn-outline-primary': !error,
            'btn-outline-danger': error
          }"
          type="button"
          title="Toggle"
          data-toggle
        >
          <i class="fa fa-calendar">
            <span aria-hidden="true" class="sr-only">Toggle</span>
          </i>
        </button>
        <button
          class="btn"
          :class="{
            'btn-outline-primary': !error,
            'btn-outline-danger': error
          }"
          type="button"
          title="Clear"
          data-clear
        >
          <i class="fa fa-times">
            <span aria-hidden="true" class="sr-only">Clear</span>
          </i>
        </button>
      </div>
    </div>
  </form-group>
</template>

<script>
import _baseInput from './_baseInput.vue'
import flatPickr from 'vue-flatpickr-component'
import 'flatpickr/dist/flatpickr.css'
//import '../../../public/css/bootstrap-molgenis-blue.css'

/** Show a data input */
export default {
  components: {
    flatPickr
  },
  extends: _baseInput,
  data: function() {
    return {
      config: {
        wrap: true, // set wrap to true only when using 'input-group'
        dateFormat: 'Y-m-d',
        allowInput: false
      }
    }
  }
}
</script>

<docs>
    Example
    ```
    <template>
        <div>
            <InputDate v-model="value" label="My date input label" help="Some help needed?"/>
            <br/>
            You typed: {{value}}
        </div>
    </template>

    <script>
        export default {
            data: function () {
                return {
                    value: null
                };
            }
        };
    </script>
    ```
    Example with default
    ```
    <template>
        <div>
            <InputDate
                    v-model="value"
                    label="My date input label"
                    defaultValue="2020-01-10"
                    help="Some help needed?"
            />
            <br/>
            You typed: {{value}}
        </div>
    </template>

    <script>
        export default {
            data: function () {
                return {
                    value: null
                };
            }
        };
    </script>
    ```
    Example with error set
    ```
    <InputDate v-model="value" label="My date input label" error="Some error message is shown"/>
    ```
</docs>
