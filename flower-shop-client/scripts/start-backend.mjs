import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const cwd = fileURLToPath(new URL('../../flowershop/', import.meta.url))
const windows = process.platform === 'win32'
console.log('Starting React + Spring Boot at http://localhost:8080 (one server).')
const child = spawn(windows ? 'cmd.exe' : './gradlew', windows ? ['/d', '/c', 'gradlew.bat bootRun'] : ['bootRun'], { cwd, stdio: 'inherit' })
child.on('error', error => { console.error(error.message); process.exitCode = 1 })
child.on('exit', code => { process.exitCode = code ?? 1 })
