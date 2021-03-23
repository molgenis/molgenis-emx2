<template>
  <div class="bg-white">
    <div class="p-2 mb-2" :class="headerCss">
      <h6>
        <RouterLink :class="headerCss" to="/">
          home
        </RouterLink>
        /
      </h6>
    </div>
    <TableExplorer
      :show-cards="defaultCards"
      :show-columns="defaultColumns"
      :show-filters="defaultFilters"
      :table="tableName"
      @click="openDetailView"
    />
  </div>
</template>

<script>
import {TableExplorer} from '@/components/ui/index.js'

const css = {
  Affiliations: 'bg-info text-white',
  Contacts: 'bg-info text-white',
  Databanks: 'bg-danger text-white',
  Datasources: 'bg-warning text-dark',
  Institutions: 'bg-info text-white',
  Models: 'bg-secondary text-white',
  Networks: 'bg-primary text-white',
  Releases: 'bg-dark text-white',
  Studies: 'bg-success text-white',
  TableMappings: 'bg-dark text-white',
  Tables: 'bg-dark text-white',
  VariableMappings: 'bg-dark text-white',
  Variables: 'bg-dark text-white',
}

export default {
  components: {
    TableExplorer,
  },
  props: {
    tableName: String,
  },
  computed: {
    defaultCards() {
      if (this.tableName == 'Institutions') {
        return true
      }
      return false
    },
    defaultColumns() {
      if (this.tableName == 'Institutions') {
        return ['name', 'acronym', 'type', 'country']
      } else if (
        ['Datasources', 'Databanks', 'Networks', 'Models'].includes(
          this.tableName,
        )
      ) {
        return ['name', 'acronym', 'type', 'provider']
      } else if (this.tableName == 'Studies') {
        return ['name', 'contents', 'networks', 'description', 'contact']
      } else if (this.tableName == 'Contacts') {
        return [
          'name',
          'institution',
          'affiliation',
          'email',
          'orcid',
          'homepage',
        ]
      } else if (this.tableName == 'Affiliations') {
        return ['name', 'homepage', 'acronym']
      } else if (this.tableName == 'Tables') {
        return [
          'release',
          'name',
          'label',
          'unitOfObservation',
          'topics',
          'description',
        ]
      } else if (this.tableName == 'Variables') {
        return [
          'release',
          'table',
          'name',
          'label',
          'format',
          'unit',
          'topics',
          'description',
          'mandatory',
        ]
      }
      return []
    },
    defaultFilters() {
      if (this.tableName == 'Institutions') {
        return ['type', 'country']
      }
      if (this.tableName == 'Studies') {
        return ['contents']
      }
      return []
    },
    detailRouteName() {
      // detailRoute is name of table minus trailing 's'
      return this.tableName.toLowerCase().slice(0, -1)
    },
    headerCss() {
      return css[this.tableName]
    },
  },
  methods: {
    openDetailView(row) {
      // in case of table
      if (this.tableName == 'Tables') {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            acronym: row.release.resource.acronym,
            name: row.name,
            version: row.release.version,
          },
        })
      } else if (
        this.tableName == 'TableMappings' ||
        this.tableName == 'VariableMappings'
      ) {
        this.$router.push({
          name: 'tablemapping',
          params: {
            fromAcronym: row.fromRelease.resource.acronym,
            fromTable: row.fromTable.name,
            fromVersion: row.fromRelease.version,
            toAcronym: row.toRelease.resource.acronym,
            toTable: row.toTable.name,
            toVersion: row.toRelease.version,
          },
        })
      } else if (this.tableName == 'Variables') {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            acronym: row.release.resource.acronym,
            name: row.name,
            table: row.table.name,
            version: row.release.version,
          },
        })
      } else if (row.version) {
        this.$router.push({
          name: this.detailRouteName,
          params: {acronym: row.resource.acronym, version: row.version},
        })
      } else if (row.acronym) {
        this.$router.push({
          name: this.detailRouteName,
          params: {acronym: row.acronym},
        })
      } else {
        this.$router.push({
          name: this.detailRouteName,
          params: {name: row.name},
        })
      }
    },
  },
}
</script>
