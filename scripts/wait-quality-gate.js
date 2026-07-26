#!/usr/bin/env node
/**
 * Attend le résultat du Quality Gate SonarQube après une analyse Maven.
 * Lit target/sonar/report-task.txt (généré par sonar-maven-plugin).
 * Variables attendues (fournies par withSonarQubeEnv) :
 *   SONAR_HOST_URL, SONAR_AUTH_TOKEN
 */
'use strict';

const fs = require('fs');
const http = require('http');
const https = require('https');
const { URL } = require('url');

const REPORT_FILE = process.env.SONAR_REPORT_TASK_FILE || 'target/sonar/report-task.txt';
const POLL_MS = Number(process.env.SONAR_QG_POLL_MS || 5000);
const TIMEOUT_MS = Number(process.env.SONAR_QG_TIMEOUT_MS || 300000);

function fail(msg) {
  console.error(`ERREUR : ${msg}`);
  process.exit(1);
}

function parseReport(file) {
  if (!fs.existsSync(file)) {
    fail(`Fichier introuvable : ${file}`);
  }
  const props = {};
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const i = line.indexOf('=');
    if (i > 0) {
      props[line.slice(0, i)] = line.slice(i + 1);
    }
  }
  return props;
}

function requestJson(urlString, token) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlString);
    const lib = url.protocol === 'https:' ? https : http;
    const auth = Buffer.from(`${token}:`).toString('base64');
    const req = lib.request(
      {
        protocol: url.protocol,
        hostname: url.hostname,
        port: url.port || (url.protocol === 'https:' ? 443 : 80),
        path: `${url.pathname}${url.search}`,
        method: 'GET',
        headers: {
          Authorization: `Basic ${auth}`,
          Accept: 'application/json',
        },
      },
      (res) => {
        let body = '';
        res.on('data', (chunk) => (body += chunk));
        res.on('end', () => {
          if (res.statusCode < 200 || res.statusCode >= 300) {
            reject(new Error(`HTTP ${res.statusCode} : ${body}`));
            return;
          }
          try {
            resolve(JSON.parse(body));
          } catch (e) {
            reject(e);
          }
        });
      }
    );
    req.on('error', reject);
    req.end();
  });
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

(async () => {
  const token = process.env.SONAR_AUTH_TOKEN;
  if (!token) {
    fail('SONAR_AUTH_TOKEN manquant (withSonarQubeEnv ?)');
  }

  const report = parseReport(REPORT_FILE);
  const serverUrl = (process.env.SONAR_HOST_URL || report.serverUrl || '').replace(/\/$/, '');
  let ceTaskUrl = report.ceTaskUrl;

  if (!ceTaskUrl) {
    fail('ceTaskUrl absent dans report-task.txt');
  }
  if (serverUrl && ceTaskUrl.includes('/api/')) {
    ceTaskUrl = `${serverUrl}/api/${ceTaskUrl.split('/api/')[1]}`;
  }

  console.log(`Attente analyse SonarQube : ${ceTaskUrl}`);
  const deadline = Date.now() + TIMEOUT_MS;
  let status = 'PENDING';
  let analysisId;

  while (Date.now() < deadline) {
    const taskPayload = await requestJson(ceTaskUrl, token);
    status = taskPayload?.task?.status;
    analysisId = taskPayload?.task?.analysisId;
    console.log(`  CE status = ${status}`);
    if (status !== 'PENDING' && status !== 'IN_PROGRESS') {
      break;
    }
    await sleep(POLL_MS);
  }

  if (status === 'PENDING' || status === 'IN_PROGRESS') {
    fail(`Timeout (${TIMEOUT_MS} ms) en attendant la fin de l'analyse`);
  }
  if (status !== 'SUCCESS') {
    fail(`Analyse SonarQube en échec : ${status}`);
  }
  if (!analysisId) {
    fail('analysisId manquant après SUCCESS');
  }

  const qgUrl = `${serverUrl}/api/qualitygates/project_status?analysisId=${analysisId}`;
  const qg = await requestJson(qgUrl, token);
  const qgStatus = qg?.projectStatus?.status;
  console.log(`Quality Gate = ${qgStatus}`);

  if (qgStatus !== 'OK') {
    fail(`Quality Gate non OK : ${qgStatus}`);
  }

  console.log('Quality Gate OK');
})().catch((err) => fail(err.message || String(err)));
