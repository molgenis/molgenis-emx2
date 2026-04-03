import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import $monacoEditorPlugin from 'vite-plugin-monaco-editor';
import devProxyConfig from '../dev-proxy.config';

const monacoEditorPlugin = $monacoEditorPlugin.default ?? $monacoEditorPlugin;

export default defineConfig(({ command }) => ({
  plugins: [
    react(),
    monacoEditorPlugin({
      languageWorkers: ['editorWorkerService', 'json'],
      customWorkers: [
        {
          label: 'graphql',
          entry: 'monaco-graphql/esm/graphql.worker.js',
        },
      ],
      publicPath: "assets",
      customDistPath: (root, buildOutDir, base) => {
          return buildOutDir + '/' + 'assets';
        },

    }),
  ],
  base: command === "serve" ? "/" : "apps/graphql-playground/",
  server: {
    port: 3000,
    proxy: devProxyConfig,
  },
}));