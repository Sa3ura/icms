import os, glob
import re

views_dir = r'E:\桌面\ICMS\ICMS-frontend\src\views'
vue_files = glob.glob(os.path.join(views_dir, '**', '*.vue'), recursive=True)

for file in vue_files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    
    def replace_op_col(m):
        full_match = m.group(0)
        width_match = re.search(r'width="(\d+)"', full_match)
        if width_match:
            w = int(width_match.group(1))
            new_full = re.sub(r'width="\d+"', f'min-width="{w + 40}"', full_match)
            return new_full
        return full_match

    new_content = re.sub(r'<el-table-column[^>]*label="操作"[^>]*>', replace_op_col, new_content)
    
    if new_content != content:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {file}')

print('Done')
