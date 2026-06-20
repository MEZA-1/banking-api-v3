/* ============================================================
   BANKING API — JAVASCRIPT CENTRAL
   api.js : client HTTP, auth, utilitaires partagés
   ============================================================ */

'use strict';

// ── Configuration ─────────────────────────────────────────────
const CONFIG = {
  BASE_URL: 'https://banking-api-v3.onrender.com',
  TOKEN_KEY: 'banking_token',
  USER_KEY:  'banking_user',
};

// ── Storage helpers ───────────────────────────────────────────
const Auth = {
  getToken()  { return localStorage.getItem(CONFIG.TOKEN_KEY); },
  getUser()   {
    try { return JSON.parse(localStorage.getItem(CONFIG.USER_KEY)); }
    catch { return null; }
  },
  setSession(token, user) {
    localStorage.setItem(CONFIG.TOKEN_KEY, token);
    localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(user));
  },
  clear() {
    localStorage.removeItem(CONFIG.TOKEN_KEY);
    localStorage.removeItem(CONFIG.USER_KEY);
  },
  isAuthenticated() { return !!this.getToken(); },
  getRole() { const u = this.getUser(); return u ? u.role : null; },
  requireAuth() {
    if (!this.isAuthenticated()) { window.location.href = '/index.html'; return false; }
    return true;
  },
  requireRole(role) {
    if (!this.requireAuth()) return false;
    if (this.getRole() !== role) {
      this.redirectToDashboard();
      return false;
    }
    return true;
  },
  redirectToDashboard() {
    const role = this.getRole();
    const routes = {
      ADMIN:       '/pages/admin.html',
      SUPERVISEUR: '/pages/superviseur.html',
      AGENT:       '/pages/agent.html',
      CLIENT:      '/pages/client.html',
    };
    window.location.href = routes[role] || '/index.html';
  },
  logout() {
    this.clear();
    window.location.href = '/index.html';
  },
};

// ── HTTP Client ────────────────────────────────────────────────
const API = {
  async request(method, path, body = null, requireAuth = true) {
    const headers = { 'Content-Type': 'application/json' };
    if (requireAuth) {
      const token = Auth.getToken();
      if (token) headers['Authorization'] = `Bearer ${token}`;
    }
    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    try {
      const res = await fetch(CONFIG.BASE_URL + path, opts);
      if (res.status === 401) { Auth.logout(); return; }
      const text = await res.text();
      const data = text ? JSON.parse(text) : {};
      if (!res.ok) throw { status: res.status, ...data };
      return data;
    } catch (err) {
      if (err instanceof TypeError) {
        throw { message: 'Impossible de joindre le serveur. Vérifiez votre connexion.' };
      }
      throw err;
    }
  },
  get(path)             { return this.request('GET',    path); },
  post(path, body)      { return this.request('POST',   path, body); },
  patch(path, body = {}) { return this.request('PATCH', path, body); },
  delete(path)          { return this.request('DELETE', path); },

  // Auth
  login(email, password)  { return this.request('POST', '/api/auth/login',    { email, motDePasse: password }, false); },
  register(data)           { return this.request('POST', '/api/auth/register', data, false); },

  // Admin
  getBanques()                         { return this.get('/api/admin/banques'); },
  createBanque(data)                   { return this.post('/api/admin/banques', data); },
  suspendBanque(id)                    { return this.patch(`/api/admin/banques/${id}/suspendre`); },
  reactivateBanque(id)                 { return this.patch(`/api/admin/banques/${id}/reactiver`); },
  getHistoriqueBanque(id, page = 0)    { return this.get(`/api/admin/banques/${id}/historique?page=${page}&size=20`); },
  retraitAdmin(id, data)               { return this.post(`/api/admin/banques/${id}/retrait`, data); },
  getAllUsers()                         { return this.get('/api/admin/utilisateurs'); },
  createSuperviseur(data)              { return this.post('/api/admin/superviseurs', data); },

  // Superviseur
  getAgents()                          { return this.get('/api/superviseur/agents'); },
  createAgent(data)                    { return this.post('/api/superviseur/agents', data); },
  getClients()                         { return this.get('/api/superviseur/clients'); },
  suspendCompte(userId)                { return this.patch(`/api/superviseur/comptes/${userId}/suspendre`); },
  reactivateCompte(userId)             { return this.patch(`/api/superviseur/comptes/${userId}/reactiver`); },
  approvisionnerAgent(agentId, data)   { return this.post(`/api/superviseur/agents/${agentId}/approvisionner`, data); },
  getHistoriqueSuperviseur(page = 0)   { return this.get(`/api/superviseur/historique?page=${page}&size=20`); },

  // Agent
  getMonCompteAgent()                  { return this.get('/api/agent/mon-compte'); },
  depot(data)                          { return this.post('/api/agent/depot', data); },
  retrait(data)                        { return this.post('/api/agent/retrait', data); },
  getHistoriqueAgent(page = 0)         { return this.get(`/api/agent/historique?page=${page}&size=20`); },

  // Client
  createCompte(data)                   { return this.post('/api/client/compte', data); },
  getMonCompteClient()                 { return this.get('/api/client/compte'); },
  transfert(data)                      { return this.post('/api/client/transfert', data); },
  getHistoriqueClient(page = 0)        { return this.get(`/api/client/historique?page=${page}&size=20`); },
};

