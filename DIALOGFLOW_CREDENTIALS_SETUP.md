# Dialogflow Credentials Setup Guide

## Quick Setup Steps

### Option 1: Create Service Account (Recommended)

1. **Go to Google Cloud Console**
   - Visit: https://console.cloud.google.com/
   - Select your project (or create a new one)

2. **Enable Dialogflow API**
   - Navigate to **APIs & Services** → **Library**
   - Search for "Dialogflow API"
   - Click **Enable**

3. **Create Service Account**
   - Go to **IAM & Admin** → **Service Accounts**
   - Click **Create Service Account**
   - Name: `dialogflow-service` (or any name)
   - Click **Create and Continue**

4. **Grant Permissions**
   - Role: **Dialogflow API Client** or **Dialogflow API User**
   - Click **Continue** → **Done**

5. **Create and Download Key**
   - Click on the service account you just created
   - Go to **Keys** tab
   - Click **Add Key** → **Create new key**
   - Choose **JSON** format
   - Click **Create**
   - The JSON file will download automatically

6. **Save the File**
   - Save it somewhere safe (e.g., `C:\Users\YourName\credentials\service-account.json`)
   - **IMPORTANT:** Never commit this file to Git!

7. **Set Environment Variable**
   - **Temporary (current session):**
     ```powershell
     $env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\service-account.json"
     ```
   
   - **Permanent (System-wide):**
     1. Press `Win + R`, type `sysdm.cpl`, press Enter
     2. Go to **Advanced** tab → **Environment Variables**
     3. Under **User variables**, click **New**
     4. Variable name: `GOOGLE_APPLICATION_CREDENTIALS`
     5. Variable value: `C:\path\to\service-account.json`
     6. Click **OK** on all dialogs

### Option 2: Skip for Now

If you don't have credentials yet:
1. **Press Enter** to skip the prompt
2. The backend will start but chat won't work until credentials are set
3. You can set them later and restart

## Verify Setup

After setting credentials, verify they work:

```powershell
# Check if variable is set
echo $env:GOOGLE_APPLICATION_CREDENTIALS

# Verify file exists
Test-Path $env:GOOGLE_APPLICATION_CREDENTIALS
```

## Dialogflow Project ID

You also need to set your Dialogflow project ID:

1. **In Google Cloud Console:**
   - Note your Project ID (shown at the top of the console)

2. **Set in application.properties:**
   ```properties
   dialogflow.project-id=your-project-id-here
   ```

   Or set as environment variable:
   ```powershell
   $env:DIALOGFLOW_PROJECT_ID="your-project-id"
   ```

## Testing

Once credentials are set:
1. Restart the backend
2. Open: http://localhost:5173/chat
3. Try sending a message
4. Check backend logs for any errors

## Troubleshooting

**Error: "Failed to obtain Google Cloud access token"**
- Verify `GOOGLE_APPLICATION_CREDENTIALS` points to valid JSON file
- Check the service account has Dialogflow API enabled
- Ensure the JSON file is not corrupted

**Error: "Dialogflow project-id is not configured"**
- Set `dialogflow.project-id` in `application.properties`
- Or set `DIALOGFLOW_PROJECT_ID` environment variable

**Error: "Permission denied"**
- Ensure service account has "Dialogflow API Client" role
- Check Dialogflow API is enabled in your project
