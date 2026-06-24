import os, glob
import re

views_dir = r'E:\桌面\ICMS\ICMS-frontend\src\views'
vue_files = glob.glob(os.path.join(views_dir, '**', '*.vue'), recursive=True)

for file in vue_files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    def normalize_width(m):
        full = m.group(0)
        # Increase min-width slightly so buttons don't squeeze awkwardly
        # Find min-width="\d+" 
        w_match = re.search(r'min-width="(\d+)"', full)
        if w_match:
            w = int(w_match.group(1))
            new_w = max(160, w) # set absolute minimum to 160px for safety
            full = re.sub(r'min-width="\d+"', f'min-width="{new_w}"', full)
            
        return full

    new_content = re.sub(r'<el-table-column[^>]*label="操作"[^>]*>', normalize_width, content)
    
    if new_content != content:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {file}')

print('Done')
