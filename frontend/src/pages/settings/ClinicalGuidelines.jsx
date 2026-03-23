import React from 'react';
import { FileText, ExternalLink, BookOpen, ShieldCheck, Stethoscope, Wind } from 'lucide-react';

const ClinicalGuidelines = () => {
  const guidelines = [
    {
      title: 'GOLD 2024 Report',
      subtitle: 'Global Strategy for Prevention, Diagnosis and Management of COPD',
      description: 'Evidence-based recommendations for the classification, diagnosis, and management of COPD. Includes updated pharmacological and non-pharmacological treatment algorithms.',
      url: 'https://goldcopd.org/2024-gold-report/',
      icon: <BookOpen size={24} color="#F59E0B" />,
      color: '#F59E0B',
      bg: 'rgba(245, 158, 11, 0.08)'
    },
    {
      title: 'BTS Emergency Oxygen Guidelines',
      subtitle: 'British Thoracic Society Guideline for Oxygen Use',
      description: 'Comprehensive guidelines for emergency oxygen use in adult patients. Covers target SpO2 ranges, oxygen delivery devices, and monitoring protocols.',
      url: 'https://www.brit-thoracic.org.uk/quality-improvement/guidelines/emergency-oxygen/',
      icon: <Wind size={24} color="#3B82F6" />,
      color: '#3B82F6',
      bg: 'rgba(59, 130, 246, 0.08)'
    }
  ];

  const quickRef = [
    {
      title: 'COPD Oxygen Targets',
      items: [
        { label: 'General COPD', value: 'SpO2 88-92%' },
        { label: 'COPD + CO2 Retention Risk', value: 'SpO2 88-92%' },
        { label: 'Acute Exacerbation', value: 'SpO2 88-92% (Controlled O2)' },
        { label: 'Without CO2 Risk', value: 'SpO2 94-98%' }
      ],
      icon: <Stethoscope size={20} color="#10B981" />
    },
    {
      title: 'Escalation Indicators',
      items: [
        { label: 'pH < 7.35', value: 'Consider NIV' },
        { label: 'PaCO2 > 45 mmHg', value: 'Risk of Type II RF' },
        { label: 'SpO2 < 85%', value: 'Urgent escalation' },
        { label: 'RR > 30/min', value: 'Respiratory distress' }
      ],
      icon: <ShieldCheck size={20} color="#EF4444" />
    }
  ];

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <div className="page-header">
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <FileText size={24} /> Clinical Guidelines
          </h1>
          <p>Reference guidelines for COPD management and oxygen therapy</p>
        </div>
      </div>

      {/* Guideline Cards */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', marginBottom: '32px' }}>
        {guidelines.map((g, idx) => (
          <div key={idx} className="card" style={{ background: g.bg, border: `1px solid ${g.color}25`, position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', top: 0, left: 0, bottom: 0, width: '4px', background: g.color }}></div>
            <div style={{ paddingLeft: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '20px' }}>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
                    {g.icon}
                    <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0 }}>{g.title}</h3>
                  </div>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', fontWeight: 500, marginBottom: '8px' }}>{g.subtitle}</p>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.6 }}>{g.description}</p>
                </div>
                <a href={g.url} target="_blank" rel="noopener noreferrer" className="btn btn-primary" style={{ flexShrink: 0, gap: '8px' }}>
                  <ExternalLink size={16} /> View PDF
                </a>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Quick Reference */}
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '16px' }}>Quick Reference</h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '20px' }}>
        {quickRef.map((section, idx) => (
          <div key={idx} className="card">
            <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              {section.icon} {section.title}
            </h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {section.items.map((item, i) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '12px', borderBottom: i < section.items.length - 1 ? '1px solid var(--border-light)' : 'none' }}>
                  <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>{item.label}</span>
                  <span style={{ fontWeight: 600, fontSize: '0.875rem' }}>{item.value}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ClinicalGuidelines;
