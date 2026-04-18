const express = require('express');
const { chromium } = require('playwright');

const app = express();
app.use(express.json({ limit: '10mb' }));

const PORT = process.env.PORT || 3001;
const USER_AGENT =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

let browser = null;

async function getBrowser() {
  if (!browser || !browser.isConnected()) {
    browser = await chromium.launch({
      headless: true,
      args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'],
    });
  }
  return browser;
}

/**
 * POST /render
 * Body: { url: string, waitFor?: string (CSS selector to wait for) }
 * Response: { html: string } | { error: string }
 */
app.post('/render', async (req, res) => {
  const { url, waitFor } = req.body;

  if (!url || typeof url !== 'string') {
    return res.status(400).json({ error: 'url (string) is required' });
  }

  let context = null;
  try {
    const b = await getBrowser();
    context = await b.newContext({
      userAgent: USER_AGENT,
      locale: 'fr-FR',
      extraHTTPHeaders: { 'Accept-Language': 'fr-FR,fr;q=0.9,en;q=0.8' },
    });

    const page = await context.newPage();

    // Block images/fonts/media to speed up rendering
    await page.route('**/*.{png,jpg,jpeg,gif,webp,svg,woff,woff2,ttf,mp4,mp3}', (route) =>
      route.abort()
    );

    // 'load' fires once the page + scripts are loaded; SPAs often keep fetching data
    // so 'networkidle' would timeout. We then wait an extra 2 s so React/Vue can
    // hydrate the initial view, or wait for a specific selector if provided.
    await page.goto(url, { waitUntil: 'load', timeout: 30000 });

    if (waitFor) {
      await page.waitForSelector(waitFor, { timeout: 10000 }).catch(() => {});
    } else {
      // Small fixed delay lets the SPA render its initial data-fetch results
      await page.waitForTimeout(2000);
    }

    const html = await page.content();
    res.json({ html });
  } catch (err) {
    console.error(`[render] ${url} — ${err.message}`);
    res.status(500).json({ error: err.message });
  } finally {
    if (context) await context.close().catch(() => {});
  }
});

app.get('/health', (_, res) => res.json({ ok: true }));

app.listen(PORT, () => console.log(`playwright-scraper listening on :${PORT}`));

// Graceful shutdown — close browser before exit
async function shutdown() {
  if (browser) {
    await browser.close().catch(() => {});
  }
  process.exit(0);
}
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
