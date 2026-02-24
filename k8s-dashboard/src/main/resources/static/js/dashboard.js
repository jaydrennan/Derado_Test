const REFRESH_INTERVAL = 10000;

function formatTimestamp(ts) {
    if (!ts) return '-';
    const d = new Date(ts);
    return d.toLocaleString();
}

function statusBadge(status) {
    const s = status.toLowerCase().replace(/\s+/g, '');
    let cls = 'badge-normal';
    if (s === 'ready' || s === 'running') cls = 'badge-ready';
    else if (s === 'notready' || s === 'pending') cls = 'badge-notready';
    else if (s.includes('failed') || s.includes('error') || s.includes('terminated')) cls = 'badge-failed';
    return `<span class="badge ${cls}">${status}</span>`;
}

function eventTypeBadge(type) {
    return type === 'Warning'
        ? '<span class="badge badge-warning">Warning</span>'
        : '<span class="badge badge-normal">Normal</span>';
}

function conditionBadges(conditions) {
    if (!conditions || conditions.length === 0) {
        return '<span class="badge badge-ready">Healthy</span>';
    }
    return conditions.map(c => `<span class="badge badge-warning">${c}</span>`).join(' ');
}

function progressBar(percent) {
    let cls = 'bg-success';
    if (percent > 80) cls = 'bg-danger';
    else if (percent > 60) cls = 'bg-warning';
    return `<div class="progress"><div class="progress-bar ${cls}" style="width:${percent}%"></div></div>
            <small class="text-muted">${percent}%</small>`;
}

async function fetchJSON(url) {
    const resp = await fetch(url);
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    return resp.json();
}

async function loadSummary() {
    try {
        const data = await fetchJSON('/api/v1/cluster/summary');
        document.getElementById('summary-nodes').textContent = `${data.readyNodes} / ${data.totalNodes}`;
        document.getElementById('summary-pods').textContent = `${data.runningPods} / ${data.totalPods}`;
        document.getElementById('summary-deployments').textContent = `${data.availableDeployments} / ${data.totalDeployments}`;
        document.getElementById('summary-namespaces').textContent = data.totalNamespaces;
    } catch (e) {
        console.error('Failed to load summary:', e);
    }
}

