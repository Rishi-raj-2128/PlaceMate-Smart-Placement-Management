import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Bell, CalendarDays, Check, CheckCheck, Clock3, Download, FileText, ShieldCheck, Users, Building2, BarChart3, RefreshCw, Video, MapPin } from 'lucide-react'
import { adminApi, interviewApi, notificationApi, resumeApi } from '../services/api.js'

const dateTime = value => value ? new Date(value).toLocaleString(undefined,{dateStyle:'medium',timeStyle:'short'}) : '—'
const dateOnly = value => value ? new Date(value).toLocaleDateString(undefined,{dateStyle:'medium'}) : '—'
const label = value => (value||'').replaceAll('_',' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase())
const statusTone = value => ({SCHEDULED:'blue',COMPLETED:'green',CANCELLED:'red',APPLIED:'blue',UNDER_REVIEW:'purple',SHORTLISTED:'green',REJECTED:'red',HIRED:'green'}[value]||'blue')

export function InterviewPage({role}) {
 const [params] = useSearchParams()
 const [items,setItems]=useState([])
 const [applicationId,setApplicationId]=useState(params.get('applicationId')||'')
 const [scheduledAt,setScheduledAt]=useState('')
 const [durationMinutes,setDurationMinutes]=useState('30')
 const [mode,setMode]=useState('ONLINE')
 const [meetingLink,setMeetingLink]=useState('')
 const [location,setLocation]=useState('')
 const [notes,setNotes]=useState('')
 const [loading,setLoading]=useState(true)
 const [saving,setSaving]=useState(false)
 const [error,setError]=useState('')
 const [success,setSuccess]=useState('')
 const recruiter=role==='Recruiter'
 const load=()=>{setLoading(true);(recruiter?interviewApi.recruiter():interviewApi.student()).then(r=>setItems(r.data)).catch(e=>setError(e.response?.data?.message||'Unable to load interviews.')).finally(()=>setLoading(false))}
 useEffect(()=>{load()},[role])
 const submit=async e=>{
  e.preventDefault();setSaving(true);setError('');setSuccess('')
  try{
   await interviewApi.schedule(applicationId,{scheduledAt:new Date(scheduledAt).toISOString(),durationMinutes:Number(durationMinutes),mode,meetingLink:meetingLink.trim()||null,location:location.trim()||null,notes:notes.trim()||null})
   setSuccess('Interview scheduled. The student has received an in-app notification.')
   setApplicationId('');setScheduledAt('');setMeetingLink('');setLocation('');setNotes('');load()
  }catch(e){setError(e.response?.data?.message||'Unable to schedule interview.')}
  finally{setSaving(false)}
 }
 const update=async(id,status)=>{setError('');try{await interviewApi.updateStatus(id,status);load()}catch(e){setError(e.response?.data?.message||'Unable to update interview status.')}}
 return <>
  <div className="page-heading"><div><span className="eyebrow">INTERVIEW WORKSPACE</span><h1>Interviews</h1><p>{recruiter?'Schedule interviews and keep candidates informed.':'Review your upcoming interviews and meeting details.'}</p></div></div>
  {error&&<div className="auth-error">{error}</div>}{success&&<div className="success-message">{success}</div>}
  {recruiter&&<form className="panel workspace-form" onSubmit={submit}>
   <div className="panel-heading"><div><h3>Schedule an interview</h3><p>Use the application ID from the Applicants workspace.</p></div><CalendarDays size={20}/></div>
   <div className="form-grid">
    <label className="form-label">Application ID<input required type="number" min="1" value={applicationId} onChange={e=>setApplicationId(e.target.value)} placeholder="e.g. 12"/></label>
    <label className="form-label">Date & time<input required type="datetime-local" min={new Date(Date.now()+60000).toISOString().slice(0,16)} value={scheduledAt} onChange={e=>setScheduledAt(e.target.value)}/></label>
    <label className="form-label">Duration (minutes)<input required type="number" min="15" max="480" step="15" value={durationMinutes} onChange={e=>setDurationMinutes(e.target.value)}/></label>
    <label className="form-label">Interview mode<select value={mode} onChange={e=>setMode(e.target.value)}><option value="ONLINE">Online</option><option value="IN_PERSON">In person</option><option value="PHONE">Phone</option></select></label>
    <label className="form-label form-span">Meeting link (for online interviews)<input type="url" value={meetingLink} onChange={e=>setMeetingLink(e.target.value)} placeholder="https://meet.google.com/..."/></label>
    <label className="form-label form-span">Location (for in-person interviews)<input value={location} onChange={e=>setLocation(e.target.value)} placeholder="Office address or room"/></label>
    <label className="form-label form-span">Notes<textarea rows="3" value={notes} onChange={e=>setNotes(e.target.value)} placeholder="Interview instructions or preparation notes"/></label>
   </div>
   <div className="post-job-actions"><button className="btn btn-primary" disabled={saving||!applicationId||!scheduledAt}>{saving?'Scheduling...':'Schedule interview'} <CalendarDays size={15}/></button></div>
  </form>}
  <div className="page-subheading"><h2>{recruiter?'Scheduled interviews':'Your interview schedule'}</h2><button className="btn btn-outline" onClick={load}><RefreshCw size={14}/> Refresh</button></div>
  {loading?<div className="panel empty-state">Loading interviews...</div>:items.length===0?<div className="panel empty-state"><CalendarDays size={30}/><h3>No interviews yet</h3><p>{recruiter?'Schedule an interview from an applicant record.':'When a recruiter schedules an interview, its details will appear here.'}</p></div>:
   <div className="workspace-list">{items.map(item=><article className="panel workspace-card" key={item.id}>
    <div className="workspace-card-icon"><CalendarDays size={21}/></div><div className="workspace-card-main">
     <div className="workspace-card-heading"><div><h3>{item.jobTitle}</h3><p>{recruiter?item.studentName+' · '+item.studentEmail:item.companyName}</p></div><span className={'status-badge '+statusTone(item.status)}>{label(item.status)}</span></div>
     <div className="workspace-meta"><span><Clock3 size={14}/>{dateTime(item.scheduledAt)}</span><span>{item.durationMinutes} minutes</span><span>{label(item.mode)}</span></div>
     {item.meetingLink&&<a className="workspace-inline-link" href={item.meetingLink} target="_blank" rel="noreferrer"><Video size={14}/> Join meeting</a>}
     {item.location&&<p className="workspace-note"><MapPin size={14}/>{item.location}</p>}
     {item.notes&&<p className="workspace-note">{item.notes}</p>}
     {recruiter&&item.status==='SCHEDULED'&&<div className="workspace-actions"><button className="btn btn-outline" onClick={()=>update(item.id,'COMPLETED')}>Mark completed</button><button className="btn btn-outline danger-button" onClick={()=>update(item.id,'CANCELLED')}>Cancel interview</button></div>}
    </div>
   </article>)}</div>}
 </>
}

export function NotificationsPage() {
 const [items,setItems]=useState([])
 const [loading,setLoading]=useState(true)
 const [error,setError]=useState('')
 const load=()=>{setLoading(true);notificationApi.list().then(r=>setItems(r.data)).catch(e=>setError(e.response?.data?.message||'Unable to load notifications.')).finally(()=>setLoading(false))}
 useEffect(()=>{load()},[])
 const unread=items.filter(x=>!x.readAt).length
 const read=async id=>{try{const r=await notificationApi.markRead(id);setItems(current=>current.map(x=>x.id===id?r.data:x))}catch(e){setError(e.response?.data?.message||'Unable to mark notification as read.')}}
 const readAll=async()=>{try{await notificationApi.markAllRead();setItems(current=>current.map(x=>({...x,readAt:x.readAt||new Date().toISOString()})))}catch(e){setError(e.response?.data?.message||'Unable to mark notifications as read.')}}
 return <>
  <div className="page-heading"><div><span className="eyebrow">UPDATES</span><h1>Notifications</h1><p>Application updates, new applicants, and interview reminders in one place.</p></div><button className="btn btn-outline" disabled={!unread} onClick={readAll}><CheckCheck size={15}/> Mark all read</button></div>
  {error&&<div className="auth-error">{error}</div>}
  {loading?<div className="panel empty-state">Loading notifications...</div>:items.length===0?<div className="panel empty-state"><Bell size={30}/><h3>You're all caught up</h3><p>New activity will appear here.</p></div>:
   <div className="workspace-list">{items.map(n=><article className={'panel notification-card '+(!n.readAt?'notification-unread':'')} key={n.id}>
    <div className="notification-icon"><Bell size={18}/></div><div className="notification-main"><div className="notification-heading"><h3>{n.title}</h3><small>{dateTime(n.createdAt)}</small></div><p>{n.message}</p><div className="notification-actions">{n.targetUrl&&<Link to={n.targetUrl}>Open <span>→</span></Link>}{!n.readAt&&<button onClick={()=>read(n.id)}><Check size={14}/> Mark read</button>}</div></div>
   </article>)}</div>}
 </>
}

function AdminTable({title,description,columns,rows,loading,error,empty='No records found.'}) {
 return <><div className="page-heading"><div><span className="eyebrow">ADMINISTRATION</span><h1>{title}</h1><p>{description}</p></div></div>{error&&<div className="auth-error">{error}</div>}
  {loading?<div className="panel empty-state">Loading records...</div>:rows.length===0?<div className="panel empty-state"><Users size={28}/><h3>Nothing to show</h3><p>{empty}</p></div>:<div className="panel table-panel"><div className="table-scroll"><table><thead><tr>{columns.map(col=><th key={col.label}>{col.label}</th>)}</tr></thead><tbody>{rows.map((row,index)=><tr key={row.id??row.companyName??index}>{columns.map(col=><td key={col.label}>{col.render?col.render(row):row[col.key]??'—'}</td>)}</tr>)}</tbody></table></div></div>}
 </>;
}

export function AdminStudentsPage(){
 const [rows,setRows]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('')
 useEffect(()=>{adminApi.users('STUDENT').then(r=>setRows(r.data)).catch(e=>setError(e.response?.data?.message||'Unable to load students.')).finally(()=>setLoading(false))},[])
 return <AdminTable title="Students" description="Student accounts registered on PlaceMate." columns={[{label:'Student',key:'fullName'},{label:'Email',key:'email'},{label:'Enrollment',key:'enrollmentNumber'},{label:'Department',key:'department'},{label:'Joined',render:r=>dateOnly(r.createdAt)}]} rows={rows} loading={loading} error={error} empty="No student accounts have registered yet."/>
}

export function AdminRecruitersPage(){
 const [rows,setRows]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[busy,setBusy]=useState(null)
 const load=()=>{setLoading(true);adminApi.users('RECRUITER').then(r=>setRows(r.data)).catch(e=>setError(e.response?.data?.message||'Unable to load recruiters.')).finally(()=>setLoading(false))}
 useEffect(()=>{load()},[])
 const verify=async row=>{setBusy(row.id);setError('');try{const r=await adminApi.verifyCompany(row.id,!row.companyVerified);setRows(current=>current.map(x=>x.id===row.id?r.data:x))}catch(e){setError(e.response?.data?.message||'Unable to update verification.')}finally{setBusy(null)}}
 return <><AdminTable title="Recruiters" description="Review recruiter accounts and verify their company claims." columns={[{label:'Recruiter',key:'fullName'},{label:'Email',key:'email'},{label:'Company',key:'companyName'},{label:'Website',render:r=>r.companyWebsite?<a href={r.companyWebsite} target="_blank" rel="noreferrer">Visit</a>:'—'},{label:'Verification',render:r=><button className={'status-badge '+(r.companyVerified?'green':'red')} disabled={busy===r.id} onClick={()=>verify(r)}>{busy===r.id?'Saving...':r.companyVerified?'Verified · Revoke':'Unverified · Verify'}</button>}]} rows={rows} loading={loading} error={error} empty="No recruiter accounts have registered yet."/><div className="page-subheading"><button className="btn btn-outline" onClick={load}><RefreshCw size={14}/> Refresh</button></div></>
}

export function AdminCompaniesPage(){
 const [rows,setRows]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('')
 useEffect(()=>{adminApi.companies().then(r=>setRows(r.data)).catch(e=>setError(e.response?.data?.message||'Unable to load companies.')).finally(()=>setLoading(false))},[])
 return <AdminTable title="Companies" description="Organizations represented by registered recruiter accounts." columns={[{label:'Company',key:'companyName'},{label:'Website',render:r=>r.website?<a href={r.website} target="_blank" rel="noreferrer">Visit website</a>:'—'},{label:'Recruiter accounts',key:'recruiterAccounts'},{label:'Verified accounts',render:r=>r.verifiedRecruiterAccounts+' / '+r.recruiterAccounts},{label:'Overall status',render:r=><span className={'status-badge '+(r.verified?'green':'red')}>{r.verified?'Verified':'Verification pending'}</span>}]} rows={rows} loading={loading} error={error} empty="Company records appear when recruiters register with a company name."/>
}

export function AdminAnalyticsPage(){
 const [data,setData]=useState(null),[placements,setPlacements]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('')
 useEffect(()=>{Promise.all([adminApi.overview(),adminApi.placements()]).then(([a,b])=>{setData(a.data);setPlacements(b.data)}).catch(e=>setError(e.response?.data?.message||'Unable to load analytics.')).finally(()=>setLoading(false))},[])
 const cards=data?[['Students',data.students],['Recruiters',data.recruiters],['Companies',data.companies],['Pending jobs',data.jobsPending],['Approved jobs',data.jobsApproved],['Applications',data.applications],['Shortlisted',data.shortlisted],['Hired',data.hired]]:[]
 return <><div className="page-heading"><div><span className="eyebrow">PLACEMENT INTELLIGENCE</span><h1>Analytics</h1><p>Live counts from PlaceMate's registered users, jobs, and applications.</p></div></div>{error&&<div className="auth-error">{error}</div>}{loading?<div className="panel empty-state">Loading analytics...</div>:<>
  <div className="admin-metric-grid">{cards.map(([name,value])=><div className="panel admin-metric" key={name}><span>{name}</span><strong>{value}</strong></div>)}</div>
  <div className="panel table-panel admin-report-panel"><div className="panel-heading"><div><h3>Hired students by department</h3><p>Distinct students with application status HIRED.</p></div><BarChart3 size={20}/></div>{placements.length===0?<p className="admin-report-empty">No hired applications have been recorded yet.</p>:<div className="table-scroll"><table><thead><tr><th>Department</th><th>Hired students</th></tr></thead><tbody>{placements.map(p=><tr key={p.department}><td>{p.department}</td><td>{p.hiredStudents}</td></tr>)}</tbody></table></div>}</div>
 </>}</>
}

