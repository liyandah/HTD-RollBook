# Troubleshooting Guide

Common issues and their solutions for the Salvation Army WhatsApp Data Collection System.

## 🔴 Backend Issues

### Backend won't start

**Error**: `Port 8080 already in use`

**Solution**:
```bash
# Find what's using port 8080
lsof -i :8080

# Kill the process
lsof -ti:8080 | xargs kill -9

# Try starting again
mvn spring-boot:run
```

---

**Error**: `Unable to connect to database`

**Solution**:
```bash
# Check if PostgreSQL is running
docker ps

# If not running, start it
docker-compose up -d

# Wait 10 seconds
sleep 10

# Check database logs
docker-compose logs postgres

# Test connection
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db -c "SELECT 1;"
```

---

**Error**: `Flyway migration failed`

**Solution**:
```bash
# Check migration status
mvn flyway:info

# Repair if needed
mvn flyway:repair

# Or reset database (⚠️ destroys all data)
docker-compose down -v
docker-compose up -d
sleep 10
mvn spring-boot:run
```

---

**Error**: `JWT secret too short`

**Solution**:
```bash
# Generate a new secret (at least 256 bits)
openssl rand -base64 64

# Add to .env file
JWT_SECRET=your_generated_secret_here
```

## 🔵 Frontend Issues

### Frontend won't start

**Error**: `Port 5173 already in use`

**Solution**:
```bash
# Kill process on 5173
lsof -ti:5173 | xargs kill -9

# Or use different port
cd frontend
npm run dev -- --port 3000
```

---

**Error**: `Cannot find module` or dependency errors

**Solution**:
```bash
cd frontend

# Remove node_modules and package-lock
rm -rf node_modules package-lock.json

# Reinstall
npm install

# Try again
npm run dev
```

---

**Error**: `Network Error` when logging in

**Solution**:
1. Check backend is running: `curl http://localhost:8080/api/auth/login`
2. Check `.env` file in frontend folder:
   ```
   VITE_API_URL=http://localhost:8080
   ```
3. Check browser console (F12) for CORS errors
4. Verify CORS configuration in backend `application.properties`

## 📱 WhatsApp Issues

### Webhook verification fails

**Error**: `403 Forbidden` or verification fails in Meta

**Checklist**:
1. ✅ ngrok is running: `ngrok http 8080`
2. ✅ Backend is running and accessible via ngrok URL
3. ✅ `META_VERIFY_TOKEN` in `.env` matches token in Meta dashboard
4. ✅ Webhook URL format: `https://your-ngrok-url.ngrok-free.app/webhooks/whatsapp`
5. ✅ Using HTTPS (not HTTP)

**Test verification**:
```bash
# Replace YOUR_TOKEN with your actual token
curl "http://localhost:8080/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=YOUR_TOKEN&hub.challenge=test123"

# Should return: test123
```

---

### Messages not being received

**Checklist**:
1. ✅ Webhook is verified in Meta dashboard (green checkmark)
2. ✅ `messages` event is subscribed
3. ✅ Test number is added to WhatsApp Business account
4. ✅ Backend logs show incoming requests

**Debug**:
```bash
# Watch backend logs
tail -f backend.log | grep "Received webhook"

# Send test message via WhatsApp
# Check if webhook request appears in logs

# Check Meta webhook logs
# Go to Meta Dashboard > WhatsApp > Configuration > Webhook logs
```

---

### Bot doesn't respond

**Possible causes**:

1. **Invalid access token**
   - Check `META_ACCESS_TOKEN` in `.env`
   - Verify token hasn't expired in Meta dashboard
   - Generate new token if needed

2. **Wrong phone number ID**
   - Check `META_PHONE_NUMBER_ID` in `.env`
   - Find correct ID in Meta Dashboard > WhatsApp > API Setup

3. **Backend error processing message**
   ```bash
   # Check logs for errors
   tail -f backend.log | grep ERROR
   ```

4. **Rate limiting**
   - Check Meta dashboard for rate limit errors
   - Wait a few minutes and try again

---

### Images not uploading

**Error**: Failed to download or save image

**Solution**:
```bash
# Check uploads directory exists and has write permissions
mkdir -p uploads
chmod 755 uploads

# Check backend logs for media download errors
tail -f backend.log | grep "download"

# Verify META_ACCESS_TOKEN is correct
# Test manually:
curl https://graph.facebook.com/v18.0/MEDIA_ID \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 🗄️ Database Issues

### Cannot connect to database

**Solution**:
```bash
# Restart PostgreSQL
docker-compose restart postgres

# Check if container is running
docker ps | grep postgres

# Check logs
docker-compose logs postgres

# Test connection
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db
```

---

### Database is full

**Solution**:
```bash
# Check database size
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db \
  -c "SELECT pg_size_pretty(pg_database_size('salvation_army_db'));"

# Check table sizes
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db \
  -c "SELECT relname, pg_size_pretty(pg_total_relation_size(relid)) FROM pg_catalog.pg_stattuple_approx ORDER BY pg_total_relation_size(relid) DESC;"

# Clean old data (adjust as needed)
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db \
  -c "DELETE FROM conversations WHERE created_at < NOW() - INTERVAL '30 days';"
