/// <reference types="vite/client" />

interface Window {
  loadTAC?: (
    src: string,
    config: Record<string, any>,
    style?: Record<string, any>
  ) => Promise<any>
}
