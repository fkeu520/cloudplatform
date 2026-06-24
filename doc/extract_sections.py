# -*- coding: utf-8 -*-
import docx

doc = docx.Document(r'D:\work\AI\output\platform\doc\产品需求说明书.DOCX')

lines = []
for i, para in enumerate(doc.paragraphs):
    text = para.text.strip()
    if text:
        style = para.style.name
        if 'Title' in style:
            lines.append(f'# {text}')
        elif 'Heading' in style:
            level = style.replace('Heading ', '')
            lines.append(f'{"#" * int(level)} {text}')
        elif 'List' in style:
            lines.append(f'- {text}')
        else:
            lines.append(text)
        lines.append('')

content = '\n'.join(lines)

# Find major section markers and output key sections
sections = {
    '客户档案': None,
    '企业档案': None,
    '智慧招商': None,
    '招商管理': None,
    '智慧物业': None,
    '物业管理': None,
    '合同管理': None,
    '业财管理': None,
    '计费管理': None,
    '园企服务': None,
    '企业工作台': None,
}

for kw in sections:
    idx = content.find(kw)
    if idx >= 0:
        sections[kw] = idx

with open(r'D:\work\AI\output\platform\doc\产品需求说明书_sections.txt', 'w', encoding='utf-8') as f:
    f.write('=== KEYWORD INDEX ===\n\n')
    for kw, idx in sorted(sections.items(), key=lambda x: x[1] if x[1] else 0):
        f.write(f'{kw}: {idx}\n')
    
    f.write('\n\n=== KEY SECTIONS ===\n\n')
    for kw, idx in sorted(sections.items(), key=lambda x: x[1] if x[1] else 0):
        if idx:
            end = min(len(content), idx + 5000)
            f.write(f'\n{"="*60}\n')
            f.write(f'SECTION: {kw} (offset {idx})\n')
            f.write(f'{"="*60}\n\n')
            f.write(content[idx:end])