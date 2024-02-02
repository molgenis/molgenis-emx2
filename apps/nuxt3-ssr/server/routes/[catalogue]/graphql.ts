import { joinURL } from 'ufo';
export default defineEventHandler((event) => {
    console.log(event.path)
    const target = joinURL('https://data-catalogue.molgeniscloud.org/', event.path)
    return proxyRequest(event, target)
  })