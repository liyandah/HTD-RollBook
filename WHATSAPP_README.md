# WhatsApp Cloud API Integration

## 📱 Overview

The Salvation Army Soldier Enrollment system integrates with **WhatsApp Cloud API (v19.0)** to collect soldier information through a conversational chatbot.

Users can complete the entire enrollment process via WhatsApp, including uploading photos of themselves and their enrollment certificates.

---

## 🎯 Features

- ✅ **Conversational Enrollment** - Step-by-step guided flow
- ✅ **Image Uploads** - Accept photos via WhatsApp
- ✅ **Smart Validation** - Validates dates, ages, and input types
- ✅ **Age-Based Logic** - Skips ID number for users under 16
- ✅ **Restart Capability** - Users can restart anytime
- ✅ **Help System** - Built-in help command
- ✅ **Secure** - All tokens from environment variables
- ✅ **Persistent** - Saves to PostgreSQL database

---

## 📖 Documentation

### Quick Start (5 Minutes)
**👉 [WHATSAPP_QUICKSTART.md](WHATSAPP_QUICKSTART.md)**
- Fastest way to get started
- Essential steps only
- Perfect for first-time testing

### Complete Testing Guide
**👉 [WHATSAPP_TESTING_GUIDE.md](WHATSAPP_TESTING_GUIDE.md)**
- Detailed step-by-step instructions
- All test scenarios
- Troubleshooting guide
- Production deployment tips

### Technical Implementation
**👉 [WHATSAPP_INTEGRATION_SUMMARY.md](WHATSAPP_INTEGRATION_SUMMARY.md)**
- Architecture overview
- Code structure
- API endpoints
- Database schema
- Security implementation

---

## 🚀 Quick Test

```bash
# 1. Set environment variables
export META_ACCESS_TOKEN="your_token"
export META_PHONE_NUMBER_ID="952859751239210"

# 2. Start ngrok
ngrok http 8081

# 3. Configure webhook in Meta Console
# URL: https://your-ngrok-url.app/webhooks/whatsapp
# Token: sa_verify_123

# 4. Start backend
mvn spring-boot:run

# 5. Send "hi" to WhatsApp test number
```

---

## 🔄 Conversation Flow

```
User sends "hi"
  ↓
Bot asks for Corps Name
  ↓
Bot asks for Enrolled Corps
  ↓
Bot asks for First Name
  ↓
Bot asks for Family Name
  ↓
Bot asks for Date of Birth (YYYY-MM-DD)
  ↓
Bot calculates age
  ├─ If age ≥ 16: Bot asks for ID Number
  └─ If age < 16: Skip to photo
  ↓
Bot asks for Person Photo
  ↓
Bot asks for Certificate Photo
  ↓
Bot asks for Favorite Song
  ↓
Bot asks for Favorite Bible Verse
  ↓
Enrollment Complete! ✅
```

---

## 🌐 Endpoints

### Webhook (Public)

```
GET  /webhooks/whatsapp  - Webhook verification (Meta)
POST /webhooks/whatsapp  - Receive messages (Meta)
```

### WhatsApp Graph API (Outbound)

```
POST https://graph.facebook.com/v19.0/{phone_id}/messages
GET  https://graph.facebook.com/v19.0/{media_id}
```

---

## 🗄️ Database

### conversations table
Tracks conversation state per WhatsApp user

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| wa_id | VARCHAR(50) | WhatsApp ID (unique) |
| state | VARCHAR(50) | Current state in flow |
| last_message_at | TIMESTAMP | Last message time |

### soldier_records table
Stores collected enrollment data

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| wa_id | VARCHAR(50) | WhatsApp ID |
| first_name | VARCHAR(100) | First name |
| family_name | VARCHAR(100) | Family name |
| dob | DATE | Date of birth |
| age | INTEGER | Calculated age |
| person_image_path | VARCHAR(500) | Path to person photo |
| cert_image_path | VARCHAR(500) | Path to certificate |
| status | VARCHAR(50) | IN_PROGRESS / COMPLETE |
| ... | ... | (other fields) |

