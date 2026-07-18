#!/bin/bash

# Salvation Army WhatsApp Data Collection System - Startup Script

set -e

echo "🎯 Starting Salvation Army WhatsApp Data Collection System..."
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker not found. Please install Docker first.${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose not found. Please install Docker Compose first.${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven not found. Please install Maven first.${NC}"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js not found. Please install Node.js first.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All prerequisites met${NC}"
echo ""

# Check if .env exists
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}⚠️  .env file not found. Copying from env.example...${NC}"
    cp env.example .env
    echo -e "${YELLOW}📝 Please edit .env file with your configuration before continuing.${NC}"
    echo -e "${YELLOW}   Press Enter when ready...${NC}"
    read
fi

# Step 1: Start Database
echo -e "${BLUE}Step 1/4: Starting PostgreSQL database...${NC}"
docker-compose up -d
echo -e "${GREEN}✅ Database started${NC}"
echo "Waiting for database to be ready..."
sleep 5
echo ""

# Step 2: Start Backend
echo -e "${BLUE}Step 2/4: Starting backend (this may take a minute)...${NC}"
echo "Backend will run on http://localhost:8080"

# Check if backend is already running
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${YELLOW}⚠️  Port 8080 is already in use. Skipping backend start.${NC}"
else
    mvn spring-boot:run > backend.log 2>&1 &
    BACKEND_PID=$!
    echo "Backend PID: $BACKEND_PID"
    echo "Waiting for backend to start (checking logs)..."
    
    for i in {1..30}; do
        if grep -q "Started WhatsAppDataCollectionApplication" backend.log 2>/dev/null; then
            echo -e "${GREEN}✅ Backend started successfully${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done
    echo ""
fi
echo ""

# Step 3: Setup Frontend
echo -e "${BLUE}Step 3/4: Setting up frontend...${NC}"
cd frontend

if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

if [ ! -f ".env" ]; then
    echo "VITE_API_URL=http://localhost:8080" > .env
fi

echo -e "${GREEN}✅ Frontend setup complete${NC}"
echo ""

# Step 4: Start Frontend
echo -e "${BLUE}Step 4/4: Starting frontend...${NC}"
echo "Frontend will run on http://localhost:5173"

if lsof -Pi :5173 -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${YELLOW}⚠️  Port 5173 is already in use. Skipping frontend start.${NC}"
else
    npm run dev > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo "Frontend PID: $FRONTEND_PID"
    sleep 3
    echo -e "${GREEN}✅ Frontend started${NC}"
fi

cd ..
echo ""

# Summary
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎉 System is now running!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}📊 Access Points:${NC}"
echo "   Frontend:  http://localhost:5173"
echo "   Backend:   http://localhost:8080"
echo "   API Docs:  http://localhost:8080/swagger-ui.html"
echo ""
echo -e "${BLUE}🔑 Default Login:${NC}"
echo "   Username: admin"
echo "   Password: admin123"
echo ""
echo -e "${BLUE}📝 Logs:${NC}"
echo "   Backend:  tail -f backend.log"
echo "   Frontend: tail -f frontend.log"
echo "   Database: docker-compose logs -f postgres"
echo ""
echo -e "${BLUE}🛑 To stop:${NC}"
echo "   Run: ./stop.sh"
echo "   Or:  pkill -f 'spring-boot:run' && pkill -f 'vite' && docker-compose down"
echo ""
echo -e "${YELLOW}📱 Next Steps:${NC}"
echo "   1. Configure WhatsApp in Meta Dashboard"
echo "   2. Start ngrok: ngrok http 8080"
echo "   3. Set webhook URL in Meta"
echo "   4. Test with WhatsApp message"
echo ""
echo -e "For more information, see README.md or QUICKSTART.md"
echo ""






