# -*- coding: utf-8 -*-
import re, sys, shutil, os
from xml.sax.saxutils import escape
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from content import BOX, APP_NAME

D = sys.argv[1]  # unpacked dir
docp = os.path.join(D, 'word/document.xml')
x = open(docp, encoding='utf-8').read()

SZ = 20  # 10pt
LANG = '<w:lang w:eastAsia="ko-KR"/>'

def runs(text, size=SZ, color=None, bold_all=False):
    out = []
    parts = re.split(r'(\*\*.+?\*\*)', text)
    for p in parts:
        if not p:
            continue
        b = bold_all or (p.startswith('**') and p.endswith('**'))
        t = p[2:-2] if p.startswith('**') and p.endswith('**') else p
        rpr = ('<w:b/><w:bCs/>' if b else '') + (f'<w:color w:val="{color}"/>' if color else '') + f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>' + LANG
        out.append(f'<w:r><w:rPr>{rpr}</w:rPr><w:t xml:space="preserve">{escape(t)}</w:t></w:r>')
    return ''.join(out)

def para(inner, before=0, after=40, ind=None, keep=False):
    ppr = (('<w:keepNext/>' if keep else '') +
           f'<w:spacing w:before="{before}" w:after="{after}" w:line="276" w:lineRule="auto"/>' +
           (f'<w:ind w:left="{ind[0]}" w:hanging="{ind[1]}"/>' if ind else '') +
           f'<w:rPr>{LANG}</w:rPr>')
    return f'<w:p><w:pPr>{ppr}</w:pPr>{inner}</w:p>'

IMG_W, IMG_H = 6126480, 5557228
def img_para():
    d = ('<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">'
         f'<wp:extent cx="{IMG_W}" cy="{IMG_H}"/><wp:docPr id="901" name="mockup"/>'
         '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
         '<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
         '<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">'
         '<pic:nvPicPr><pic:cNvPr id="901" name="mockup.png"/><pic:cNvPicPr/></pic:nvPicPr>'
         '<pic:blipFill><a:blip r:embed="rIdMock1"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
         f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{IMG_W}" cy="{IMG_H}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>'
         '</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>')
    return para(d, before=60, after=80)

def render(text):
    ps = []
    first = True
    for line in text.split('\n'):
        if line.startswith('## '):
            ps.append(para(runs(line[3:], size=21, color='16324F', bold_all=True), before=0 if first else 160, after=60, keep=True))
        elif line.startswith('- '):
            ps.append(para(runs('• ' + line[2:]), after=30, ind=(300, 200)))
        elif line.startswith('~ '):
            ps.append(para(runs(line[2:], size=16, color='667085'), before=80))
        elif line == '[IMG]':
            ps.append(img_para())
        elif line.strip() == '':
            ps.append(para('', after=0))
        else:
            ps.append(para(runs(line), before=0 if first else 60, after=40))
        first = False
    return ''.join(ps)

# --- replace the 7 content-control paragraphs (box 8 left blank) ---
cell_paras = list(re.finditer(r'<w:p [^>]*>(?:(?!</w:p>).)*?<w:sdt>.*?</w:sdt></w:p>', x, flags=re.S))
assert len(cell_paras) == 8, len(cell_paras)
for i in range(7, 0, -1):  # replace from the end to keep offsets valid
    m = cell_paras[i - 1]
    x = x[:m.start()] + render(BOX[i]) + x[m.end():]

# --- section 01 / 04 fields ---
x, n = re.subn(r'앱명 / 프로젝트명: _+', '앱명 / 프로젝트명: ' + escape(APP_NAME), x); assert n == 3, n
x = x.replace('참가 단위: □ 개인 ', '참가 단위: ■ 개인 ', 1)
x, n = re.subn(r'(<w:t xml:space="preserve"> )□( </w:t></w:r><w:r><w:rPr><w:rFonts w:hint="eastAsia"/><w:lang w:eastAsia="ko-KR"/></w:rPr><w:t>일반)', r'\1■\2', x); assert n == 1, n
x, n = re.subn(r'참가 인원: _+명', '참가 인원: 1명', x); assert n == 1, n
x, n = re.subn(r'(<w:t>역할: )_+', r'\1기획·개발(1인)', x); assert n == 1, n
open(docp, 'w', encoding='utf-8').write(x)

# --- image part ---
os.makedirs(os.path.join(D, 'word/media'), exist_ok=True)
shutil.copy(sys.argv[2], os.path.join(D, 'word/media/mockup.png'))
relp = os.path.join(D, 'word/_rels/document.xml.rels')
r = open(relp, encoding='utf-8').read()
if 'rIdMock1' not in r:
    r = r.replace('</Relationships>', '<Relationship Id="rIdMock1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/mockup.png"/></Relationships>')
    open(relp, 'w', encoding='utf-8').write(r)
ctp = os.path.join(D, '[Content_Types].xml')
c = open(ctp, encoding='utf-8').read()
if 'Extension="png"' not in c:
    c = c.replace('<Default Extension="xml"', '<Default Extension="png" ContentType="image/png"/><Default Extension="xml"')
    open(ctp, 'w', encoding='utf-8').write(c)
print('ok')
