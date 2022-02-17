<template>
  <FormGroup v-bind="$props" v-on="$listeners">
    <Spinner v-if="loading" />
    <div v-else>
      <div v-if="list && count > maxNum">
        <FilterWell
          v-for="(item, key) in selection"
          :key="JSON.stringify(item)"
          :label="flattenObject(item)"
          @click="deselect(key)"
        />
        <ButtonAlt class="pl-1" icon="fa fa-clear" @click="emitClear">
          clear selection
        </ButtonAlt>
      </div>
      <div v-else>
        <ButtonAlt
          v-if="selection.length > 0"
          class="pl-1"
          icon="fa fa-clear"
          @click="emitClear"
        >
          clear selection
        </ButtonAlt>
      </div>
      <div
        :class="
          showMultipleColumns ? 'd-flex align-content-stretch flex-wrap' : ''
        "
      >
        <div
          class="form-check custom-control custom-checkbox"
          :class="showMultipleColumns ? 'col-12 col-md-6 col-lg-4' : ''"
          v-for="(row, index) in notSelectedRows"
          :key="index"
        >
          <input
            v-if="list"
            :id="id + index"
            class="form-check-input"
            type="checkbox"
            :value="getPkey(row)"
            v-model="selection"
            @change="$emit('input', selection)"
          />
          <input
            v-else
            class="form-check-input"
            :id="id + index"
            :name="id"
            type="radio"
            :value="getPkey(row)"
            v-model="selection"
            @change="$emit('input', getPkey(row))"
          />
          <label class="form-check-label" :for="id + index">
            {{ flattenObject(getPkey(row)) }}
          </label>
        </div>
        <ButtonAlt
          class="pl-0"
          :class="showMultipleColumns ? 'col-12 col-md-6 col-lg-4' : ''"
          icon="fa fa-search"
          @click="showSelect = true"
        >
          {{
            count > limit ? 'view all ' + count + ' options.' : 'view as table'
          }}
        </ButtonAlt>
      </div>
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
          />
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
          />
        </template>
        <template v-slot:footer>
          <ButtonAlt @click="closeSelect">Close</ButtonAlt>
        </template>
      </LayoutModal>
    </div>
  </FormGroup>
</template>

<script>
import _baseInput from './_baseInput';
import TableSearch from '../tables/TableSearch';
import LayoutModal from '../layout/LayoutModal';
import MessageError from './MessageError';
import FormGroup from './_formGroup';
import ButtonAlt from './ButtonAlt';
import TableMixin from '../mixins/TableMixin';
import FilterWell from '../tables/FilterWell';
import Spinner from '../layout/Spinner';

export default {
  extends: _baseInput,
  mixins: [TableMixin],
  data: function () {
    return {
      showSelect: false,
      selectIdx: null,
      options: [],
      id: Math.random(),
      selection: []
    };
  },
  components: {
    Spinner,
    TableSearch,
    MessageError,
    LayoutModal,
    FormGroup,
    ButtonAlt,
    FilterWell
  },
  props: {
    /** change if graphql URL != 'graphql'*/
    graphqlURL: {
      default: 'graphql',
      type: String
    },
    filter: Object,
    multipleColumns: Boolean,
    maxNum: {type: Number, default: 11}
  },
  computed: {
    title() {
      return 'Select ' + this.table;
    },
    notSelectedRows() {
      if (this.data) {
        let result = this.data.filter(
          (row) =>
            this.count <= this.maxNum ||
            !this.selection.some(
              (v) =>
                this.flattenObject(this.getPkey(row)) == this.flattenObject(v)
            )
        );
        //truncate on maxNum (we overquery because we include all selected which might not be in query)
        result.length = Math.min(result.length, this.maxNum);
        return result;
      }
      return [];
    },
    showMultipleColumns() {
      return this.multipleColumns && this.count > 12;
    }
  },
  methods: {
    deselect(key) {
      this.selection.splice(key, 1);
      this.$emit('input', this.selection);
    },
    emitClear() {
      if (this.list) this.$emit('input', []);
      else this.$emit('input', null);
    },
    select(event) {
      if (this.list) {
        this.$emit('input', this.selection);
      } else {
        this.$emit('input', event);
      }
    },
    closeSelect() {
      this.showSelect = false;
      this.reload();
      if (this.list) {
        this.$emit('input', this.selection);
      }
    },
    openSelect(idx) {
      this.showSelect = true;
      this.selectIdx = idx;
    },
    flattenObject(object) {
      let result = '';
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === 'object') {
          result += this.flattenObject(object[key]);
        } else {
          result += ' ' + object[key];
        }
      });
      return result;
    }
  },
  watch: {
    value() {
      this.selection = this.value ? this.value : [];
      //we overquery because we include all selected which might not be in query
      this.limit = this.maxNum + this.selection.length;
    }
  },
  created() {
    this.limit = this.maxNum + this.selection.length;
    this.selection = this.value ? this.value : [];
    this.reloadMetadata();
  }
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
              table="Tag"
              graphqlURL="/pet store/graphql"
    />
    Selection: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: []
      };
    }
  };
</script>
```
</docs>
