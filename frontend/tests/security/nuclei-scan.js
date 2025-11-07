/**
 * Nuclei Vulnerability Scanner Integration
 *
 * This script runs Nuclei vulnerability scans against the application
 * to identify known security issues
 */

const { spawn } = require('child_process')
const fs = require('fs')
const path = require('path')

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'
const OUTPUT_DIR = path.join(__dirname, '../../results')
const NUCLEI_CONFIG = path.join(__dirname, 'nuclei-config.yml')

// Ensure output directory exists
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
}

console.log('=== NUCLEI VULNERABILITY SCAN ===')
console.log(`Target: ${BASE_URL}`)
console.log(`Output Directory: ${OUTPUT_DIR}\n`)

// Create nuclei config
const config = `
# Nuclei Configuration
# https://github.com/projectdiscovery/nuclei

# Global settings
globals:
  - BASE_URL as base_url
  - TEMPLATE_PATH as template_path

# Output settings
output:
  - json
  - sarif
  - csv

# Filter settings
severity: [high, critical, medium]
exclude_tags: [dos, spam]

# Rate limiting
rate-limit: 150
bulk-size: 25
max-height: 150

# Timeouts
timeout: 5
retries: 1

# Severity thresholds
severity-threshold: medium

# Custom headers
custom-headers:
  User-Agent: BSS Security Scanner
  Accept: application/json

# Exclusions (paths to skip)
exclude-tags: [fuzz, crawl]
`

fs.writeFileSync(NUCLEI_CONFIG, config)
console.log('Nuclei configuration created\n')

/**
 * Run nuclei scan
 */
function runNucleiScan() {
  return new Promise((resolve, reject) => {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
    const outputFile = path.join(OUTPUT_DIR, `nuclei-results-${timestamp}.json`)

    const args = [
      '-target', BASE_URL,
      '-config', NUCLEI_CONFIG,
      '-json',
      '-output', outputFile,
      '-silent',
      '-rate-limit', '100',
      '-timeout', '5',
      '-retries', '1',
    ]

    console.log('Running nuclei scan...')
    console.log(`Command: nuclei ${args.join(' ')}\n`)

    const nuclei = spawn('nuclei', args, {
      stdio: ['inherit', 'pipe', 'pipe'],
      env: { ...process.env },
    })

    let stdout = ''
    let stderr = ''

    nuclei.stdout.on('data', (data) => {
      const message = data.toString()
      stdout += message
      process.stdout.write(message)
    })

    nuclei.stderr.on('data', (data) => {
      const message = data.toString()
      stderr += message
      process.stderr.write(message)
    })

    nuclei.on('close', (code) => {
      console.log(`\nNuclei scan completed with exit code: ${code}\n`)

      if (code === 0 || code === 1) {
        // 0 = no issues, 1 = issues found
        resolve({ code, outputFile, stdout, stderr })
      } else {
        reject(new Error(`Nuclei scan failed with code ${code}: ${stderr}`))
      }
    })

    nuclei.on('error', (err) => {
      reject(err)
    })
  })
}

/**
 * Analyze nuclei results
 */
