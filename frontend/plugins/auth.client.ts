export default defineNuxtPlugin(() => {
  const authStore = useAuthStore()
  const token = localStorage.getItem('jobdesk_token')
  if (token) {
    authStore.setToken(token)
  }
})
