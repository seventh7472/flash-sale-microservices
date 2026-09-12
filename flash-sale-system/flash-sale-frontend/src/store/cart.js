import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
export const useCartStore = defineStore('cart', () => {
  const items = ref(JSON.parse(localStorage.getItem('cart') || '[]'))
  function save() { localStorage.setItem('cart', JSON.stringify(items.value)) }
  function add(a) {
    const t = a.t || 'normal'
    const ex = items.value.find(i => i.id === a.id && i.t === t)
    if (ex) { ex.q++ } else { items.value.push({id:a.id,t,prod:a.productId,n:a.activityName,p:a.flashPrice,s:a.totalStock,q:1}) }
    save()
  }
  function upd(id, q) { const i = items.value.find(x => x.id === id); if (i) { i.q = Math.max(1, Math.min(99, q)); save() } }
  function rm(id) { items.value = items.value.filter(i => i.id !== id); save() }
  function clr() { items.value = []; save() }
  const cnt = computed(() => items.value.reduce((s,i) => s + i.q, 0))
  const tot = computed(() => items.value.reduce((s,i) => s + i.p * i.q, 0))
  return { items, cnt, tot, add, upd, rm, clr }
})