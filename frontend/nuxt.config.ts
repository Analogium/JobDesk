export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },

  modules: [
    '@nuxt/eslint',
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
    '@vueuse/nuxt',
  ],

  runtimeConfig: {
    public: {
      apiUrl: process.env.NUXT_PUBLIC_API_URL ?? 'http://localhost:8000',
      // Identité de l'éditeur affichée dans les pages légales (mentions obligatoires
      // art. 6 LCEN) et comme contact pour l'exercice des droits RGPD. Renseignée par
      // l'environnement pour ne pas figer une adresse personnelle dans un dépôt public.
      legal: {
        editor: process.env.NUXT_PUBLIC_LEGAL_EDITOR ?? '',
        status: process.env.NUXT_PUBLIC_LEGAL_STATUS ?? '',
        address: process.env.NUXT_PUBLIC_LEGAL_ADDRESS ?? '',
        contact: process.env.NUXT_PUBLIC_LEGAL_CONTACT ?? '',
      },
    },
  },

  typescript: {
    strict: true,
  },

  app: {
    head: {
      title: 'JobDesk',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Suivi intelligent de candidatures' },
      ],
    },
  },
})
