import httpProxy from 'http-proxy';
import { eventHandler, sendRedirect } from 'h3';
import type { H3Event } from 'h3';

const target = process.env.NUXT_PUBLIC_API_BASE || "https://emx2.dev.molgenis.org/"

const proxy = httpProxy.createProxyServer({
    target,
    changeOrigin: true,
    selfHandleResponse: false,
})

export default eventHandler(async (event: H3Event) => {
    const { req, res } = event.node

    // Redirect /cms or /cms/ exactly to /cms/apps/central
    const url = req.url || ''
    if (url === '/cms' || url === '/cms/') {
        return sendRedirect(event, '/cms/apps/central/', 302)
    }


    if (req.url?.startsWith('/cms')) {
        if (req.url.endsWith('favicon.ico')) {
            req.url = '/favicon.ico'
        } else {
            req.url = req.url.replace(/^\/cms/, '')
        }
        await new Promise<void>((resolve, reject) => {
            proxy.web(req, res, {}, (err) => (err ? reject(err) : resolve()))
        })
        event.res.end()
        return
    }

    if (req.url?.startsWith('/apps')) {
        await new Promise<void>((resolve, reject) => {
            proxy.web(req, res, {}, (err) => (err ? reject(err) : resolve()))
        })
        event.res.end()
        return
    }

    // continue with other Nitro handling
})