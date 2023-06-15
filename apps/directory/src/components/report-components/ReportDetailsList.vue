<template>
  <table class="mg-report-details-list mb-3">
    <caption v-show="false">Details list</caption>
    <template v-for="(value, key) in reportDetails">
      <tr v-if="showRow(value)" :key="key">
        <!-- Header -->
        <th scope="row" class="pr-1" v-if="showKey(value.type)">{{ key }}:</th>

        <!--Type bool-->
        <td v-if="value.type === 'bool'">
          <span v-if="value.value" class="badge badge-info">yes</span>
          <span v-else class="badge badge-info">no</span>
        </td>
        <!--Type string-->
        <td v-else-if="value.type.includes('string')" colspan="2">{{ value.value }}</td>
        <!--Type url-->
        <td v-else-if="value.type === 'url'">
          <a :href="value.value" target="_blank" rel="noopener noreferrer">
            <i class="fa fa-fw fa-globe" aria-hidden="true"></i>
            <span class="mg-icon-text">Website</span>
          </a>
        </td>
        <!--Type email-->
        <td v-else-if="value.type === 'email'" colspan="2">
          <a :href="'mailto:' + value.value">
            <i class="fa fa-fw fa-paper-plane" aria-hidden="true"></i>
            <span class="mg-icon-text">{{ uiText['email'] }}</span>
          </a>
        </td>
        <!--Type phone-->
        <td v-else-if="value.type === 'phone'">
          <i class="fa fa-fw fa-phone" aria-hidden="true"></i>
          <span class="mg-icon-text">{{ value.value }}</span>
        </td>
        <!--Type list-->
        <td v-else-if="value.type === 'list' && value.value.length > 0">
          <span
            v-for="(val, index) in value.value"
            class="m-1 badge"
            :key="index"
            :class="'badge-' + (value.badgeColor ? value.badgeColor : 'success')">{{ val }}</span>
        </td>
        <!--Type report-->
        <td v-else-if="value.type === 'report'" colspan="2">
          <router-link :to="value.value">
            <i class="fa fa-fw fa-address-card" aria-hidden="true"></i>
            <span class="mg-icon-text">Overview</span>
          </router-link>
        </td>
      </tr>
    </template>
  </table>
</template>

<style scoped>
.mg-icon-text {
  margin-left: 0.2rem;
}
</style>

<script>
import { mapGetters } from 'vuex'
export default {
  name: 'ReportDetailsList',
  // Object with as key the variable, as value an object with two keys: value and type
  props: {
    reportDetails: {
      [String]: {
        value: String,
        type:
          'string' |
          'email' |
          'url' |
          'bool' |
          'list' |
          'phone' |
          'report' |
          'string-with-key',
        batchColor: {
          type:
            'success' |
            'warning' |
            'info' |
            'secondary' |
            'danger' |
            'light' |
            'dark',
          required: false
        }
      }
    }
  },
  methods: {
    showRow (value) {
      return (value.value && value.value.length !== 0) || value.type === 'bool'
    },
    showKey (type) {
      return ['bool', 'string-with-key', 'list'].includes(type)
    }
  },
  computed: {
    ...mapGetters(['uiText'])
  }
}
</script>
