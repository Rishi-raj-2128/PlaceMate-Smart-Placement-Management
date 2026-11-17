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
  await page.getByRole('navigation').getByRole('link', { name: 'Post a job' }).click()
  await expect(page.getByRole('heading', { name: /Post a new job/i })).toBeVisible()
})

test('complete placement lifecycle: recruiter posting, admin approval, student application, shortlist, interview and notification', async ({ page }) => {
  const suffix = `${Date.now()}-${Math.random().toString(16).slice(2, 7)}`
  const recruiterEmail = `lifecycle-recruiter-${suffix}@example.test`
  const studentEmail = `lifecycle-student-${suffix}@example.test`
  const password = 'StrongPass123'
  const jobTitle = `E2E Software Engineer ${suffix}`
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000)
  tomorrow.setSeconds(0, 0)
  const interviewDate = new Date(tomorrow.getTime() - tomorrow.getTimezoneOffset() * 60000).toISOString().slice(0, 16)

  // Recruiter creates a pending job.
  await page.goto('/register')
  await page.getByRole('button', { name: 'Recruiter' }).click()
  await page.getByLabel('Recruiter name').fill('Lifecycle Recruiter')
  await page.getByLabel('Email address').fill(recruiterEmail)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByLabel('Confirm password').fill(password)
  await page.getByLabel('Company name').fill('Lifecycle Test Company')
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await page.getByRole('navigation').getByRole('link', { name: 'Post a job' }).click()
  // Routes are wrapped in Framer Motion with AnimatePresence mode="wait". Do not
  // interact while the old route is exiting: the new route can still be remounted
  // at the end of the 200ms transition, clearing controlled form state mid-test.
  await expect(page.locator('.dashboard-content > div').first()).toHaveCSS('transform', 'none')
  await expect(page.locator('.post-job-form')).toBeVisible()
  // Scope selectors to the actual job form so similarly named registration fields
  // or future layout controls can never receive the test input accidentally.
  const jobForm = page.locator('.post-job-form')
  const titleInput = jobForm.locator('input[name="title"]')
  const companyInput = jobForm.locator('input[name="companyName"]')
  const locationInput = jobForm.locator('input[name="location"]')
  const skillsInput = jobForm.locator('input[name="requiredSkills"]')
  const descriptionInput = jobForm.locator('textarea[name="description"]')
  await titleInput.fill(jobTitle)
  await expect(titleInput).toHaveValue(jobTitle)
  await companyInput.fill('Lifecycle Test Company')
  await expect(companyInput).toHaveValue('Lifecycle Test Company')
  await locationInput.fill('Mohali · Hybrid')
  await expect(locationInput).toHaveValue('Mohali · Hybrid')
  await jobForm.locator('select[name="employmentType"]').selectOption('FULL_TIME')
  await jobForm.locator('input[name="minCgpa"]').fill('6.0')
  await skillsInput.fill('Java, Spring Boot')
  await expect(skillsInput).toHaveValue('Java, Spring Boot')
  await descriptionInput.fill('Browser-tested placement lifecycle role.')
  await expect(descriptionInput).toHaveValue('Browser-tested placement lifecycle role.')
  // Assert controlled form values before submitting so regressions in React state handling are caught.
  const formValidity = await page.locator('form').evaluate(form => ({
    valid: form.checkValidity(),
    invalidFields: [...form.elements].filter(field => !field.checkValidity()).map(field => ({
      name: field.name, value: field.value, validationMessage: field.validationMessage,
    })),
  }))
  expect(formValidity, JSON.stringify(formValidity)).toEqual({ valid: true, invalidFields: [] })
  const createJobResponsePromise = page.waitForResponse(response =>
    response.request().method() === 'POST' &&
    new URL(response.url()).pathname.endsWith('/api/recruiter/jobs'),
    { timeout: 15000 }
  )
  await page.getByRole('button', { name: 'Submit for approval' }).click()
  let createJobResponse
  try {
    createJobResponse = await createJobResponsePromise
  } catch (error) {
    const visibleError = await page.locator('.auth-error').allTextContents()
    throw new Error(`Job form did not reach POST /api/recruiter/jobs. Visible errors: ${visibleError.join(' | ') || 'none'}. ${error.message}`)
  }
  expect(createJobResponse.status(), await createJobResponse.text()).toBe(201)
  await expect(page.getByRole('status')).toContainText('Job saved and submitted for admin approval.')

  // Admin is provisioned only in the isolated CI E2E environment.
  await page.getByRole('button', { name: 'Sign out' }).click()
  await page.goto('/login')
  await page.getByLabel('Email address').fill(process.env.E2E_ADMIN_EMAIL)
  await page.getByLabel('Password', { exact: true }).fill(process.env.E2E_ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await page.getByRole('link', { name: 'Job approvals' }).click()
  const approvalCard = page.locator('.approval-card').filter({ hasText: jobTitle })
  await expect(approvalCard).toBeVisible()
  await approvalCard.getByRole('button', { name: 'Approve' }).click()
  await expect(approvalCard).toHaveCount(0)

  // Student completes profile and applies to the now-approved opening.
  await page.getByRole('button', { name: 'Sign out' }).click()
  await page.goto('/register')
  await page.getByLabel('Full name').fill('Lifecycle Student')
  await page.getByLabel('Email address').fill(studentEmail)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByLabel('Confirm password').fill(password)
  await page.getByLabel('Enrollment number').fill(`LIFE-${suffix}`)
  await page.getByLabel('Department').fill('Computer Science')
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await page.getByRole('link', { name: 'My profile' }).click()
  await page.getByLabel('CGPA (out of 10)').fill('8.2')
  await page.getByLabel('Graduation year').fill('2028')
  await page.getByLabel('Degree').fill('B.Tech')
  await page.getByLabel('Branch / specialization').fill('Computer Science')
  await page.getByLabel(/Skills/).fill('Java, Spring Boot, SQL')
  await page.getByRole('button', { name: 'Save profile' }).click()
  await expect(page.getByRole('status')).toContainText('Your profile has been saved successfully.')
  await page.getByRole('link', { name: 'Find jobs' }).click()
  const jobCard = page.locator('.job-card').filter({ hasText: jobTitle })
  await expect(jobCard).toBeVisible()
  await jobCard.getByRole('button', { name: 'Apply now' }).click()
  await expect(page.getByRole('status')).toContainText('Application submitted')

  // Recruiter shortlists the new application and schedules an interview.
  await page.getByRole('button', { name: 'Sign out' }).click()
  await page.goto('/login')
  await page.getByLabel('Email address').fill(recruiterEmail)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await page.getByRole('link', { name: 'Applicants' }).click()
  const applicantCard = page.locator('.applicant-card').filter({ hasText: studentEmail })
  await expect(applicantCard).toBeVisible()
  await applicantCard.getByLabel('Application status').selectOption('SHORTLISTED')
  await expect(applicantCard.getByText('Shortlisted')).toBeVisible()
  await applicantCard.getByRole('link', { name: 'Schedule interview' }).click()
  await expect(page.getByRole('heading', { name: 'Interviews' })).toBeVisible()
  await page.getByLabel('Date & time').fill(interviewDate)
  await page.getByLabel('Duration (minutes)').fill('30')
  await page.getByLabel('Interview mode').selectOption('ONLINE')
  await page.getByLabel('Meeting link (for online interviews)').fill('https://meet.example.test/placemate-e2e')
  await page.getByLabel('Notes').fill('Bring your project portfolio.')
  await page.getByRole('button', { name: 'Schedule interview' }).click()
  await expect(page.getByText('Interview scheduled. The student has received an in-app notification.')).toBeVisible()

  // Student sees both the shortlist update and the interview notification.
  await page.getByRole('button', { name: 'Sign out' }).click()
  await page.goto('/login')
  await page.getByLabel('Email address').fill(studentEmail)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await page.getByRole('link', { name: 'Notifications' }).click()
  await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Application status updated' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Interview scheduled' })).toBeVisible()
  await expect(page.getByText(new RegExp(jobTitle))).toBeVisible()
})
