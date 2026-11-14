import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ArrowRight, BriefcaseBusiness, Check, CheckCircle2, Plus, Trash2 } from 'lucide-react'
import { jobApi } from '../services/api.js'

export function RecruiterJobsPage() {
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const load = () => {
    setLoading(true)
    jobApi.mine()
      .then(response => setJobs(response.data))
      .catch(err => setError(err.response?.data?.message || 'Unable to load your job postings.'))
      .finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])
  const remove = async (id) => {
    if (!window.confirm('Delete this job posting?')) return
    try { await jobApi.remove(id); load() }
    catch (err) { setError(err.response?.data?.message || 'Could not delete this job.') }
  }
  return <>
    <div className="page-heading"><div><span className="eyebrow">RECRUITER WORKSPACE</span><h1>Manage jobs</h1><p>Review your postings and track their approval status.</p></div><Link to="/post-job" className="btn btn-primary"><Plus size={16}/> Post a job</Link></div>
    {error && <div className="auth-error">{error}</div>}
    {loading ? <div className="panel empty-state">Loading your job postings...</div> :
      jobs.length === 0 ? <div className="panel empty-state"><BriefcaseBusiness size={28}/><h3>No job postings yet</h3><p>Create your first opportunity. Students can see it after admin approval.</p><Link to="/post-job" className="btn btn-primary">Create a job <ArrowRight size={15}/></Link></div> :
      <div className="panel table-panel"><div className="table-scroll"><table><thead><tr><th>POSITION</th><th>LOCATION</th><th>TYPE</th><th>STATUS</th><th>ACTIONS</th></tr></thead><tbody>
        {jobs.map(job => <tr key={job.id}><td><strong>{job.title}</strong><br/><small>{job.companyName}</small></td><td>{job.location}</td><td>{job.employmentType}</td><td><span className={'status-badge '+(job.status==='APPROVED'?'green':job.status==='REJECTED'?'red':'blue')}>{job.status}</span></td><td><Link className="text-link" to={"/applicants?jobId="+job.id}>Applicants</Link> <Link className="text-link" to="/post-job" state={{job}}>Edit</Link> <button className="text-link" onClick={() => remove(job.id)}>Delete</button></td></tr>)}
      </tbody></table></div></div>}
  </>
}

const emptyJob = { title:'', description:'', companyName:'', location:'', employmentType:'FULL_TIME', salaryMin:'', salaryMax:'', minCgpa:'', requiredSkills:'', deadline:'' }