function analyzeResults(outputFile) {
  console.log('=== ANALYZING NUCLEI RESULTS ===\n')

  if (!fs.existsSync(outputFile)) {
    console.log('No output file found')
    return
  }

  const content = fs.readFileSync(outputFile, 'utf8')
  const lines = content.trim().split('\n').filter(line => line.trim())

  if (lines.length === 0) {
    console.log('✓ No vulnerabilities found by nuclei')
    return
  }

  let criticalCount = 0
  let highCount = 0
  let mediumCount = 0
  let lowCount = 0
  let infoCount = 0

  const vulnerabilities = []

  for (const line of lines) {
    try {
      const result = JSON.parse(line)
      const severity = result['info']?.severity || 'unknown'
      const name = result['info']?.name || 'Unknown'
      const reference = result['info']?.reference || []

      const vuln = {
        severity,
        name,
        reference,
        type: result['type'],
        host: result['host'],
        matched: result['matched'],
      }

      vulnerabilities.push(vuln)

      switch (severity?.toLowerCase()) {
        case 'critical':
          criticalCount++
          break
        case 'high':
          highCount++
          break
        case 'medium':
          mediumCount++
          break
        case 'low':
          lowCount++
          break
        case 'info':
          infoCount++
          break
      }
    } catch (err) {
      console.log('Error parsing line:', err.message)
    }
  }

  console.log(`Scan Results Summary:`)
  console.log(`  Total Findings: ${vulnerabilities.length}`)
  console.log(`  Critical: ${criticalCount}`)
  console.log(`  High: ${highCount}`)
  console.log(`  Medium: ${mediumCount}`)
  console.log(`  Low: ${lowCount}`)
  console.log(`  Info: ${infoCount}\n`)

  if (vulnerabilities.length > 0) {
    console.log('Top Findings:')
    vulnerabilities.slice(0, 10).forEach((vuln, index) => {
      console.log(`\n${index + 1}. [${vuln.severity.toUpperCase()}] ${vuln.name}`)
      console.log(`   Host: ${vuln.host}`)
      if (vuln.matched) {
        console.log(`   Matched: ${JSON.stringify(vuln.matched)}`)
      }
    })
  }

  // Generate summary report
  const summaryFile = outputFile.replace('.json', '-summary.txt')
  const summary = `
NUCLEI VULNERABILITY SCAN SUMMARY
==================================
Date: ${new Date().toISOString()}
Target: ${BASE_URL}
Total Findings: ${vulnerabilities.length}

Severity Breakdown:
  Critical: ${criticalCount}
  High: ${highCount}
  Medium: ${mediumCount}
  Low: ${lowCount}
  Info: ${infoCount}

Critical Issues:
${vulnerabilities.filter(v => v.severity?.toLowerCase() === 'critical').map(v => `  - ${v.name} (${v.host})`).join('\n') || '  None'}

High Issues:
${vulnerabilities.filter(v => v.severity?.toLowerCase() === 'high').map(v => `  - ${v.name} (${v.host})`).join('\n') || '  None'}

Recommendations:
1. Address all Critical and High severity issues immediately
2. Review Medium severity issues based on risk tolerance
3. Consider using this scan in your CI/CD pipeline
4. Run regular scans to identify new vulnerabilities

Full results: ${outputFile}
`

  fs.writeFileSync(summaryFile, summary)
  console.log(`\nSummary report saved to: ${summaryFile}`)

  // Test passes if no critical or high severity issues
  if (criticalCount > 0 || highCount > 0) {
    console.log('\n⚠️  Critical or High severity vulnerabilities found!')
    return false
  } else {
    console.log('\n✓ No critical or high severity vulnerabilities found')
    return true
  }
}

/**
 * Main execution
 */
async function main() {
  try {
    const { code, outputFile } = await runNucleiScan()
    const passed = analyzeResults(outputFile)

    // Exit with appropriate code
    if (passed) {
      console.log('\n=== NUCLEI SCAN PASSED ===')
      process.exit(0)
    } else {
      console.log('\n=== NUCLEI SCAN FAILED ===')
      process.exit(1)
    }
  } catch (error) {
    console.error('\n=== NUCLEI SCAN ERROR ===')
    console.error(error.message)

    if (error.message.includes('not found') || error.message.includes('command not found')) {
      console.log('\nNuclei is not installed. Installing...')
      const install = spawn('bash', ['-c', 'curl -L https://github.com/projectdiscovery/nuclei/releases/latest/download/nuclei_2.9.15_linux_amd64.zip -o nuclei.zip && unzip nuclei.zip && sudo mv nuclei /usr/local/bin/'], {
        stdio: 'inherit',
      })

      install.on('close', (code) => {
        if (code === 0) {
          console.log('Nuclei installed successfully. Please run the scan again.')
          process.exit(0)
        } else {
          console.log('Failed to install nuclei')
          process.exit(1)
        }
      })
    } else {
      process.exit(1)
    }
  }
}

main()