export function AdminReportsPage(){
 const [rows,setRows]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('')
 useEffect(()=>{adminApi.applications().then(r=>setRows(r.data)).catch(e=>setError(e.response?.data?.message||'Unable to load placement records.')).finally(()=>setLoading(false))},[])
 const exportCsv=()=>{
  const columns=['Application ID','Student','Email','Department','Company','Job','Status','Applied At']
  const values=rows.map(r=>[r.id,r.studentName,r.studentEmail,r.department,r.companyName,r.jobTitle,r.status,r.appliedAt])
  const escape=value=>'"'+String(value??'').replaceAll('"','""')+'"'
  const csv=[columns,...values].map(row=>row.map(escape).join(',')).join('\r\n')
  const url=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}))
  const a=document.createElement('a');a.href=url;a.download='placemate-applications-report.csv';a.click();URL.revokeObjectURL(url)
 }
 return <><AdminTable title="Reports" description="Review placement application records and export a CSV report." columns={[{label:'Student',key:'studentName'},{label:'Department',key:'department'},{label:'Company',key:'companyName'},{label:'Position',key:'jobTitle'},{label:'Status',render:r=><span className={'status-badge '+statusTone(r.status)}>{label(r.status)}</span>},{label:'Applied',render:r=>dateOnly(r.appliedAt)}]} rows={rows} loading={loading} error={error} empty="Applications will appear here after students apply to approved jobs."/><div className="page-subheading"><span>{rows.length} application records</span><button className="btn btn-primary" disabled={!rows.length} onClick={exportCsv}><Download size={15}/> Export CSV</button></div></>
}
