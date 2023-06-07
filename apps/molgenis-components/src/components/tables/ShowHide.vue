<template>
  <ButtonDropdown :icon="icon" :label="label" v-slot="scope">
    <form class="px-4 py-3" style="min-width: 15rem">
      <IconAction icon="times" @click="scope.close" class="float-right" />
      <h6>
        {{ label }}
      </h6>
      <b>
        data (
        <ButtonAlt class="pl-0" @click="showAll(true)">all</ButtonAlt>
        <ButtonAlt class="p-0" @click="hideAll(true)">none</ButtonAlt>
        )
      </b>
      <div v-for="(col, key) in columns" :key="key">
        <span v-if="exclude.length == 0 || !exclude.includes(col.columnType)">
          <b class="ml-0" v-if="col.name == 'mg_draft'">
            metadata (
            <ButtonAlt class="pl-0" @click="showAll(false)">all</ButtonAlt>
            <ButtonAlt class="p-0" @click="hideAll(false)">none</ButtonAlt>
            )
          </b>
          <div class="form-check" v-if="col.columnType != 'HEADING'">
            <input
              class="form-check-input"
              type="checkbox"
              :checked="
                col[checkAttribute] == undefined
                  ? defaultValue
                  : col[checkAttribute]
              "
              @input.prevent="change(key, !col[checkAttribute])"
              :id="col.name" />
            <label class="form-check-label" :for="col.name">
              {{ col.name }}
            </label>
          </div>
        </span>
      </div>
    </form>
  </ButtonDropdown>
</template>

<script>
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonDropdown from "../forms/ButtonDropdown.vue";
import IconAction from "../forms/IconAction.vue";

export default {
  components: { ButtonAlt, ButtonDropdown, IconAction },
  props: {
    label: {
      type: String,
      required: true,
    },
    icon: {
      type: String,
      required: true,
    },
    columns: Array,
    exclude: {
      type: Array,
      default: () => [],
    },
    checkAttribute: String,
    defaultValue: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    value(col) {
      return col[this.checkAttribute] == undefined
        ? this.defaultValue
        : col[this.checkAttribute];
    },
    change(key, value) {
      let update = JSON.parse(JSON.stringify(this.columns));
      update[key][this.checkAttribute] = value;
      this.$emit("update:columns", update);
    },
    hideAll(data) {
      let update = JSON.parse(JSON.stringify(this.columns));
      for (var key in update) {
        if (
          (data && !update[key].name.startsWith("mg_")) ||
          (!data && update[key].name.startsWith("mg_"))
        ) {
          update[key][this.checkAttribute] = false;
        }
      }
      this.$emit("update:columns", update);
    },
    showAll(data) {
      let update = JSON.parse(JSON.stringify(this.columns));
      for (var key in update) {
        if (
          (data && !update[key].name.startsWith("mg_")) ||
          (!data && update[key].name.startsWith("mg_"))
        ) {
          update[key][this.checkAttribute] = true;
        }
      }
      this.$emit("update:columns", update);
    },
  },
  emits: ["update:columns"],
};
</script>

<docs>
<template>
  <demo-item>
    <ShowHide
      label="filters"
      icon="filter"
      checkAttribute="showFilter"
      :columns ="columns"
      :exclude="['HEADING', 'FILE']"
      @update:columns="onUpdateColumns"
    />

    <ShowHide
      class="ml-2"
      label="columns"
      icon="columns"
      checkAttribute="showColumn"
      :columns ="columns"
      :defaultValue="true"
      @update:columns="onUpdateColumns"
    />
    <br/>Columns state:<br/> {{columns}}
  </demo-item>
</template>

<script>
export default {
  data: function () {
    return {
      columns: [],
    };
  },
  methods: {
    onUpdateColumns($event) {
      this.columns = $event;
    },
  },
  mounted: async function () {
    const client = this.$Client.newClient("pet store");
    const tableMetaData = await client.fetchTableMetaData("Pet");
    this.columns = tableMetaData.columns;
  },
};
</script>
</docs>
