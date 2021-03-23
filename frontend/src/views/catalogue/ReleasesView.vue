<template>
  <div v-if="release" class="container bg-white">
    <div class="p-2 bg-dark text-white">
      <h6>
        <RouterLink class="text-white" to="/">
          home
        </RouterLink>
        /
        <RouterLink class="text-white" to="/list/Releases">
          releases
        </RouterLink>
        /
      </h6>
    </div>
    <h1>
      <small>Release:</small><br>{{ release.resource.acronym }} ({{
        release.version
      }})
    </h1>
    <h6>{{ resourceType }}</h6>
    <p>
      <RouterLink
        :to="{
          name: resourceType.toLowerCase(),
          params: { acronym: release.resource.acronym },
        }"
      >
        {{ release.resource.acronym }} -
        {{ release.resource.name }}
      </RouterLink>
    </p>
    <h6>Release</h6>
    <p>{{ release.version }}</p>
    <h6>Tables in this release</h6>
    <div class="mt-4">
      <TableExplorer
        :filter="{
          release: {
            version: { equals: version },
            resource: { acronym: { equals: acronym } },
          },
        }"
        :show-header="false"
        table="Tables"
        @click="openTable"
      />
    </div>
  </div>
</template>

<script>
import {request} from 'graphql-request'

import {TableExplorer} from '@/components/ui/index.js'

export default {
  components: {TableExplorer},
  props: {
    acronym: String,
    version: String,
  },
  data() {
    return {
      release: null,
    }
  },
  computed: {
    // eslint-disable-next-line vue/return-in-computed-property
    resourceType() {
      if (this.release) {
        return this.release.resource.mg_tableclass.split('.')[1].slice(0, -1)
      }
    },
  },
  created() {
    this.reload()
  },
  methods: {
    openTable(row) {
      this.$router.push({
        name: 'table',
        params: {
          acronym: this.acronym,
          name: row.name,
          version: this.version,
        },
      })
    },
    reload() {
      // eslint-disable-next-line no-console
      console.log(this.version + ' ' + this.resourceAcronym)
      request(
        'graphql',
        `query Releases($acronym:String,$version:String){
        Releases(filter:{resource:{acronym:{equals:[$acronym]}},version:{equals:[$version]}}){resource{acronym,name,mg_tableclass},version}
        }`,
        {
          acronym: this.acronym,
          version: this.version,
        },
      )
        .then((data) => {
          this.release = data.Releases[0]
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
        })
        .finally(() => {
          this.loading = false
        })
    },
  },
}
</script>
