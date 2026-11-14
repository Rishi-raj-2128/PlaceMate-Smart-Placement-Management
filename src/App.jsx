import { useEffect, useMemo, useState } from 'react'
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import {
  ArrowDownRight, ArrowRight, ArrowUpRight, Bell, BriefcaseBusiness, Building2, Check,
  ChevronDown, CircleHelp, Clock3, Code2, FileText, Filter, GraduationCap, LayoutDashboard,
  LogOut, Menu, MoreHorizontal, Search, Settings, ShieldCheck, Sparkles, Target, Users,
  X, Bookmark, MapPin, IndianRupee, CalendarDays, TrendingUp, ChartNoAxesCombined,
  UserRound, Mail, LockKeyhole, Eye, EyeOff, Plus, SlidersHorizontal, CheckCircle2,
  CircleX, Send, ExternalLink, Zap, Award, BookOpen, MessageSquare
} from 'lucide-react'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, BarChart, Bar } from 'recharts'
import { authApi, jobApi, profileApi, resumeApi, notificationApi, adminApi, interviewApi } from './services/api.js'
import { RecruiterJobsPage, PostJobPage, JobApprovalsPage } from './pages/JobManagement.jsx'
import { StudentApplicationsPage, RecruiterApplicantsPage } from './pages/ApplicationPages.jsx'
import { InterviewPage, NotificationsPage, AdminStudentsPage, AdminRecruitersPage, AdminCompaniesPage, AdminAnalyticsPage, AdminReportsPage } from './pages/WorkspacePages.jsx'

const jobs = [
  { id: 1, company: 'Microsoft', role: 'Software Engineer', place: 'Bengaluru · Hybrid', salary: '₹18–24 LPA', cgpa: '7.5+', skills: ['Java', 'DSA', 'OOP'], days: '3 days left', color: '#2563eb', logo: 'M', match: 96, type: 'Full-time' },
  { id: 2, company: 'Deloitte', role: 'Analyst – Technology', place: 'Hyderabad · On-site', salary: '₹8–10 LPA', cgpa: '7.0+', skills: ['SQL', 'Python', 'Communication'], days: '5 days left', color: '#86bc25', logo: 'D', match: 91, type: 'Full-time' },
  { id: 3, company: 'Adobe', role: 'Frontend Developer Intern', place: 'Noida · Hybrid', salary: '₹55K / month', cgpa: '7.5+', skills: ['React', 'JavaScript', 'CSS'], days: '1 week left', color: '#e34b4b', logo: 'A', match: 88, type: 'Internship' },
  { id: 4, company: 'Amazon', role: 'SDE – Graduate', place: 'Chennai · On-site', salary: '₹20–28 LPA', cgpa: '8.0+', skills: ['Java', 'Problem solving', 'DBMS'], days: '9 days left', color: '#f59e0b', logo: 'a', match: 84, type: 'Full-time' }
]
const activity = [{month:'Jan',apps:18,selected:5},{month:'Feb',apps:26,selected:8},{month:'Mar',apps:22,selected:7},{month:'Apr',apps:34,selected:12},{month:'May',apps:29,selected:10},{month:'Jun',apps:43,selected:17}]
const demoApplications = [
  {company:'Microsoft',role:'Software Engineer',date:'12 Nov 2026',status:'Under review',tone:'blue'},
  {company:'Deloitte',role:'Technology Analyst',date:'10 Nov 2026',status:'Shortlisted',tone:'green'},
  {company:'Adobe',role:'Frontend Intern',date:'08 Nov 2026',status:'Interview scheduled',tone:'purple'}
]

function Brand({light=false}) { return <Link to="/" className={`brand ${light?'brand-light':''}`}><span className="brand-mark"><GraduationCap size={21}/></span><span>place<span className="brand-accent">mate</span></span></Link> }
function Button({children,variant='primary',to,onClick,className=''}) { const cls=`btn btn-${variant} ${className}`; return to ? <Link className={cls} to={to}>{children}</Link> : <button className={cls} onClick={onClick}>{children}</button> }

