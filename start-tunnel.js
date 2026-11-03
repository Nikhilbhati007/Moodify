const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const logFile = path.join(__dirname, 'tunnel.log');
// Clear the log file first
fs.writeFileSync(logFile, '');

console.log("Starting localtunnel...");
fs.appendFileSync(logFile, "Starting localtunnel...\n");

const lt = spawn('npx', ['localtunnel', '--port', '3000'], { shell: true });

lt.stdout.on('data', (data) => {
  const output = data.toString();
  console.log("STDOUT:", output);
  fs.appendFileSync(logFile, output);
});

lt.stderr.on('data', (data) => {
  const output = data.toString();
  console.error("STDERR:", output);
  fs.appendFileSync(logFile, output);
});

lt.on('close', (code) => {
  console.log(`localtunnel process exited with code ${code}`);
  fs.appendFileSync(logFile, `\nProcess exited with code ${code}\n`);
});
