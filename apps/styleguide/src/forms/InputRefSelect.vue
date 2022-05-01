<template>
  <FormGroup v-bind="$props" v-on="$listeners">
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx"
      v-bind="$props"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      :showClear="showClear(idx)"
      @add="addRow"
    >
      <input v-if="readonly" type="hidden" class="form-control" />
      <select
        :id="id"
        :disabled="readonly"
        @click="openSelect(idx)"
        :class="{ 'form-control': true, 'is-invalid': errorMessage }"
      >
        <option
          v-if="valueArray[idx] && !showSelect"
          :value="valueArray[idx]"
          selected
          :readonly="readonly"
        >
          {{ refLabel ? applyJsTemplate(el) : flattenObject(el) }}
        </option>
      </select>
    </InputAppend>
    <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
      <template v-slot:body>
        <TableSearch
          :table="table"
          :filter="filter"
          @select="select($event)"
          @deselect="deselect(selectIdx)"
          :graphqlURL="graphqlURL"
        >
          <template v-slot:rowheader="slotProps">
            <ButtonAction @click="select(slotProps.rowkey)">
              Select
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
<style scoped>
::v-deep .input-append {
  margin-left: -3rem;
}
</style>
<script>
import _baseInput from "./_baseInput";
import TableSearch from "../tables/TableSearch";
import FormGroup from "./_formGroup";
import {
  ButtonAlt,
  ButtonAction,
  LayoutModal,
} from "@molgenis/molgenis-components";
import InputAppend from "./_inputAppend";

export default {
  name: "InputRefSelect",
  extends: _baseInput,
  data: function () {
    return {
      showSelect: false,
      selectIdx: null,
    };
  },
  components: {
    TableSearch,
    LayoutModal,
    FormGroup,
    ButtonAction,
    ButtonAlt,
    InputAppend,
  },
  props: {
    /** change if graphql URL != 'graphql'*/
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    table: String,
    filter: Object,
    refLabel: String,
  },
  computed: {
    title() {
      return "Select " + this.table;
    },
  },
  methods: {
    select(event) {
      this.showSelect = false;
      this.emitValue(event, this.selectIdx);
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
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === "object") {
          result += this.flattenObject(object[key]);
        } else {
          result += " " + object[key];
        }
      });
      return result;
    },
    applyJsTemplate(object) {
      const names = Object.keys(object);
      const vals = Object.values(object);
      try {
        return new Function(...names, "return `" + this.refLabel + "`;")(
          ...vals
        );
      } catch (err) {
        return (
          err.message +
          " we got keys:" +
          JSON.stringify(names) +
          " vals:" +
          JSON.stringify(vals) +
          " and template: " +
          this.refLabel
        );
      }
    },
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <InputRefSelect v-model="value" table="Pet" graphqlURL="/pet store/graphql"/>
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
    <InputRefSelect
        v-model="value"
        table="Pet"
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
    <InputRefSelect
        v-model="value"
        table="Pet"
        :filter="{category:{name: {equals:'dog'}}}"
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
    <InputRefSelect :list="true"
                    v-model="value"
                    table="Pet"
                    graphqlURL="/pet store/graphql"
    />
    Selection: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: [{name: 'spike'}]
      };
    }
  };
</script>
```
</docs>
