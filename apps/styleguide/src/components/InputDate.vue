<template>
  <form-group v-bind="$props">
    <div class="input-group" v-for="(el, idx) in arrayValue" :key="idx">
      <input
        v-if="readonly"
        readonly
        :value="arrayValue[idx]"
        :class="{ 'form-control': true, 'is-invalid': error }"
      />
      <FlatPickr
        v-else
        v-model="arrayValue[idx]"
        :defaultDate="arrayValue[idx]"
        style="background: white"
        class="form-control active"
        :class="{ 'is-invalid': error }"
        :config="config"
        :placeholder="placeholder"
        :disabled="readonly"
      />
      <div class="input-group-append">
        <AppendPlus v-if="showPlus(idx)" v-bind="$props" @add="addRow" />
        <AppendClear
          v-if="showClear(idx)"
          v-bind="$props"
          @clear="clearValue(idx)"
        />
        <button
          class="btn"
          :class="{
            'btn-outline-primary': !error,
            'btn-outline-danger': error
          }"
          type="button"
          title="Toggle"
          :disabled="readonly"
          data-toggle
        >
          <i class="fa fa-calendar">
            <span aria-hidden="true" class="sr-only">Toggle</span>
          </i>
        </button>
      </div>
    </div>
  </form-group>
</template>

<script>
import _baseInput from './_baseInput.vue'
import FlatPickr from 'vue-flatpickr-component'
import 'flatpickr/dist/flatpickr.css'
import AppendPlus from './_appendPlus'
import AppendClear from './_appendClear'

//import '../../../public/css/bootstrap-molgenis-blue.css'

/** Show a data input */
export default {
  extends: _baseInput,
  components: {
    FlatPickr,
    AppendPlus,
    AppendClear
  },
  computed: {
    config() {
      return {
        wrap: true, // set wrap to true only when using 'input-group'
        dateFormat: 'Y-m-d',
        allowInput: false,
        clickOpens: !this.readonly
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
    Example readonly with default value
    ```
    <template>
        <div>
            <InputDate :readonly="true" :defaultValue="value" v-model="value" label="My date input label"
                       help="Some help needed?"/>
            <br/>
            You typed: {{value}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    value: '2020-1-1'
                };
            }
        };
    </script>
    ```
    Example with default value
    ```
    <template>
        <div>
            <InputDate
                    v-model="value"
                    label="My date input label"
                    :defaultValue="value"
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
                    value: '2020-01-10'
                };
            }
        };
    </script>
    ```
    Example with error set
    ```
    <InputDate label="My date input label" error="Some error message is shown"/>
    ```
    Example with list set
    ```
    <template>
        <div>
            <InputDate :list="true" v-model="value" :defaultValue="value" label="My date input label"/>
            Value: {{value}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    value: [null]
                };
            }
        };
    </script>
    ```
    Example with list default
    ```
    <template>
        <div>
            <InputDate :list="true" v-model="value" :defaultValue="value" label="My date input label"/>
            Value: {{value}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    value: ['2020-1-1', '2020-1-2']
                };
            }
        };
    </script>
    ```
</docs>
