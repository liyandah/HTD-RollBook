# Windows Setup Guide

Quick setup guide for Windows users.

## 🚨 **Issue: Database "salvation_army_db" does not exist**

### **What Happened?**
The backend tried to connect to PostgreSQL, but the database `salvation_army_db` hasn't been created yet.

### **Solution (3 Options)**

---

## ✅ **Option 1: Use Updated Batch Files (Easiest)**

I've updated the batch files to handle this automatically!

### **Quick Start:**
1. **Start Docker Desktop** (if not running)
   - Open Docker Desktop from Start Menu
   - Wait for whale icon in system tray to stop animating

2. **Run the setup:**
   ```
   Double-click: start-all.bat
   ```

The batch files now:
- ✅ Check if Docker is running
- ✅ Start PostgreSQL container
- ✅ Create database automatically
- ✅ Start backend and frontend

---

## ✅ **Option 2: Manual Database Creation**

If you prefer to do it manually:

### **Step 1: Start Docker Desktop**
- Open Docker Desktop
- Wait until it's running (whale icon in system tray)

### **Step 2: Start PostgreSQL**
```batch
docker-compose up -d
```

### **Step 3: Create Database**
```batch
Double-click: create-database.bat
```

Or run manually:
```batch
docker exec salvation-army-db psql -U postgres -c "CREATE DATABASE salvation_army_db;"
```

### **Step 4: Start Backend**
```batch
Double-click: start-backend.bat
```

---

## ✅ **Option 3: Use pgAdmin (GUI)**

1. Download [pgAdmin](https://www.pgadmin.org/download/)
2. Connect to PostgreSQL:
   - Host: localhost
   - Port: 5432
   - Username: postgres
   - Password: YOUR_DB_PASSWORD
3. Right-click "Databases" → "Create" → "Database"
4. Name: `salvation_army_db`
5. Click "Save"

---

## 📋 **Complete Setup Checklist**

### **1. Prerequisites**
- [ ] Docker Desktop installed and running
- [ ] Java 17 installed
- [ ] Maven installed
- [ ] Node.js installed

### **2. First Time Setup**

**A. Start Docker Desktop**
```
Start Menu → Docker Desktop
Wait for it to start (whale icon)
```

**B. Setup Database**
```
Method 1: Double-click create-database.bat
Method 2: Run start-all.bat (does it automatically)
```

**C. Configure Environment**
```
Edit .env file with your WhatsApp credentials
```

**D. Start Application**
```
Double-click: start-all.bat
```

### **3. Verify Everything Works**

Check these URLs:
- [ ] Backend: http://localhost:8080/swagger-ui.html
- [ ] Frontend: http://localhost:5173
- [ ] Login works with: admin / admin123

---

## 🐛 **Common Issues**

### **Docker Desktop Not Starting**
**Error:** `Docker Desktop is not running`

**Fix:**
1. Open Docker Desktop from Start Menu
2. Wait 30-60 seconds for it to start
3. Check system tray for whale icon
4. Run batch file again

---

### **Port Already in Use**
**Error:** `Port 5432 already in use`

**Fix:**
```batch
# Stop all containers
docker-compose down

# Check what's using the port
netstat -ano | findstr :5432

# Kill the process (replace PID with actual process ID)
taskkill /F /PID <PID>

# Start again
docker-compose up -d
```

---

### **Database Already Exists Error**
**Error:** `database "salvation_army_db" already exists`

This is actually OK! It means the database was already created. Just continue starting the backend.

---

### **Can't Connect to Database**
**Error:** `Connection refused` or `Connection timeout`

**Fix:**
```batch
# Check if PostgreSQL is running
docker ps

# Should see: salvation-army-db

# If not running:
docker-compose up -d
timeout /t 10 /nobreak

# Check logs
docker-compose logs postgres
```

---

### **Backend Won't Start**
**Error:** Various Spring Boot errors

**Fix:**
```batch
# 1. Check database is running
docker ps | findstr "salvation-army-db"

# 2. Check database exists
docker exec salvation-army-db psql -U postgres -c "\l" | findstr "salvation_army_db"

# 3. If database doesn't exist:
create-database.bat

# 4. Try starting backend again
start-backend.bat
```

---

## 🎯 **Quick Reference**

### **Start Everything**
```batch
start-all.bat
```

### **Start Individual Services**
```batch
# Database only
docker-compose up -d

# Backend only
start-backend.bat

# Frontend only
start-frontend.bat
```

### **Stop Everything**
```batch
stop-all.bat
```

### **Check Status**
```batch
check-status.bat
```

### **Create/Reset Database**
```batch
create-database.bat
```

---

## 📝 **Database Connection Details**

For manual connections (pgAdmin, DBeaver, etc.):

```
Host:     localhost
Port:     5432
Database: salvation_army_db
Username: postgres
Password: YOUR_DB_PASSWORD
```

---

## 🔧 **Manual Database Commands**

### **Check if database exists:**
```batch
docker exec salvation-army-db psql -U postgres -c "\l" | findstr "salvation_army_db"
```

### **Create database:**
```batch
docker exec salvation-army-db psql -U postgres -c "CREATE DATABASE salvation_army_db;"
```

### **Drop database (⚠️ deletes all data):**
```batch
docker exec salvation-army-db psql -U postgres -c "DROP DATABASE salvation_army_db;"
```

### **Connect to database:**
```batch
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db
```

### **List tables:**
```batch
docker exec salvation-army-db psql -U postgres -d salvation_army_db -c "\dt"
```

---

## 🎉 **Success Checklist**

When everything is working, you should see:

- [ ] Docker Desktop running (whale icon in tray)
- [ ] `docker ps` shows `salvation-army-db` container
- [ ] Backend starts without errors
- [ ] Frontend opens in browser
- [ ] Can login to dashboard
- [ ] No database connection errors

---

## 🆘 **Still Having Issues?**

1. **Run Diagnostics:**
   ```batch
   check-status.bat
   ```

2. **Check Logs:**
   - Backend: Look at the backend window
   - Database: `docker-compose logs postgres`
   - Frontend: Browser console (F12)

3. **Full Reset:**
   ```batch
   stop-all.bat
   docker-compose down -v
   create-database.bat
   start-all.bat
   ```

4. **Get Help:**
   - Check: TROUBLESHOOTING.md
   - Review: README.md
   - Logs: Save output from check-status.bat

---

## 📞 **Quick Support Commands**

```batch
REM Check Docker
docker --version
docker ps

REM Check Database
docker exec salvation-army-db psql -U postgres -c "\l"

REM Check Java
java -version

REM Check Maven
mvn -version

REM Check Node
node -version
npm -version

REM Check Ports
netstat -ano | findstr ":8080"
netstat -ano | findstr ":5173"
netstat -ano | findstr ":5432"
```

---

**You're all set! 🚀**

For detailed documentation, see [README.md](README.md)






