import { describe, it, expect } from 'vitest'
import { normalizeText, matchesQuery } from '~/utils/search'
import type { Application } from '~/types'

function makeApp(overrides: Partial<Application> = {}): Application {
  return {
    id: '550e8400-e29b-41d4-a716-446655440000',
    companyName: 'ACME',
    jobTitle: 'Developer',
    jobUrl: null,
    jobDescription: null,
    location: null,
    contractType: null,
    salaryRange: null,
    status: 'APPLIED',
    appliedAt: null,
    source: 'MANUAL',
    notes: null,
    createdAt: '2026-01-01T00:00:00+00:00',
    updatedAt: '2026-01-01T00:00:00+00:00',
    statusHistories: [],
    contacts: [],
    ...overrides,
  }
}

describe('normalizeText', () => {
  it('lowercases and strips accents', () => {
    expect(normalizeText('Société Générale')).toBe('societe generale')
    expect(normalizeText('DÉVELOPPEUR')).toBe('developpeur')
  })
})

describe('matchesQuery', () => {
  it('matches on the company name, case- and accent-insensitively', () => {
    const app = makeApp({ companyName: 'Société Générale' })
    expect(matchesQuery(app, 'societe')).toBe(true)
    expect(matchesQuery(app, 'GÉNÉRALE')).toBe(true)
  })

  it('matches on the job title, the location and the notes', () => {
    const app = makeApp({ jobTitle: 'Développeur Fullstack', location: 'Lyon', notes: 'via LinkedIn' })
    expect(matchesQuery(app, 'fullstack')).toBe(true)
    expect(matchesQuery(app, 'lyon')).toBe(true)
    expect(matchesQuery(app, 'linkedin')).toBe(true)
  })

  it('requires every term to match, regardless of the field or the order', () => {
    const app = makeApp({ companyName: 'Acme', jobTitle: 'Développeur' })
    expect(matchesQuery(app, 'acme dev')).toBe(true)
    expect(matchesQuery(app, 'dev acme')).toBe(true)
    // « google » n'apparaît nulle part : la candidature ne correspond pas.
    expect(matchesQuery(app, 'acme google')).toBe(false)
  })

  it('treats a blank query as matching everything', () => {
    const app = makeApp()
    expect(matchesQuery(app, '')).toBe(true)
    expect(matchesQuery(app, '   ')).toBe(true)
  })

  it('does not crash on null optional fields', () => {
    const app = makeApp({ location: null, notes: null })
    expect(matchesQuery(app, 'acme')).toBe(true)
    expect(matchesQuery(app, 'nowhere')).toBe(false)
  })
})
