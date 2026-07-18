# 🌐 Public IP Setup for Chat

This guide will help you make the chat page accessible via a public IP address.

## Option 1: Using ngrok (Recommended for Development/Testing)

### Step 1: Install ngrok
1. Download ngrok from: https://ngrok.com/download
2. Extract and add to your PATH, or use the full path

### Step 2: Start ngrok Tunnel
```powershell
# Start ngrok tunnel pointing to your backend
ngrok http 8081
```

### Step 3: Get Your Public URL
ngrok will display a public URL like:
```
Forwarding: https://abc123.ngrok-free.app -> http://localhost:8081
```

### Step 4: Update Frontend Configuration
1. Open `frontend/.env` (or create it)
2. Add your ngrok URL:
```env
VITE_API_URL=https://abc123.ngrok-free.app
```

### Step 5: Restart Frontend
```powershell
cd frontend
npm run dev
```

### Step 6: Access Chat
- Chat page: `http://localhost:5173/chat`
- Or share the ngrok URL: `https://abc123.ngrok-free.app/chat` (if frontend is also tunneled)

---

## Option 2: Using Your Actual Public IP (For Production)

### Step 1: Find Your Public IP
```powershell
# Check your public IP
curl ifconfig.me
# Or visit: https://whatismyipaddress.com
```

### Step 2: Configure Router/Firewall
1. **Port Forwarding**: Forward port 8081 to your computer's local IP
   - Router Admin Panel → Port Forwarding
   - External Port: 8081
   - Internal IP: Your computer's local IP (e.g., 192.168.1.100)
   - Internal Port: 8081

2. **Firewall**: Allow port 8081
   ```powershell
   # Windows Firewall
   netsh advfirewall firewall add rule name="Spring Boot 8081" dir=in action=allow protocol=TCP localport=8081
   ```

### Step 3: Update Configuration
The server is already configured to bind to `0.0.0.0` (all interfaces) in `application.properties`:
```properties
server.address=0.0.0.0
server.port=8081
```

### Step 4: Update CORS (if needed)
If accessing from a different domain, update `application.properties`:
```properties
app.cors.allowed-origins=http://localhost:5173,http://YOUR_PUBLIC_IP:5173,http://YOUR_DOMAIN:5173
```

Or set environment variable:
```powershell
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://YOUR_PUBLIC_IP:5173"
```

### Step 5: Update Frontend
Update `frontend/.env`:
```env
VITE_API_URL=http://YOUR_PUBLIC_IP:8081
```

### Step 6: Access Chat
- Local: `http://localhost:5173/chat`
- Public: `http://YOUR_PUBLIC_IP:5173/chat` (if frontend is also accessible)
- Or: `http://YOUR_PUBLIC_IP:8081/chat` (if serving frontend from backend)

---

## Option 3: Deploy to Cloud (Recommended for Production)

### Cloud Platforms:
- **Heroku**: Free tier available
- **AWS EC2**: Pay-as-you-go
- **DigitalOcean**: $5/month
- **Railway**: Free tier available
- **Render**: Free tier available

### Quick Deploy Example (Railway):
1. Create account at https://railway.app
2. Connect your GitHub repository
3. Railway auto-detects Spring Boot
4. Add environment variables
5. Deploy - get public URL automatically

---

## 🔒 Security Notes

1. **HTTPS**: Use HTTPS in production (ngrok provides this automatically)
2. **Authentication**: Chat endpoints are public (`/api/bot/**`), but admin endpoints require JWT
3. **CORS**: Already configured to allow all origins via `WebConfig.java`
4. **Firewall**: Only expose necessary ports

---

## 🧪 Testing Public Access

### Test Backend:
```powershell
# From another device/network
curl http://YOUR_PUBLIC_IP:8081/api/bot/message -X POST -H "Content-Type: application/json" -d '{"sessionId":"test","message":"hello"}'
```

### Test Chat Page:
1. Open browser on mobile/another device
2. Navigate to: `http://YOUR_PUBLIC_IP:5173/chat`
3. Try sending a message

---

## 📝 Quick Reference

### Current Configuration:
- **Backend Port**: 8081
- **Backend Address**: 0.0.0.0 (all interfaces)
- **Chat Endpoint**: `/api/bot/message`
- **Chat Page**: `/chat`
- **CORS**: Enabled for all origins

### Environment Variables:
```powershell
# Set CORS origins (optional)
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://YOUR_PUBLIC_IP:5173"

# Start backend
cd "C:\Liyanda project\SA!"
mvn spring-boot:run
```

---

## 🆘 Troubleshooting

### Can't access from outside network:
1. Check firewall: `netsh advfirewall firewall show rule name="Spring Boot 8081"`
2. Check router port forwarding
3. Verify server is bound to 0.0.0.0 (not localhost)
4. Check if ISP blocks incoming connections

### CORS errors:
- Already configured to allow all origins in `WebConfig.java`
- If issues persist, check browser console for specific error

### Connection refused:
- Ensure backend is running
- Check port 8081 is not blocked
- Verify `server.address=0.0.0.0` in application.properties
