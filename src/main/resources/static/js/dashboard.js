const apiBasePath = window.APP_CONFIG?.basePath || '';

const healthState = document.getElementById('health-state');
const visitCount = document.getElementById('visit-count');
const versionValue = document.getElementById('version-value');
const visitsList = document.getElementById('visits-list');
const form = document.getElementById('visit-form');
const formStatus = document.getElementById('form-status');
const refreshButton = document.getElementById('refresh-visits');

function formatVisitedAt(value) {
    if (!value) {
        return 'Just now';
    }

    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString();
}

async function fetchJson(url, options) {
    const response = await fetch(url, {
        headers: {
            'Content-Type': 'application/json'
        },
        ...options
    });

    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Request failed with ${response.status}`);
    }

    return response.status === 204 ? null : response.json();
}

async function loadOverview() {
    try {
        const [health, version, visits] = await Promise.all([
            fetchJson(`${apiBasePath}/health`),
            fetchJson(`${apiBasePath}/version`),
            fetchJson(`${apiBasePath}/visits`)
        ]);

        healthState.textContent = health?.status || 'unknown';
        versionValue.textContent = version?.version || 'unknown';
        visitCount.textContent = Array.isArray(visits) ? String(visits.length) : '0';
        renderVisits(visits);
    } catch (error) {
        healthState.textContent = 'offline';
        versionValue.textContent = 'unavailable';
        visitsList.innerHTML = `<div class="empty-state">Unable to load the live API: ${error.message}</div>`;
    }
}

function renderVisits(visits) {
    if (!Array.isArray(visits) || visits.length === 0) {
        visitsList.innerHTML = '<div class="empty-state">No visits recorded yet. Use the form to create the first one.</div>';
        return;
    }

    visitsList.innerHTML = visits
        .slice()
        .reverse()
        .map((visit) => `
      <article class="visit-item">
        <strong>${visit.path || '/'}</strong>
        <div>${visit.message || 'No message provided'}</div>
        <div class="visit-meta">${formatVisitedAt(visit.visitedAt)}</div>
      </article>
    `)
        .join('');
}

form?.addEventListener('submit', async (event) => {
    event.preventDefault();
    formStatus.classList.remove('success');
    formStatus.textContent = 'Saving...';

    const formData = new FormData(form);
    const payload = Object.fromEntries(formData.entries());

    try {
        await fetchJson(`${apiBasePath}/visits`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        formStatus.textContent = 'Visit saved successfully.';
        formStatus.classList.add('success');
        form.reset();
        await loadOverview();
    } catch (error) {
        formStatus.textContent = `Could not save visit: ${error.message}`;
    }
});

refreshButton?.addEventListener('click', loadOverview);
loadOverview();