function Landing() {
  const [mobileOpen,setMobileOpen]=useState(false)
  const features=[['Smart matching','Discover roles that align with your skills, interests and eligibility.',Target],['One clear journey','Track every application milestone from applied to selected.',ChartNoAxesCombined],['Built for everyone','A shared workspace for students, recruiters and placement teams.',Users]]
  return <div className="landing">
    <header className="site-header"><div className="container nav-wrap"><Brand/><nav className={mobileOpen?'nav-links open':'nav-links'}><a href="#about" onClick={()=>setMobileOpen(false)}>About</a><a href="#features" onClick={()=>setMobileOpen(false)}>Features</a><a href="#how" onClick={()=>setMobileOpen(false)}>How it works</a><a href="#contact" onClick={()=>setMobileOpen(false)}>Contact</a></nav><div className="nav-actions"><Link className="login-link" to="/login">Log in</Link><Button to="/register">Get started <ArrowRight size={16}/></Button></div><button className="mobile-toggle" onClick={()=>setMobileOpen(!mobileOpen)} aria-label="Toggle menu">{mobileOpen?<X/>:<Menu/>}</button></div></header>
    <main>
      <section className="hero-section"><div className="hero-glow glow-one"/><div className="hero-glow glow-two"/><div className="container hero-grid"><motion.div className="hero-copy" initial={{opacity:0,y:24}} animate={{opacity:1,y:0}} transition={{duration:.7}}><div className="eyebrow"><span className="eyebrow-dot"/> THE NEXT STEP STARTS HERE</div><h1>Your career journey <span>starts here.</span></h1><p className="hero-lead">Discover opportunities, connect with recruiters, and take the next step toward your dream career.</p><div className="hero-buttons"><Button to="/register">Explore opportunities <ArrowRight size={17}/></Button><Button variant="outline" to="/login">I already have an account</Button></div><div className="hero-proof"><div className="avatar-stack"><i>R</i><i>A</i><i>S</i><i>+</i></div><div><strong>Made for ambitious futures</strong><small>One platform. More possibilities.</small></div></div></motion.div>
      <motion.div className="hero-visual" initial={{opacity:0,scale:.94}} animate={{opacity:1,scale:1}} transition={{duration:.8,delay:.15}}><div className="visual-orbit orbit-a"/><div className="visual-orbit orbit-b"/><div className="hero-window"><div className="window-top"><div className="window-dots"><i/><i/><i/></div><span>placemate.app / overview</span><MoreHorizontal size={18}/></div><div className="preview-content"><div className="preview-greeting"><div><small>MONDAY, NOVEMBER 12</small><h3>Your next chapter awaits.</h3></div><span className="preview-avatar">R</span></div><div className="preview-stats"><div><span>Matched roles</span><strong>24</strong><small className="positive">↑ 12% this week</small></div><div><span>Profile strength</span><strong>86%</strong><div className="mini-progress"><i/></div></div></div><div className="preview-label">TOP MATCHES FOR YOU <span>View all</span></div><div className="preview-job"><b className="mini-logo ms">M</b><div><strong>Software Engineer</strong><small>Microsoft · Bengaluru</small></div><em>96% match</em></div><div className="preview-job"><b className="mini-logo del">D</b><div><strong>Technology Analyst</strong><small>Deloitte · Hyderabad</small></div><em>91% match</em></div></div></div><div className="float-card float-match"><span className="float-icon"><Sparkles size={16}/></span><div><strong>Great match!</strong><small>Your skills align with 9 roles</small></div></div><div className="float-card float-alert"><span className="float-icon green"><Check size={16}/></span><div><strong>Application update</strong><small>You're shortlisted 🎉</small></div></div></motion.div></div><div className="container hero-bottom"><span>YOUR CAREER, CONNECTED</span><div><b>STUDENTS</b><b>RECRUITERS</b><b>PLACEMENT TEAMS</b></div></div></section>
      <section className="stats-strip"><div className="container stats-row"><div><strong>3-in-1</strong><span>Connected ecosystem</span></div><div><strong>24/7</strong><span>Opportunity access</span></div><div><strong>1 place</strong><span>For your entire journey</span></div><div><strong>100%</strong><span>Career-focused</span></div></div></section>
      <section className="section-pad" id="features"><div className="container"><div className="section-heading"><span className="eyebrow">A SMARTER WAY FORWARD</span><h2>Everything you need to <span>move forward.</span></h2><p>Less chasing updates. More time building the future you want.</p></div><div className="feature-grid">{features.map(([title,desc,Icon],i)=><motion.article className="feature-card" key={title} whileHover={{y:-6}} transition={{duration:.2}}><div className="feature-icon">{<Icon size={22}/>}</div><span className="feature-number">0{i+1}</span><h3>{title}</h3><p>{desc}</p><a href="#how">Explore feature <ArrowUpRight size={15}/></a></motion.article>)}</div></div></section>
      <section className="how-section section-pad" id="how"><div className="container how-grid"><div><span className="eyebrow">SIMPLE BY DESIGN</span><h2>From first click to <span>first offer.</span></h2><p className="muted-copy">A transparent, organized process that keeps every stakeholder in sync.</p><Button to="/register">Get started today <ArrowRight size={16}/></Button></div><div className="steps"><div className="step"><b>01</b><div><h3>Build your profile</h3><p>Showcase your skills, education and aspirations.</p></div></div><div className="step"><b>02</b><div><h3>Find your fit</h3><p>Explore relevant roles with clear eligibility insights.</p></div></div><div className="step"><b>03</b><div><h3>Track your progress</h3><p>Stay on top of applications, interviews and outcomes.</p></div></div></div></div></section>
      <section className="cta-section" id="contact"><div className="container cta-box"><div><span className="eyebrow light-eyebrow">YOUR NEXT CHAPTER</span><h2>Big opportunities.<br/>One place to find them.</h2><p>Make your next move with PlaceMate.</p></div><Button to="/register" variant="white">Create your account <ArrowRight size={17}/></Button><div className="cta-decoration"/></div></section>
    </main><footer className="site-footer"><div className="container footer-row"><Brand/><span>© 2026 PlaceMate. Built for what’s next.</span><div><a href="#features">Features</a><Link to="/login">Log in</Link><Link to="/register">Get started</Link></div></div></footer>
  </div>
}

