<script setup lang="ts">
import { ref, watch, onUnmounted, nextTick } from 'vue'

interface Props {
  modelValue?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [token: string]
}>()

const loading = ref(false)
let tacInstance: any = null

const initCaptcha = async () => {
  loading.value = true

  if (typeof window.loadTAC !== 'function') {
    console.error('loadTAC not found, check load.min.js')
    loading.value = false
    return
  }

  await nextTick()

  const bindEl = '#slider-captcha-box'
  const container = document.querySelector(bindEl) as HTMLElement
  if (container) {
    container.innerHTML = ''
  }

  const captchaConfig = {
    requestCaptchaDataUrl: '/captcha/genCaptcha',
    validCaptchaUrl: '/captcha/check',
    bindEl: bindEl,
    validSuccess: (res: any, config: any, tac: any) => {
      console.log('captcha success', res)
      if (tac && typeof tac.destroyWindow === 'function') {
        tac.destroyWindow()
      }
      const token = res?.data?.id || res?.data?.token || ''
      emit('success', token)
      emit('update:modelValue', false)
      loading.value = false
    },
    validFail: (res: any, config: any, tac: any) => {
      console.log('captcha fail', res)
      if (tac && typeof tac.reloadCaptcha === 'function') {
        tac.reloadCaptcha()
      }
    },
    btnRefreshFun: (el: HTMLElement, tac: any) => {
      if (tac && typeof tac.reloadCaptcha === 'function') {
        tac.reloadCaptcha()
      }
    },
    btnCloseFun: (el: HTMLElement, tac: any) => {
      if (tac && typeof tac.destroyWindow === 'function') {
        tac.destroyWindow()
      }
      emit('update:modelValue', false)
      loading.value = false
    }
  }

  const style = {}

  window.loadTAC('/score/tac', captchaConfig, style)
    .then((tac: any) => {
      tacInstance = tac
      loading.value = false
      tac.init()
    })
    .catch((e: any) => {
      console.error('captcha load failed', e)
      loading.value = false
    })
}

watch(() => props.modelValue, async (newVal) => {
  if (newVal) {
    if (tacInstance && typeof tacInstance.destroyWindow === 'function') {
      tacInstance.destroyWindow()
    }
    tacInstance = null
    await initCaptcha()
  } else {
    if (tacInstance && typeof tacInstance.destroyWindow === 'function') {
      tacInstance.destroyWindow()
    }
    tacInstance = null
  }
})

onUnmounted(() => {
  if (tacInstance && typeof tacInstance.destroyWindow === 'function') {
    tacInstance.destroyWindow()
  }
})
</script>

<template>
  <div v-if="modelValue" class="captcha-wrapper">
    <div id="slider-captcha-box"></div>
  </div>
</template>

<style scoped>
.captcha-wrapper {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
}

#slider-captcha-box {
  width: 335px;
  height: 318px;
}
</style>