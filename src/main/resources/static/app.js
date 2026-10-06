let token = localStorage.getItem('voyage_token') || '';
let currentUser = null;
const $ = id => document.getElementById(id);

function authHeaders(){ return token ? {'Authorization':'Bearer '+token} : {}; }
function setMessage(text, ok=false){ const el=$('authMessage'); el.textContent=text; el.style.color=ok?'#237a4a':'#b42318'; }

async function api(url, options={}){
  const headers = {...authHeaders(), ...(options.headers||{})};
  if(options.body && !headers['Content-Type']) headers['Content-Type']='application/json';
  const res=await fetch(url,{...options,headers});
  const data=await res.json().catch(()=>({}));
  if(!res.ok) throw new Error(data.message || 'Something went wrong.');
  return data;
}

async function loadProfile(){
  try{ currentUser=await api('/api/auth/me'); renderUser(); }
  catch(e){ logoutLocal(); }
}
function renderUser(){
  const initial=(currentUser.name||'T')[0].toUpperCase();
  $('avatar').textContent=initial; $('profileAvatar').textContent=initial;
  $('navUser').textContent=currentUser.name.split(' ')[0]; $('profileName').textContent=currentUser.name;
  $('profileEmail').textContent=currentUser.email; $('profileTrips').textContent=currentUser.tripsPlanned;
}
function showApp(){ $('authView').classList.add('hidden'); $('appView').classList.remove('hidden'); loadProfile(); loadPlaces(); showPage('home'); }
function showAuth(){ $('authView').classList.remove('hidden'); $('appView').classList.add('hidden'); }
function logoutLocal(){ token=''; localStorage.removeItem('voyage_token'); currentUser=null; showAuth(); }

async function login(){
  setMessage(''); const email=$('loginEmail').value.trim(), password=$('loginPassword').value;
  if(!email||!password){setMessage('Enter your email and password.');return;}
  $('loginBtn').disabled=true; $('loginBtn').textContent='Signing in...';
  try{ const data=await api('/api/auth/login',{method:'POST',body:JSON.stringify({email,password})}); token=data.token; localStorage.setItem('voyage_token',token); showApp(); }
  catch(e){setMessage(e.message);} finally{$('loginBtn').disabled=false;$('loginBtn').innerHTML='Sign In <span>→</span>';}
}
async function register(){
  setMessage(''); const name=$('regName').value.trim(), email=$('regEmail').value.trim(), password=$('regPassword').value;
  if(!name||!email||!password){setMessage('Fill in all fields.');return;}
  try{ await api('/api/auth/register',{method:'POST',body:JSON.stringify({name,email,password})}); setMessage('Account created. You can now sign in.',true); $('regPassword').value=''; document.querySelector('[data-auth="login"]').click(); $('loginEmail').value=email; }
  catch(e){setMessage(e.message);}
}

document.querySelectorAll('.auth-tab').forEach(tab=>tab.addEventListener('click',()=>{
  document.querySelectorAll('.auth-tab').forEach(t=>t.classList.remove('active')); tab.classList.add('active');
  $('loginForm').classList.toggle('hidden',tab.dataset.auth!=='login'); $('registerForm').classList.toggle('hidden',tab.dataset.auth!=='register'); setMessage('');
}));
$('loginBtn').addEventListener('click',login); $('registerBtn').addEventListener('click',register); $('logoutBtn').addEventListener('click',async()=>{try{await api('/api/auth/logout',{method:'POST'});}catch(e){} logoutLocal();});
$('loginPassword').addEventListener('keydown',e=>{if(e.key==='Enter')login()}); $('regPassword').addEventListener('keydown',e=>{if(e.key==='Enter')register()});

document.querySelectorAll('[data-page]').forEach(el=>el.addEventListener('click',()=>showPage(el.dataset.page)));
function showPage(id){
  document.querySelectorAll('.page').forEach(p=>p.classList.remove('active-page'));
  const page=$(id); if(!page)return; page.classList.add('active-page');
  document.querySelectorAll('.nav-tab').forEach(t=>t.classList.toggle('active',t.dataset.page===id));
  window.scrollTo({top:0,behavior:'smooth'});
  if(id==='trips')loadHistory(); if(id==='destinations')loadDestinations(); if(id==='profile')loadProfile();
}

