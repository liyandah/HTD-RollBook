# Quick Fix: Data Not Displaying on Frontend

## Most Common Issue: Not Logged In

**The backend requires authentication!** If you're not logged in, you'll get 401 errors and no data will load.

### Solution:
1. **Go to the login page**: `http://localhost:5173/login`
2. **Login with credentials**:
   - Username: `admin`
   - Password: `admin123`
3. **After login**, you should be redirected to the dashboard
4. **Data should now load**

## Check if You're Logged In

Open browser Console (F12) and run:
```javascript
localStorage.getItem('token')
```

- If it returns `null` → You're NOT logged in → Go to `/login`
- If it returns a long string → You're logged in → Check other issues below

## Check API URL

The frontend needs to know where the backend is. Check:

1. **File**: `frontend/.env`
2. **Should contain**: `VITE_API_URL=http://localhost:8081`
3. **If using ngrok**: `VITE_API_URL=https://your-ngrok-url.ngrok-free.app`
4. **After changing**: Restart frontend dev server

## Check Backend is Running

1. Look for backend terminal window
2. Should show: `Started WhatsAppDataCollectionApplication`
3. Should be running on port **8081**

## Enhanced Debugging

The code now includes detailed logging. Check your browser console (F12) for:

- `[API Client] Base URL: ...` - Shows API URL being used
- `[API Request] GET /api/records ...` - Shows each API call
- `[API] GET /api/records - Status: 200` - Shows successful responses
- `[API Error]` - Shows detailed error information

## Step-by-Step Debugging

1. **Open Browser Console** (F12)
2. **Refresh the page**
3. **Look for these messages**:
   - ✅ `[API Client] Base URL: http://localhost:8081` - Good!
   - ✅ `[API Request] GET /api/records - Token: ...` - Good! (means logged in)
   - ❌ `[API Request] GET /api/records - NO TOKEN FOUND!` - **Problem!** Need to login
   - ❌ `[API Error] Status: 401` - **Problem!** Not authenticated
   - ❌ `[API Error] Status: 404` - **Problem!** Backend not running or wrong URL
   - ❌ `[API Error] Network Error` - **Problem!** Backend not accessible

## Quick Test

1. **Login**: Go to `/login` and login
2. **Check Console**: Should see `[API Request] ... Token: ...`
3. **Check Network Tab**: 
   - Open Network tab (F12)
   - Refresh page
   - Look for `/api/records` request
   - Click it → Check "Headers" tab
   - Should see: `Authorization: Bearer ...`
   - Check "Response" tab → Should see JSON data

## Still Not Working?

1. **Clear browser cache and localStorage**:
   ```javascript
   // In browser console
   localStorage.clear();
   location.reload();
   ```

2. **Restart both frontend and backend**

3. **Check database has data**:
   - Connect to PostgreSQL
   - Run: `SELECT COUNT(*) FROM soldier_records;`
   - If count is 0, there's no data to display!

4. **Check backend logs** for any errors

## Summary

**Most likely issue**: You're not logged in or token expired
**Quick fix**: Go to `/login` and login again
**Check**: Browser console for detailed error messages
