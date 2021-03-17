
import { defineConfig } from 'vite'
import pyritePackage from './package.json'
import vue from '@vitejs/plugin-vue'

process.env.VITE_VERSION = pyritePackage.version

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

