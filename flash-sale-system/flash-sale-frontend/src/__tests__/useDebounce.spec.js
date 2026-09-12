 import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

 /**
  * 防抖 composable 测试（抢购按钮防重复提交）
  *
  * useDebounce(fn, delay) → { execute: () => void, isPending: Ref<boolean> }
  *
  * 预期行为：
  * 1. 立即连续调用多次 → 只执行一次
  * 2. 执行期间 isPending = true（按钮应 disabled）
  * 3. 延迟结束后 isPending = false（按钮重新启用）
  */
 describe('useDebounce', () => {
   beforeEach(() => { vi.useFakeTimers() })
   afterEach(() => { vi.useRealTimers() })

   it('should execute the function immediately on first call', () => {
     const fn = vi.fn()
     const { execute } = useDebounce(fn, 1000)
     execute()
     expect(fn).toHaveBeenCalledTimes(1)
   })

   it('should prevent repeated calls within delay period', () => {
     const fn = vi.fn()
     const { execute } = useDebounce(fn, 1000)
     execute()   // 第1次 → 执行
     execute()   // 第2次 → 忽略
     execute()   // 第3次 → 忽略
     expect(fn).toHaveBeenCalledTimes(1)
   })

   it('should set isPending=true during the delay period', () => {
     const { execute, isPending } = useDebounce(() => {}, 1000)
     expect(isPending.value).toBe(false)
     execute()
     expect(isPending.value).toBe(true)
   })

   it('should set isPending=false after delay elapses', () => {
     const { execute, isPending } = useDebounce(() => {}, 1000)
     execute()
     expect(isPending.value).toBe(true)
     vi.advanceTimersByTime(1000)
     expect(isPending.value).toBe(false)
   })

   it('should allow a new call after delay elapses', () => {
     const fn = vi.fn()
     const { execute } = useDebounce(fn, 1000)
     execute()
     expect(fn).toHaveBeenCalledTimes(1)
     vi.advanceTimersByTime(1000)
     execute()
     expect(fn).toHaveBeenCalledTimes(2)
   })
 })
 import { useDebounce } from '../composables/useDebounce'
