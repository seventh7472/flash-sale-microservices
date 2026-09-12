 import { ref } from 'vue'

 export function useDebounce(fn, delay = 1000) {
   const isPending = ref(false)
   let timer = null

   function execute(...args) {
     if (isPending.value) return
     isPending.value = true
     fn(...args)
     timer = setTimeout(() => { isPending.value = false }, delay)
   }

   function reset() {
     if (timer) clearTimeout(timer)
     isPending.value = false
   }

   return { execute, isPending, reset }
 }
