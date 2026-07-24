document.addEventListener('DOMContentLoaded',()=>{
    const nav=document.querySelector('.navbar');
    window.addEventListener('scroll',()=>{nav.classList.toggle('scrolled',window.scrollY>30)});
    document.querySelectorAll('a[href^="#"]').forEach(a=>{a.addEventListener('click',e=>{e.preventDefault();const t=document.querySelector(a.getAttribute('href'));if(t)t.scrollIntoView({behavior:'smooth',block:'start'})})});
    const obs=new IntersectionObserver(es=>{es.forEach(e=>{if(e.isIntersecting){e.target.style.opacity='1';e.target.style.transform='translateY(0)'}})},{threshold:.1});
    document.querySelectorAll('.feature-card,.step-card,.timeline-item,.faq-item,.contact-card').forEach(el=>{el.style.opacity='0';el.style.transform='translateY(20px)';el.style.transition='opacity .5s,transform .5s';obs.observe(el)});
})