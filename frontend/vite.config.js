
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
            '/api': 'http://localhost:8080',
            '/apps': 'http://localhost:8080',
            '/graphql': 'http://localhost:8080/api'
        },
    },

})

