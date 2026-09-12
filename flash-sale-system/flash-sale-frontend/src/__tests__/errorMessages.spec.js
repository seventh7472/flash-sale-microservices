 import { describe, it, expect } from 'vitest'

 /**
  * 后端错误码 → 前端提示文案映射测试
  *
  * getErrorMessage(code: number) → string
  *
  * 映射表：
  *   2001 → "库存不足"
  *   3002 → "重复抢购"
  *   1002 → "触发限流"
  *   1003 → "作弊拦截"
  *   其他 → 兜底提示
  */
 describe('errorMessages', () => {
   it('should return "库存不足" for code 2001', () => {
     expect(getErrorMessage(2001)).toBe('库存不足，下次手速快一点～')
   })

   it('should return "重复抢购" for code 3002', () => {
     expect(getErrorMessage(3002)).toBe('您已参与过此秒杀活动，不可重复抢购')
   })

   it('should return "触发限流" for code 1002', () => {
     expect(getErrorMessage(1002)).toBe('请求过于频繁，请稍后重试')
   })

   it('should return "作弊拦截" for code 1003', () => {
     expect(getErrorMessage(1003)).toBe('检测到异常行为，已拦截本次请求')
   })

  it('should return "活动已结束" for code 2003', () => {
    expect(getErrorMessage(2003)).toBe('秒杀活动已结束')
  })

  it('should return "请先登录" for HTTP 401', () => {
    expect(getErrorMessage(401)).toBe('请先登录')
  })

  it('should return "无权限访问" for HTTP 403', () => {
    expect(getErrorMessage(403)).toBe('无权限访问')
  })

  it('should return "请求资源不存在" for HTTP 404', () => {
    expect(getErrorMessage(404)).toBe('请求资源不存在')
  })

  it('should return "服务器开小差了，请稍后重试" for HTTP 500', () => {
    expect(getErrorMessage(500)).toBe('服务器开小差了，请稍后重试')
  })

  it('should cover common HTTP error codes', () => {
    expect(getErrorMessage(400)).toBe('请求参数有误，请检查后重试')
    expect(getErrorMessage(409)).toBe('数据冲突，请刷新后重试')
    expect(getErrorMessage(429)).toBe('系统繁忙，请稍后重试')
  })

  it('should return generic message for unknown error code', () => {
    expect(getErrorMessage(9999)).toBe('系统繁忙，请稍后重试')
  })
 })
 import { getErrorMessage } from '../utils/errorMessages'
