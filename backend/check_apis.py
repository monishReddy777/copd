import os, re, sys
import django

sys.path.append('c:/Users/monis/Downloads/cdss2 (6)/CDSS COPD (3)/CDSS COPD/CDSS_COPD')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'backend.settings')
django.setup()

from django.urls import get_resolver

resolver = get_resolver()
django_paths = set()
for url_pattern in resolver.url_patterns:
    if hasattr(url_pattern, 'url_patterns'): # api/
        for p in url_pattern.url_patterns:
            django_paths.add('/api/' + str(p.pattern))

react_dir = 'c:/Users/monis/Downloads/cdss2 (6)/cdss-web/src/api'
react_endpoints = set()
for file in os.listdir(react_dir):
    if file.endswith('.js'):
        with open(os.path.join(react_dir, file), 'r', encoding='utf-8') as f:
            content = f.read()
            matches = re.findall(r'api\.(?:get|post|put|delete|patch)\([`\'\"](/[^`\'\"]+)[`\'\"]', content)
            for m in matches:
                clean_path = re.sub(r'\$\{.*?\}', '<id>', m)
                react_endpoints.add('/api' + clean_path)

with open('missing_endpoints.txt', 'w', encoding='utf-8') as f:
    f.write('--- MISSING ENDPOINTS IN BACKEND ---\n')
    for path in sorted(react_endpoints):
        found = False
        for dp in django_paths:
            dp_regex = re.sub(r'\<[^>]+\>', '<id>', str(dp))
            if path.strip('/') == dp_regex.strip('/'):
                found = True
                break
        if not found:
            f.write(f'MISSING: {path}\n')
