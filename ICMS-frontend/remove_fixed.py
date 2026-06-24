import os, glob
import re

views_dir = r'E:\桌面\ICMS\ICMS-frontend\src\views'
vue_files = glob.glob(os.path.join(views_dir, '**', '*.vue'), recursive=True)

for file in vue_files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    
    # Remove fixed="right"
    new_content = new_content.replace(' fixed="right"', '')
    # Remove fixed="left" (if any)
    new_content = new_content.replace(' fixed="left"', '')
    # Remove standalone fixed attribute inside el-table-column
    # <el-table-column prop="device_code" label="设备编码" width="130" fixed />
    new_content = re.sub(r'(<el-table-column[^>]+?)\s+fixed\s*(/?>)', r'\1\2', new_content)
    
    # Also to prevent squeezing, let's remove strict width="" constraints and turn them into min-width
    # if width is smaller than say 150, it might be fine, but let's change ALL width="XXX" to min-width="XXX"
    # Wait, some columns like status or icons are fine with strict width.
    # The main issue is the user doesn't want the "draggable" scrollbar. The horizontal scrollbar appears because 
    # the sum of `min-width` and `width` is greater than the container width. 
    # But removing `fixed` gets rid of the "sticky drag" effect which is the most annoying part.
    
    if new_content != content:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {file}')

print('Done')
