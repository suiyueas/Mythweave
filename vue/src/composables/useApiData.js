import { ref } from 'vue'

export function useApiData(fetchFn) {
  const data = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function execute(...args) {
    loading.value = true
    error.value = null
    try {
      data.value = await fetchFn(...args)
      return data.value
    } catch (e) {
      error.value = e.message || '请求失败'
      return null
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, execute }
}