const studentMenu=[['Overview',LayoutDashboard,'/dashboard'],['Notifications',Bell,'/notifications'],['My profile',UserRound,'/profile'],['Find jobs',BriefcaseBusiness,'/jobs'],['Recommended',Sparkles,'/recommended'],['My applications',FileText,'/applications'],['Interviews',CalendarDays,'/interviews']]
const recruiterMenu=[['Overview',LayoutDashboard,'/dashboard'],['Notifications',Bell,'/notifications'],['Company profile',Building2,'/profile'],['Post a job',Plus,'/post-job'],['Manage jobs',BriefcaseBusiness,'/jobs'],['Applicants',Users,'/applicants'],['Interviews',CalendarDays,'/interviews']]
const adminMenu=[['Overview',LayoutDashboard,'/dashboard'],['Notifications',Bell,'/notifications'],['Students',GraduationCap,'/students'],['Recruiters',Users,'/recruiters'],['Companies',Building2,'/companies'],['Job approvals',ShieldCheck,'/approvals'],['Applications',FileText,'/applications'],['Analytics',ChartNoAxesCombined,'/analytics'],['Reports',FileText,'/reports']]
function DashboardLayout({children,role,onLogout,user}) {
 const [open,setOpen]=useState(false), [notice,setNotice]=useState(false), [unread,setUnread]=useState(0)
 useEffect(()=>{notificationApi.unreadCount().then(r=>setUnread(r.data)).catch(()=>{})},[role])
 const menu=role==='Recruiter'?recruiterMenu:role==='Admin'?adminMenu:studentMenu
 return <div className="app-shell"><aside className={open?'sidebar sidebar-open':'sidebar'}><div className="side-brand"><Brand light/><button className="side-close" onClick={()=>setOpen(false)}><X size={18}/></button></div><div className="workspace-label">WORKSPACE</div><nav className="side-nav">{menu.map(([label,Icon,path])=><NavLink key={label} to={path} onClick={()=>setOpen(false)} className={({isActive})=>isActive?'side-link active':'side-link'}><Icon size={18}/><span>{label}</span>{label==='Find jobs'&&<i className="side-count">8</i>}</NavLink>)}</nav><div className="side-bottom"><div className="help-card"><div className="help-icon"><CircleHelp size={17}/></div><strong>Need a hand?</strong><p>Our team is here to help you.</p><button onClick={()=>alert('Support centre coming soon.')}>Visit help centre <ArrowUpRight size={13}/></button></div><button className="side-link" onClick={onLogout}><LogOut size={18}/><span>Sign out</span></button><div className="side-user"><div className="user-avatar">{(user?.fullName||"U").charAt(0).toUpperCase()}</div><div><strong>{user?.fullName||"Account"}</strong><small>{role} account</small></div><MoreHorizontal size={18}/></div></div></aside>{open&&<div className="sidebar-overlay" onClick={()=>setOpen(false)}/>}<div className="main-shell"><header className="app-topbar"><button className="mobile-toggle app-menu" onClick={()=>setOpen(true)}><Menu/></button><div className="breadcrumb">Workspace <span>/</span> <b>{role} overview</b></div><div className="top-actions"><span className="role-switch"><span>Workspace</span><b>{role}</b></span><button className="icon-button notification-button" onClick={()=>setNotice(!notice)} aria-label="Open notifications"><Bell size={18}/>{unread>0&&<i/>}</button>{notice&&<div className="notification-pop"><b>Notifications</b><p>{unread>0?unread+" unread notification"+(unread===1?"":"s"):"You are all caught up."}</p><Link to="/notifications" onClick={()=>setNotice(false)}>Open notification centre <ArrowRight size={13}/></Link></div>}<div className="top-user"><span className="user-avatar">{(user?.fullName||"U").charAt(0).toUpperCase()}</span><div><b>{user?.fullName||"Account"}</b><small>{role}</small></div><ChevronDown size={14}/></div></div></header><main className="dashboard-content"><AnimatePresence mode="wait"><motion.div key={useLocation().pathname+role} initial={{opacity:0,y:10}} animate={{opacity:1,y:0}} exit={{opacity:0,y:-6}} transition={{duration:.2}}>{children}</motion.div></AnimatePresence></main></div></div>
}

