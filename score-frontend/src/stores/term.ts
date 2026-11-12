import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getTerms } from '@/api/term'
import type { Term } from '@/types'

const STORAGE_KEY = 'selectedTerm'

function termKey(term: Term) {
  return `${term.academicYear}|${term.semester}`
}

export const useTermStore = defineStore('term', () => {
  const terms = ref<Term[]>([])
  const selectedKey = ref<string>(localStorage.getItem(STORAGE_KEY) || '')
  const loaded = ref(false)

  const selected = computed<Term | undefined>(() => {
    return (
      terms.value.find((term) => termKey(term) === selectedKey.value) ||
      terms.value.find((term) => term.current) ||
      terms.value[0]
    )
  })

  const academicYear = computed(() => selected.value?.academicYear || '')
  const semester = computed(() => selected.value?.semester || '')
  const termName = computed(() => selected.value?.name || '')

  function select(term: Term) {
    selectedKey.value = termKey(term)
    localStorage.setItem(STORAGE_KEY, selectedKey.value)
  }

  function selectByKey(key: string) {
    const term = terms.value.find((item) => termKey(item) === key)
    if (term) select(term)
  }

  async function load(force = false) {
    if (loaded.value && !force) return
    const res = await getTerms()
    terms.value = res.data || []
    const exists = terms.value.some((term) => termKey(term) === selectedKey.value)
    if (!exists) {
      const fallback = terms.value.find((term) => term.current) || terms.value[0]
      if (fallback) select(fallback)
    }
    loaded.value = true
  }

  return {
    terms,
    selectedKey,
    selected,
    academicYear,
    semester,
    termName,
    loaded,
    load,
    select,
    selectByKey,
    termKey,
  }
})
