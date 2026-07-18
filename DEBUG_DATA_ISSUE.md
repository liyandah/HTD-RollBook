# Debugging Guide: Data Not Displaying on Frontend

## Quick Checks

### 1. Check Browser Console
Open your browser's Developer Tools (F12) and check the Console tab. Look for:
- `[API Client] Base URL: ...` - This shows what API URL is being used
- `[API] GET /api/records ...` - These show API calls being made
- Any red error messages

### 2. Check Network Tab
1. Open Developer Tools (F12)
2. Go to the **Network** tab
3. Refresh the page
4. Look for API calls to `/api/records`, `/api/users`, etc.
5. Click on each request and check:
   - **Status**: Should be 200 (green)
   - **Response**: Should show JSON data
   - **Headers**: Check if Authorization header is present

### 3. Verify Backend is Running
The backend should be running on port **8081**. Check:
- Backend terminal window shows "Started WhatsAppDataCollectionApplication"
- No error messages in backend logs

### 4. Check API URL Configuration
The frontend uses `VITE_API_URL` from `.env` file. Check:
- File: `frontend/.env`
- Should contain: `VITE_API_URL=http://localhost:8081` (or your ngrok URL)

## Common Issues & Solutions

### Issue 1: CORS Errors
**Symptoms**: Console shows "Access to XMLHttpRequest blocked by CORS policy"

**Solution**: 
- Backend CORS is already configured
- If using ngrok, make sure the ngrok URL is in `application.properties` CORS allowed origins
- Restart backend after CORS changes

### Issue 2: 401 Unauthorized
**Symptoms**: API calls return 401 status

**Solution**:
- Make sure you're logged in
- Check if token exists: Open Console and type `localStorage.getItem('token')`
- If no token, log in again at `/login`

### Issue 3: 404 Not Found
**Symptoms**: API calls return 404 status

**Solution**:
- Verify backend is running on port 8081
- Check API endpoint URLs match backend routes
- Check backend logs for route registration

### Issue 4: Empty Response
**Symptoms**: API returns 200 but no data

**Solution**:
- Check database has data: Connect to PostgreSQL and query tables
- Check backend logs for any errors
- Verify database connection in backend logs

### Issue 5: Wrong API URL
**Symptoms**: Network tab shows requests going to wrong URL

**Solution**:
- Check `frontend/.env` file has correct `VITE_API_URL`
- Restart frontend dev server after changing `.env`
- Clear browser cache

## Manual Testing

### Test Backend Directly
Open these URLs in your browser (while logged in):
- `http://localhost:8081/api/records/dashboard`
- `http://localhost:8081/api/records?page=0&size=20`
- `http://localhost:8081/api/users`

You should see JSON data. If you see HTML or an error, the backend has an issue.

### Test Frontend API Client
Open browser Console (F12) and run:
```javascript
// Check API base URL
console.log('API Base URL:', import.meta.env.VITE_API_URL);

// Check if token exists
console.log('Token:', localStorage.getItem('token'));

// Test API call manually
import http from './src/api/apiClient';
http.get('/api/records/dashboard').then(r => console.log('Success:', r.data)).catch(e => console.error('Error:', e));
```

## Database Check

### Verify Data Exists
Connect to PostgreSQL and check:
```sql
-- Check records
SELECT COUNT(*) FROM soldier_records;

-- Check users
SELECT COUNT(*) FROM users;

-- View sample records
SELECT id, record_code, first_name, family_name, status FROM soldier_records LIMIT 5;
```

## Next Steps

1. **Check Console Logs**: The enhanced logging will show exactly what's happening
2. **Check Network Tab**: See if requests are being made and what responses you get
3. **Check Backend Logs**: Look for any errors in the Spring Boot console
4. **Verify Database**: Make sure data actually exists in the database

## Enhanced Logging

The code now includes enhanced logging that will show:
- API base URL on page load
- Every API request with method, URL, and status
- Full response data structure
- Error details with status codes and messages

Check your browser console for these detailed logs!
