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
        :name="id"
        type="checkbox"
        :value="getPkey(row)"
        v-model="arrayValue"
        @change="$emit('input', value)"
      />
      <input
        v-else
        class="form-check-input"
        :name="id"
        type="radio"
        :value="getPkey(row)"
        v-model="arrayValue[0]"
        @change="$emit('input', value)"
      />
      <label class="form-check-label">
        {{ flattenObject(getPkey(row)) }}
      </label>
    </div>
    <ButtonAlt class="pl-1" icon="fa fa-search" @click="showSelect = true">
      more
    </ButtonAlt>
    <LayoutModal v-if="showSelect" :title="title" @close="closeSelect">
      <template v-slot:body>
        <MessageError v-if="error">{{ error }}</MessageError>
        <TableSearch
          v-if="list"
          v-model="arrayValue"
          :table="refTable"
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
          :table="refTable"
          :filter="filter"
          @select="select($event)"
          @deselect="deselect($event)"
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
    refTable: String,
    filter: Object,
  },
  computed: {
    title() {
      return "Select " + this.refTable;
    },
    //overrides TableMixin
    table() {
      return this.refTable;
    },
  },
  methods: {
    loadOptions() {},
    select(event) {
      this.showSelect = false;
      //in case of radio button, it is the first
      this.arrayValue[0] = event;
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
};
</script>

<docs>
You have to be have server running and be signed in for this to work

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
        label="My pets"
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
