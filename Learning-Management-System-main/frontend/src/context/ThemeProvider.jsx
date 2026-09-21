import React, { useEffect, useLayoutEffect, useMemo, useState } from 'react';
import ThemeContext from './themeContext';

const THEME_STORAGE_KEY = 'app_theme';
const DEFAULT_THEME = 'light';
const VALID_THEMES = new Set(['light', 'dark']);

const getStoredTheme = () => {
  try {
    const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    return VALID_THEMES.has(storedTheme) ? storedTheme : DEFAULT_THEME;
  } catch (error) {
    console.error(error);
    return DEFAULT_THEME;
  }
};

const applyThemeToDocument = (theme) => {
  document.documentElement.dataset.theme = theme;
  document.documentElement.classList.toggle('dark', theme === 'dark');
  document.documentElement.style.colorScheme = theme;
};

export default function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(getStoredTheme);

  useLayoutEffect(() => {
    applyThemeToDocument(theme);
  }, [theme]);

  useEffect(() => {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch (error) {
      console.error(error);
    }
  }, [theme]);

  const value = useMemo(
    () => ({
      theme,
      isDark: theme === 'dark',
      setTheme: (nextTheme) => {
        if (VALID_THEMES.has(nextTheme)) {
          setThemeState(nextTheme);
        }
      },
      toggleTheme: () => {
        setThemeState((currentTheme) => (currentTheme === 'dark' ? 'light' : 'dark'));
      },
    }),
    [theme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