export function PostJobPage() {
  const location = useLocation()
  const existing = location.state?.job
  const [form, setForm] = useState(() => existing ? {
    ...existing, requiredSkills:(existing.requiredSkills||[]).join(', '),
    salaryMin:existing.salaryMin ?? '', salaryMax:existing.salaryMax ?? '',
    minCgpa:existing.minCgpa ?? '', deadline:existing.deadline || ''
  } : emptyJob)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const change = event => setForm(current => ({ ...current, [event.target.name]:event.target.value }))
  const submit = async event => {
    event.preventDefault(); setSaving(true); setError(''); setSuccess('')
    const payload = {
      title:form.title, description:form.description, companyName:form.companyName,
      location:form.location, employmentType:form.employmentType,
      salaryMin:form.salaryMin===''?null:Number(form.salaryMin),
      salaryMax:form.salaryMax===''?null:Number(form.salaryMax),
      minCgpa:form.minCgpa===''?null:Number(form.minCgpa),
      requiredSkills:form.requiredSkills.split(',').map(s=>s.trim()).filter(Boolean),
      deadline:form.deadline || null
    }
    try {
      if (existing?.id) await jobApi.update(existing.id,payload)
      else await jobApi.create(payload)
      setSuccess('Job saved and submitted for admin approval.')
      if (!existing) setForm(emptyJob)
    } catch (err) { setError(err.response?.data?.message || 'Could not save this job. Please check the details.') }
    finally { setSaving(false) }
  }
  return <>
    <div className="page-heading"><div><span className="eyebrow">RECRUITER WORKSPACE</span><h1>{existing?'Edit job posting':'Post a new job'}</h1><p>New and edited postings require admin approval before appearing to students.</p></div></div>
    <form className="panel post-job-form" onSubmit={submit}>
      <div className="form-grid">
        <label className="form-label">Job title<input name="title" value={form.title} onChange={change} maxLength={150} required placeholder="e.g. Graduate Software Engineer"/></label>
        <label className="form-label">Company name<input name="companyName" value={form.companyName} onChange={change} maxLength={150} required placeholder="Company name"/></label>
        <label className="form-label">Location<input name="location" value={form.location} onChange={change} maxLength={150} required placeholder="City · Work mode"/></label>
        <label className="form-label">Employment type<select name="employmentType" value={form.employmentType} onChange={change}><option value="FULL_TIME">Full-time</option><option value="INTERNSHIP">Internship</option><option value="PART_TIME">Part-time</option><option value="CONTRACT">Contract</option></select></label>
        <label className="form-label">Minimum salary (₹ / year)<input type="number" min="0" name="salaryMin" value={form.salaryMin} onChange={change} placeholder="Optional"/></label>
        <label className="form-label">Maximum salary (₹ / year)<input type="number" min="0" name="salaryMax" value={form.salaryMax} onChange={change} placeholder="Optional"/></label>
        <label className="form-label">Minimum CGPA<input type="number" min="0" max="10" step="0.01" name="minCgpa" value={form.minCgpa} onChange={change} placeholder="Optional"/></label>
        <label className="form-label">Application deadline<input type="date" name="deadline" value={form.deadline} onChange={change}/></label>
        <label className="form-label form-span">Required skills <span className="optional-label">Comma separated</span><input name="requiredSkills" value={form.requiredSkills} onChange={change} placeholder="Java, SQL, React"/></label>
        <label className="form-label form-span">Job description<textarea name="description" value={form.description} onChange={change} rows="6" maxLength={10000} required placeholder="Describe responsibilities, qualifications and benefits..."/></label>
      </div>
      {error && <div className="auth-error">{error}</div>}
      {success && <div className="success-message" role="status">{success}</div>}
      <div className="post-job-actions"><Link to="/jobs" className="btn btn-outline">Cancel</Link><button className="btn btn-primary" disabled={saving}>{saving?'Saving...':existing?'Save changes':'Submit for approval'} <ArrowRight size={15}/></button></div>
    </form>
  </>
}

export function JobApprovalsPage() {
  const [jobs,setJobs] = useState([])
  const [loading,setLoading] = useState(true)
  const [error,setError] = useState('')
  const [notes,setNotes] = useState({})
  const load = () => {
    setLoading(true)
    jobApi.pending().then(response=>setJobs(response.data))
      .catch(err=>setError(err.response?.data?.message||'Unable to load pending approvals.'))
      .finally(()=>setLoading(false))
  }
  useEffect(()=>{load()},[])
  const review = async (id,status) => {
    try {
      await jobApi.review(id,{status,reviewNote:notes[id]||''})
      setJobs(current=>current.filter(job=>job.id!==id))
    } catch(err) { setError(err.response?.data?.message||'Review action failed.') }
  }
  return <>
    <div className="page-heading"><div><span className="eyebrow">ADMIN WORKSPACE</span><h1>Job approvals</h1><p>Review recruiter-submitted opportunities before publishing them to students.</p></div></div>
    {error&&<div className="auth-error">{error}</div>}
    {loading?<div className="panel empty-state">Loading pending jobs...</div>:jobs.length===0?<div className="panel empty-state"><CheckCircle2 size={28}/><h3>All caught up</h3><p>There are no job postings waiting for review.</p></div>:
      <div className="approval-list">{jobs.map(job=><article className="panel approval-card" key={job.id}>
        <div className="approval-heading"><div><span className="status-badge blue">PENDING REVIEW</span><h3>{job.title}</h3><p>{job.companyName} · {job.location} · {job.employmentType}</p></div><small>Posted by {job.recruiterName}</small></div>
        <p className="approval-description">{job.description}</p>
        <div className="job-tags">{(job.requiredSkills||[]).map(skill=><span key={skill}>{skill}</span>)}</div>
        <label className="form-label">Review note <span className="optional-label">Optional</span><input value={notes[job.id]||''} onChange={e=>setNotes(current=>({...current,[job.id]:e.target.value}))} placeholder="Reason or feedback for recruiter"/></label>
        <div className="post-job-actions"><button className="btn btn-outline" onClick={()=>review(job.id,'REJECTED')}>Reject</button><button className="btn btn-primary" onClick={()=>review(job.id,'APPROVED')}>Approve <Check size={15}/></button></div>
      </article>)}</div>}
  </>
}