---

## 🔒 Security

- ✅ **No Hardcoded Secrets** - All tokens from environment
- ✅ **Webhook Verification** - Uses `META_VERIFY_TOKEN`
- ✅ **Public Webhook** - Accessible to Meta only
- ✅ **Protected Admin Routes** - JWT authentication
- ✅ **Secure Image Storage** - UUID filenames
- ✅ **.env Gitignored** - Never commit secrets

---

## 📂 Files

```
src/main/java/.../
├── controller/WhatsAppWebhookController.java  # Webhook endpoint
├── service/WhatsAppService.java               # State machine + API
├── entity/Conversation.java                   # Conversation model
├── entity/SoldierRecord.java                  # Record model
└── config/StartupConfig.java                  # Upload dir setup

WHATSAPP_QUICKSTART.md           # ⚡ Quick start (5 min)
WHATSAPP_TESTING_GUIDE.md        # 📖 Complete guide
WHATSAPP_INTEGRATION_SUMMARY.md  # 🔧 Technical docs

env.example                      # Template for secrets
test-webhook-payload.json        # Sample webhook data
uploads/                         # Image storage (auto-created)
```

---

## 🧪 Testing

### Test Commands

```bash
# Test webhook verification
curl "http://localhost:8081/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=sa_verify_123&hub.challenge=test"

# Test message reception
curl -X POST http://localhost:8081/webhooks/whatsapp \
  -H "Content-Type: application/json" \
  -d @test-webhook-payload.json

# Check conversation state
psql -U postgres -d salvation_army_db \
  -c "SELECT * FROM conversations ORDER BY last_message_at DESC LIMIT 5;"
```

---

## 💡 Tips

1. **Temporary Tokens Expire** - Get new token from Meta every 24h
2. **Use ngrok Inspector** - View webhooks at http://127.0.0.1:4040
3. **Test Locally First** - Use test payloads before live testing
4. **Check Logs** - Watch `logs/spring-boot.log` for debugging
5. **Database State** - Query `conversations` table to see current state

---

## 🐛 Common Issues

### Bot doesn't respond
- Check webhook is subscribed to `messages` in Meta Console
- Verify ngrok is running and URL is correct
- Check `META_ACCESS_TOKEN` is valid (not expired)

### Image upload fails
- Ensure `uploads/` directory exists
- Check disk space available
- Verify `META_ACCESS_TOKEN` has correct permissions

### Webhook verification failed
- Check `META_VERIFY_TOKEN` matches in both places
- Ensure callback URL is correct HTTPS URL from ngrok
- Wait 30 seconds and try again

---

## 🚀 Production Deployment

For production use:

1. **Get Permanent Token** - System User Token (never expires)
2. **Use Real Domain** - Replace ngrok with `https://api.your-domain.com`
3. **Enable @Async** - Replace `new Thread()` with Spring `@Async`
4. **Add Monitoring** - CloudWatch, Datadog, etc.
5. **Set Up Alerts** - For webhook failures, API errors
6. **Business Verification** - Complete Meta Business Verification

See **WHATSAPP_TESTING_GUIDE.md** for complete production deployment guide.

---

## 📞 Support

- **Technical Issues**: Check backend logs and ngrok inspector
- **Meta API Issues**: Visit Meta Developer Support
- **Database Issues**: Check PostgreSQL logs and connections

---

## 📚 Resources

- [Meta WhatsApp Cloud API Docs](https://developers.facebook.com/docs/whatsapp/cloud-api)
- [WhatsApp Business API](https://developers.facebook.com/docs/whatsapp/business-management-api)
- [ngrok Documentation](https://ngrok.com/docs)

---

**🎉 Ready to start? See [WHATSAPP_QUICKSTART.md](WHATSAPP_QUICKSTART.md) for 5-minute setup!**

---

**Last Updated:** 2026-01-14  
**API Version:** WhatsApp Cloud API v19.0  
**Framework:** Spring Boot 3.2.1, Java 17





