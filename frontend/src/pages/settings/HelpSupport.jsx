import React, { useState } from 'react';
import { HelpCircle, Mail, Phone, MessageCircle, ChevronDown, ChevronUp, BookOpen, Shield, Activity } from 'lucide-react';

const Section = ({ question, answer, defaultOpen = false }) => {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div style={{ border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
      <button onClick={() => setOpen(!open)} style={{
        width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '16px 20px', background: open ? 'var(--bg-secondary)' : 'transparent',
        border: 'none', color: 'var(--text-primary)', cursor: 'pointer', fontSize: '0.9375rem', fontWeight: 600, textAlign: 'left'
      }}>
        <span>{question}</span>
        {open ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
      </button>
      {open && (
        <div style={{ padding: '16px 20px', borderTop: '1px solid var(--border)' }}>
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7, fontSize: '0.875rem', margin: 0 }}>{answer}</p>
        </div>
      )}
    </div>
  );
};

const HelpSupport = () => {
  const faqs = [
    { q: 'How do I add a new patient?', a: 'Navigate to "Add Patient" from the sidebar or dashboard. Fill in the patient demographics, ward/bed information, and admission details. Click "Save" to create the patient record.' },
    { q: 'How do I record vitals for a patient?', a: 'Go to the patient detail page and click on the "Vitals & Spirometry" tab. Click "Record Vitals" to enter SpO2, heart rate, respiratory rate, blood pressure, temperature, and consciousness level.' },
    { q: 'How does the AI Risk Analysis work?', a: 'The AI risk analysis uses the patient\'s latest vitals, ABG results, and clinical data to calculate a risk score. Navigate to the patient\'s "AI Analysis" tab and click "View Trends" to see the full analysis.' },
    { q: 'What do the escalation criteria mean?', a: 'Escalation criteria are clinical thresholds that, when met, indicate the need for immediate intervention. These include SpO2 < 88%, pH < 7.35, and respiratory rate > 30/min. When triggered, they appear highlighted in the Escalation tab.' },
    { q: 'How do I schedule a reassessment?', a: 'From the patient\'s "Escalation" tab, click "Schedule Now". Select the reassessment type (Routine, Urgent, or Post Therapy Change), interval, and any notes. Staff will be notified of pending reassessments.' },
    { q: 'How do I access clinical guidelines?', a: 'Click "Guidelines" in the sidebar under the Clinical section. You can access the GOLD 2024 Report and BTS Emergency Oxygen Guidelines directly from there.' },
    { q: 'What notification types are available?', a: 'There are three notification types: Critical (immediate action required), Warning (review recommended), and Info (general updates). You can filter notifications by type on the Notifications page.' }
  ];

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <HelpCircle size={24} /> Help & Support
          </h1>
          <p>Get help with using the CDSS COPD Clinical Decision Support System</p>
        </div>
      </div>

      {/* Quick Links */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '32px' }}>
        <div className="card" style={{ textAlign: 'center', padding: '24px' }}>
          <BookOpen size={28} color="var(--accent-primary)" style={{ margin: '0 auto 12px' }} />
          <h4 style={{ fontWeight: 600, marginBottom: '4px' }}>Documentation</h4>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>User guides and tutorials</p>
        </div>
        <div className="card" style={{ textAlign: 'center', padding: '24px' }}>
          <Shield size={28} color="#10B981" style={{ margin: '0 auto 12px' }} />
          <h4 style={{ fontWeight: 600, marginBottom: '4px' }}>Privacy Policy</h4>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Data protection info</p>
        </div>
        <div className="card" style={{ textAlign: 'center', padding: '24px' }}>
          <Activity size={28} color="#8B5CF6" style={{ margin: '0 auto 12px' }} />
          <h4 style={{ fontWeight: 600, marginBottom: '4px' }}>System Status</h4>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>All systems operational</p>
        </div>
      </div>

      {/* FAQ */}
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '16px' }}>Frequently Asked Questions</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '32px' }}>
        {faqs.map((faq, idx) => (
          <Section key={idx} question={faq.q} answer={faq.a} defaultOpen={idx === 0} />
        ))}
      </div>

      {/* Contact */}
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '16px' }}>Contact Support</h2>
      <div className="card">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: 'rgba(59, 130, 246, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Mail size={18} color="#3B82F6" />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Email</div>
              <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>support@cdss-copd.com</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: 'rgba(16, 185, 129, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Phone size={18} color="#10B981" />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Phone</div>
              <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>+91 44-2745-3000</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: 'rgba(139, 92, 246, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <MessageCircle size={18} color="#8B5CF6" />
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Version</div>
              <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>CDSS COPD v2.0</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HelpSupport;
