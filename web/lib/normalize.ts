/**
 * Digits-only key for threading; falls back to the raw string for short/alpha senders.
 * Mirrors the Android client so the same contact in different formats threads together.
 */
export function normalizeAddress(s: string): string {
  const digits = s.replace(/\D/g, "")
  return digits.length > 0 ? digits : s
}