```

## 🖼️ Image Issues

### Images not displaying in dashboard

**Checklist**:
1. ✅ Images exist in `uploads/` directory: `ls -lh uploads/`
2. ✅ File paths in database are correct:
   ```bash
   docker exec -it salvation-army-db psql -U postgres -d salvation_army_db \
     -c "SELECT person_image_path, cert_image_path FROM soldier_records WHERE person_image_path IS NOT NULL LIMIT 5;"
   ```
3. ✅ Backend file serving endpoint is working:
   ```bash
   curl -I http://localhost:8080/uploads/FILENAME.jpg
   # Should return 200 OK
   ```
4. ✅ Check browser console (F12) for 404 or CORS errors

**Solution**:
```bash
# Check file permissions
chmod 644 uploads/*

# Verify uploads directory configuration in application.properties
# app.upload.dir=uploads/

# Restart backend
pkill -f "spring-boot:run"
mvn spring-boot:run
```

## 🔐 Authentication Issues

### Cannot login to dashboard

**Error**: `Authentication failed` or `Invalid credentials`

**Solution**:
1. Check credentials match `.env` file:
   ```properties
   ADMIN_USERNAME=admin
   ADMIN_PASSWORD=admin123
   ```

2. Verify backend is running:
   ```bash
   curl http://localhost:8080/swagger-ui.html
   ```

3. Test login endpoint directly:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```

4. Check browser console (F12) for errors

---

### JWT token expired

**Error**: `401 Unauthorized` after some time

**Solution**:
- This is normal behavior (tokens expire after 24 hours by default)
- Simply log out and log back in
- Or adjust `jwt.expiration` in `application.properties` (value in milliseconds)

## 🐳 Docker Issues

### Docker containers won't start

**Error**: `Error starting userland proxy`

**Solution**:
```bash
# Stop all containers
docker-compose down

# Remove all containers and volumes
docker-compose down -v

# Restart Docker daemon
# On Mac/Windows: Restart Docker Desktop
# On Linux:
sudo systemctl restart docker

# Start again
docker-compose up -d
```

---

### Out of disk space

**Solution**:
```bash
# Check Docker disk usage
docker system df

# Clean up
docker system prune -a --volumes

# Remove old images
docker image prune -a

# Check uploads directory size
du -sh uploads/

# Remove old uploads if needed (be careful!)
find uploads/ -type f -mtime +30 -delete
```

## 🌐 Network Issues

### CORS errors in browser

**Error**: `Access to XMLHttpRequest blocked by CORS policy`

**Solution**:
1. Add your frontend URL to backend CORS config:
   ```properties
   # In application.properties or .env
   CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
   ```

2. Verify `WebConfig.java` has CORS configuration

3. Restart backend

4. Clear browser cache and reload

---

### ngrok issues

**Error**: `ngrok not found` or tunnel won't start

**Solution**:
```bash
# Install ngrok
# Download from: https://ngrok.com/download

# Or use package manager
# Mac: brew install ngrok
# Linux: snap install ngrok

# Authenticate (first time only)
ngrok authtoken YOUR_AUTHTOKEN

# Start tunnel
ngrok http 8080

# If getting rate limited, try different region
ngrok http 8080 --region=eu
```

## 📊 Performance Issues

### Backend is slow

**Possible causes**:

1. **Database queries**
   - Check slow queries in PostgreSQL logs
   - Add indexes if needed

2. **Too many records**
   - Add pagination to queries
   - Archive old records

3. **Image processing**
   - Images are downloaded synchronously
   - Consider adding async processing

4. **Memory issues**
   ```bash
   # Increase JVM memory
   export MAVEN_OPTS="-Xmx1024m"
   mvn spring-boot:run
   ```

---

### Dashboard loads slowly

**Solution**:
1. Check network tab in browser (F12)
2. Optimize image sizes
3. Use pagination for large datasets
4. Enable browser caching

## 🆘 Getting Help

### Collect debugging information

When asking for help, provide:

1. **Backend logs**:
   ```bash
   tail -100 backend.log > debug-backend.txt
   ```

2. **Database status**:
   ```bash
   docker exec -it salvation-army-db psql -U postgres -d salvation_army_db \
     -c "SELECT tablename, n_live_tup FROM pg_stat_user_tables;" > debug-db.txt
   ```

3. **Configuration** (remove sensitive data):
   ```bash
   cat .env | grep -v "TOKEN\|SECRET\|PASSWORD" > debug-config.txt
   ```

4. **Browser console errors** (F12 > Console > screenshot)

5. **Exact error message** (copy full text)

### Check logs in real-time

```bash
# Backend
tail -f backend.log

# Frontend (if running in background)
tail -f frontend.log

# Database
docker-compose logs -f postgres

# All together
tail -f backend.log frontend.log & docker-compose logs -f
```

### Reset everything

If all else fails:

```bash
# Stop everything
./stop.sh

# Clean everything
docker-compose down -v
mvn clean
rm -rf frontend/node_modules
rm -rf uploads/*
rm -f backend.log frontend.log

# Start fresh
docker-compose up -d
sleep 10
mvn spring-boot:run &
cd frontend && npm install && npm run dev
```

---

For more information, see:
- [README.md](README.md) - Full documentation
- [QUICKSTART.md](QUICKSTART.md) - Quick setup guide
- [COMMANDS.md](COMMANDS.md) - Command reference