function PageHeading({eyebrow,title,description,action}) { return <div className="page-heading"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{action}</div> }
function StatCard({label,value,change,Icon,color}) { return <motion.div className="stat-card" whileHover={{y:-3}}><div className="stat-top"><span>{label}</span><i className={`stat-icon ${color}`}><Icon size={19}/></i></div><div className="stat-value">{value}</div><div className="stat-foot"><span className="stat-change"><ArrowUpRight size={13}/>{change}</span><span>vs last month</span></div></motion.div> }
function Dashboard({role}) {
 const isStudent=role==='Student', isRecruiter=role==='Recruiter'
 const [stats,setStats]=useState([])
 const [recent,setRecent]=useState([])
 const [loading,setLoading]=useState(true)
 const [error,setError]=useState('')
 useEffect(()=>{
  let active=true
  const load=async()=>{
   setLoading(true);setError('')
   try{
    if(isStudent){
     const [jobs,applications,matches,interviews]=await Promise.all([jobApi.browse(),jobApi.myApplications(),profileApi.matches(),interviewApi.student()])
     const apps=applications.data
     if(active){
      setStats([['Available jobs',jobs.data.length,BriefcaseBusiness,'blue'],['Eligible jobs',matches.data.filter(x=>x.eligible).length,Target,'purple'],['Applications sent',apps.length,Send,'orange'],['Shortlisted',apps.filter(x=>x.status==='SHORTLISTED').length,Award,'green']])
      setRecent(apps.slice(0,5).map(x=>({id:x.id,company:x.companyName,role:x.jobTitle,date:x.appliedAt,status:x.status})))
     }
    }else if(isRecruiter){
     const [jobs,interviews]=await Promise.all([jobApi.mine(),interviewApi.recruiter()])
     const groups=await Promise.all(jobs.data.map(async job=>({job,applicants:(await jobApi.applicants(job.id)).data})))
     const applicants=groups.flatMap(g=>g.applicants.map(a=>({...a,jobTitle:g.job.title,companyName:g.job.companyName}))).sort((x,y)=>new Date(y.appliedAt)-new Date(x.appliedAt))
     if(active){
      setStats([['Active jobs',jobs.data.filter(j=>j.status==='APPROVED').length,BriefcaseBusiness,'blue'],['Total applicants',applicants.length,Users,'purple'],['Shortlisted',applicants.filter(x=>x.status==='SHORTLISTED').length,Award,'green'],['Interviews',interviews.data.filter(x=>x.status==='SCHEDULED').length,CalendarDays,'orange']])
      setRecent(applicants.slice(0,5).map(x=>({id:x.id,company:x.studentName,role:x.jobTitle,date:x.appliedAt,status:x.status})))
     }
    }else{
     const [overview,applications]=await Promise.all([adminApi.overview(),adminApi.applications()])
     const d=overview.data
     if(active){
      setStats([['Total students',d.students,GraduationCap,'blue'],['Recruiters',d.recruiters,Users,'purple'],['Companies',d.companies,Building2,'green'],['Pending jobs',d.jobsPending,ShieldCheck,'orange']])
      setRecent(applications.data.slice(0,5).map(x=>({id:x.id,company:x.studentName,role:x.companyName+' · '+x.jobTitle,date:x.appliedAt,status:x.status})))
     }
    }
   }catch(e){if(active)setError(e.response?.data?.message||'Unable to load live dashboard data. Check your API connection and permissions.')}
   finally{if(active)setLoading(false)}
  }
  load()
  return()=>{active=false}
 },[role])
 const date=value=>value?new Date(value).toLocaleDateString(undefined,{day:'2-digit',month:'short',year:'numeric'}):'—'
 const tone=status=>({APPLIED:'blue',UNDER_REVIEW:'purple',SHORTLISTED:'green',REJECTED:'red',HIRED:'green'}[status]||'blue')
 return <>
  <PageHeading eyebrow="YOUR OVERVIEW" title={isStudent?'Your placement journey':isRecruiter?'Recruiter workspace':'Placement overview'} description={isStudent?'A live view of your opportunities and application progress.':isRecruiter?'A live view of your job postings, applicants, and interviews.':'Live counts across students, recruiters, jobs, and applications.'} action={<Button to={isStudent?'/jobs':isRecruiter?'/post-job':'/analytics'}><Plus size={16}/>{isStudent?'Explore jobs':isRecruiter?'Post a job':'View analytics'}</Button>}/>
  {error&&<div className="auth-error">{error}</div>}
  {loading?<div className="panel empty-state">Loading live workspace data...</div>:<>
   <div className="stat-grid">{stats.map(([label,value,Icon,color])=><div className="stat-card" key={label}><div className="stat-top"><span>{label}</span><i className={'stat-icon '+color}><Icon size={19}/></i></div><div className="stat-value">{value}</div><div className="stat-foot"><span className="stat-change"><CheckCircle2 size={13}/>Live</span><span>from PlaceMate API</span></div></div>)}</div>
   <div className="panel table-panel dashboard-live-table"><div className="panel-heading"><div><h3>{isStudent?'Recent applications':isRecruiter?'Recent applicants':'Recent placement activity'}</h3><p>Latest records from your PlaceMate workspace</p></div><Link to={isStudent?'/applications':isRecruiter?'/applicants':'/reports'} className="text-link">View all <ArrowRight size={15}/></Link></div>
    {recent.length===0?<div className="empty-state"><FileText size={26}/><h3>No recent activity</h3><p>{isStudent?'Apply to an approved job to start tracking your progress.':isRecruiter?'New applications to your approved jobs will appear here.':'Placement activity will appear as students apply and recruiters update applications.'}</p></div>:<div className="table-scroll"><table><thead><tr><th>{isStudent?'COMPANY':isRecruiter?'CANDIDATE':'STUDENT'}</th><th>{isStudent?'POSITION':isRecruiter?'JOB':'COMPANY / POSITION'}</th><th>DATE</th><th>STATUS</th></tr></thead><tbody>{recent.map(row=><tr key={row.id}><td><div className="company-cell"><b className="company-logo">{(row.company||'P').charAt(0).toUpperCase()}</b><strong>{row.company}</strong></div></td><td>{row.role}</td><td>{date(row.date)}</td><td><span className={'status-badge '+tone(row.status)}>{String(row.status||'').replaceAll('_',' ')}</span></td></tr>)}</tbody></table></div>}
   </div>
  </>}
 </>
}
function JobCard({job,onApply,applied=false,applying=false,allowApply=true}) {
 const [saved,setSaved]=useState(false)
 return <motion.article className="job-card" whileHover={{y:-4}}>
  <div className="job-card-top"><div className="company-identity"><span className="company-logo large" style={{background:job.color}}>{job.logo}</span><div><strong>{job.company}</strong><small>{String(job.type||'Role').replaceAll('_',' ')} · Approved posting</small></div></div><button className={saved?'save-btn saved':'save-btn'} onClick={()=>setSaved(!saved)} aria-label="Save job"><Bookmark size={18} fill={saved?'currentColor':'none'}/></button></div>
  <h3>{job.role}</h3>
  <div className="job-meta"><span><MapPin size={14}/>{job.place}</span><span><IndianRupee size={14}/>{job.salary}</span></div>
  <div className="job-tags">{job.skills.map(s=><span key={s}>{s}</span>)}</div>
  {job.recommended&&<div className="match-explanation"><span className={job.eligible?'eligibility-pill eligible':'eligibility-pill not-eligible'}>{job.eligible?'Eligible':'Not eligible yet'}</span><p>{(job.reasons||[]).join(' ')}</p></div>}
  <div className="job-card-bottom"><div><span className="match-score"><Sparkles size={13}/>{job.recommended?job.match+'% match':job.match>0?job.match+'% match':'Approved listing'}</span><small>{job.days} · CGPA {job.cgpa}</small></div>{allowApply?<button className="btn btn-primary" disabled={applied||applying} onClick={()=>onApply(job)}>{applying?"Applying...":applied?"Applied":"Apply now"} <ArrowRight size={14}/></button>:<Button onClick={()=>onApply(job)} variant="outline">View details <ArrowRight size={14}/></Button>}</div>
 </motion.article>
}
function JobsPage({title='Find your next opportunity',allowApply=true,recommended=false}) {
 const [query,setQuery]=useState('')
 const [filter,setFilter]=useState('All')
 const [jobs,setJobs]=useState([])
 const [loading,setLoading]=useState(true)
 const [error,setError]=useState('')
 const [toast,setToast]=useState('')
 const [appliedIds,setAppliedIds]=useState(new Set())
 const [applyingId,setApplyingId]=useState(null)
 useEffect(()=>{
  let active=true
  setLoading(true);setError('')
  const request=recommended?profileApi.matches():jobApi.browse({q:query.trim()||undefined,type:filter==='All'?undefined:filter})
  request.then(response=>{
   if(!active)return
   const rows=recommended?response.data.map(item=>({...item.job,eligible:item.eligible,matchPercentage:item.matchPercentage,reasons:item.reasons})):response.data
   const mapped=rows.map(job=>({
    id:job.id,company:job.companyName,role:job.title,place:job.location,
    salary:job.salaryMin!=null&&job.salaryMax!=null?'₹'+Number(job.salaryMin).toLocaleString('en-IN')+'–₹'+Number(job.salaryMax).toLocaleString('en-IN'):job.salaryMax!=null?'Up to ₹'+Number(job.salaryMax).toLocaleString('en-IN'):job.salaryMin!=null?'From ₹'+Number(job.salaryMin).toLocaleString('en-IN'):'Not disclosed',
    cgpa:job.minCgpa!=null?job.minCgpa+'+':'Not specified',
    skills:job.requiredSkills||[],days:job.deadline?'Apply by '+job.deadline:'Open until filled',
    color:'#2563eb',logo:(job.companyName||'P').charAt(0).toUpperCase(),
    match:recommended?job.matchPercentage:0,type:job.employmentType,description:job.description,
    recommended,eligible:job.eligible,reasons:job.reasons
   }))
   setJobs(recommended?mapped.filter(job=>(filter==='All'||String(job.type).toUpperCase().replaceAll('-','_')===filter.toUpperCase().replaceAll('-','_'))&&(!query||[job.role,job.company,...job.skills].join(' ').toLowerCase().includes(query.toLowerCase()))):mapped)
  }).catch(err=>{if(active)setError(err.response?.data?.message||'Unable to load jobs. Check that the backend is running.')})
   .finally(()=>{if(active)setLoading(false)})
  return ()=>{active=false}
 },[query,filter,recommended])
 useEffect(()=>{if(!allowApply)return;let active=true;jobApi.myApplications().then(r=>{if(active)setAppliedIds(new Set(r.data.map(a=>a.jobId)))}).catch(()=>{});return()=>{active=false}},[allowApply])
 const viewDetails=job=>{setToast(job.role+' at '+job.company+': '+job.description);setTimeout(()=>setToast(''),6000)}
 const applyToJob=async job=>{setApplyingId(job.id);setToast('');try{await jobApi.apply(job.id);setAppliedIds(current=>new Set([...current,job.id]));setToast('Application submitted for '+job.role+' at '+job.company+'.')}catch(err){setToast(err.response?.data?.message||'Could not submit application. Please try again.')}finally{setApplyingId(null)}}
 return <><PageHeading eyebrow={recommended?'PERSONALIZED FOR YOU':'OPPORTUNITIES'} title={title} description={recommended?'Compare your CGPA and skills with each recruiter’s requirements.':'Browse approved openings published by recruiters.'}/><div className="job-toolbar"><div className="search-field"><Search size={18}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search roles, companies or skills..."/></div><div className="filter-buttons"><SlidersHorizontal size={17}/>{['All','Full-time','Internship'].map(f=><button key={f} className={filter===f?'filter-active':''} onClick={()=>setFilter(f)}>{f}</button>)}</div></div><div className="results-line"><span><b>{jobs.length} opportunities</b> currently available</span></div>
 {loading&&<div className="empty-state"><p>Loading {recommended?'your job matches':'approved opportunities'}...</p></div>}
 {error&&!loading&&<div className="empty-state"><CircleX size={28}/><h3>Could not load jobs</h3><p>{error}</p></div>}
 {!loading&&!error&&<><div className="jobs-grid">{jobs.map(job=><JobCard key={job.id} job={job} onApply={allowApply?applyToJob:viewDetails} applied={appliedIds.has(job.id)} applying={applyingId===job.id} allowApply={allowApply}/>)}</div>{jobs.length===0&&<div className="empty-state"><Search size={28}/><h3>No matching roles found</h3><p>{recommended?'Complete your profile or adjust your search to see more matches.':'Try another keyword or check back later for new openings.'}</p></div>}</>}
 {toast&&<div className="toast" role="status">{toast}</div>}</>
}
function Applications(){return <><PageHeading eyebrow="YOUR JOURNEY" title="My applications" description="Every opportunity, every update, all in one place."/><div className="panel table-panel"><div className="panel-heading"><div><h3>Application tracker</h3><p>Follow your progress from application to outcome</p></div><span className="status-badge blue">12 total applications</span></div><div className="table-scroll"><table><thead><tr><th>COMPANY</th><th>POSITION</th><th>APPLIED</th><th>STATUS</th><th>NEXT STEP</th></tr></thead><tbody>{demoApplications.map((a,i)=><tr key={a.company}><td><div className="company-cell"><b className={`company-logo logo-${i}`}>{a.company[0]}</b><strong>{a.company}</strong></div></td><td>{a.role}</td><td>{a.date}</td><td><span className={`status-badge ${a.tone}`}>{a.status}</span></td><td><button className="text-link">View details <ArrowRight size={14}/></button></td></tr>)}</tbody></table></div></div><div className="panel timeline-panel"><h3>Application journey</h3><div className="timeline"><span className="timeline-done">Applied</span><i/><span className="timeline-done">Under review</span><i/><span className="timeline-current">Shortlisted</span><i/><span>Interview</span><i/><span>Outcome</span></div></div></>}
function Profile(){
 const [profile,setProfile]=useState(null)
 const [form,setForm]=useState({cgpa:'',graduationYear:'',degree:'',branch:'',skills:'',resumeUrl:''})
 const [loading,setLoading]=useState(true)
 const [saving,setSaving]=useState(false)
 const [error,setError]=useState('')
 const [success,setSuccess]=useState('')
 const [resumeFile,setResumeFile]=useState(null)
 const [resumeBusy,setResumeBusy]=useState(false)
 const [resumeMessage,setResumeMessage]=useState('')
 useEffect(()=>{
  let active=true
  profileApi.get().then(response=>{
   if(!active)return
   const p=response.data
   setProfile(p)
   setForm({cgpa:p.cgpa??'',graduationYear:p.graduationYear??'',degree:p.degree??'',branch:p.branch??'',skills:(p.skills||[]).join(', '),resumeUrl:p.resumeUrl??''})
  }).catch(err=>{if(active)setError(err.response?.data?.message||'Unable to load your student profile.')})
    .finally(()=>{if(active)setLoading(false)})
  return()=>{active=false}
 },[])
 const change=e=>setForm(current=>({...current,[e.target.name]:e.target.value}))
 const submit=async e=>{
  e.preventDefault();setSaving(true);setError('');setSuccess('')
  const payload={
   cgpa:form.cgpa===''?null:Number(form.cgpa),
   graduationYear:form.graduationYear===''?null:Number(form.graduationYear),
   degree:form.degree.trim()||null,branch:form.branch.trim()||null,
   skills:form.skills.split(',').map(s=>s.trim()).filter(Boolean),
   resumeUrl:form.resumeUrl.trim()||null
  }
  try{
   const response=await profileApi.update(payload)
   setProfile(response.data)
   setForm({cgpa:response.data.cgpa??'',graduationYear:response.data.graduationYear??'',degree:response.data.degree??'',branch:response.data.branch??'',skills:(response.data.skills||[]).join(', '),resumeUrl:response.data.resumeUrl??''})
   setSuccess('Your profile has been saved successfully.')
  }catch(err){setError(err.response?.data?.message||'Could not save your profile. Please check the entered details.')}
  finally{setSaving(false)}
 }
 if(loading)return <div className="panel empty-state">Loading your profile...</div>
 return <>
  <PageHeading eyebrow="YOUR IDENTITY" title="My profile" description="Keep your academic details and skills up to date to get more accurate job matches."/>
  {error&&<div className="auth-error">{error}</div>}
  <div className="profile-page-grid">
   <section className="panel profile-hero-card"><div className="profile-cover"/><div className="profile-person"><div className="profile-avatar">{(profile?.fullName||'S').charAt(0).toUpperCase()}</div><div><h2>{profile?.fullName||'Student'}</h2><p>{profile?.degree||'Degree not added'} · {profile?.branch||profile?.department||'Department not added'}</p><span><Mail size={13}/>{profile?.email}</span></div><span className="status-badge blue">Student account</span></div>
    <div className="profile-details"><div><small>ENROLLMENT NUMBER</small><b>{profile?.enrollmentNumber||'Not provided'}</b></div><div><small>CGPA</small><b>{profile?.cgpa??'Not added'}{profile?.cgpa!=null?' / 10':''}</b></div><div><small>GRADUATION YEAR</small><b>{profile?.graduationYear||'Not added'}</b></div></div>
   </section>
   <form className="panel student-profile-form" onSubmit={submit}>
    <div className="panel-heading"><div><h3>Academic profile</h3><p>These details power your eligibility and job match results.</p></div><Award size={20}/></div>
    <div className="form-grid">
     <label className="form-label">CGPA (out of 10)<input type="number" name="cgpa" min="0" max="10" step="0.01" value={form.cgpa} onChange={change} placeholder="e.g. 8.25"/></label>
     <label className="form-label">Graduation year<input type="number" name="graduationYear" min="2000" max="2100" value={form.graduationYear} onChange={change} placeholder="e.g. 2028"/></label>
     <label className="form-label">Degree<input name="degree" maxLength="120" value={form.degree} onChange={change} placeholder="e.g. B.Tech"/></label>
     <label className="form-label">Branch / specialization<input name="branch" maxLength="120" value={form.branch} onChange={change} placeholder="e.g. Computer Science"/></label>
     <label className="form-label form-span">Skills <span className="optional-label">Comma separated</span><input name="skills" value={form.skills} onChange={change} placeholder="Java, React, SQL, DSA"/></label>
     <label className="form-label form-span">Resume link or uploaded file name<input type="text" name="resumeUrl" maxLength="500" value={form.resumeUrl} onChange={change} placeholder="https://drive.google.com/..."/></label>
     <div className="form-label form-span">Upload resume (PDF, DOC, DOCX · max 5 MB)<input type="file" accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" onChange={e=>{setResumeFile(e.target.files?.[0]||null);setResumeMessage('')}}/><div className="resume-actions"><button type="button" className="btn btn-outline" disabled={!resumeFile||resumeBusy} onClick={async()=>{setResumeBusy(true);setResumeMessage('');try{await resumeApi.upload(resumeFile);const refreshed=await profileApi.get();setProfile(refreshed.data);setForm(current=>({...current,resumeUrl:refreshed.data.resumeUrl||''}));setResumeFile(null);setResumeMessage('Resume uploaded successfully.')}catch(err){setResumeMessage(err.response?.data?.message||'Resume upload failed.')}finally{setResumeBusy(false)}}}>{resumeBusy?'Uploading...':'Upload resume'} <FileText size={14}/></button><button type="button" className="btn btn-outline" disabled={!profile?.resumeUrl} onClick={async()=>{try{const r=await resumeApi.studentDownload();const url=URL.createObjectURL(r.data);const a=document.createElement('a');a.href=url;a.download='PlaceMate-resume';a.click();URL.revokeObjectURL(url)}catch(err){setResumeMessage(err.response?.data?.message||'Could not download resume.')}}}><ExternalLink size={14}/> Download uploaded resume</button></div>{resumeMessage&&<small className="resume-message">{resumeMessage}</small>}</div>
    </div>
    {success&&<div className="success-message" role="status">{success}</div>}
    <div className="post-job-actions"><button className="btn btn-primary" disabled={saving}>{saving?'Saving profile...':'Save profile'} <Check size={15}/></button></div>
   </form>
  </div>
 </>
}
function GenericPage({title,description,icon:Icon=FileText}){return <><PageHeading eyebrow="WORKSPACE" title={title} description={description}/><div className="panel empty-state"><div className="empty-icon"><Icon size={28}/></div><h3>{title} workspace</h3><p>This section is ready for integration with the PlaceMate Spring Boot APIs. Connect your backend to load live records here.</p><Button onClick={()=>alert('Backend integration is not connected yet.')}>Configure workspace <ArrowRight size={15}/></Button></div></>}
function Login({register=false,onAuthenticated}) {
 const [role,setRole]=useState('Student')
 const [show,setShow]=useState(false)
 const [fullName,setFullName]=useState('')
 const [email,setEmail]=useState('')
 const [password,setPassword]=useState('')
 const [confirmPassword,setConfirmPassword]=useState('')
 const [enrollmentNumber,setEnrollmentNumber]=useState('')
 const [department,setDepartment]=useState('')
 const [companyName,setCompanyName]=useState('')
 const [companyWebsite,setCompanyWebsite]=useState('')
 const [loading,setLoading]=useState(false)
 const [error,setError]=useState('')
 const nav=useNavigate()

 const submit=async(e)=>{
  e.preventDefault()
  setError('')
  if(register&&password!==confirmPassword){setError('Passwords do not match.');return}
  if(register&&role==='Student'&&(!enrollmentNumber.trim()||!department.trim())){setError('Please enter your enrollment number and department.');return}
  if(register&&role==='Recruiter'&&!companyName.trim()){setError('Please enter your company name.');return}
  setLoading(true)
  try {
   const response=register
    ? await authApi.register({
       fullName:fullName.trim(),email:email.trim(),password,role:role.toUpperCase(),
       ...(role==='Student'?{enrollmentNumber:enrollmentNumber.trim(),department:department.trim()}:{companyName:companyName.trim(),companyWebsite:companyWebsite.trim()||null})
      })
    : await authApi.login({email:email.trim(),password})
   onAuthenticated(response.data)
   nav('/dashboard',{replace:true})
  } catch(err) {
   setError(err.response?.data?.message || (err.request?'Cannot reach PlaceMate server. Start the Spring Boot backend and try again.':'Something went wrong. Please try again.'))
  } finally {setLoading(false)}
 }

 return <div className="auth-page"><div className="auth-brand"><Brand/><Link to="/">Back to home <ArrowRight size={15}/></Link></div><div className="auth-layout"><div className="auth-art"><div className="auth-art-content"><span className="eyebrow light-eyebrow">YOUR FUTURE, IN FOCUS</span><h1>Make your next move a <span>great one.</span></h1><p>Opportunities don't wait. Bring your career journey into focus with PlaceMate.</p><div className="auth-art-note"><Sparkles size={18}/><span>One connected space for every career milestone.</span></div></div><div className="auth-art-orb"/></div><div className="auth-form-side"><form className="auth-form" onSubmit={submit}><div className="auth-mobile-brand"><Brand/></div><span className="eyebrow">{register?'CREATE YOUR ACCOUNT':'WELCOME BACK'}</span><h2>{register?'Start your journey':'Good to see you again.'}</h2><p>{register?'Choose your role and create your PlaceMate account.':'Enter your details to continue to your workspace.'}</p>
 {register&&<><div className="role-picker"><button type="button" className={role==='Student'?'chosen':''} onClick={()=>setRole('Student')}><GraduationCap size={17}/> Student</button><button type="button" className={role==='Recruiter'?'chosen':''} onClick={()=>setRole('Recruiter')}><Building2 size={17}/> Recruiter</button></div><label className="form-label">{role==='Student'?'Full name':'Recruiter name'}<input value={fullName} onChange={e=>setFullName(e.target.value)} placeholder="Your full name" maxLength={120} required/></label></>}
 <label className="form-label">Email address<div className="input-icon"><Mail size={17}/><input type="email" value={email} onChange={e=>setEmail(e.target.value)} placeholder="you@example.com" autoComplete="email" required/></div></label>
 <label className="form-label">Password<div className="input-icon"><LockKeyhole size={17}/><input type={show?'text':'password'} value={password} onChange={e=>setPassword(e.target.value)} placeholder="At least 8 characters" minLength={8} autoComplete={register?'new-password':'current-password'} required/><button type="button" onClick={()=>setShow(!show)} aria-label={show?'Hide password':'Show password'}>{show?<EyeOff size={17}/>:<Eye size={17}/>}</button></div></label>
 {register&&<><label className="form-label">Confirm password<input type="password" value={confirmPassword} onChange={e=>setConfirmPassword(e.target.value)} placeholder="Re-enter your password" minLength={8} autoComplete="new-password" required/></label>
 {role==='Student'?<><label className="form-label">Enrollment number<input value={enrollmentNumber} onChange={e=>setEnrollmentNumber(e.target.value)} placeholder="Your college enrollment number" required/></label><label className="form-label">Department<input value={department} onChange={e=>setDepartment(e.target.value)} placeholder="e.g. Computer Science" required/></label></>:<><label className="form-label">Company name<input value={companyName} onChange={e=>setCompanyName(e.target.value)} placeholder="Your company" required/></label><label className="form-label">Company website <span className="optional-label">Optional</span><input type="url" value={companyWebsite} onChange={e=>setCompanyWebsite(e.target.value)} placeholder="https://example.com"/></label></>}</>}
 {!register&&<div className="form-options"><label><input type="checkbox"/> Remember me</label><button type="button" onClick={()=>setError('Password reset will be available after email delivery is configured.')}>Forgot password?</button></div>}
 {error&&<div className="auth-error" role="alert">{error}</div>}
 <button className="btn btn-primary auth-submit" type="submit" disabled={loading}>{loading?'Please wait...':register?'Create account':'Log in'} {!loading&&<ArrowRight size={16}/>}</button>
 <div className="auth-switch">{register?'Already have an account?':'New to PlaceMate?'} <Link to={register?'/login':'/register'}>{register?'Log in':'Create an account'}</Link></div><div className="auth-disclaimer"><ShieldCheck size={15}/> Secure access · Role-based workspaces</div>{register&&<small className="auth-note">Admin accounts are provisioned by the placement team and cannot be created here.</small>}</form></div></div></div>
}

