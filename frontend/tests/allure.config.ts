/**
 * Allure Reporting Configuration
 *
 * This file configures Allure reporting for the test suite
 */

import { AllureReporter } from '@playwright/test/reporter'

class CustomAllureReporter extends AllureReporter {
  constructor() {
    super({
      outputDir: './allure-results',
      clean: true,
      suiteTitleTruncateSize: 50,
      testCaseIdTruncateSize: 21,
    })
  }
}

export default CustomAllureReporter
