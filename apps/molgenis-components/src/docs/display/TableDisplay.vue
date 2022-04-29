<template>
  <demo-item id="table-display" label="Table Display">
    <label class="font-italic">example</label>

    <table-display
      :columns="[
        { name: 'firstName', label: 'First name' },
        { name: 'lastName', label: 'Sir name' },
        { name: 'occupation', label: 'Occupation' },
      ]"
      :rows="[
        {
          firstName: 'Albus',
          lastName: 'Dumbledore',
          occupation: 'Headmaster',
        },
        { firstName: 'Rubeus', occupation: 'Grounds keeper' },
        { lastName: 'Snape', occupation: 'Professor' },
      ]"
    ></table-display>
  </demo-item>
</template>

