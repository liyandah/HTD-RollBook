# WhatsApp Testing - Quick Start (5 Minutes)

## ⚡ Fastest Way to Test WhatsApp Integration

### Prerequisites
- ✅ Meta Developer Account
- ✅ WhatsApp Business App created
- ✅ ngrok installed
- ✅ Backend compiled (`mvn compile`)

---

## 🚀 Step-by-Step

### 1️⃣ Get Credentials (Meta Console)

Visit: https://developers.facebook.com/apps/

1. **Select your app** → WhatsApp → API Setup
2. **Copy these 3 values:**

```
Temporary Access Token:  EAAxxxxxxxxxxxxxxxxxxxxxxxxx
Phone Number ID:         952859751239210  
Test Number To:          +1 555 025 3393 (or similar)
```

---

### 2️⃣ Set Environment Variables

**Windows (PowerShell):**
```powershell
$env:META_ACCESS_TOKEN="EAAxxxxxxxxxxxxxxxxxxxxxxx"
$env:META_PHONE_NUMBER_ID="952859751239210"
$env:META_VERIFY_TOKEN="sa_verify_123"
```

**Linux/Mac:**
```bash
export META_ACCESS_TOKEN="EAAxxxxxxxxxxxxxxxxxxxxxxx"
export META_PHONE_NUMBER_ID="952859751239210"
export META_VERIFY_TOKEN="sa_verify_123"
```

**Or create .env file:**
```bash
cp env.example .env
# Edit .env with your values
```

---

### 3️⃣ Start ngrok

```bash
ngrok http 8081
```

**Copy the HTTPS URL:**
```
https://abc123def456.ngrok-free.app
```

---

### 4️⃣ Configure Meta Webhook

1. **Meta Console** → WhatsApp → Configuration
2. **Click "Edit"** next to Webhook
3. **Enter:**
   - Callback URL: `https://abc123def456.ngrok-free.app/webhooks/whatsapp`
   - Verify Token: `sa_verify_123`
4. **Click "Verify and Save"**
5. **Subscribe to:** ✅ `messages`

---

### 5️⃣ Start Backend

```bash
mvn spring-boot:run
```

**Wait for:**
```
✅ Started WhatsAppDataCollectionApplication
✅ Tomcat started on port(s): 8081
✅ Created uploads directory
```

---

### 6️⃣ Send Test Message

1. **Open WhatsApp** on your phone
2. **Send to:** The Meta test number
3. **Type:** `hi`

**Expected Response:**
```
Welcome to The Salvation Army Soldier Enrollment! 🎯

I'll guide you through collecting your information. 
Type 'help' anytime for assistance or 'restart' to begin again.

Let's begin! What is your current Corps name?
```

---

## ✅ Success!

If you got the welcome message, your WhatsApp integration is working! 🎉

### Next Steps:

1. **Complete the flow** - Answer all questions
2. **Test image upload** - Send a photo when asked
3. **Check database** - See conversation and soldier_record
4. **Test restart** - Type `restart` anytime
5. **Test help** - Type `help`

---

## 🔍 Troubleshooting

### Bot doesn't respond?

```bash
# Check ngrok is running
curl https://abc123def456.ngrok-free.app/webhooks/whatsapp

# Check backend logs
tail -f logs/spring-boot.log

# Test webhook locally
curl -X POST http://localhost:8081/webhooks/whatsapp \
  -H "Content-Type: application/json" \
  -d @test-webhook-payload.json
```

### Webhook verification failed?

1. Check `META_VERIFY_TOKEN` matches in both places
2. Ensure ngrok URL is correct
3. Wait 30 seconds and try again

### Access token expired?

Temporary tokens expire in 24 hours. Get a new one from Meta Console.

---

## 📚 Full Documentation

For complete testing guide, see:
- **WHATSAPP_TESTING_GUIDE.md** - Complete step-by-step testing
- **WHATSAPP_INTEGRATION_SUMMARY.md** - Technical implementation details

---

**🎯 You're ready! Send "hi" and start testing!**





