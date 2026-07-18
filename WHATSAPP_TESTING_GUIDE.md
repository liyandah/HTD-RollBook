# WhatsApp Cloud API Testing Guide

## 🎯 Overview

This guide walks you through testing the WhatsApp Cloud API integration for The Salvation Army Soldier Enrollment system. The chatbot collects soldier information through a conversational flow.

---

## 📋 Prerequisites

1. ✅ **Meta Developer Account** - [developers.facebook.com](https://developers.facebook.com)
2. ✅ **WhatsApp Business App** - Created in Meta Developer Console
3. ✅ **Test Phone Number** - Provided by Meta (or your own verified number)
4. ✅ **ngrok** (for local testing) - [ngrok.com](https://ngrok.com)
5. ✅ **PostgreSQL Running** - Database must be accessible

---

## 🚀 Quick Start (5 Minutes)

### Step 1: Get Your WhatsApp Credentials

1. Go to [Meta Developer Console](https://developers.facebook.com/apps/)
2. Select your WhatsApp Business App (or create one)
3. Navigate to **WhatsApp > API Setup**
4. Copy these values:
   - **Temporary Access Token** (valid 24 hours)
   - **Phone Number ID** (Test number provided by Meta)
   - **Test WhatsApp Number** (To send messages to)

### Step 2: Configure Environment Variables

Create a `.env` file or set environment variables:

```bash
# Copy from env.example
cp env.example .env

# Edit .env with your values
META_ACCESS_TOKEN=EAAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
META_PHONE_NUMBER_ID=952859751239210
META_VERIFY_TOKEN=sa_verify_123
```

**⚠️ IMPORTANT:** Never commit `META_ACCESS_TOKEN` to Git!

### Step 3: Start ngrok (Local Tunnel)

```bash
# Install ngrok if you haven't
# Download from https://ngrok.com/download

# Start ngrok on port 8081 (backend port)
ngrok http 8081
```

You'll see output like:
```
Forwarding  https://abc123def456.ngrok.io -> http://localhost:8081
```

**Copy the HTTPS URL** - this is your webhook URL.

### Step 4: Configure Meta Webhook

1. In Meta Developer Console, go to **WhatsApp > Configuration**
2. Click **Edit** next to Webhook
3. Enter:
   - **Callback URL**: `https://abc123def456.ngrok.io/webhooks/whatsapp`
   - **Verify Token**: `sa_verify_123` (must match `META_VERIFY_TOKEN`)
4. Click **Verify and Save**
5. Subscribe to these webhook fields:
   - ✅ `messages`
   - ✅ `message_status` (optional)

### Step 5: Start the Backend

```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using start script
./start-backend.bat

# Option 3: Using IDE
# Run WhatsAppDataCollectionApplication.java
```

Wait for:
```
✅ Started WhatsAppDataCollectionApplication
✅ Tomcat started on port(s): 8081
✅ Created uploads directory: uploads/
```

### Step 6: Send a Test Message

1. Open WhatsApp on your phone
2. Send a message to the **Meta Test Number** (provided in Meta Console)
3. Type: `hi` or `hello`

**Expected Response:**
```
Welcome to The Salvation Army Soldier Enrollment! 🎯

I'll guide you through collecting your information. Type 'help' anytime for assistance or 'restart' to begin again.

Let's begin! What is your current Corps name?
```

---

## 🔄 Complete Conversation Flow

### States and Prompts

| State | User Sends | Bot Responds |
|-------|------------|--------------|
| **START** | `hi` / `hello` | Welcome message + asks for Corps Name |
| **ASK_CORPS_NAME** | Text (Corps name) | Asks for Enrolled Corps |
| **ASK_ENROLLED_CORPS** | Text (Enrolled corps) | Asks for First Name |
| **ASK_FIRST_NAME** | Text (First name) | Asks for Family Name |
| **ASK_FAMILY_NAME** | Text (Family name) | Asks for Date of Birth (YYYY-MM-DD) |
| **ASK_DOB** | Text (1990-05-15) | Asks for ID Number (if age ≥ 16) or Photo |
| **ASK_ID_NUMBER** | Text (ID number) | Asks for Person Photo |
| **ASK_PERSON_IMAGE** | Image (selfie) | Asks for Certificate Photo |
| **ASK_CERT_IMAGE** | Image (certificate) | Asks for Favorite Song |
| **ASK_SONG** | Text (favorite song) | Asks for Favorite Bible Verse |
| **ASK_BIBLE_VERSE** | Text (bible verse) | Thank you message + COMPLETE |
| **COMPLETE** | Any message | "Already completed" message |

### Special Commands

- **`restart`** - Clears current progress and starts over
- **`help`** - Shows help message with commands and privacy note

---

## 📸 Testing Image Uploads

### Test Person Image:
1. Reach the `ASK_PERSON_IMAGE` state
2. Send any photo from your WhatsApp
3. Bot downloads and saves to `uploads/person_27xxxxxxxxx_uuid.jpg`
4. Moves to next state

### Test Certificate Image:
1. Reach the `ASK_CERT_IMAGE` state
2. Send certificate photo
3. Bot downloads and saves to `uploads/cert_27xxxxxxxxx_uuid.jpg`
4. Completes enrollment

### Verify Upload:
```bash
# Check uploads directory
ls -la uploads/

# You should see files like:
person_27821234567_abc123.jpg
cert_27821234567_def456.jpg
```

---

## 🧪 Test Scenarios

### Scenario 1: Happy Path (Age ≥ 16)
```
You: hi
Bot: Welcome message...

You: Cape Town Central Corps
Bot: Great! What Corps were you enrolled in?

You: Cape Town Central Corps
Bot: Perfect! What is your first name?

You: John
Bot: Thank you! What is your family name?

You: Doe
Bot: Got it! What is your date of birth? (YYYY-MM-DD)

You: 1990-05-15
Bot: Thank you! Please provide your ID number.

You: 9005155555088
Bot: Perfect! Now, please send a photo of yourself.

[Send selfie photo]
Bot: Great! Now please send a photo of your enrollment certificate.

[Send certificate photo]
Bot: Excellent! What is your favorite Salvation Army song?

You: Amazing Grace
Bot: Wonderful! Finally, what is your favorite Bible verse?

You: John 3:16
Bot: 🎉 Thank you! Your enrollment information has been successfully collected...
```

### Scenario 2: Young Person (Age < 16)
```
You: 2010-01-15  (at DOB step)
Bot: Thank you! Now, please send a photo of yourself.
(Skips ID number step)
```

### Scenario 3: Validation Errors
```
# Invalid date format
You: 15-05-1990
Bot: Invalid date format. Please provide YYYY-MM-DD...

# Future date
You: 2030-01-01
Bot: Invalid date format or future date...

# Text when expecting image
You: hello (at ASK_PERSON_IMAGE state)
Bot: Please send an image of yourself.

# Image when expecting text
[Send image] (at ASK_FIRST_NAME state)
Bot: Please provide your first name as text.
```

### Scenario 4: Restart Flow
```
You: restart (at any point)
Bot: Restarting enrollment process...
Bot: Welcome message... (starts fresh)
```

### Scenario 5: Help Command
```
You: help
Bot: 📋 *Salvation Army Enrollment Help*
     
     Commands:
     • Type 'restart' to start over
     • Type 'help' to see this message
     
     Privacy Note: Your information is securely stored...
```

---

## 🔍 Debugging & Monitoring

### Check Backend Logs

```bash
# Watch logs in real-time
tail -f logs/spring-boot.log

# Or check console output
```

**Look for:**
```
INFO  - Webhook verification request: mode=subscribe, token=sa_verify_123
INFO  - Webhook verified successfully
INFO  - Received webhook request: {...}
INFO  - Processing message from 27821234567: type=text, text=hello
INFO  - Sent message to 27821234567: response code 200
INFO  - Saved image: uploads/person_27821234567_abc123.jpg
```

### Check Database Records

```sql
-- Check conversation state
SELECT * FROM conversations WHERE wa_id = '27821234567';

-- Check soldier record progress
SELECT * FROM soldier_records 
WHERE wa_id = '27821234567' AND status = 'IN_PROGRESS';

-- Check completed records
SELECT * FROM soldier_records WHERE status = 'COMPLETE';
```

### Check Uploaded Files

```bash
# List all uploaded images
ls -lh uploads/

# Check file sizes (should be > 0)
du -sh uploads/*

# View recent uploads
ls -lt uploads/ | head -10
```

### ngrok Inspector

1. Open http://127.0.0.1:4040 (ngrok web interface)
2. View all webhook requests/responses
3. See payload structure
4. Replay requests for debugging

---

## ⚠️ Common Issues & Solutions

### Issue 1: Webhook Verification Failed (403)

**Symptoms:**
- Meta shows "Verification Failed"
- Backend logs: "Webhook verification failed"

**Solutions:**
1. Check `META_VERIFY_TOKEN` matches in both Meta Console and backend
2. Ensure ngrok is running and URL is correct
3. Check backend is running on port 8081
4. Try verification again (can take 30 seconds)

### Issue 2: No Response from Bot

**Symptoms:**
- Send message but bot doesn't reply
- No logs in backend

**Solutions:**
1. Check webhook is subscribed to `messages` field in Meta Console
2. Verify ngrok tunnel is active
3. Check backend logs for errors
4. Ensure `META_ACCESS_TOKEN` is valid (regenerate if expired)
5. Check Meta App is not in Development Mode restrictions

### Issue 3: Image Upload Fails

**Symptoms:**
- Bot says "Failed to download image"
- No files in uploads/ directory

**Solutions:**
1. Check `uploads/` directory exists and is writable
2. Verify `META_ACCESS_TOKEN` is correct
3. Check Meta API rate limits
4. Look for network errors in logs
5. Try smaller image (< 5MB)

### Issue 4: Invalid Date Format

**Symptoms:**
- Bot keeps asking for DOB even after sending

**Solutions:**
1. Use exact format: `YYYY-MM-DD` (e.g., `1990-05-15`)
2. Not `DD/MM/YYYY` or `MM/DD/YYYY`
3. Not future dates
4. Check for typos (0 vs O, 1 vs l)

### Issue 5: Webhook Not Receiving Messages

**Check:**
```bash
# 1. Is ngrok running?
curl https://abc123def456.ngrok.io/webhooks/whatsapp

# 2. Is backend healthy?
curl http://localhost:8081/actuator/health

# 3. Test webhook manually
curl -X POST https://abc123def456.ngrok.io/webhooks/whatsapp \
  -H "Content-Type: application/json" \
  -d '{test webhook}'
```

---

## 📊 Meta Developer Console Dashboard

### Verify Webhook Status

1. Go to WhatsApp > Configuration
2. Check webhook status: **✅ Connected**
3. View webhook subscriptions: **✅ messages**

### Check API Requests

1. Go to WhatsApp > Analytics (if available)
2. View message delivery rates
3. Check for errors

### Test Number Conversations

1. Go to WhatsApp > Getting Started
2. View "Conversation with Test Number"
3. See message history

---

## 🔐 Security Notes

1. **Access Token Expiry**: Temporary tokens expire in 24 hours
   - Get new token from Meta Console
   - Update `META_ACCESS_TOKEN` environment variable
   - Restart backend

2. **Webhook Endpoint**: `/webhooks/whatsapp` is public (no JWT required)
   - This is intentional for Meta to send messages
   - Verified using `META_VERIFY_TOKEN`

3. **Sensitive Data**: All PII is stored securely
   - ID numbers encrypted at rest (planned)
   - Images stored with UUID filenames
   - Conversations tracked by waId only

4. **Rate Limits**: Meta has API rate limits
   - Test number: ~50 messages/day
   - Production: Higher limits after approval

---

## 🚀 Production Deployment

### 1. Get Permanent Access Token

Temporary tokens expire in 24 hours. For production:

1. Complete Meta Business Verification
2. Request WhatsApp Business API access
3. Generate System User Token (never expires)
4. Store in secure secrets manager (AWS Secrets Manager, Azure Key Vault)

### 2. Use Production WhatsApp Number

1. Verify your business phone number with Meta
2. Update `META_PHONE_NUMBER_ID`
3. Update webhook to production URL

### 3. Deploy with Proper Domain

Instead of ngrok:
```
https://api.salvationarmy.org/webhooks/whatsapp
```

### 4. Enable @Async Processing

Replace the `new Thread()` hack with:
```java
@Async
@Transactional
public void processMessage(String waId, ...) {
    // Process asynchronously with proper thread pool
}
```

### 5. Add Monitoring

- CloudWatch/Azure Monitor for logs
- Metrics for message processing time
- Alerts for webhook failures
- Dashboard for enrollment stats

---

## 📞 Support

### If You Get Stuck:

1. **Check Logs**: Always start with backend logs
2. **ngrok Inspector**: View webhook payloads
3. **Database**: Check conversation state
4. **Meta Console**: Verify webhook status

### Useful Commands:

```bash
# Restart everything
./stop-all.bat && ./start-all.bat

# Check database
psql -U postgres -d salvation_army_db

# View logs
tail -f logs/spring-boot.log

# Test webhook locally
curl -X POST http://localhost:8081/webhooks/whatsapp \
  -H "Content-Type: application/json" \
  -d @test-webhook-payload.json
```

---

## ✅ Success Checklist

Before considering testing complete, verify:

- [ ] Webhook verified successfully in Meta Console
- [ ] Bot responds to "hi" message
- [ ] Complete full conversation flow (all states)
- [ ] Images upload and save to `uploads/` directory
- [ ] `restart` command works and clears data
- [ ] `help` command shows help message
- [ ] Date validation works (rejects future dates)
- [ ] Age < 16 skips ID number step
- [ ] Age ≥ 16 asks for ID number
- [ ] Database has conversation record
- [ ] Database has soldier_record with status COMPLETE
- [ ] All fields populated correctly
- [ ] Image paths stored correctly in DB
- [ ] Can view images via `/uploads/{filename}` endpoint
- [ ] ngrok logs show all webhook calls
- [ ] Backend logs show no errors

---

**🎉 You're ready to test! Send "hi" to your WhatsApp test number and watch the magic happen!**

---

## 📚 Additional Resources

- [Meta WhatsApp Cloud API Docs](https://developers.facebook.com/docs/whatsapp/cloud-api)
- [WhatsApp Business API Overview](https://developers.facebook.com/docs/whatsapp/business-management-api)
- [ngrok Documentation](https://ngrok.com/docs)
- [Spring Boot Async Processing](https://spring.io/guides/gs/async-method/)

---

**Last Updated:** 2026-01-14  
**Tested With:** WhatsApp Cloud API v19.0, Spring Boot 3.2.1, Java 17





