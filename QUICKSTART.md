# Quick Start Guide

## 🚀 Get Running in 5 Minutes

### 1. Start Database (30 seconds)
```bash
docker-compose up -d
```

### 2. Configure Backend (1 minute)
```bash
# Copy and edit environment file
cp env.example .env

# Edit .env - REQUIRED changes:
# - META_VERIFY_TOKEN: Set a custom token (e.g., "my_token_123")
# - META_ACCESS_TOKEN: Get from Meta WhatsApp Business
# - META_PHONE_NUMBER_ID: Get from Meta WhatsApp Business
# - JWT_SECRET: Change to a long random string
```

### 3. Start Backend (1 minute)
```bash
mvn spring-boot:run
```

Wait for: `Started WhatsAppDataCollectionApplication`

### 4. Start Frontend (2 minutes)
```bash
cd frontend
npm install
echo "VITE_API_URL=http://localhost:8080" > .env
npm run dev
```

### 5. Access Dashboard
Open: http://localhost:5173

Login:
- Username: `admin`
- Password: `admin123`

---

## 📱 WhatsApp Testing Setup

### Quick ngrok Setup
```bash
# Install ngrok (if not installed)
# Visit: https://ngrok.com/download

# Start tunnel
ngrok http 8080

# Copy the HTTPS URL (e.g., https://abc123.ngrok-free.app)
```

### Configure Meta WhatsApp
1. Go to: https://developers.facebook.com/
2. Your App > WhatsApp > Configuration
3. Click "Edit" on Webhook
4. Enter:
   - Callback URL: `https://YOUR-NGROK-URL/webhooks/whatsapp`
   - Verify Token: (same as META_VERIFY_TOKEN in .env)
5. Subscribe to: `messages`
6. Click "Verify and Save"

### Test It
1. Send message to your WhatsApp Business number
2. Bot should respond with enrollment welcome
3. Follow prompts to test flow
4. Check dashboard for new records

---

## 🔍 Verify Everything Works

### Backend Health Check
```bash
curl http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'
```

Should return: `{"token":"...", "type":"Bearer", "username":"admin"}`

### Database Check
```bash
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db -c "SELECT * FROM conversations;"
```

### Frontend Check
Open browser to: http://localhost:5173
Should see login page with Salvation Army branding

---

## 📊 Key URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger Docs | http://localhost:8080/swagger-ui.html |
| Database | localhost:5432 |

---

## 🆘 Quick Fixes

### "Port already in use"
```bash
# Backend (8080)
lsof -ti:8080 | xargs kill -9

# Frontend (5173)
lsof -ti:5173 | xargs kill -9

# Database (5432)
docker-compose down
docker-compose up -d
```

### "Database connection failed"
```bash
docker-compose restart
# Wait 10 seconds, then restart backend
```

### "Can't login to dashboard"
Check:
1. Backend is running (check http://localhost:8080/swagger-ui.html)
2. Credentials are correct (default: admin/admin123)
3. Browser console for errors (F12)

### "Webhook verification failed"
Check:
1. ngrok is running and HTTPS URL is correct
2. META_VERIFY_TOKEN in .env matches Meta configuration
3. Backend logs show verification request

---

## 📝 Conversation Flow Test

Send these messages to test:

1. Any text → Bot starts enrollment
2. `My Local Corps` → Corps name
3. `Central Corps` → Enrolled corps
4. `John` → First name
5. `Doe` → Family name
6. `1990-05-15` → Date of birth
7. `1234567890123` → ID number (if 16+)
8. [Send image] → Person photo
9. [Send image] → Certificate photo
10. `Amazing Grace` → Favorite song
11. `John 3:16` → Favorite verse
12. ✅ Complete!

Special commands:
- `help` → Show help
- `restart` → Start over

---

## 🎉 You're Ready!

Your system is now running:
- ✅ WhatsApp bot collecting data
- ✅ Dashboard showing records
- ✅ Images stored locally
- ✅ Export to CSV working

For detailed documentation, see [README.md](README.md)






