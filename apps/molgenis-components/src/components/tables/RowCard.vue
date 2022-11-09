<template>
  <div class="card">
    <div class="card-body" @click="$emit('click', row)">
      <div class="card-text">
        <span class="float-right">
          <RowButton
            v-if="canEdit"
            type="edit"
            class="d-inline"
            :table="tableName"
            :pkey="getKey(row)"
            @close="$emit('reload')"
            @edit="$emit('edit', row)"
          />
          <RowButton
            v-if="canEdit"
            type="delete"
            class="mt-0"
            :table="tableName"
            :pkey="getKey(row)"
            @close="$emit('reload')"
            @delete="$emit('delete', row)"
          />
        </span>
        <VueTemplate v-if="template" :template="template" :row="row" />
        <dl v-else>
          <div v-for="col in columns" :key="col.id">
            <dt
              v-if="col.showColumn && row[col.id] && col.id != 'mg_tableclass'"
              class="pr-3"
            >
              {{ col.name }}
            </dt>
            <dd
              class="pl-3"
              v-if="col.showColumn && row[col.id] && col.id != 'mg_tableclass'"
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
import RenderValue from "./RenderValue.vue";
import RowButton from "./RowButton.vue";
import VueTemplate from "../layout/VueTemplate.vue";

export default {
  components: {
    RenderValue,
    RowButton,
    VueTemplate,
  },
  props: {
    columns: Array,
    tableName: String,
    row: Object,
    template: String,
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
