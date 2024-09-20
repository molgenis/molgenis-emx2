import { expect, test } from '@nuxt/test-utils/playwright'
import { fileURLToPath } from 'node:url'

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL('..', import.meta.url))
  }
})

test('test', async ({ page, goto }) => {
  await goto('/', { waitUntil: 'hydration' })
  await expect(page.getByRole('heading', {level: 1})).toHaveText('SSR Catalogue DEV')
})