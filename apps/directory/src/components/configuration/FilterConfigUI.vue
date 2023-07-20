<template>
  <div>
    <div class="mb-3">
      <h3>Filters</h3>
      <div class="d-flex justify-content-between flex-wrap">
        <small class="d-inline-block"
          >Rearrange the filters by dragging and dropping. Unchecking a filter
          means it will be shown under <i>More filters</i> by default.</small
        >
        <button @click="emitAdd" class="btn btn-info mt-2">
          Add new filter
          <span class="fa fa-plus fa-lg ml-1" aria-hidden="true"></span>
        </button>
      </div>
    </div>

    <draggable
      v-model="appConfig.filterFacets"
      group="filterFacets"
      @start="dragStart()"
      @end="sync"
      item-key="facetTitle"
    >
      <template #item="{ element }">
        <div
          @click="activeFilter = element.facetTitle"
          class="list-group-item d-flex"
          :class="{ editing: element.index === filterIndex }"
        >
          {{ element.index + 1 }}. {{ element.label || element.facetTitle }}
          <small class="ml-auto" v-if="element.builtIn"
            >Rearranging this has no effect in the application.</small
          >
          <label v-if="!element.builtIn" class="ml-auto"
            ><input type="checkbox" @change="sync"
          /></label>
          <button
            v-if="!element.builtIn"
            @click="editFilter(element.index)"
            class="edit-button"
          >
            <span
              class="fa-regular fa-pen-to-square fa-lg"
              aria-hidden="true"
            ></span>
          </button>
        </div>
      </template>
    </draggable>
  </div>
</template>

<script>
import Draggable from "vuedraggable";

export default {
  name: "simple",
  components: {
    Draggable,
  },
  props: {
    config: {
      type: String,
      required: () => true,
    },
    hasUpdated: {
      type: Number,
      required: false
    }
  },
  data() {
    return {
      appConfig: {},
      activeFilter: "",
      dragging: false,
      filterIndex: -1,
    };
  },
  watch: {
    config() {
      this.setData();
    },
    hasUpdated() {
      this.filterIndex = -1;
    }
  },
  methods: {
    dragStart() {
      this.dragging = true;
      this.editFilter(-1);
    },
    setData() {
      if (this.config && this.config.length) {
        /** add index here because vue sortable doesnt expose indexes */
        const appConfig = JSON.parse(this.config);

        appConfig.filterFacets.forEach((element, index) => {
          element.index = index;
        });

        this.appConfig = appConfig;
      }
    },
    sync() {
      this.appConfig.filterFacets.forEach((element, index) => {
        element.index = index;
      });
      this.draggable = false;
      this.$emit("update", JSON.stringify(this.appConfig));
      this.editFilter(-1);
    },
    editFilter(index) {
      /** reset when toggled */
      if (this.filterIndex === index) {
        index = -1;
      }

      this.filterIndex = index;
      this.$emit("edit", index);
    },
    emitAdd() {
      this.$emit("add");
    },
  },
  mounted() {
    this.setData();
  },
};
</script>
<style scoped>
.list-group-item:hover {
  cursor: grab;
}

.ghost {
  opacity: 0.5;
  background: #c8ebfb;
}

.edit-button {
  position: relative;
  bottom: 0.05rem;
  margin-left: 1rem;
  background: transparent;
  border: none;
}

.edit-button:hover {
  cursor: pointer;
  color: black;
}

.editing {
  box-shadow: inset 0.5px 0.5px 0 black, inset -1.5px -1.5px 0 black;
}
</style>
