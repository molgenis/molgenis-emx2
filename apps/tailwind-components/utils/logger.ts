import consola from 'consola';

const logger = consola.withTag('nuxt-app');

if (process.env.NODE_ENV === 'production') {
    logger.level = 0; // Disable logs in production
}

export default logger;