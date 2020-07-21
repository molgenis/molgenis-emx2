<script>
import InputRadio from "./InputRadio";

/** Show true/false switch */
export default {
  extends: InputRadio,
  props: {
    options: {
      default: () => [true, false]
    }
  }
};
</script>

<docs>
    Example
    ```
    <InputBoolean label="My first boolean" help="do you need some boolean help?"/>
    ```
    Example with defaultValue
    ```
    <template>
        <div>
            <InputBoolean v-model="value" :defaultValue="value" label="My first boolean"
                          help="do you need some boolean help?"/>
            value: {{value}}
        </div>
    </template>
    <script>
        export default {
            data() {
                return {
                    value: false
                }
            }
        }
    </script>
    ```
</docs>
