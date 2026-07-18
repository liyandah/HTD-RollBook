# In-House Chatbot Platform - Setup Guide

## ✅ Implementation Complete

This document describes the complete in-house chatbot platform for Salvation Army registration. The bot runs entirely within your system with no external dependencies.

## 📋 What Was Implemented

### Backend Components

1. **Database Tables** (Flyway Migration V7)
   - `chat_session` - Stores conversation state and registration data
   - `chat_message` - Stores all chat messages (user and bot)
   - Updated `soldier_registration` - Added `chat_session_id` and `age` columns

2. **Entities**
   - `ChatSession` - JPA entity for chat sessions
   - `ChatMessage` - JPA entity for chat messages

3. **Repositories**
   - `ChatSessionRepository` - CRUD operations for sessions
   - `ChatMessageRepository` - Message storage and retrieval

4. **ChatbotService** - State Machine Engine
   - Implements complete registration flow
   - Handles validation and business logic
   - Manages session state transitions

5. **BotController** - REST API
   - `POST /api/bot/message` - Main endpoint for chat interactions

### Frontend Components

1. **ChatPage** (`/chat`)
   - React component with modern chat UI
   - Start and Reset buttons
   - Status badges (ACTIVE/COMPLETE/CANCELLED)
   - Session persistence via localStorage
   - Auto-scroll and typing indicators

## 🔄 Conversation Flow

### States

1. **START** - Initial state
2. **ASK_FIRST_NAME** - Collecting first name
3. **ASK_LAST_NAME** - Collecting last name
4. **ASK_DOB** - Collecting date of birth
5. **ASK_ID** - Collecting ID number (only if age >= 16)
6. **ASK_SONG** - Collecting favorite song (optional)
7. **ASK_VERSE** - Collecting favorite Bible verse (optional)
8. **CONFIRM** - Showing summary and waiting for confirmation
9. **COMPLETE** - Registration saved successfully
10. **CANCELLED** - User cancelled registration

### Flow Steps

1. User types `start` or clicks "Start" button
2. Bot asks for First Name (validates: letters only, no numbers/symbols)
3. Bot asks for Last Name (same validation)
4. Bot asks for Date of Birth (accepts DD/MM/YYYY or YYYY-MM-DD)
5. Backend calculates age from DOB
6. **If age >= 16:** Bot asks for National ID (required, cannot skip)
7. **If age < 16:** Skip ID step automatically
8. Bot asks for Favorite Song (optional, can type "skip")
9. Bot asks for Favorite Bible Verse (optional, can type "skip")
10. Bot shows summary and asks:
    - Reply `1` to Confirm and save
    - Reply `3` to Cancel
11. If confirmed: Data saved to `soldier_registration` table

### Global Commands

- `reset` - Restart registration from beginning (clears session data)
- `cancel` - Cancel and stop registration

## 🗄️ Database Schema

### chat_session Table

