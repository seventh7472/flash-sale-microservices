 import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

 /**
  * 倒计时 composable 测试
  *
  * useCountdown(endTime: Date) → { seconds: Ref<number>, isExpired: Ref<boolean>, formatted: Ref<string> }
  *
  * 预期行为：
  * 1. endTime 已过 → seconds=0, isExpired=true
  * 2. endTime 在未来 → seconds>0, isExpired=false
  * 3. 每秒递减
  * 4. 倒计时结束后自动禁用按钮场景
  */
 describe('useCountdown', () => {
   beforeEach(() => { vi.useFakeTimers() })
   afterEach(() => { vi.useRealTimers() })

   it('should return 0 when endTime is in the past', () => {
     const { seconds, isExpired } = useCountdown(new Date(Date.now() - 1000))
     expect(seconds.value).toBe(0)
     expect(isExpired.value).toBe(true)
   })

   it('should return remaining seconds when endTime is in the future', () => {
     const future = new Date(Date.now() + 5000)
     const { seconds, isExpired } = useCountdown(future)
     expect(seconds.value).toBeGreaterThan(0)
     expect(seconds.value).toBeLessThanOrEqual(5)
     expect(isExpired.value).toBe(false)
   })

   it('should decrease every second', () => {
     const future = new Date(Date.now() + 3000)
     const { seconds } = useCountdown(future)
     const initial = seconds.value
     vi.advanceTimersByTime(1000)
     expect(seconds.value).toBe(initial - 1)
   })

   it('should expire when countdown reaches 0', () => {
     const { isExpired } = useCountdown(new Date(Date.now() + 1000))
     expect(isExpired.value).toBe(false)
     vi.advanceTimersByTime(1000)
     expect(isExpired.value).toBe(true)
   })

   it('should format as mm:ss correctly', () => {
     // 65 秒 → "01:05"
     const future = new Date(Date.now() + 65000)
     const { formatted } = useCountdown(future)
     expect(formatted.value).toBe('01:05')
   })

  it('should format 0 seconds as "00:00"', () => {
    const { formatted } = useCountdown(new Date())
    expect(formatted.value).toBe('00:00')
  })

  it('should mark countdown as urgent when under one hour', () => {
    const { urgent } = useCountdown(new Date(Date.now() + 59 * 60 * 1000))
    expect(urgent.value).toBe(true)
  })

  it('should not mark countdown as urgent when one hour or more', () => {
    const { urgent } = useCountdown(new Date(Date.now() + 2 * 60 * 60 * 1000))
    expect(urgent.value).toBe(false)
  })
})
 import { useCountdown } from '../composables/useCountdown'
