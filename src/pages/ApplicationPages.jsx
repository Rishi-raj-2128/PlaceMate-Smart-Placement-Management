import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { BriefcaseBusiness, CheckCircle2, Clock3, Users } from 'lucide-react'
import { jobApi } from '../services/api.js'

const statusTone = status => ({
  APPLIED:'blue', UNDER_REVIEW:'purple', SHORTLISTED:'green',
  REJECTED:'red', HIRED:'green'
}[status] || 'blue')

const prettyStatus = status => (status || '').replaceAll('_',' ').toLowerCase()
  .replace(/\b\w/g, letter => letter.toUpperCase())

const dateLabel = value => value ? new Date(value).toLocaleDateString(undefined,{day:'2-digit',month:'short',year:'numeric'}) : '—'

export function StudentApplicationsPage() {
  const [items,setItems] = useState([])
  const [loading,setLoading] = useState(true)
  const [error,setError] = useState('')
  useEffect(() => {
    let active = true
    jobApi.myApplications()
      .then(response => { if(active)setItems(response.data) })
      .catch(err => { if(active)setError(err.response?.data?.message || 'Unable to load your applications.') })
      .finally(() => { if(active)setLoading(false) })
    return () => { active = false }
  },[])
  return <>
    <div className="page-heading"><div><span className="eyebrow">YOUR JOURNEY</span><h1>My applications</h1><p>Track every opportunity and follow your application progress.</p></div><span className="status-badge blue">{items.length} total</span></div>
    {error&&<div className="auth-error">{error}</div>}
    {loading?<div className="panel empty-state">Loading your applications...</div>:
      items.length===0?<div className="panel empty-state"><BriefcaseBusiness size={30}/><h3>No applications yet</h3><p>When you apply to an approved job, it will appear here with its latest status.</p></div>:
      <div className="application-list">{items.map(item=><article className="panel application-card" key={item.id}>
        <div className="application-company-mark">{(item.companyName||'C').charAt(0).toUpperCase()}</div>
        <div className="application-main"><div className="application-title-row"><div><h3>{item.jobTitle}</h3><p>{item.companyName} · {item.jobLocation}</p></div><span className={'status-badge '+statusTone(item.status)}>{prettyStatus(item.status)}</span></div>
          <div className="application-meta"><span><Clock3 size={14}/> Applied {dateLabel(item.appliedAt)}</span>{item.recruiterNote&&<span className="application-note">Recruiter note: {item.recruiterNote}</span>}</div>
        </div>
      </article>)}</div>}
  </>
}

export function RecruiterApplicantsPage() {
  const [searchParams,setSearchParams] = useSearchParams()
  const [jobs,setJobs] = useState([])
  const [selectedJobId,setSelectedJobId] = useState(searchParams.get('jobId')||'')
  const [items,setItems] = useState([])
  const [loadingJobs,setLoadingJobs] = useState(true)
  const [loadingApplicants,setLoadingApplicants] = useState(false)
  const [error,setError] = useState('')
  const [savingId,setSavingId] = useState(null)
  const [notes,setNotes] = useState({})
  useEffect(() => {
    let active = true
    jobApi.mine().then(response => {
      if(!active)return
      setJobs(response.data)
      const requested = searchParams.get('jobId')
      const chosen = response.data.some(job=>String(job.id)===String(requested)) ? String(requested) : response.data[0] ? String(response.data[0].id) : ''
      setSelectedJobId(chosen)
    }).catch(err=>{if(active)setError(err.response?.data?.message||'Unable to load your job postings.')})
      .finally(()=>{if(active)setLoadingJobs(false)})
    return ()=>{active=false}
  },[])
  useEffect(() => {
    if(!selectedJobId){setItems([]);return}
    let active=true
    setLoadingApplicants(true);setError('')
    jobApi.applicants(selectedJobId).then(response=>{if(active)setItems(response.data)})
      .catch(err=>{if(active)setError(err.response?.data?.message||'Unable to load applicants for this job.')})
      .finally(()=>{if(active)setLoadingApplicants(false)})
    setSearchParams({jobId:selectedJobId},{replace:true})
    return ()=>{active=false}
  },[selectedJobId])
  const updateStatus = async (applicationId,status) => {
    setSavingId(applicationId);setError('')
    try {
      const response=await jobApi.updateApplicationStatus(applicationId,{status,recruiterNote:notes[applicationId]||null})
      setItems(current=>current.map(item=>item.id===applicationId?response.data:item))
    } catch(err) { setError(err.response?.data?.message||'Unable to update this application.') }
    finally { setSavingId(null) }
  }
  const selectedJob=jobs.find(job=>String(job.id)===String(selectedJobId))
  return <>
    <div className="page-heading"><div><span className="eyebrow">RECRUITER WORKSPACE</span><h1>Applicants</h1><p>Review candidates and manage the hiring pipeline for your job postings.</p></div></div>
    {error&&<div className="auth-error">{error}</div>}
    {loadingJobs?<div className="panel empty-state">Loading your job postings...</div>:jobs.length===0?<div className="panel empty-state"><Users size={28}/><h3>No job postings yet</h3><p>Post a job and wait for students to apply after admin approval.</p></div>:<>
      <div className="panel applicant-filter"><label className="form-label">Select job posting<select value={selectedJobId} onChange={e=>setSelectedJobId(e.target.value)}>{jobs.map(job=><option key={job.id} value={job.id}>{job.title} — {job.companyName}</option>)}</select></label><div><strong>{items.length}</strong><span> applicants</span></div></div>
      {selectedJob&&selectedJob.status!=='APPROVED'&&<div className="info-banner">This job is currently {prettyStatus(selectedJob.status).toLowerCase()}. Applicant records are shown for this posting.</div>}
      {loadingApplicants?<div className="panel empty-state">Loading applicants...</div>:items.length===0?<div className="panel empty-state"><CheckCircle2 size={28}/><h3>No applicants yet</h3><p>Applications will appear here when students apply to this posting.</p></div>:
        <div className="applicant-list">{items.map(item=><article className="panel applicant-card" key={item.id}>
          <div className="applicant-avatar">{(item.studentName||'S').charAt(0).toUpperCase()}</div>
          <div className="applicant-details"><div className="applicant-heading"><div><h3>{item.studentName}</h3><p>{item.studentEmail}</p></div><span className={'status-badge '+statusTone(item.status)}>{prettyStatus(item.status)}</span></div>
            <div className="applicant-meta"><span>{item.enrollmentNumber||'Enrollment not provided'}</span><span>{item.department||'Department not provided'}</span><span>Applied {dateLabel(item.appliedAt)}</span></div>
            <div className="applicant-actions"><input aria-label="Recruiter note" value={notes[item.id]??item.recruiterNote??''} onChange={e=>setNotes(current=>({...current,[item.id]:e.target.value}))} placeholder="Optional note for student"/><select aria-label="Application status" value={item.status} disabled={savingId===item.id} onChange={e=>updateStatus(item.id,e.target.value)}>{['UNDER_REVIEW','SHORTLISTED','REJECTED','HIRED'].map(status=><option key={status} value={status}>{prettyStatus(status)}</option>)}</select><span>{savingId===item.id?'Saving...':'Update status'}</span></div>
          </div>
        </article>)}</div>}
    </>}
  </>
}
