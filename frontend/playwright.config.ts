import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
	testDir: './e2e',
	use: { baseURL: 'http://127.0.0.1:4174' },
	webServer: {
		command: 'npm run preview:browser',
		url: 'http://127.0.0.1:4174',
		reuseExistingServer: false
	},
	projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
});
