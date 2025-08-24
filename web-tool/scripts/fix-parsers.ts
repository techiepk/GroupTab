#!/usr/bin/env node

import { query } from "@anthropic-ai/claude-code";
import postgres from 'postgres';
import { config } from 'dotenv';

// Load environment variables
config({ path: "./.env.local" });

const sql = postgres(process.env.DATABASE_URL!);

async function fixParsers() {
  console.log('🚀 Starting parser fix process...\n');

  // Fetch failed submissions with bank info
  const failures = await sql`
    SELECT 
      s.*,
      b.id as bank_id,
      b.code as bank_code,
      b.name as bank_name
    FROM submissions s
    LEFT JOIN banks b ON s.detected_bank_id = b.id
    WHERE s.status = 'pending'
      AND s.is_parseable = false
      AND s.user_expected IS NOT NULL
    ORDER BY s.created_at DESC
    LIMIT 1
  `;

  if (!failures || failures.length === 0) {
    console.log('No failed submissions to process');
    await sql.end();
    return;
  }

  console.log(`Found ${failures.length} failed submissions\n`);

  // Group by bank
  const grouped: Record<string, any[]> = {};
  failures.forEach(f => {
    const bankCode = f.bank_code || 'unknown';
    if (!grouped[bankCode]) grouped[bankCode] = [];
    grouped[bankCode].push(f);
  });

  console.log(`Processing ${Object.keys(grouped).length} banks...\n`);

  // Process each bank
  for (const [bankCode, submissions] of Object.entries(grouped)) {
    const bankName = submissions[0]?.bank_name || bankCode;
    console.log(`\n📦 Processing ${bankName} (${submissions.length} failures)`);

    // Show the first submission details
    console.log('\n  Sample SMS to fix:');
    console.log(`  SMS: "${submissions[0].sms_body}"`);
    console.log(`  Sender: ${submissions[0].sender}`);
    console.log(`  Expected Amount: ${submissions[0].user_expected?.amount}`);
    console.log(`  Expected Merchant: ${submissions[0].user_expected?.merchant}`);
    console.log('\n');

    const prompt = `
You need to fix the ${bankCode} bank parser to handle these failed SMS messages.

These SMS messages are currently failing to parse. Users have provided the correct values that should be extracted:

${submissions.slice(0, 20).map((s, i) => `
Sample ${i + 1}:
SMS: "${s.sms_body}"
Sender: ${s.sender}
What the user expects:
- Amount: ${s.user_expected?.amount || 'not provided'}
- Merchant: ${s.user_expected?.merchant || 'not provided'}
- Transaction Type: ${s.user_expected?.type || 'not provided'}

What the parser currently extracts:
- Amount: ${s.parsed_data?.amount || 'nothing'}
- Merchant: ${s.parsed_data?.merchant || 'nothing'}
`).join('\n')}

Your task:
1. First, read the existing parser files to understand current patterns:
   - Kotlin: /Users/sarim/AndroidStudioProjects/pennywisecompose/app/src/main/java/com/pennywiseai/tracker/data/parser/bank/${bankCode}BankParser.kt
   - TypeScript: /Users/sarim/AndroidStudioProjects/pennywisecompose/web-tool/lib/parsers/banks/${bankCode.toLowerCase()}.ts

2. Analyze why these SMS messages are failing (missing patterns)

3. Add new regex patterns to both parsers to handle these cases:
   - Add patterns at the beginning of the relevant extract methods for higher priority
   - Include comments explaining what each new pattern catches
   - Make sure patterns are specific enough to avoid false positives

4. Ensure both Kotlin and TypeScript implementations stay synchronized

5. DO NOT remove or modify existing patterns - only add new ones

6. Test your patterns mentally against the samples to ensure they would extract the correct values

7. Always fix both parsers.

Please fix both parser files now.`;

    // Let Claude fix the parsers
    console.log('  🤖 Claude is analyzing and fixing the parsers...');

    for await (const message of query({
      prompt, options: {
        permissionMode: "acceptEdits"
      }
    })) {
      if (message.type === "result") {
        console.log('  ✅ Parser fix complete');
        console.log('  Duration:', message.duration_ms, 'ms');
        console.log('  Total cost:', message.total_cost_usd, 'USD');
      } else if (message.type === "assistant") {
        console.log('  🤖 Claude:', message.message);
      } else if (message.type === "user") {
        console.log('  👤 User message received');
      } else {
        console.log('  📝 Message type:', message.type);
        console.log('     Content:', JSON.stringify(message, null, 2));
      }
    }

    // Mark submissions as implemented
    const submissionIds = submissions.map(s => s.id);
    // await sql`
    //   UPDATE submissions
    //   SET 
    //     status = 'implemented',
    //     reviewed_at = ${new Date().toISOString()},
    //     admin_notes = 'Fixed by automated parser update system'
    //   WHERE id = ANY(${submissionIds})
    // `;

    console.log(`  📊 Marked ${submissionIds.length} submissions as implemented`);
  }

  console.log('\n✅ All parsers updated!');
  console.log('\nTo create a PR, run:');
  console.log('  git add -A');
  console.log('  git commit -m "fix: update bank parsers for failed SMS patterns"');
  console.log('  git push');
  console.log('  gh pr create');

  await sql.end();
}

// Run the script
fixParsers().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
