import { test, expect } from '@playwright/test'

const uniqueEmail = (prefix) => `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}@example.test`

test('student can register, open the live dashboard, update profile and sign out', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Your career journey starts here.' })).toBeVisible()
  await page.getByRole('link', { name: 'Get started' }).first().click()

  await expect(page.getByRole('heading', { name: 'Start your journey' })).toBeVisible()
  await page.getByLabel('Full name').fill('E2E Student')
  await page.getByLabel('Email address').fill(uniqueEmail('student'))
  await page.getByLabel('Password', { exact: true }).fill('StrongPass123')
  await page.getByLabel('Confirm password').fill('StrongPass123')
  await page.getByLabel('Enrollment number').fill(`E2E-${Date.now()}`)
  await page.getByLabel('Department').fill('Computer Science')
  await page.getByRole('button', { name: 'Create account' }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: 'Your placement journey' })).toBeVisible()
  await page.getByRole('link', { name: 'My profile' }).click()
  await expect(page.getByRole('heading', { name: 'My profile' })).toBeVisible()
  await page.getByLabel('CGPA (out of 10)').fill('8.4')
  await page.getByLabel('Graduation year').fill('2028')
  await page.getByLabel('Degree').fill('B.Tech')
  await page.getByLabel('Branch / specialization').fill('Computer Science')
  await page.getByLabel(/Skills/).fill('Java, Spring Boot, React')
  await page.getByRole('button', { name: 'Save profile' }).click()
  await expect(page.getByRole('status')).toContainText('Your profile has been saved successfully.')

  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { name: 'Good to see you again.' })).toBeVisible()
})

test('recruiter can register and access recruiter-only workspace', async ({ page }) => {
  await page.goto('/register')
  await page.getByRole('button', { name: 'Recruiter' }).click()
  await page.getByLabel('Recruiter name').fill('E2E Recruiter')
  await page.getByLabel('Email address').fill(uniqueEmail('recruiter'))
  await page.getByLabel('Password', { exact: true }).fill('StrongPass123')
  await page.getByLabel('Confirm password').fill('StrongPass123')
  await page.getByLabel('Company name').fill('E2E Placement Labs')
  await page.getByLabel('Company website').fill('https://example.test')
  await page.getByRole('button', { name: 'Create account' }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: 'Recruiter workspace' })).toBeVisible()
  await expect(page.getByText('Recruiter account')).toBeVisible()
  await page.getByRole('link', { name: 'Post a job' }).click()
  await expect(page.getByRole('heading', { name: /Post a new job/i })).toBeVisible()
})
