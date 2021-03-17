
import { defineConfig } from 'vite'
import emx2Package from './package.json'
import vue from '@vitejs/plugin-vue'

process.env.EMX2_VERSION = emx2Package.version

export default defineConfig({
    plugins: [
        vue(),
    ],
    server: {
        proxy: {
            '/graphql': 'http://localhost:8080/api',
            '/apps': 'http://localhost:8080'
        },
    },

})

