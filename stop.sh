#!/bin/bash

# Salvation Army WhatsApp Data Collection System - Shutdown Script

echo "🛑 Stopping Salvation Army WhatsApp Data Collection System..."
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Stop backend
echo -e "${BLUE}Stopping backend...${NC}"
if pgrep -f "spring-boot:run" > /dev/null; then
    pkill -f "spring-boot:run"
    echo -e "${GREEN}✅ Backend stopped${NC}"
else
    echo -e "${YELLOW}⚠️  Backend was not running${NC}"
fi

# Stop frontend
echo -e "${BLUE}Stopping frontend...${NC}"
if pgrep -f "vite" > /dev/null; then
    pkill -f "vite"
    echo -e "${GREEN}✅ Frontend stopped${NC}"
else
    echo -e "${YELLOW}⚠️  Frontend was not running${NC}"
fi

# Stop database
echo -e "${BLUE}Stopping database...${NC}"
docker-compose down
echo -e "${GREEN}✅ Database stopped${NC}"

echo ""
echo -e "${GREEN}✅ All services stopped${NC}"
echo ""
echo "To start again, run: ./start.sh"
echo ""






