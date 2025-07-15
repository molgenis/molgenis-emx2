import { eventHandler, sendRedirect, readRawBody } from 'h3'
import { request } from 'undici'

const target = process.env.NUXT_PUBLIC_API_BASE || "https://emx2.dev.molgenis.org"

export default eventHandler(async (event) => {
    const { req, res } = event.node
    const url = req.url || ''

    if (url === '/cms' || url === '/cms/') {
        return sendRedirect(event, '/cms/apps/central/', 302)
    }

    if (url.startsWith('/cms')) {
        const backendPath = url.endsWith('favicon.ico') ? '/favicon.ico' : url.replace(/^\/cms/, '')
        const method = req.method || 'GET'
        const body = method === 'GET' || method === 'HEAD' ? undefined : await readRawBody(event)

        const headers = { ...req.headers }
        delete headers.host

        const backendRes = await request(`${target}${backendPath}`, {
            method,
            headers,
            body,
        })

        res.statusCode = backendRes.statusCode

        // Fix here: iterate with for-in over plain object
        for (const key in backendRes.headers) {
            const value = backendRes.headers[key]
            if (value && key.toLowerCase() !== 'transfer-encoding') {
                res.setHeader(key, value)
            }
        }

        for await (const chunk of backendRes.body) {
            res.write(chunk)
        }
        res.end()
        return
    }

    if (url.startsWith('/apps')) {
        const method = req.method || 'GET'
        const body = method === 'GET' || method === 'HEAD' ? undefined : await readRawBody(event)
        const headers = { ...req.headers }
        delete headers.host

        const backendRes = await request(`${target}${url}`, {
            method,
            headers,
            body,
        })

        res.statusCode = backendRes.statusCode

        for (const key in backendRes.headers) {
            const value = backendRes.headers[key]
            if (value && key.toLowerCase() !== 'transfer-encoding') {
                res.setHeader(key, value)
            }
        }

        for await (const chunk of backendRes.body) {
            res.write(chunk)
        }
        res.end()
        return
    }
})
