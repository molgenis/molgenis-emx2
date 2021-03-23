<template>
  <div style="min-width: 10em;">
    <InputString
      v-if="
        columnType.startsWith('STRING') ||
          columnType.startsWith('TEXT') ||
          columnType.startsWith('UUID')
      "
      :list="true"
      :or="true"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputRangeInt
      v-else-if="columnType.startsWith('INT')"
      :list="true"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputRangeDecimal
      v-else-if="columnType.startsWith('DECIMAL')"
      :list="true"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputRangeDate
      v-else-if="columnType.startsWith('DATE')"
      :list="true"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputCheckbox
      v-else-if="columnType.startsWith('BOOL')"
      :list="true"
      :options="['true', 'false']"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <InputRef
      v-else-if="columnType.startsWith('REF') || columnType == 'MREF'"
      :limit="7"
      :list="true"
      :table="refTable"
      :value="conditions"
      @input="$emit('update:conditions', $event)"
    />
    <div v-else>
      ERROR {{ column }}
    </div>
  </div>
</template>

<script>
import InputCheckbox from '../forms/InputCheckbox.vue'
import InputRangeDate from '../forms/InputRangeDate.vue'
import InputRangeDecimal from '../forms/InputRangeDecimal.vue'
import InputRangeInt from '../forms/InputRangeInt.vue'
import InputRef from '../forms/InputRef.vue'
import InputString from '../forms/InputString.vue'

export default {
  components: {
    InputCheckbox,
    InputRangeDate,
    InputRangeDecimal,
    InputRangeInt,
    InputRef,
    InputString,
  },
  props: {
    columnType: String,
    conditions: Array,
    refTable: String,
  },
  emits: ['update:conditions'],
}
</script>
