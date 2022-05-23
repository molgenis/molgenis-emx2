<template>
  <div class="hero-image">
    <div class="hero-text">
      <h1>Cohorts, biobanks and dataset of the UMCG</h1>

      <p>
        Universitair Medisch Centrum Groningen, the Netherlands
      </p>
    </div>
  </div>
</template>

<script>
export default {};
</script>

<style scoped>
/* The hero image */
.hero-image {
  /* Use "linear-gradient" to add a darken background effect to the image (photographer.jpg). This will make the text easier to read */
  background-image: linear-gradient(
      rgba(1, 127, 253, 0.6),
      rgba(1, 127, 253, 0.8)
    ),
    url("https://image.focuspoints.io/general-1.jpg?_jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJmb2N1c1BvaW50WSI6MC4wLCJmb2N1c1BvaW50WCI6MC4wLCJ3aWR0aCI6MTQ0MCwiaXNzIjoidW1jZyIsImFjdGlvbiI6InRyYW5zZm9ybSIsInVybCI6Imh0dHBzOi8vdW1jZ3Jlc2VhcmNoLm9yZy9kb2N1bWVudHMvNzcwNTM0Lzc3NTkwNy9nZW5lcmFsLTEuanBnL2NlNGVkMTM5LTMxYzMtOTg3Mi02NjBiLWU3ZjVjNDFhNTY0OD90PTE2Mjk3MDYzODQyOTMmZG93bmxvYWQ9dHJ1ZSIsImhlaWdodCI6MzYwfQ.ulaZWsVt6k6Uil4zLdaxpnLrWZJubDttUIlE5hr5yqgXW7ACAD5nF1Kpl4R-Wd2QU2haLYJt0zvzMWv2843gfA");

  /* Set a specific height */
  height: 250px;

  /* Position and center the image to scale nicely on all screens */
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  position: relative;
}

/* Place text in the middle of the image */
.hero-text {
  text-align: left;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
}
</style>