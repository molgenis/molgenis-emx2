<template>
  <div class="card" @click="$emit('click', row)">
    <span class="float-right">
          <RowButtonDelete
            class="d-inline"
            v-if="canEdit"
            :table="tableName"
            :pkey="getKey(row)"
            @close="$emit('reload')"
          />
          <RowButtonEdit
            class="mt-0"
            v-if="canEdit"
            :table="tableName"
            :pkey="getKey(row)"
            @close="$emit('reload')"
          />
      </span>
        <VueTemplate v-if="template" :template="template" :a="row" />
        <div v-else>
          <div v-for="col in columnsSorted" :key="col.name" class="ml-0 mr-0" :class="{'border-top border-primary':col.columnType == 'H1'}">
            <span v-if="col.columnType == 'H1'" >
              <h4 class="bg-primary text-white p-2 mb-0 clear">{{col.name}}</h4>
              <p class="bg-light p-2">{{col.description}}</p>
            </span>
            <span v-else-if="col.columnType == 'H2'">
              <h5 class="bg-primary text-white p-2 mb-0">{{col.name}}</h5>
              <p class="bg-light p-2">{{col.description}}</p>
            </span>
            <span v-else>
              <h6 class="pl-2 pr-2">{{ toSentence(col.name) }}</h6>
              <p><RenderValue :col="col" :row="row" class="pl-2 pr-2"/></p>
            </span>
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
import VueTemplate from "../layout/VueTemplate";

export default {
  components: {
    RenderValue,
    RowButtonEdit,
    RowButtonDelete,
    VueTemplate,
  },
  props: {
    columns: Array,
    tableName: String,
    data: Array,
    template: String,
    canEdit: Boolean,
  },
  computed: {
    row() {
      return this.data[0];
    },
    columnsSorted() {
      return [...this.columns].sort((a, b) => {
        return a.position - b.position;
      }).filter(c => !c.name.startsWith("mg_"));
    },
  },
  methods: {
    getKey(row) {
      let result = {};
      this.columns
        .filter((c) => c.key == 1)
        .map((c) => (result[c.name] = row[c.name]));
      return result;
    },
    toSentence(str) {
      return (
          str
              // insert a space before all caps
              .replace(/([A-Z])/g, " $1")
              // uppercase the first character
              .replace(/^./, function (str) {
                return str.toUpperCase();
              })
      );
    },
  },
};
</script>
