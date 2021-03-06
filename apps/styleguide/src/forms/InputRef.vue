<template>
  <FormGroup v-bind="$props">
    <div
      class="form-check custom-control custom-checkbox"
      v-for="row in data"
      :key="JSON.stringify(row)"
    >
      <input
        v-if="list"
        class="form-check-input"
        type="checkbox"
        :value="getPkey(row)"
        v-model="selection"
        @change="$emit('input', selection)"
      />
      <input
        v-else
        class="form-check-input"
        :name="id"
        type="radio"
        :value="getPkey(row)"
        v-model="selection"
        @change="$emit('input', getPkey(row))"
      />
      <label class="form-check-label">
        {{ flattenObject(getPkey(row)) }}
      </label>
    </div>
    <ButtonAlt class="pl-1" icon="fa fa-search" @click="showSelect = true">
      more
    </ButtonAlt>
    <ButtonAlt class="pl-1" icon="fa fa-clear" @click="emitClear">
      clear
    </ButtonAlt>
    <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
      <template v-slot:body>
        <MessageError v-if="errorMessage">{{ graphqlError }}</MessageError>
        <TableSearch
          v-if="list"
          :selection.sync="selection"
          :table="table"
          :filter="filter"
          :graphqlURL="graphqlURL"
          :showSelect="true"
          :limit="10"
        >
          <template name="colheader">
            <RowButtonAdd v-if="canEdit" :table="table" @close="reload" />
          </template>
        </TableSearch>
        <TableSearch
          v-else
          :selection="[valueArray[0]]"
          :table="table"
          :filter="filter"
          @select="select($event)"
          @deselect="emitClear"
          :graphqlURL="graphqlURL"
          :showSelect="true"
          :limit="10"
        >
          <template name="colheader">
            <RowButtonAdd v-if="canEdit" :table="table" @close="reload" />
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
import TableMixin from "../mixins/TableMixin";

export default {
  extends: _baseInput,
  mixins: [TableMixin],
  data: function () {
    return {
      showSelect: false,
      selectIdx: null,
      options: [],
      id: Math.random(),
      selection: [],
    };
  },
  components: {
    TableSearch,
    MessageError,
    LayoutModal,
    FormGroup,
    ButtonAlt,
  },
  props: {
    /** change if graphql URL != 'graphql'*/
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    filter: Object,
  },
  computed: {
    title() {
      return "Select " + this.table;
    },
  },
  methods: {
    emitClear() {
      if (this.list) this.$emit("input", []);
      else this.$emit("input", null);
    },
    select(event) {
      if (this.list) {
        this.$emit("input", this.selection);
      } else {
        this.$emit("input", event);
      }
    },
    closeSelect() {
      this.showSelect = false;
      if (this.list) {
        this.$emit("input", this.selection);
      }
    },
    openSelect(idx) {
      this.showSelect = true;
      this.selectIdx = idx;
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
  },
  watch: {
    value() {
      this.selection = this.value ? this.value : [];
    },
  },
  created() {
    this.limit = 8;
    this.selection = this.value ? this.value : [];
    this.reloadMetadata();
  },
};
</script>

<docs>
You have to be have server running and be signed in for this to work

Example
```
<template>
  <div>
    <!-- normally you don't need graphqlURL, default url = 'graphql' just works -->
    <InputRef v-model="value" table="Pet" graphqlURL="/pet store/graphql"/>
    Selection: {{ JSON.stringify(value) }}
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
        label="My pets"
        v-model="value"
        table="Pet"
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
        table="Pet"
        :filter="{category:{name:{equals:'dog'}}}"
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
        value: [{'name': 'spike'}]
      };
    }
  };
</script>
```
</docs>
