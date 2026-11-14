import { test, expect } from '@playwright/test'

const uniqueEmail = (prefix) => `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}@example.test`
const password = 'StrongPass123'

test('student can register, reach the live dashboard, sign out and sign back in', async ({ page }) => {
  const email = uniqueEmail('e2e-student')

  await page.goto('/register')
  await expect(page.getByRole('heading', { name: 'Start your journey' })).toBeVisible()
  await page.getByLabel('Full name').fill('E2E Student')
  await page.getByLabel('Email address').fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByLabel('Confirm password').fill(password)
  await page.getByLabel('Enrollment number').fill(`E2E-${Date.now()}`)
  await page.getByLabel('Department').fill('Computer Science')
  await page.getByRole('button', { name: /Create account/ }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: 'Your placement journey' })).toBeVisible()
  await expect(page.getByText('Student account')).toBeVisible()

  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await page.getByLabel('Email address').fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: 'Your placement journey' })).toBeVisible()
})

test('recruiter can register and reach the recruiter workspace', async ({ page }) => {
  await page.goto('/register')
  await page.getByRole('button', { name: 'Recruiter' }).click()
  await page.getByLabel('Recruiter name').fill('E2E Recruiter')
  await page.getByLabel('Email address').fill(uniqueEmail('e2e-recruiter'))
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByLabel('Confirm password').fill(password)
  await page.getByLabel('Company name').fill('E2E Placement Labs')
  await page.getByLabel('Company website').fill('https://example.test')
  await page.getByRole('button', { name: /Create account/ }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: 'Recruiter workspace' })).toBeVisible()
  await expect(page.getByText('Recruiter account')).toBeVisible()
  await expect(page.getByRole('navigation').getByRole('link', { name: 'Post a job' })).toBeVisible()
})

test('guest is redirected to login when opening a protected workspace route', async ({ page }) => {
  await page.goto('/dashboard')
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { name: 'Good to see you again.' })).toBeVisible()
})

test('admin self-registration is rejected by the API', async ({ request }) => {
  const response = await request.post('http://127.0.0.1:8080/api/auth/register', {
    data: {
      fullName: 'Unauthorized Admin',
      email: uniqueEmail('e2e-admin'),
      password,
      role: 'ADMIN',
    },
  })
  expect(response.status()).toBe(403)
})