// ── Toast ──────────────────────────────────────────────────────
const Toast = {
  _container: null,
  init() {
    this._container = document.getElementById('toast-container');
    if (!this._container) {
      this._container = document.createElement('div');
      this._container.id = 'toast-container';
      this._container.className = 'toast-container';
      document.body.appendChild(this._container);
    }
  },
  show(message, type = 'info', duration = 4000) {
    if (!this._container) this.init();
    const icons = { success: 'fa-check-circle', error: 'fa-exclamation-circle',
                    warning: 'fa-exclamation-triangle', info: 'fa-info-circle' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <i class="fas ${icons[type]} toast-icon"></i>
      <span class="toast-message">${message}</span>
      <button class="toast-close" onclick="this.parentElement.remove()">
        <i class="fas fa-times"></i>
      </button>`;
    this._container.appendChild(toast);
    if (duration > 0) setTimeout(() => {
      toast.classList.add('removing');
      setTimeout(() => toast.remove(), 280);
    }, duration);
    return toast;
  },
  success(msg, d) { return this.show(msg, 'success', d); },
  error(msg, d)   { return this.show(msg, 'error',   d); },
  warning(msg, d) { return this.show(msg, 'warning', d); },
  info(msg, d)    { return this.show(msg, 'info',    d); },
};

// ── Modal ──────────────────────────────────────────────────────
const Modal = {
  open(id)  {
    const el = document.getElementById(id);
    if (el) { el.classList.add('open'); document.body.style.overflow = 'hidden'; }
  },
  close(id) {
    const el = document.getElementById(id);
    if (el) { el.classList.remove('open'); document.body.style.overflow = ''; }
  },
  closeAll() {
    document.querySelectorAll('.modal-overlay.open').forEach(m => {
      m.classList.remove('open');
    });
    document.body.style.overflow = '';
  },
};
// Close modal on overlay click
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-overlay')) Modal.closeAll();
});

// ── Formatters ─────────────────────────────────────────────────
const Fmt = {
  money(n) {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-CM', {
      minimumFractionDigits: 0, maximumFractionDigits: 2
    }).format(n) + ' FCFA';
  },
  date(d) {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  },
  dateShort(d) {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  },
  typeLabel(t) {
    const map = {
      DEPOT: 'Dépôt', RETRAIT: 'Retrait',
      TRANSFERT_INTERNE: 'Transfert interne',
      TRANSFERT_INTERBANCAIRE: 'Transfert interbancaire',
      APPROVISIONNEMENT_AGENT: 'Approvisionnement',
      RETRAIT_ADMIN: 'Retrait admin',
    };
    return map[t] || t;
  },
  typeBadge(t) {
    const map = {
      DEPOT: 'badge-depot', RETRAIT: 'badge-retrait',
      TRANSFERT_INTERNE: 'badge-transfert',
      TRANSFERT_INTERBANCAIRE: 'badge-transfert',
      APPROVISIONNEMENT_AGENT: 'badge-appro',
      RETRAIT_ADMIN: 'badge-admin-op',
    };
    return `badge ${map[t] || 'badge-muted'}`;
  },
  initials(nom, prenom) {
    return ((prenom || ' ')[0] + (nom || ' ')[0]).toUpperCase();
  },
  roleBadge(role) {
    const map = {
      ADMIN: 'admin', SUPERVISEUR: 'superviseur',
      AGENT: 'agent', CLIENT: 'client'
    };
    return map[role] || 'client';
  },
  statutBanque(s) {
    return s === 'ACTIVE'
      ? '<span class="badge badge-success"><i class="fas fa-circle" style="font-size:6px"></i> Active</span>'
      : '<span class="badge badge-danger"><i class="fas fa-circle" style="font-size:6px"></i> Suspendue</span>';
  },
  statutCompte(s) {
    return s === 'ACTIF'
      ? '<span class="badge badge-success">Actif</span>'
      : '<span class="badge badge-danger">Suspendu</span>';
  },
};

// ── Error message extractor ────────────────────────────────────
function getErrorMessage(err) {
  if (typeof err === 'string') return err;
  if (err?.message) return err.message;
  if (err?.validationErrors) {
    return Object.values(err.validationErrors).join(' — ');
  }
  return 'Une erreur inattendue est survenue.';
}

// ── Loader ─────────────────────────────────────────────────────
const Loader = {
  show() {
    const l = document.getElementById('page-loader');
    if (l) l.classList.remove('hidden');
  },
  hide() {
    const l = document.getElementById('page-loader');
    if (l) l.classList.add('hidden');
  },
};

// ── Sidebar toggle (mobile) ────────────────────────────────────
function initSidebar() {
  const toggle = document.getElementById('sidebar-toggle');
  const sidebar = document.querySelector('.sidebar');
  const overlay = document.getElementById('sidebar-overlay');
  if (toggle && sidebar) {
    toggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    if (overlay) overlay.addEventListener('click', () => sidebar.classList.remove('open'));
  }
}

// ── Populate sidebar user info ─────────────────────────────────
function populateSidebarUser() {
  const user = Auth.getUser();
  if (!user) return;
  const nameEl  = document.getElementById('sb-user-name');
  const roleEl  = document.getElementById('sb-user-role');
  const avatarEl = document.getElementById('sb-avatar');
  if (nameEl)   nameEl.textContent  = `${user.nomComplet || user.email}`;
  if (roleEl)   roleEl.className    = `role-badge ${Fmt.roleBadge(user.role)}`;
  if (roleEl)   roleEl.textContent  = user.role;
  if (avatarEl) {
    const parts = (user.nomComplet || '').split(' ');
    avatarEl.textContent = ((parts[0]||'?')[0] + (parts[1]||'?')[0]).toUpperCase();
  }
}

// ── Active nav item ────────────────────────────────────────────
function setActiveNav(id) {
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  const el = document.getElementById(id);
  if (el) el.classList.add('active');
}

// ── Export helpers ──────────────────────────────────────────────
const Export = {
  toCSV(rows, headers, filename) {
    const lines = [headers.join(';')];
    rows.forEach(r => lines.push(r.join(';')));
    const blob = new Blob(['\uFEFF' + lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename + '.csv';
    a.click();
  },
  printTable(tableId, title) {
    const table = document.getElementById(tableId);
    if (!table) return;
    const w = window.open('', '_blank');
    w.document.write(`
      <html><head><title>${title}</title>
      <style>
        body{font-family:Arial,sans-serif;font-size:12px;margin:20px}
        h2{color:#144272;margin-bottom:16px}
        table{border-collapse:collapse;width:100%}
        th,td{border:1px solid #ddd;padding:8px;text-align:left}
        th{background:#144272;color:white}
        tr:nth-child(even){background:#f0f4f8}
      </style></head>
      <body><h2>${title}</h2>${table.outerHTML}</body></html>`);
    w.print();
    w.close();
  },
};

// ── DOM ready ─────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  Toast.init();
  initSidebar();
  populateSidebarUser();
  setTimeout(Loader.hide, 400);
});
