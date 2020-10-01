<template>
  <FormGroup v-bind="$props">
    <InputAppend
      v-for="(el, idx) in arrayValue"
      :key="idx"
      v-bind="$props"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      :showClear="showClear(idx)"
      @add="addRow"
    >
      <select
        :id="id"
        @click="openSelect(idx)"
        :class="{ 'form-control': true, 'is-invalid': error }"
      >
        <option
          v-if="arrayValue[idx] && !showSelect"
          :value="arrayValue[idx]"
          selected
        >
          {{ flattenObject(arrayValue[idx]) }}
        </option>
      </select>
    </InputAppend>
    <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
      <template v-slot:body>
        <MessageError v-if="error">{{ error }}</MessageError>
        <TableSearch
          :table="refTable"
          :defaultValue="[arrayValue[selectIdx]]"
          :filter="filter"
          @select="select($event, selectIdx)"
          @deselect="deselect(selectIdx)"
          :graphqlURL="graphqlURL"
        >
          <template v-slot:rowheader="slotProps">
            <ButtonAction @click="select(slotProps.rowkey)"
              >Select
            </ButtonAction>
          </template>
        </TableSearch>
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="closeSelect">Close</ButtonAlt>
      </template>
    </LayoutModal>
  </FormGroup>
</template>

<script>
import _baseInput from "./_baseInput";
import TableSearch from "../tables/TableSearch";
import LayoutModal from "../layout/LayoutModal";
import MessageError from "./MessageError";
import FormGroup from "./_formGroup";
import ButtonAlt from "./ButtonAlt";
import InputAppend from "./_inputAppend";
import ButtonAction from "./ButtonAction";

export default {
  extends: _baseInput,
  data: function() {
    return {
      showSelect: false,
      selectIdx: null
    };
  },
  components: {
    TableSearch,
    MessageError,
    LayoutModal,
    FormGroup,
    ButtonAction,
    ButtonAlt,
    InputAppend
  },
  props: {
    /** change if graphql URL != 'graphql'*/
    graphqlURL: {
      default: "graphql",
      type: String
    },
    refTable: String,
    filter: Object
  },
  computed: {
    title() {
      return "Select " + this.refTable;
    }
  },
  methods: {
    select(event) {
      this.showSelect = false;
      this.arrayValue[this.selectIdx] = event;
      this.emitValue();
    },
    closeSelect() {
      this.showSelect = false;
    },
    openSelect(idx) {
      this.showSelect = true;
      this.selectIdx = idx;
    },
    deselect(idx) {
      this.showSelect = false;
      this.clearValue(idx);
      this.emitValue();
    },
    flattenObject(object) {
      let result = "";
      Object.keys(object).forEach(key => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === "object") {
          result += this.flattenObject(object[key]);
        } else {
          result += " " + object[key];
        }
      });
      return result;
    }
  }
};
</script>

<docs>
Example
```
<template>
  <div>
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <InputRef v-model="value" refTable="Pet" graphqlURL="/pet store/graphql"/>
    Selection: {{ value }}
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
Example with default value
```
<template>
  <div>
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <InputRef
        v-model="value"
        refTable="Pet"
        :defaultValue="value"
        graphqlURL="/pet store/graphql"
    />
    Selection: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: {name: 'spike'}
      };
    }
  };
</script>
```
Example with filter
```
<template>
  <div>
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <InputRef
        v-model="value"
        refTable="Pet"
        :filter="{category:{name:'dog'}}"
        :defaultValue="value"
        graphqlURL="/pet store/graphql"
    />
    Selection: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: {name: 'spike'}
      };
    }
  };
</script>
```
Example with list
```
<template>
  <div>
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <InputRef :list="true"
              v-model="value"
              refTable="Pet"
              :defaultValue="[{name:'spike'},{name:'pooky'}]"
              graphqlURL="/pet store/graphql"
    />
    Selection: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: ['spike']
      };
    }
  };
</script>
```
</docs>
