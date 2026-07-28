const TIMEZONE_SUFFIX_PATTERN = /(Z|[+-]\d{2}:\d{2})$/i

export const parseUtcAwareDate = (value) => {
  if (value == null || value === '') {
    return null
  }

  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : new Date(value.getTime())
  }

  if (typeof value === 'number') {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? null : date
  }

  if (typeof value === 'string') {
    const normalizedValue = value.trim()
    if (!normalizedValue) {
      return null
    }

    const utcCompatibleValue = TIMEZONE_SUFFIX_PATTERN.test(normalizedValue)
      ? normalizedValue
      : `${normalizedValue}Z`

    const date = new Date(utcCompatibleValue)
    return Number.isNaN(date.getTime()) ? null : date
  }

  return null
}

export const toTimestamp = (value) => {
  const date = parseUtcAwareDate(value)
  return date ? date.getTime() : NaN
}

export const formatDateTime = (
  value,
  {
    fallback = '—',
    locale,
    options = {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    },
  } = {},
) => {
  const date = parseUtcAwareDate(value)
  if (!date) {
    return fallback
  }

  return date.toLocaleString(locale, options)
}
