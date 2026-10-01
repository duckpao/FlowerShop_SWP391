import sys
content = open(r'E:\FlowerShop\flower-shop-client\src\services\catalogService.js', 'r', encoding='utf-8').read()

imports = "import { API_BASE } from '../apiBase'\n\nasync function get(path) {"
content = content.replace("async function get(path) {", imports)

fetch_call = "const response = await fetch(`${API_BASE}${path}`, { cache: 'no-store' })"
content = content.replace("const response = await fetch(path, { cache: 'no-store' })", fetch_call)

# also fix the mojibake in error message
content = content.replace("KhA'ng ti `c d_ liu sn phcm.", "Không tải được dữ liệu.")

open(r'E:\FlowerShop\flower-shop-client\src\services\catalogService.js', 'w', encoding='utf-8').write(content)
