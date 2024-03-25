<script setup lang="ts">
const isShown = ref(false);
const isShownWithFooter = ref(true);
</script>

<template>
  <div class="flex h-screen">
    <h1>Modal</h1>

    <Modal :shown="isShown" @close="isShown = false">
      <div class="py-6">This is a modal</div>
      <p class="py-6">
        Good resolutions are useless attempts to interfere with scientific laws.
        Their origin is pure vanity. Their result is absolutely nil. They give
        us, now and then, some of those luxurious sterile emotions that have a
        certain charm for the weak. That is all that can be said for them. They
        are simply cheques that men draw on a bank where they have no account.
      </p>
    </Modal>

    <div class="m-auto">
      <button
        @click="isShown = true"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
      >
        Show modal
      </button>
    </div>

    <h3>Modal with footer</h3>
    <Modal
      :shown="isShownWithFooter"
      @close="isShownWithFooter = false"
      :include-footer="true"
    >
      <template #header>
        <div class="">This is a header text</div>
      </template>

      <p class="py-2">
        Man is least himself when he talks in his own person. Give him a mask,
        and he will tell you the truth.
      </p>
      <p class="py-2">
        THE STUDIO was filled with the rich odour of roses, and when the light
        summer wind stirred amidst the trees of the garden there came through
        the open door the heavy scent of the lilac, or the most delicate perfume
        of the pink-flowering thorn.
      </p>
      <div class="py-2">
        From the corner of the divan of Persian saddlebags on which he was
        lying, smoking, as was his custom, innumerable cigarettes, Lord Henry
        Wotton could just catch the gleam of the honey-sweet and honey-coloured
        blossoms of a laburnum, whose tremulous branches seemed hardly able to
        bear the burden of a beauty so flamelike as theirs; and now and then the
        fantastic shadows of birds in flight flitted across the long
        tussore-silk curtains that were stretched in front of the huge window,
        producing a kind of momentary Japanese effect, and making him think of
        those pallid jade-faced painters of Tokio who, through the medium of an
        art that is necessarily immobile, seek to convey the sense of swiftness
        and motion. The sullen murmur of the bees shouldering their way through
        the long unmown grass, or circling with monotonous insistence round the
        dusty gilt horns of the straggling woodbine, seemed to make the
        stillness more oppressive. The dim roar of London was like the bourdon
        note of a distant organ.
      </div>

      <p class="py-2">
        In the centre of the room, clamped to an upright easel, stood the
        full-length portrait of a young man of extraordinary personal beauty,
        and in front of it, some little distance away, was sitting the artist
        himself, Basil Hallward, whose sudden disappearance some years ago
        caused, at the time, such public excitement, and gave rise to so many
        strange conjectures.
      </p>

      <p>The end. this should be visible</p>
      <template #footer>
        <button
          class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
        >
          Primary action
        </button>
      </template>
    </Modal>

    <div class="m-auto">
      <button
        @click="isShownWithFooter = true"
        class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
      >
        Show modal with footer
      </button>
    </div>
  </div>
</template>
