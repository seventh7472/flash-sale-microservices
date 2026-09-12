import { ref, computed, onUnmounted, unref } from 'vue'
export function useCountdown(endTime) {
  const now = ref(Date.now())
  const timer = setInterval(() => { now.value = Date.now() }, 1000)
  onUnmounted(() => clearInterval(timer))
  const targetMs = computed(() => {
    const val = unref(endTime)
    if (val instanceof Date) return val.getTime()
    return new Date(val).getTime()
  })
  const diffMs = computed(() => Math.max(0, targetMs.value - now.value))
  const seconds = computed(() => Math.floor(diffMs.value / 1000))
  const isExpired = computed(() => seconds.value <= 0)
  const hours = computed(() => Math.floor(seconds.value / 3600))
  const minutes = computed(() => Math.floor((seconds.value % 3600) / 60))
  const secs = computed(() => seconds.value % 60)
  const urgent = computed(() => seconds.value > 0 && seconds.value < 3600)
  const formatted = computed(() => {
    const s = Math.max(0, seconds.value)
    const m = Math.floor(s / 60)
    const sec = s % 60
    return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  })
  return { seconds, isExpired, formatted, hours, minutes, secs, urgent }
}
