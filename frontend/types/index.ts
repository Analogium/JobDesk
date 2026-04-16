export type ApplicationStatus =
  | 'DRAFT'
  | 'APPLIED'
  | 'WAITING'
  | 'RELAUNCH'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REFUSED'
  | 'ABANDONED'

export type ContractType =
  | 'CDI'
  | 'CDD'
  | 'FREELANCE'
  | 'INTERNSHIP'
  | 'ALTERNANCE'
  | 'OTHER'

export type ApplicationSource =
  | 'linkedin'
  | 'wttj'
  | 'indeed'
  | 'manual'
  | 'other'

export interface User {
  id: string
  email: string
  name: string
  avatarUrl: string | null
  lastMailScanAt: string | null
  createdAt: string
}

export interface StatusHistory {
  id: string
  previousStatus: ApplicationStatus | null
  newStatus: ApplicationStatus
  changedAt: string
  trigger: 'manual' | 'auto_mail'
  notes: string | null
}

export interface Contact {
  id: string
  name: string
  email: string | null
  role: string | null
  notes: string | null
}

export interface Application {
  id: string
  companyName: string
  jobTitle: string
  jobUrl: string | null
  jobDescription: string | null
  location: string | null
  contractType: ContractType | null
  salaryRange: string | null
  status: ApplicationStatus
  appliedAt: string | null
  source: ApplicationSource
  notes: string | null
  createdAt: string
  updatedAt: string
  statusHistories: StatusHistory[]
  contacts: Contact[]
}

export interface ApplicationCreatePayload {
  companyName: string
  jobTitle: string
  jobUrl?: string
  jobDescription?: string
  location?: string
  contractType?: ContractType
  salaryRange?: string
  status?: ApplicationStatus
  appliedAt?: string
  source?: ApplicationSource
  notes?: string
}

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  DRAFT: 'À envoyer',
  APPLIED: 'Envoyée',
  WAITING: 'En attente',
  RELAUNCH: 'Relancé',
  INTERVIEW: 'Entretien',
  OFFER: 'Offre reçue',
  REFUSED: 'Refus',
  ABANDONED: 'Abandonné',
}

export const STATUS_COLORS: Record<ApplicationStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  APPLIED: 'bg-blue-100 text-blue-700',
  WAITING: 'bg-yellow-100 text-yellow-700',
  RELAUNCH: 'bg-orange-100 text-orange-700',
  INTERVIEW: 'bg-purple-100 text-purple-700',
  OFFER: 'bg-green-100 text-green-700',
  REFUSED: 'bg-red-100 text-red-700',
  ABANDONED: 'bg-gray-100 text-gray-500',
}

export const CONTRACT_LABELS: Record<ContractType, string> = {
  CDI: 'CDI',
  CDD: 'CDD',
  FREELANCE: 'Freelance',
  INTERNSHIP: 'Stage',
  ALTERNANCE: 'Alternance',
  OTHER: 'Autre',
}

export const SOURCE_LABELS: Record<ApplicationSource, string> = {
  linkedin: 'LinkedIn',
  wttj: 'Welcome to the Jungle',
  indeed: 'Indeed',
  manual: 'Manuel',
  other: 'Autre',
}
