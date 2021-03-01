<template>
  <div class="card">
    <div class="card-header d-flex justify-content-between">
      <h6 class="card-title mb-0 d-flex d-inline pt-2">
        <span
          v-for="col in columns.filter((c) => c.key == 1)"
          :key="col.name + '_head'"
        >
          <RenderValue
            v-if="col.key == 1 && row[col.name]"
            :col="col"
            :row="row"
          />
        </span>
      </h6>
      <span>
        <RowButtonDelete
          class="d-inline"
          v-if="canEdit"
          :table="tableName"
          :pkey="getKey(row)"
          @close="$emit('reload')" />
        <RowButtonEdit
          class="mt-0"
          v-if="canEdit"
          :table="tableName"
          :pkey="getKey(row)"
          @close="$emit('reload')"
      /></span>
    </div>
    <div class="card-body" @click="$emit('click', row)">
      <div class="card-text">
        <dl>
          <div v-for="col in columns" :key="col.name">
            <dt
              v-if="
                col.showColumn && row[col.name] && col.name != 'mg_tableclass'
              "
              class="pr-3"
            >
              {{ col.name }}
            </dt>
            <dd
              class="pl-3"
              v-if="
                col.showColumn && row[col.name] && col.name != 'mg_tableclass'
              "
            >
              <RenderValue :col="col" :row="row" />
            </dd>
          </div>
        </dl>
      </div>
    </div>
  </div>
</template>

<style scoped>
dl {
  width: 100%;
  overflow: hidden;
}

dt {
  float: left;
  width: 35%;
  text-align: right;
}

dd {
  float: left;
  width: 65%;
  /* adjust the width; make sure the total of both is 100% */
  padding: 0;
  margin: 0;
}
</style>

<script>
import RenderValue from "./RenderValue";
import RowButtonEdit from "./RowButtonEdit";
import RowButtonDelete from "./RowButtonDelete";

export default {
  components: {
    RenderValue,
    RowButtonEdit,
    RowButtonDelete,
  },
  props: {
    columns: Array,
    tableName: String,
    row: Object,
    canEdit: Boolean,
  },
  methods: {
    getKey(row) {
      let result = {};
      this.columns
        .filter((c) => c.key == 1)
        .map((c) => (result[c.name] = row[c.name]));
      return result;
    },
  },
};
</script>