export default function App(){
 const location=useLocation()
 const [auth,setAuth]=useState(()=>({
  token:localStorage.getItem('placemate_token'),
  user:JSON.parse(localStorage.getItem('placemate_user')||'null')
 }))
 const [checking,setChecking]=useState(()=>Boolean(localStorage.getItem('placemate_token')))
 const role=auth.user?.role==='RECRUITER'?'Recruiter':auth.user?.role==='ADMIN'?'Admin':'Student'

 useEffect(()=>{
  const handleUnauthorized=()=>{
   setAuth({token:null,user:null})
   window.location.assign('/login')
  }
  window.addEventListener('placemate:unauthorized',handleUnauthorized)
  return()=>window.removeEventListener('placemate:unauthorized',handleUnauthorized)
 },[])

 useEffect(()=>{
  if(!auth.token){setChecking(false);return}
  let active=true
  authApi.me().then(response=>{
   if(!active)return
   localStorage.setItem('placemate_user',JSON.stringify(response.data))
   setAuth(current=>({...current,user:response.data}))
  }).catch(()=>{
   if(!active)return
   localStorage.removeItem('placemate_token')
   localStorage.removeItem('placemate_user')
   setAuth({token:null,user:null})
  }).finally(()=>{if(active)setChecking(false)})
  return ()=>{active=false}
 },[])

 const handleAuthenticated=(data)=>{
  localStorage.setItem('placemate_token',data.token)
  localStorage.setItem('placemate_user',JSON.stringify(data.user))
  setAuth({token:data.token,user:data.user})
 }
 const logout=()=>{
  localStorage.removeItem('placemate_token')
  localStorage.removeItem('placemate_user')
  setAuth({token:null,user:null})
  window.location.assign('/login')
 }

 if(checking)return <div className="auth-loading">Checking your PlaceMate session...</div>
 return <Routes>
  <Route path="/" element={<Landing/>}/>
  <Route path="/login" element={auth.token?<Navigate to="/dashboard" replace/>:<Login onAuthenticated={handleAuthenticated}/>}/>
  <Route path="/register" element={auth.token?<Navigate to="/dashboard" replace/>:<Login register onAuthenticated={handleAuthenticated}/>}/>
  <Route path="*" element={auth.token?<DashboardLayout role={role} onLogout={logout} user={auth.user}><Routes>
   <Route path="/dashboard" element={<Dashboard role={role}/>}/>
   <Route path="/jobs" element={role === "Recruiter" ? <RecruiterJobsPage/> : <JobsPage allowApply={role === "Student"}/>}/>
   <Route path="/recommended" element={<JobsPage title="Recommended for you" allowApply={role === "Student"} recommended={role === "Student"}/>}/>
   <Route path="/applications" element={role === "Student" ? <StudentApplicationsPage/> : role === "Admin" ? <AdminReportsPage/> : <GenericPage title="Applications" description="Application tracking is available in the student workspace." icon={FileText}/>}/>
   <Route path="/profile" element={role === "Student" ? <Profile/> : <GenericPage title="Company profile" description="Manage your organization details and recruiter information." icon={Building2}/>}/>
   <Route path="/post-job" element={<PostJobPage/>}/>
   <Route path="/applicants" element={role === "Recruiter" ? <RecruiterApplicantsPage/> : <GenericPage title="Applicants" description="Applicant management is available in the recruiter workspace." icon={Users}/>}/>
   <Route path="/interviews" element={<InterviewPage role={role}/>}/>
   <Route path="/notifications" element={<NotificationsPage/>}/>
   <Route path="/students" element={role === "Admin" ? <AdminStudentsPage/> : <GenericPage title="Students" description="Manage student profiles and placement readiness." icon={GraduationCap}/>}/>
   <Route path="/recruiters" element={role === "Admin" ? <AdminRecruitersPage/> : <GenericPage title="Recruiters" description="Manage recruiter access and activity." icon={Users}/>}/>
   <Route path="/companies" element={role === "Admin" ? <AdminCompaniesPage/> : <GenericPage title="Companies" description="Manage organizations participating in campus placements." icon={Building2}/>}/>
   <Route path="/approvals" element={<JobApprovalsPage/>}/>
   <Route path="/analytics" element={role === "Admin" ? <AdminAnalyticsPage/> : <GenericPage title="Placement analytics" description="Explore placement performance across departments and drives." icon={ChartNoAxesCombined}/>}/>
   <Route path="/reports" element={role === "Admin" ? <AdminReportsPage/> : <GenericPage title="Reports" description="Create and review placement reports." icon={FileText}/>}/>
   <Route path="*" element={<Navigate to="/dashboard" replace/>}/>
  </Routes></DashboardLayout>:<Navigate to="/login" state={{from:location.pathname}} replace/>}/>
 </Routes>
}
