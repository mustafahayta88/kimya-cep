// TAB SWITCHING
function switchTab(tabId) {
    document.querySelectorAll('.tab-page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.getElementById(tabId).classList.add('active');
    const navBtn = document.querySelector(`[data-tab="${tabId}"]`);
    if (navBtn) navBtn.classList.add('active');
    window.scrollTo({top: 0, behavior: 'smooth'});
}

// FEATURE OVERLAY
const features = {
    'periyodik': {title: 'Periyodik Tablo', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg><h3 style="margin-top:16px;color:var(--text)">Periyodik Tablo</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">118 elementi keşfedin. Elemente tıklayarak detaylı bilgi alın.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'element': {title: 'Element Explorer', content: '<div style="text-align:center;padding:40px 0"><div style="width:80px;height:80px;border-radius:20px;background:rgba(0,212,170,0.1);border:1px solid rgba(0,212,170,0.2);display:flex;align-items:center;justify-content:center;margin:0 auto"><span style="font-size:28px;font-weight:800;color:var(--accent)">Au</span></div><h3 style="margin-top:16px;color:var(--text)">Element Explorer</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">118 elementin detaylı bilgisi, elektron konfigürasyonu ve özellikleri.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'lewis': {title: 'Lewis Yapıları', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--purple)" stroke-width="1.5"><circle cx="12" cy="8" r="3"/><circle cx="6" cy="16" r="3"/><circle cx="18" cy="16" r="3"/><line x1="12" y1="11" x2="7" y2="14"/><line x1="12" y1="11" x2="17" y2="14"/></svg><h3 style="margin-top:16px;color:var(--text)">Lewis Yapıları</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">25 molekül için Lewis nokta yapısı çizimleri.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'organic': {title: 'Organik Kimya', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--orange)" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg><h3 style="margin-top:16px;color:var(--text)">Organik Kimya</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">Tepkimeler, mekanizmalar ve organik bileşikler.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'termodinamik': {title: 'Fizikokimya', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--red)" stroke-width="1.5"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg><h3 style="margin-top:16px;color:var(--text)">Fizikokimya</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">Termodinamik, elektrokimya ve kimyasal denge hesaplamaları.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'not': {title: 'Ders Notları', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--pink)" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg><h3 style="margin-top:16px;color:var(--text)">Ders Notları</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">Kimya formülleri ve özetler.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'molkutlesi': {title: 'Molarite Hesaplama', content: '<div style="padding:20px 0"><div style="background:var(--bg2);border:1px solid var(--border);border-radius:16px;padding:24px"><h3 style="color:var(--text);margin-bottom:16px">Molarite Hesaplama</h3><p style="color:var(--dim);font-size:14px;margin-bottom:20px">M = n / V formülü ile molarite hesaplayın.</p><div style="display:flex;flex-direction:column;gap:12px"><input type="number" placeholder="Mol (n)" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><input type="number" placeholder="Hacim (L)" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><button onclick="calcMolarity()" style="background:linear-gradient(135deg,var(--accent),var(--accent2));color:#000;border:none;border-radius:12px;padding:14px;font-size:14px;font-weight:700;cursor:pointer;font-family:inherit">Hesapla</button></div><div id="molarity-result" style="margin-top:16px;text-align:center;font-size:18px;font-weight:700;color:var(--accent);display:none"></div></div></div>'},
    'ph': {title: 'pH Hesaplama', content: '<div style="padding:20px 0"><div style="background:var(--bg2);border:1px solid var(--border);border-radius:16px;padding:24px"><h3 style="color:var(--text);margin-bottom:16px">pH Hesaplama</h3><p style="color:var(--dim);font-size:14px;margin-bottom:20px">Asit veya baz derişiminden pH hesaplayın.</p><div style="display:flex;flex-direction:column;gap:12px"><input type="number" placeholder="H⁺ konsantrasyonu (mol/L)" step="any" id="ph-input" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><button onclick="calcPH()" style="background:linear-gradient(135deg,var(--accent),var(--accent2));color:#000;border:none;border-radius:12px;padding:14px;font-size:14px;font-weight:700;cursor:pointer;font-family:inherit">Hesapla</button></div><div id="ph-result" style="margin-top:16px;text-align:center;font-size:18px;font-weight:700;color:var(--accent);display:none"></div></div></div>'},
    'seyreltme': {title: 'Seyreltme Hesaplama', content: '<div style="padding:20px 0"><div style="background:var(--bg2);border:1px solid var(--border);border-radius:16px;padding:24px"><h3 style="color:var(--text);margin-bottom:16px">Seyreltme Hesaplama</h3><p style="color:var(--dim);font-size:14px;margin-bottom:20px">C₁V₁ = C₂V₂ formülü ile seyreltme hesaplayın.</p><div style="display:flex;flex-direction:column;gap:12px"><input type="number" placeholder="C₁ (başlangıç derişimi)" step="any" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><input type="number" placeholder="V₁ (başlangıç hacmi)" step="any" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><input type="number" placeholder="C₂ (son derişim)" step="any" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><button style="background:linear-gradient(135deg,var(--accent),var(--accent2));color:#000;border:none;border-radius:12px;padding:14px;font-size:14px;font-weight:700;cursor:pointer;font-family:inherit">Hesapla</button></div></div></div>'},
    'gaz': {title: 'Gaz Yasaları', content: '<div style="padding:20px 0"><div style="background:var(--bg2);border:1px solid var(--border);border-radius:16px;padding:24px"><h3 style="color:var(--text);margin-bottom:8px">Gaz Yasaları</h3><p style="color:var(--accent);font-size:13px;font-weight:600;margin-bottom:16px">PV = nRT</p><p style="color:var(--dim);font-size:14px;margin-bottom:20px">İdeal gaz yasasına göre bilinmeyeni hesaplayın.</p><div style="display:flex;flex-direction:column;gap:12px"><input type="number" placeholder="Basınç (atm)" step="any" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><input type="number" placeholder="Hacim (L)" step="any" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><input type="number" placeholder="Sıcaklık (K)" step="any" style="background:var(--bg3);border:1px solid var(--border);border-radius:12px;padding:14px;color:var(--text);font-size:14px;font-family:inherit;outline:none"><button style="background:linear-gradient(135deg,var(--accent),var(--accent2));color:#000;border:none;border-radius:12px;padding:14px;font-size:14px;font-weight:700;cursor:pointer;font-family:inherit">Hesapla</button></div></div></div>'},
    'titrasyon': {title: 'Titrasyon Hesaplama', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--pink)" stroke-width="1.5"><path d="M9 3h6v5l4 8H5l4-8V3z"/><line x1="9" y1="3" x2="15" y2="3"/></svg><h3 style="margin-top:16px;color:var(--text)">Titrasyon Simülasyonu</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">Asit-baz titrasyonu simülatörü ve hesaplama aracı.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'redox': {title: 'Redoks Hesaplama', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--red)" stroke-width="1.5"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg><h3 style="margin-top:16px;color:var(--text)">Redoks Tepkimeleri</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">Oksidasyon durumları ve elektron aktarımı hesaplamaları.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'ppm': {title: 'ppm / ppb Hesaplama', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--yellow)" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/></svg><h3 style="margin-top:16px;color:var(--text)">ppm / ppb Hesaplama</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">Derişim birimleri dönüşümleri: ppm, ppb, mol/L, g/L.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'ftir': {title: 'FT-IR Simülasyonu', content: '<div style="padding:20px 0"><div style="background:var(--bg2);border:1px solid var(--border);border-radius:16px;padding:24px"><h3 style="color:var(--text);margin-bottom:16px">FT-IR Spektroskopi</h3><div style="background:var(--bg);border-radius:12px;padding:20px;margin-bottom:16px"><svg width="100%" height="120" viewBox="0 0 400 120"><defs><linearGradient id="specGrad" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="var(--accent)"/><stop offset="1" stop-color="var(--blue)"/></linearGradient></defs><path d="M0,10 Q20,10 40,30 Q60,50 80,20 Q100,5 120,40 Q140,80 160,60 Q180,40 200,90 Q220,100 240,50 Q260,20 280,70 Q300,90 320,40 Q340,15 360,60 Q380,80 400,10" fill="none" stroke="url(#specGrad)" stroke-width="2"/><text x="20" y="115" fill="var(--muted)" font-size="10">4000</text><text x="180" y="115" fill="var(--muted)" font-size="10">2000</text><text x="360" y="115" fill="var(--muted)" font-size="10">500</text><text x="160" y="12" fill="var(--muted)" font-size="10">Dalga Sayısı (cm⁻¹)</text></svg></div><p style="color:var(--dim);font-size:14px">İşlevsel grupların spektrumunu analiz edin.</p></div></div>'},
    'kalibrasyon': {title: 'Kalibrasyon Eğrisi', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="1.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg><h3 style="margin-top:16px;color:var(--text)">Kalibrasyon Eğrisi</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">Lineer regresyon ile kalibrasyon eğrisi oluşturun.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'molarite': {title: 'Molarite Hesaplama', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--blue)" stroke-width="1.5"><path d="M9 3h6v5l4 8H5l4-8V3z"/><line x1="9" y1="3" x2="15" y2="3"/></svg><h3 style="margin-top:16px;color:var(--text)">Molarite Hesaplama</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">M = n / V ile molarite hesaplayın.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'},
    'enstrumantal': {title: 'Enstrümantal Analiz', content: '<div style="text-align:center;padding:40px 0"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="1.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg><h3 style="margin-top:16px;color:var(--text)">Enstrümantal Analiz</h3><p style="color:var(--dim);margin-top:8px;font-size:14px">AAS, FTIR ve UV-Vis spektroskopi simülasyonları.</p><p style="color:var(--muted);margin-top:16px;font-size:12px">Bu özellik uygulamada mevcuttur</p></div>'}
};

function showFeature(id) {
    const f = features[id];
    if (!f) return;
    document.getElementById('feature-title').textContent = f.title;
    document.getElementById('feature-content').innerHTML = f.content;
    document.getElementById('feature-overlay').classList.add('show');
    document.body.style.overflow = 'hidden';
}

function closeFeature() {
    document.getElementById('feature-overlay').classList.remove('show');
    document.body.style.overflow = '';
}

// CALCULATORS
function calcMolarity() {
    const inputs = document.querySelectorAll('#feature-content input[type="number"]');
    const n = parseFloat(inputs[0]?.value);
    const v = parseFloat(inputs[1]?.value);
    const result = document.getElementById('molarity-result');
    if (n && v && v !== 0) {
        result.style.display = 'block';
        result.textContent = `M = ${(n/v).toFixed(4)} mol/L`;
    }
}

function calcPH() {
    const h = parseFloat(document.getElementById('ph-input')?.value);
    const result = document.getElementById('ph-result');
    if (h && h > 0) {
        const ph = -Math.log10(h);
        result.style.display = 'block';
        result.textContent = `pH = ${ph.toFixed(2)}`;
    }
}

// SEARCH
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', function(e) {
            const q = e.target.value.toLowerCase();
            document.querySelectorAll('.category-card, .tool-item').forEach(card => {
                const text = card.textContent.toLowerCase();
                card.style.display = text.includes(q) || !q ? '' : 'none';
            });
        });
    }
});

// HAPTIC FEEDBACK (mobile)
document.querySelectorAll('.nav-item, .category-card, .tool-item, .quick-card, .continue-item').forEach(el => {
    el.addEventListener('click', () => {
        if (navigator.vibrate) navigator.vibrate(10);
    });
});