async function loadPlaces(){
  try{ const places=await fetch('/api/places').then(r=>r.json());
    $('start').innerHTML=''; $('destination').innerHTML='';
    places.forEach(p=>{ $('start').add(new Option(p.placeName,p.placeName)); $('destination').add(new Option(p.placeName,p.placeName)); });
    $('start').value='Chennai'; $('destination').value='Madurai';
  }catch(e){$('error').textContent='Could not load destinations.';}
}
async function planTrip(){
  $('error').textContent=''; const payload={start:$('start').value,destination:$('destination').value,days:Number($('days').value),budget:Number($('budget').value),preference:$('preference').value};
  if(payload.start===payload.destination){$('error').textContent='Starting location and destination must be different.';return;}
  $('planBtn').disabled=true; $('planBtn').textContent='Finding optimal route...';
  try{const data=await api('/api/trips/plan',{method:'POST',body:JSON.stringify(payload)}); renderResult(data); currentUser.tripsPlanned=(currentUser.tripsPlanned||0)+1; renderUser();}
  catch(e){$('error').textContent=e.message;}
  finally{$('planBtn').disabled=false;$('planBtn').innerHTML='Create Travel Plan <span>✦</span>';}
}
function renderResult(data){
  $('result').classList.remove('hidden'); $('distance').textContent=data.totalDistanceKm+' km'; $('time').textContent=data.totalTravelTimeHours+' hrs'; $('cost').textContent='₹'+data.totalTravelCost;
  $('resultTitle').textContent=(data.preference==='time'?'Fastest route found':data.preference==='cost'?'Cheapest route found':'Shortest route found');
  $('budgetStatus').textContent=data.withinBudget?'Within Budget':'Over Budget'; $('budgetStatus').style.background=data.withinBudget?'#edf8f0':'#fff0f0'; $('budgetStatus').style.color=data.withinBudget?'#237a4a':'#b42318';
  $('route').innerHTML=data.route.map((p,i)=>`<span class="route-node">${p}</span>${i<data.route.length-1?'<span class="arrow">→</span>':''}`).join('');
  $('itinerary').innerHTML=data.itinerary.map(day=>`<div class="day"><h3>Day ${day.day}</h3><ul>${day.places.map(p=>`<li>${p}</li>`).join('')}</ul></div>`).join('');
  $('result').scrollIntoView({behavior:'smooth',block:'start'});
}
$('planBtn').addEventListener('click',planTrip);

async function loadHistory(){
  const box=$('tripHistory'); box.innerHTML='<div class="history-card">Loading your trips...</div>';
  try{const trips=await api('/api/auth/history');
    if(!trips.length){box.innerHTML='<div class="history-card"><h3>No trips yet</h3><p>Go to Plan Trip and generate your first optimized journey.</p></div>';return;}
    box.innerHTML=trips.map(t=>`<article class="history-card"><span class="eyebrow">${t.preference==='time'?'FASTEST':t.preference==='cost'?'CHEAPEST':'SHORTEST'}</span><h3>${t.start} → ${t.destination}</h3><p class="route-mini">${t.route.join(' → ')}</p><p>${t.totalDistanceKm} km • ${t.totalTravelTimeHours} hrs • ₹${t.totalTravelCost}</p></article>`).join('');
  }catch(e){box.innerHTML='<div class="history-card"><p>'+e.message+'</p></div>';}
}
async function loadDestinations(){
  try{const places=await fetch('/api/places').then(r=>r.json()); $('destinationGrid').innerHTML=places.map(p=>`<article class="destination-card"><span class="tag">${p.category}</span><h3>${p.placeName}</h3><p>${p.city}<br>Visit time: ${p.averageVisitHours} hrs<br>Entry fee: ₹${p.entryFee}</p></article>`).join('');}catch(e){}
}

if(token) showApp(); else showAuth();