async function loadNodes() {
    try {
        const nodes = await fetchJSON('/api/v1/nodes');
        const tbody = document.getElementById('nodes-table');
        tbody.innerHTML = nodes.map(n => `
            <tr>
                <td>${n.name}</td>
                <td>${statusBadge(n.status)}</td>
                <td>${conditionBadges(n.conditions)}</td>
                <td>${n.kubeletVersion}</td>
                <td>${n.osImage}</td>
                <td>${n.architecture}</td>
                <td>${n.containerRuntime}</td>
                <td>${n.capacity?.cpu || '-'}</td>
                <td>${n.capacity?.memory || '-'}</td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load nodes:', e);
    }
}

async function loadPods() {
    try {
        const pods = await fetchJSON('/api/v1/pods');
        renderPods(pods);
    } catch (e) {
        console.error('Failed to load pods:', e);
    }
}

function renderPods(pods) {
    const filter = document.getElementById('pod-namespace-filter').value.toLowerCase();
    const filtered = filter ? pods.filter(p => p.namespace.toLowerCase().includes(filter)) : pods;
    const tbody = document.getElementById('pods-table');
    tbody.innerHTML = filtered.map(p => `
        <tr>
            <td>${p.name}</td>
            <td>${p.namespace}</td>
            <td>${statusBadge(p.status)}</td>
            <td>${p.nodeName || '-'}</td>
            <td>${p.podIP || '-'}</td>
            <td>${p.restartCount}</td>
            <td>${p.cpuRequest || '-'} / ${p.cpuLimit || '-'}</td>
            <td>${p.memoryRequest || '-'} / ${p.memoryLimit || '-'}</td>
            <td>${formatTimestamp(p.creationTimestamp)}</td>
        </tr>
    `).join('');
}

async function loadDeployments() {
    try {
        const deployments = await fetchJSON('/api/v1/deployments');
        const tbody = document.getElementById('deployments-table');
        tbody.innerHTML = deployments.map(d => `
            <tr>
                <td>${d.name}</td>
                <td>${d.namespace}</td>
                <td>${d.readyReplicas} / ${d.desiredReplicas}</td>
                <td>${d.availableReplicas} / ${d.desiredReplicas}</td>
                <td>${d.strategy}</td>
                <td>${formatTimestamp(d.creationTimestamp)}</td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load deployments:', e);
    }
}

async function loadEvents() {
    try {
        const events = await fetchJSON('/api/v1/events?limit=50');
        const tbody = document.getElementById('events-table');
        tbody.innerHTML = events.map(e => `
            <tr>
                <td>${eventTypeBadge(e.type)}</td>
                <td>${e.reason}</td>
                <td>${e.involvedObject}</td>
                <td class="text-truncate" style="max-width:400px" title="${e.message}">${e.message}</td>
                <td>${e.namespace || '-'}</td>
                <td>${e.count}</td>
                <td>${formatTimestamp(e.lastTimestamp)}</td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load events:', e);
    }
}

async function loadMetrics() {
    try {
        const metrics = await fetchJSON('/api/v1/metrics/nodes');
        const unavail = document.getElementById('metrics-unavailable');
        const tbody = document.getElementById('metrics-table');
        if (metrics.length === 0) {
            unavail.classList.remove('d-none');
            tbody.innerHTML = '';
            return;
        }
        unavail.classList.add('d-none');
        tbody.innerHTML = metrics.map(m => `
            <tr>
                <td>${m.name}</td>
                <td>${m.cpuUsage}</td>
                <td>${m.cpuCapacity}</td>
                <td>${progressBar(m.cpuPercent)}</td>
                <td>${m.memoryUsage}</td>
                <td>${m.memoryCapacity}</td>
                <td>${progressBar(m.memoryPercent)}</td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load metrics:', e);
    }
}

let cachedPodMetrics = [];

async function loadPodMetrics() {
    try {
        cachedPodMetrics = await fetchJSON('/api/v1/metrics/pods');
        renderPodMetrics(cachedPodMetrics);
    } catch (e) {
        console.error('Failed to load pod metrics:', e);
    }
}

function renderPodMetrics(podMetrics) {
    const filter = document.getElementById('pod-metrics-namespace-filter').value.toLowerCase();
    const filtered = filter ? podMetrics.filter(p => p.namespace.toLowerCase().includes(filter)) : podMetrics;
    const tbody = document.getElementById('pod-metrics-table');
    tbody.innerHTML = filtered.map(p => {
        const containerDetail = p.containers && p.containers.length > 0
            ? p.containers.map(c => `${c.name}: ${c.cpuUsage} / ${c.memoryUsage}`).join('<br>')
            : '-';
        return `
            <tr>
                <td>${p.name}</td>
                <td>${p.namespace}</td>
                <td>${p.cpuUsage}</td>
                <td>${p.memoryUsage}</td>
                <td><small>${containerDetail}</small></td>
            </tr>
        `;
    }).join('');
}

function updateTimestamp() {
    document.getElementById('last-updated').textContent = 'Last updated: ' + new Date().toLocaleTimeString();
}

let cachedPods = [];

async function refreshAll() {
    await Promise.all([
        loadSummary(),
        loadNodes(),
        (async () => {
            try {
                cachedPods = await fetchJSON('/api/v1/pods');
                renderPods(cachedPods);
            } catch (e) {
                console.error('Failed to load pods:', e);
            }
        })(),
        loadDeployments(),
        loadEvents(),
        loadMetrics(),
        loadPodMetrics()
    ]);
    updateTimestamp();
}

document.addEventListener('DOMContentLoaded', () => {
    refreshAll();
    setInterval(refreshAll, REFRESH_INTERVAL);

    document.getElementById('pod-namespace-filter').addEventListener('input', () => {
        renderPods(cachedPods);
    });

    document.getElementById('pod-metrics-namespace-filter').addEventListener('input', () => {
        renderPodMetrics(cachedPodMetrics);
    });
});
