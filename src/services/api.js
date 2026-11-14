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
    if (error.response?.status === 401) {
      localStorage.removeItem('placemate_token')
      localStorage.removeItem('placemate_user')
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

export default api
