/** abstract component that is used as superclass, will not be shown in style
guide */

<script>
const uuidv4 = require('uuid/v4');

export default {
  props: {
    /** whether to show clear buttons */
    clear: {
      type: Boolean,
      default: true
    },
    description: String,
    /** whether metadata can be edited */
    editMeta: Boolean,
    errorMessage: null,
    inplace: Boolean,
    label: String,
    list: {
      type: Boolean,
      default: false
    },
    parser: Function,
    placeholder: String,
    readonly: {
      type: Boolean,
      default: false
    },
    required: {
      type: Boolean,
      default: false
    },
    value: {type: [String, Number, Object, Array, Boolean], default: null}
  },
  data() {
    return {
      id: uuidv4(),
      /** whether list input should show empty input */
      showNewItem: false,
      focus: false
    };
  },
  computed: {
    valueArray() {
      let result = this.value;
      if (!Array.isArray(result)) {
        result = [result];
      }
      result = this.removeNulls(result);
      if (result.length === 0 || (this.list && this.showNewItem)) {
        result.push(null);
      }
      return result;
    }
  },
  methods: {
    removeNulls(arr) {
      return arr.filter((v) => v === 0 || v);
    },
    //emit update with new item on list
    emitValue(event, index) {
      const value = event ? (event.target ? event.target.value : event) : null;
      if (this.list) {
        this.$emit(
          'input',
          this.updateValueArrayValue(this.valueArray, value, index)
        );
      } else {
        this.$emit('input', this.useParserIfAvailable(value));
      }
    },
    updateValueArrayValue(valueArray, value, index) {
      let newValueArray = valueArray;
      newValueArray[index] = this.useParserIfAvailable(value);
      if (
        this.showNewItem &&
        newValueArray[newValueArray.length - 1] !== null
      ) {
        this.showNewItem = false;
      }
      return this.removeNulls(newValueArray);
    },
    useParserIfAvailable(value) {
      return this.parser ? this.parser(value) : value;
    },
    toggleFocus() {
      this.focus = !this.focus;
    },
    addRow() {
      this.showNewItem = true;
    },
    clearValue(idx) {
      let result = this.valueArray;
      if (this.list) {
        result.splice(idx, 1);
      } else {
        result = null;
      }
      this.$emit('input', result);
    },
    showPlus(idx) {
      //always on last line
      return (
        this.list &&
        !this.showNewItem &&
        idx === this.valueArray.length - 1 &&
        this.valueArray[idx] !== null
      );
    },
    //always show on empty lines in list view
    showClear() {
      return this.clear && !this.readonly && this.value !== null;
    },
    showMinus(idx) {
      return this.list && !this.showPlus(idx);
    }
  },
  directives: {
    focus: {
      inserted(el, binding) {
        if (binding.value) {
          el.focus();
        }
      }
    }
  }
};
</script>
