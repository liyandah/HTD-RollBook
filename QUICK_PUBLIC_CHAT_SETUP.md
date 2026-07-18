# 🚀 Quick Public IP Setup for Chat

## Your Public IP: `41.220.17.138`

## Option 1: ngrok (Easiest - Recommended)

### Step 1: Install ngrok
Download from: https://ngrok.com/download

### Step 2: Start Backend Tunnel
```powershell
ngrok http 8081
```
Copy the HTTPS URL (e.g., `https://abc123.ngrok-free.app`)

### Step 3: Update Frontend Config
Create `frontend/.env`:
```env
VITE_API_URL=https://abc123.ngrok-free.app
```

### Step 4: Start Services
```powershell
# Terminal 1: Backend
cd "C:\Liyanda project\SA!"
mvn spring-boot:run

# Terminal 2: Frontend
cd "C:\Liyanda project\SA!\frontend"
npm run dev
```

### Step 5: Access Chat
- **Local**: `http://localhost:5173/chat`
- **Public Backend**: The ngrok URL handles API calls automatically

---

## Option 2: Use Your Public IP Directly

### Step 1: Configure Router Port Forwarding
1. Forward port **8081** (backend) to your computer's local IP
2. Forward port **5173** (frontend) to your computer's local IP

### Step 2: Update Frontend Config
Create `frontend/.env`:
```env
VITE_API_URL=http://41.220.17.138:8081
```

### Step 3: Start Services
```powershell
# Backend (already configured with server.address=0.0.0.0)
cd "C:\Liyanda project\SA!"
mvn spring-boot:run

# Frontend (needs to bind to 0.0.0.0)
cd "C:\Liyanda project\SA!\frontend"
npm run dev -- --host 0.0.0.0
```

### Step 4: Access Chat
- **Public**: `http://41.220.17.138:5173/chat`

---

## Option 3: ngrok for Both Frontend & Backend

### Step 1: Start Two ngrok Tunnels
```powershell
# Terminal 1: Backend
ngrok http 8081

# Terminal 2: Frontend  
ngrok http 5173
```

### Step 2: Update Frontend Config
Use the backend ngrok URL in `frontend/.env`:
```env
VITE_API_URL=https://backend-ngrok-url.ngrok-free.app
```

### Step 3: Access Chat
- **Public**: `https://frontend-ngrok-url.ngrok-free.app/chat`

---

## 🔧 Current Configuration

- **Backend**: Already configured to bind to `0.0.0.0:8081` ✅
- **Frontend**: Runs on `localhost:5173` (needs `--host 0.0.0.0` for public access)
- **Public IP**: `41.220.17.138`
- **Chat Endpoint**: `/chat`
- **API Endpoint**: `/api/bot/message` (public, no auth required)

---

## ⚠️ Important Notes

1. **Firewall**: Allow ports 8081 and 5173 in Windows Firewall
2. **Router**: Configure port forwarding if using Option 2
3. **HTTPS**: ngrok provides HTTPS automatically (recommended)
4. **Security**: Chat endpoints are public - consider adding rate limiting

---

## 🧪 Test Public Access

After setup, test from another device/network:
```powershell
# Test backend
curl http://YOUR_PUBLIC_IP:8081/api/bot/message -X POST -H "Content-Type: application/json" -d '{\"sessionId\":\"test\",\"message\":\"hello\"}'

# Or test via browser
# Open: http://YOUR_PUBLIC_IP:5173/chat
```
