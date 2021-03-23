<template>
  <div class="card">
    <div class="card-body" @click="$emit('click', row)">
      <div class="card-text">
        <span class="float-right">
          <RowButtonDelete
            v-if="canEdit"
            class="d-inline"
            :pkey="getKey(row)"
            :table="tableName"
            @close="$emit('reload')"
          />
          <RowButtonEdit
            v-if="canEdit"
            class="mt-0"
            :pkey="getKey(row)"
            :table="tableName"
            @close="$emit('reload')"
          />
        </span>
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
              v-if="
                col.showColumn && row[col.name] && col.name != 'mg_tableclass'
              "
              class="pl-3"
            >
              <RenderValue :col="col" :row="row" />
            </dd>
          </div>
        </dl>
      </div>
    </div>
  </div>
</template>

<script>
import RenderValue from './RenderValue.vue'
import RowButtonDelete from './RowButtonDelete.vue'
import RowButtonEdit from './RowButtonEdit.vue'

export default {
  components: {
    RenderValue,
    RowButtonDelete,
    RowButtonEdit,
  },
  props: {
    canEdit: Boolean,
    columns: Array,
    row: Object,
    tableName: String,
  },
  emits: ['click', 'reload'],
  methods: {
    getKey(row) {
      let result = {}
      this.columns
        .filter((c) => c.key == 1)
        .map((c) => (result[c.name] = row[c.name]))
      return result
    },
  },
}
</script>

<style scoped>
dl {
  overflow: hidden;
  width: 100%;
}

dt {
  float: left;
  text-align: right;
  width: 35%;
}

dd {
  float: left;
  margin: 0;
  /* adjust the width; make sure the total of both is 100% */
  padding: 0;
  width: 65%;
}
</style>