```sql
CREATE TABLE chat_session (
    session_id VARCHAR(255) PRIMARY KEY,
    state VARCHAR(50) NOT NULL DEFAULT 'START',
    corps_id INTEGER,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    dob DATE,
    age INTEGER,
    id_number VARCHAR(100),
    favorite_song TEXT,
    bible_verse TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### chat_message Table

```sql
CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    sender VARCHAR(10) NOT NULL,  -- USER or BOT
    message_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_session(session_id) ON DELETE CASCADE
);
```

### soldier_registration Table

Updated with:
- `chat_session_id VARCHAR(255)` - Links to chat session
- `age INTEGER` - Calculated age

## 🔧 Configuration

### application.properties

```properties
# In-House Chatbot Configuration
bot.fixed-corps-id=${BOT_FIXED_CORPS_ID:}
```

Set `bot.fixed-corps-id` to a fixed corps ID, or leave empty for NULL.

### Security

The `/api/bot/**` endpoint is publicly accessible (no authentication required).

## 📡 API Endpoints

### POST /api/bot/message

**Request:**
```json
{
  "sessionId": "uuid-here",
  "message": "start"
}
```

**Response:**
```json
{
  "replyText": "Welcome! Let's start your registration...",
  "state": "ASK_FIRST_NAME",
  "status": "ACTIVE"
}
```

## 🚀 Running the Application

### Backend

```bash
mvn spring-boot:run
```

Backend will be available at: `http://localhost:8081`

### Frontend

```bash
cd frontend
npm run dev
```

Frontend will be available at: `http://localhost:5173`

### Access Chat

Open: `http://localhost:5173/chat`

## 📝 Sample Conversation

```
User: start
Bot: Welcome! Let's start your registration.

What is your first name?

User: John
Bot: Thank you! What is your last name?

User: Doe
Bot: Great! What is your date of birth?
Please enter in format DD/MM/YYYY or YYYY-MM-DD

User: 15/03/1990
Bot: Thank you! Please provide your National ID number.

User: 1234567890
Bot: Thank you! What is your favorite song? (Type 'skip' if you don't have one)

User: Amazing Grace
Bot: What is your favorite Bible verse? (Type 'skip' if you don't have one)

User: John 3:16
Bot: Please review your registration:

First Name: John
Last Name: Doe
Date of Birth: 1990-03-15
Age: 34
ID Number: 1234567890
Favorite Song: Amazing Grace
Favorite Bible Verse: John 3:16

Reply:
1 to Confirm and save
3 to Cancel

User: 1
Bot: ✅ Registration submitted successfully! Thank you for registering.
```

## 🔍 Checking Database

### View Chat Sessions

```sql
SELECT * FROM chat_session ORDER BY created_at DESC;
```

### View Chat Messages

```sql
SELECT * FROM chat_message 
WHERE session_id = 'your-session-id' 
ORDER BY created_at ASC;
```

### View Saved Registrations

```sql
SELECT * FROM soldier_registration 
ORDER BY created_at DESC;
```

### View Complete Conversation

```sql
SELECT 
    cm.sender,
    cm.message_text,
    cm.created_at
FROM chat_message cm
WHERE cm.session_id = 'your-session-id'
ORDER BY cm.created_at ASC;
```

## ✅ Validation Rules

### Name Validation
- Must contain only letters, spaces, hyphens, and apostrophes
- Minimum 2 characters
- Rejects values that look like IDs (contain 6+ digits)

### Date of Birth
- Accepts formats: DD/MM/YYYY or YYYY-MM-DD
- Cannot be in the future
- Age is automatically calculated

### ID Number
- Required only if age >= 16
- Cannot be skipped for users 16+
- Optional for users under 16

### Optional Fields
- Favorite Song: Can type "skip" to leave empty
- Favorite Bible Verse: Can type "skip" to leave empty

## 🐛 Troubleshooting

### Session Not Found
- Check that sessionId is being generated and stored in localStorage
- Verify sessionId is sent with each request

### State Not Updating
- Check database connection
- Verify Flyway migration V7 ran successfully
- Check backend logs for errors

### Registration Not Saving
- Verify all required fields are collected
- Check that user replied "1" to confirm
- Review backend logs for validation errors

### Date Parsing Issues
- Ensure date format matches DD/MM/YYYY or YYYY-MM-DD
- Check backend logs for parsing errors

## 📊 Session Management

- **Session ID**: Generated as UUID in browser, stored in localStorage
- **Persistence**: Session data persists across page refreshes
- **Reset**: Type "reset" or click Reset button to clear session
- **Cancel**: Type "cancel" to stop and mark session as CANCELLED

## 🎯 Next Steps

1. **Start Backend**: `mvn spring-boot:run`
2. **Start Frontend**: `cd frontend && npm run dev`
3. **Open Chat**: Navigate to `http://localhost:5173/chat`
4. **Test Flow**: Click "Start" and complete registration
5. **Verify Data**: Check `soldier_registration` table in database

## 📚 Additional Notes

- All chat messages are stored in `chat_message` table for audit trail
- Session state is persisted in `chat_session` table
- Completed registrations are saved to `soldier_registration` table
- The bot handles edge cases and provides helpful error messages
- No external services required - everything runs in-house
