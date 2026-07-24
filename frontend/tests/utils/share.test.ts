import { describe, it, expect } from 'vitest'
import { shareUrl, isShareExpired } from '~/utils/share'

describe('shareUrl', () => {
  it('builds the public /shared URL from an origin and a token', () => {
    expect(shareUrl('https://jobdesk.example', 'abc123')).toBe(
      'https://jobdesk.example/shared/abc123',
    )
  })

  it('does not double the slash when the origin has a trailing one', () => {
    expect(shareUrl('https://jobdesk.example/', 'abc123')).toBe(
      'https://jobdesk.example/shared/abc123',
    )
  })
})

describe('isShareExpired', () => {
  const now = new Date('2026-07-24T12:00:00Z')

  it('treats a null expiry as never expiring', () => {
    expect(isShareExpired(null, now)).toBe(false)
  })

  it('is expired once the expiry is in the past', () => {
    expect(isShareExpired('2026-07-23T12:00:00Z', now)).toBe(true)
  })

  it('is not expired while the expiry is still in the future', () => {
    expect(isShareExpired('2026-07-25T12:00:00Z', now)).toBe(false)
  })
})
