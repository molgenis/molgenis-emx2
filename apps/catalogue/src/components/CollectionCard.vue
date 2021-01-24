<template>
  <div class="col-sm-3 py-2">
    <div class="card">
      <div class="card-body">
        <h4 class="card-title">
          <RouterLink :to="'/collection-datasets/' + collection.acronym">
            <small class="float-right">
              <span
                v-if="collection.type"
                v-for="type in collection.type"
                class="badge badge-primary"
              >
                {{ type.name }}
              </span>
            </small>
            <small>
              {{ collection.acronym }}
            </small>
            <br />
            {{ collection.name }}
          </RouterLink>
        </h4>
        <span v-if="collection.host_organisation">
          <label>Provider:</label> {{ collection.host_organisation.name }}<br />
        </span>
        <span v-if="collection.website">
          <label>website: </label>
          <a href="collection.website">{{ collection.website }}</a>
        </span>
        <ReadMore
          :text="collection.description"
          length="200"
          v-if="collection.description"
        />
        <div v-if="tab === 'Variables'">
          <ul>
            <div v-for="table in collection.tables">
              {{ table.name }}
              <ul>
                <li v-for="variable in table.variables">
                  {{ variable.name }}
                </li>
              </ul>
            </div>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  components: { ReadMore },
  props: {
    collection: Object,
  },
  data() {
    return {
      tab: "Description",
    };
  },
};
</script>
