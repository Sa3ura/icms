import os, glob
import re

views_dir = r'E:\桌面\ICMS\ICMS-frontend\src\views'
vue_files = glob.glob(os.path.join(views_dir, '**', '*.vue'), recursive=True)

for file in vue_files:
    if 'Login.vue' in file or 'dashboard' in file:
        continue # Already handled
    
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content.replace('shadow="hover"', 'shadow="never"')
    new_content = re.sub(r'<el-table(.*?) stripe', r'<el-table\1', new_content)
    new_content = re.sub(r'<el-table(.*?) border', r'<el-table\1', new_content)
    
    if new_content != content:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {file}')
print('Done!')
