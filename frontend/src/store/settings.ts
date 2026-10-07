import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Theme = 'system' | 'light' | 'dark'

interface Settings {
  theme: Theme
  language: string
  /** Show your own pieces at the bottom when playing Black. */
  flipBoard: boolean
  setTheme: (theme: Theme) => void
  setLanguage: (language: string) => void
  setFlipBoard: (flip: boolean) => void
}

/** Per-browser UI preferences, persisted to localStorage. */
export const useSettings = create<Settings>()(
  persist(
    (set) => ({
      theme: 'system',
      language: navigator.language.startsWith('pt') ? 'pt' : 'en',
      flipBoard: true,
      setTheme: (theme) => set({ theme }),
      setLanguage: (language) => set({ language }),
      setFlipBoard: (flipBoard) => set({ flipBoard }),
    }),
    { name: 'draughts-settings' },
  ),
)
