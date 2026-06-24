import os, glob
import re

views_dir = r'E:\桌面\ICMS\ICMS-frontend\src\views'
vue_files = glob.glob(os.path.join(views_dir, '**', '*.vue'), recursive=True)

for file in vue_files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    def optimize_op_col(m):
        full = m.group(0)
        # Shrink min-width because link buttons are tiny
        w_match = re.search(r'min-width="(\d+)"', full)
        if w_match:
            w = int(w_match.group(1))
            new_w = max(90, int(w * 0.45)) # significantly reduce
            full = re.sub(r'min-width="\d+"', f'min-width="{new_w}"', full)
            
        # Add fixed="right" back so they don't have to scroll to see it
        if 'fixed' not in full:
            full = full.replace('>', ' fixed="right">')
            
        return full

    new_content = re.sub(r'<el-table-column[^>]*label="操作"[^>]*>', optimize_op_col, content)
    
    if new_content != content:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {file}')

print('Done')
