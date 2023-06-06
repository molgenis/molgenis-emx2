<script setup>
defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <ButtonGroup>
      <Button label="All 299 variables" type="tertiary" size="medium" />
      <Button label="Harmonized variables" type="outline" size="medium" />
    </ButtonGroup>

    <List
      :columnCount="2"
      title="Variables per topic"
      class="mt-18"
      type="link">
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Administrative information</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline"
          >Birth pregnany and reproductive health history</a
        >
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Child</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline"
          >Cognition, personality and psychological measure</a
        >
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Death</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Diseases</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Lifestyle & behaviours</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Administrative information</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline"
          >Birth pregnany and reproductive health history</a
        >
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Child</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline"
          >Cognition, personality and psychological measure</a
        >
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Death</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Diseases</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
      <ListItem
        type="link"
        class="font-normal text-blue-500 before:content-['›'] before:mr-2">
        <a href="#" class="hover:underline">Lifestyle & behaviours</a>
        <span class="text-gray-400 inline-block ml-1">- 9</span>
      </ListItem>
    </List>
  </ContentBlock>
</template>
