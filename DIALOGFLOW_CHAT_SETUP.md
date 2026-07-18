# Dialogflow Chat Integration - Setup Guide

## ✅ Implementation Complete

This document describes the Dialogflow webhook and chat page implementation for the Salvation Army Registration Bot.

## 📋 What Was Implemented

### Backend Components

1. **DialogflowWebhookController** (`/api/dialogflow/webhook`)
   - Handles Dialogflow ES webhook fulfillment requests
   - Saves registration data to PostgreSQL when `REG_CONFIRM` or `REG_SUBMIT` intent is triggered
   - Already existed and was enhanced with better error handling

2. **DialogflowClientService**
   - Calls Google Dialogflow ES DetectIntent API
   - Maintains conversation context using sessionId
   - Returns bot responses to the frontend

3. **ChatController** (`/api/chat/message`)
   - REST endpoint for the web chat interface
   - Accepts `{ sessionId, message }` and returns `{ replyText }`
   - Public endpoint (no authentication required)

4. **SoldierRegistrationService**
   - Already existed and handles saving registration data
   - Parses Dialogflow parameters and handles "skip" values
   - Safely parses date of birth from ISO datetime strings

### Frontend Components

1. **ChatPage** (`/chat`)
   - React component with modern chat UI
   - Message bubbles (user right, bot left)
   - Typing indicator
   - Auto-scroll to latest message
   - Session persistence using localStorage
   - "Start" quick button

### Database

- **soldier_registration** table already exists (Flyway migration V6)
- Columns: id, corps_id, first_name, last_name, dob, id_number, favorite_song, bible_verse, dialogflow_session, created_at

## 🔧 Configuration Steps

### 1. Google Cloud Service Account

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to **IAM & Admin** → **Service Accounts**
3. Create a new service account or use existing one
4. Grant **Dialogflow API User** role
5. Create and download JSON key
6. Set environment variable:

```powershell
# Windows PowerShell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\service-account.json"
```

### 2. Application Properties

Add to `src/main/resources/application.properties`:

```properties
dialogflow.project-id=your-project-id-here
dialogflow.language-code=en
```

Or set via environment variable:
```powershell
$env:DIALOGFLOW_PROJECT_ID="your-project-id"
```

### 3. Dialogflow Console Setup

1. Go to [Dialogflow Console](https://dialogflow.cloud.google.com/)
2. Select your agent (SalvationArmyRegistrationBot)
3. Navigate to **Fulfillment**
4. Enable **Webhook**
5. Set webhook URL:
   - Local development: `https://your-ngrok-url.ngrok-free.app/api/dialogflow/webhook`
   - Production: `https://your-domain.com/api/dialogflow/webhook`
6. Save

4. In your final confirmation intent (`REG_CONFIRM`):
   - Enable **Use webhook fulfillment**
   - Save

### 4. Run the Application

**Backend:**
```bash
# Make sure PostgreSQL is running
# Set GOOGLE_APPLICATION_CREDENTIALS environment variable
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install  # if needed
npm run dev
```

### 5. Test the Chat

1. Open browser: `http://localhost:5173/chat` (or your frontend port)
2. Click "Start" or type a message
3. Complete the registration flow
4. When you confirm, the data should be saved to PostgreSQL

## 📡 API Endpoints

### POST /api/chat/message
**Request:**
```json
{
  "sessionId": "uuid-here",
  "message": "Hello"
}
```

**Response:**
```json
{
  "replyText": "Hello! How can I help you?"
}
```

### POST /api/dialogflow/webhook
**Request:** (Dialogflow ES v2 format)
```json
{
  "responseId": "...",
  "session": "projects/PROJECT_ID/agent/sessions/SESSION_ID",
  "queryResult": {
    "intent": {
      "displayName": "REG_CONFIRM"
    },
    "parameters": {
      "firstName": "John",
      "lastName": "Doe",
      "dob": "1990-01-01T12:00:00+02:00",
      "idNumber": "1234567890",
      "favoriteSong": "Amazing Grace",
      "bibleVerse": "John 3:16"
    }
  }
}
```

**Response:**
```json
{
  "fulfillmentText": "Registration saved successfully. God bless you."
}
```

## 🔍 Verification

1. **Check Database:**
   ```sql
   SELECT * FROM soldier_registration ORDER BY created_at DESC;
   ```

2. **Check Logs:**
   - Look for: "Received final registration intent: REG_CONFIRM"
   - Look for: "Saving registration for user..."
   - Look for: "Saved registration successfully"

3. **Test Flow:**
   - Start chat → Complete registration → Confirm
   - Verify record appears in database

## 🐛 Troubleshooting

### "Dialogflow project-id is not configured"
- Set `dialogflow.project-id` in `application.properties` or environment variable

### "Error calling Dialogflow DetectIntent API"
- Verify `GOOGLE_APPLICATION_CREDENTIALS` is set correctly
- Check service account has Dialogflow API access
- Verify project ID matches your Dialogflow agent

### Webhook not receiving requests
- Check ngrok is running (for local dev)
- Verify webhook URL in Dialogflow console
- Check backend logs for incoming requests
- Ensure `/api/dialogflow/webhook` is publicly accessible

### Registration not saving
- Check Dialogflow intent name matches `REG_CONFIRM` or `REG_SUBMIT`
- Verify webhook fulfillment is enabled in the intent
- Check database connection
- Review backend logs for errors

## 📝 Notes

- Session ID is stored in browser localStorage and persists across page refreshes
- The chat page is publicly accessible (no authentication required)
- All other admin endpoints remain protected
- Personal data (beyond firstName + lastName initials) is not logged

## 🎯 Next Steps

1. Set up Google Cloud service account
2. Configure Dialogflow project ID
3. Set up ngrok for local webhook testing (if needed)
4. Test the full registration flow
5. Deploy to production and update webhook URL
