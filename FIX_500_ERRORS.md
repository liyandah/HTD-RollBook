# 🔧 Fix 500 Internal Server Errors

## The Problem

You're getting 500 errors because:
1. ❌ The backend hasn't been restarted with the new code
2. ❌ The new Java files haven't been compiled yet
3. ❌ The database migration for the users table hasn't run

## ✅ Quick Fix (3 Steps)

### Step 1: Stop Everything

Close all running terminal windows that have the backend/frontend running.

Or in terminal:
```bash
# In project root
stop-all.bat
```

### Step 2: Restart the Backend & Frontend

```bash
# Option A: Start both together
start-all.bat

# Option B: Start separately
start-backend.bat  # Wait for this to fully start
start-frontend.bat # Then start this
```

### Step 3: Wait for Backend to Fully Start

The backend takes **30-60 seconds** to start. You'll see:

```
✅ Started WhatsAppDataCollectionApplication in X.XXX seconds
✅ Tomcat started on port(s): 8081 (http)
```

**Watch for these messages:**
- `Flyway: Migrating schema...` ✅ (Database migration running)
- `Flyway: Successfully applied 2 migrations` ✅ (Users table created!)
- `JPA initialized successfully` ✅
- `Started WhatsAppDataCollectionApplication` ✅

### Step 4: Refresh Your Browser

Go to: http://localhost:5173

The errors should be gone! 🎉

---

## 🔍 If Errors Persist

### Check Backend Logs

Look for compilation errors in the backend terminal:

```
❌ Compilation failure
❌ ClassNotFoundException
❌ Table 'users' doesn't exist
```

### Solution: Clean Rebuild

```bash
# Stop everything first
stop-all.bat

# Clean rebuild backend
mvn clean install

# Start again
start-all.bat
```

---

## 🗄️ Database Issues?

If you see **"Table 'users' doesn't exist"**:

### Option 1: Let Flyway Create It
The migration `V2__Create_Users_Table.sql` will auto-create the users table on startup.

### Option 2: Manual Creation
If Flyway fails, run this SQL manually:

```sql
-- Connect to your PostgreSQL database
psql -U postgres -d salvation_army_db

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);

-- Insert default admin user (password: admin123)
INSERT INTO users (username, email, password, full_name, role, status)
VALUES (
    'admin',
    'admin@salvationarmy.org',
    '$2a$10$XPTyZ6hCL3KGEemqZyh8LuGYvN7xLZJGN3p.1j7xkG8qJd5lYvGYa',
    'Administrator',
    'ADMIN',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;
```

---

## 🎯 Expected Behavior After Fix

### ✅ What Should Work:

1. **User Management** (`/users`)
   - View all users ✅
   - Create new user ✅
   - Delete user ✅

2. **Reports** (`/reports`)
   - Monthly statistics charts with real data ✅
   - No more 500 errors ✅

3. **Records** (`/records`)
   - Create records ✅
   - View/Edit/Delete records ✅

### ✅ No More Errors:
- ❌ ~~Failed to load resource: 500~~
- ❌ ~~AxiosError~~
- ✅ All API calls return 200 OK

---

## 🧪 Test After Fix

### Test 1: Check Backend is Running
Open: http://localhost:8081/actuator/health

Should see:
```json
{"status":"UP"}
```

### Test 2: Check Users API
In browser console (F12):
```javascript
fetch('http://localhost:8081/api/users', {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('token')
  }
})
.then(r => r.json())
.then(console.log)
```

Should see array of users, not 500 error.

### Test 3: Check Reports API
```javascript
fetch('http://localhost:8081/api/reports/monthly?months=6', {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('token')
  }
})
.then(r => r.json())
.then(console.log)
```

Should see monthly data array, not 500 error.

---

## 📊 Verify Database Migration

### Check if users table exists:

```bash
# Connect to database
psql -U postgres -d salvation_army_db

# List tables
\dt

# Should see:
# soldier_records
# conversations
# users         <-- NEW TABLE
# flyway_schema_history
```

### Check Flyway migration status:

```sql
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

Should see:
```
V1 | Initial Schema    | [timestamp] | true
V2 | Create Users Table | [timestamp] | true  <-- NEW
```

---

## 🚨 Still Not Working?

### Nuclear Option: Fresh Start

```bash
# 1. Stop everything
stop-all.bat

# 2. Drop and recreate database
psql -U postgres
DROP DATABASE salvation_army_db;
CREATE DATABASE salvation_army_db;
\q

# 3. Clean rebuild
mvn clean install

# 4. Start everything
start-all.bat
```

This will:
- ✅ Recreate the database from scratch
- ✅ Run all migrations (V1 and V2)
- ✅ Create users table with admin user
- ✅ Recompile all new Java code

---

## 💡 Pro Tips

1. **Wait for Full Startup**: Don't refresh the page until backend shows "Started WhatsAppDataCollectionApplication"

2. **Check Port 8081**: Make sure nothing else is using port 8081
   ```bash
   netstat -ano | findstr :8081
   ```

3. **Check PostgreSQL**: Ensure PostgreSQL is running
   ```bash
   # Windows
   Get-Service postgresql*
   ```

4. **Clear Browser Cache**: Sometimes old API responses are cached
   - Press Ctrl+Shift+R to hard refresh
   - Or clear site data in DevTools (F12)

---

## ✅ Success Indicators

When everything is working:
1. ✅ No 500 errors in browser console
2. ✅ User Management page shows real users
3. ✅ Reports page shows monthly charts
4. ✅ Backend log shows "Started WhatsAppDataCollectionApplication"
5. ✅ `flyway_schema_history` has 2 entries

**After following these steps, everything should work perfectly! 🚀**





