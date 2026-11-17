import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('placemate_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem('placemate_token')) {
      localStorage.removeItem('placemate_token')
      localStorage.removeItem('placemate_user')
      window.dispatchEvent(new Event('placemate:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export const authApi = {
  register: (payload) => api.post('/auth/register', payload),
  login: (payload) => api.post('/auth/login', payload),
  me: () => api.get('/auth/me'),
}

export const jobApi = {
  browse: (params = {}) => api.get('/jobs', { params }),
  get: (id) => api.get(`/jobs/${id}`),
  mine: () => api.get('/recruiter/jobs'),
  create: (payload) => api.post('/recruiter/jobs', payload),
  update: (id, payload) => api.put(`/recruiter/jobs/${id}`, payload),
  remove: (id) => api.delete(`/recruiter/jobs/${id}`),
  pending: () => api.get('/admin/jobs/pending'),
  review: (id, payload) => api.patch(`/admin/jobs/${id}/review`, payload),
  apply: (jobId) => api.post(`/student/jobs/${jobId}/applications`),
  myApplications: () => api.get('/student/applications'),
  applicants: (jobId) => api.get(`/recruiter/jobs/${jobId}/applications`),
  updateApplicationStatus: (applicationId, payload) => api.patch(`/recruiter/applications/${applicationId}/status`, payload),
}

export const profileApi = {
  get: () => api.get('/student/profile'),
  update: (payload) => api.put('/student/profile', payload),
  matches: () => api.get('/student/jobs/matches'),
}

export const interviewApi = {
  student: () => api.get('/student/interviews'),
  recruiter: () => api.get('/recruiter/interviews'),
  schedule: (applicationId, payload) => api.post(`/recruiter/applications/${applicationId}/interviews`, payload),
  reschedule: (id, payload) => api.put(`/recruiter/interviews/${id}`, payload),
  updateStatus: (id, status) => api.patch(`/recruiter/interviews/${id}/status`, null, { params: { status } }),
}

export const notificationApi = {
  list: () => api.get('/notifications'),
  unreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id) => api.patch(`/notifications/${id}/read`),
  markAllRead: () => api.post('/notifications/read-all'),
}

export const adminApi = {
  overview: () => api.get('/admin/overview'),
  users: (role) => api.get('/admin/users', { params: { role } }),
  companies: () => api.get('/admin/companies'),
  verifyCompany: (id, verified) => api.patch(`/admin/recruiters/${id}/company-verification`, { verified }),
  applications: () => api.get('/admin/applications'),
  placements: () => api.get('/admin/reports/placements'),
}

export const resumeApi = {
  upload: (file) => { const data = new FormData(); data.append('file', file); return api.post('/student/profile/resume', data) },
  studentDownload: () => api.get('/student/resume', { responseType: 'blob' }),
  recruiterDownload: (applicationId) => api.get(`/recruiter/applications/${applicationId}/resume`, { responseType: 'blob' }),
}

export default api
