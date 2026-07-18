# Command Reference

Complete list of commands for development and deployment.

## 📦 Backend Commands

### Build & Run

```bash
# Run with Maven (development)
mvn spring-boot:run

# Clean and build
mvn clean install

# Build without tests
mvn clean package -DskipTests

# Run JAR
java -jar target/whatsapp-data-collection-1.0.0.jar

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Database

```bash
# Start PostgreSQL (Docker)
docker-compose up -d

# Stop PostgreSQL
docker-compose down

# View PostgreSQL logs
docker-compose logs -f postgres

# Connect to database
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db

# Run migrations manually
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Rollback last migration (careful!)
mvn flyway:undo
```

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=WhatsAppServiceTest

# Run with coverage
mvn clean test jacoco:report

# Integration tests only
mvn verify
```

### Docker

```bash
# Build Docker image
docker build -t salvation-army-backend:latest .

# Run backend container
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/salvation_army_db \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
  salvation-army-backend:latest

# Run everything with Docker Compose
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop all containers
docker-compose down -v
```

## 🎨 Frontend Commands

### Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Start on different port
npm run dev -- --port 3000

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint

# Format code (if prettier configured)
npm run format
```

### Environment

```bash
# Create .env file
echo "VITE_API_URL=http://localhost:8080" > .env

# Production build with custom API
VITE_API_URL=https://api.production.com npm run build
```

## 🌐 ngrok Commands

```bash
# Start tunnel
ngrok http 8080

# Start with custom subdomain (paid account)
ngrok http 8080 --subdomain=salvation-army

# Start with custom region
ngrok http 8080 --region=eu

# View active tunnels
ngrok http://127.0.0.1:4040
```

## 🔍 Debugging Commands

### Check Services

```bash
# Check if backend is running
curl http://localhost:8080/actuator/health

# Test login endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test webhook verification
curl "http://localhost:8080/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=your_token&hub.challenge=test123"

# Check database connection
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db -c "\dt"
```

### View Logs

```bash
# Backend logs (if running with Maven)
# Check console output

# Backend logs (if running JAR)
tail -f logs/application.log

# Docker logs
docker-compose logs -f postgres
docker-compose logs -f backend

# Frontend logs
# Check browser console (F12)
```

### Port Management

```bash
# Check what's using port 8080
lsof -i :8080

# Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Check what's using port 5432
lsof -i :5432

# Check what's using port 5173
lsof -i :5173
```

## 📊 Database Commands

### PostgreSQL CLI

```bash
# Connect to database
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db

# List all tables
\dt

# Describe table
\d conversations
\d soldier_records

# View records
SELECT * FROM conversations;
SELECT * FROM soldier_records ORDER BY created_at DESC LIMIT 10;

# Count records by status
SELECT status, COUNT(*) FROM soldier_records GROUP BY status;

# Delete test data
DELETE FROM conversations WHERE wa_id = 'test_user';
DELETE FROM soldier_records WHERE wa_id = 'test_user';

# Exit
\q
```

### Backup & Restore

```bash
# Backup database
docker exec -t salvation-army-db pg_dump -U postgres salvation_army_db > backup.sql

# Restore database
docker exec -i salvation-army-db psql -U postgres salvation_army_db < backup.sql

# Backup to custom format
docker exec -t salvation-army-db pg_dump -U postgres -Fc salvation_army_db > backup.dump

# Restore from custom format
docker exec -i salvation-army-db pg_restore -U postgres -d salvation_army_db < backup.dump
```

## 🚀 Deployment Commands

### Production Build

```bash
# Backend
mvn clean package -DskipTests
java -jar target/whatsapp-data-collection-1.0.0.jar

# Frontend
cd frontend
npm run build
# Deploy dist/ folder to web server
```

### Environment Setup

```bash
# Set production environment variables (Linux/Mac)
export DATABASE_URL=jdbc:postgresql://prod-db:5432/salvation_army_db
export META_ACCESS_TOKEN=your_production_token
export JWT_SECRET=your_production_secret

# Set production environment variables (Windows PowerShell)
$env:DATABASE_URL="jdbc:postgresql://prod-db:5432/salvation_army_db"
$env:META_ACCESS_TOKEN="your_production_token"
$env:JWT_SECRET="your_production_secret"
```

## 🧹 Cleanup Commands

```bash
# Clean Maven build
mvn clean

# Remove node_modules
rm -rf frontend/node_modules

# Clean Docker
docker-compose down -v
docker system prune -a

# Remove uploads
rm -rf uploads/*

# Remove logs
rm -rf logs/*
```

## 📱 WhatsApp Testing

### Test Message Flow

```bash
# Send test webhook request
curl -X POST http://localhost:8080/webhooks/whatsapp \
  -H "Content-Type: application/json" \
  -d '{
    "object": "whatsapp_business_account",
    "entry": [{
      "id": "123",
      "changes": [{
        "value": {
          "messaging_product": "whatsapp",
          "metadata": {
            "display_phone_number": "1234567890",
            "phone_number_id": "123456789"
          },
          "contacts": [{
            "profile": {"name": "Test User"},
            "wa_id": "1234567890"
          }],
          "messages": [{
            "from": "1234567890",
            "id": "msg123",
            "timestamp": "1234567890",
            "type": "text",
            "text": {"body": "Hello"}
          }]
        },
        "field": "messages"
      }]
    }]
  }'
```

## 🔐 Security Commands

### Generate Secrets

```bash
# Generate JWT secret (Linux/Mac)
openssl rand -base64 64

# Generate JWT secret (Windows - PowerShell)
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))

# Generate random token
openssl rand -hex 32
```

## 📈 Monitoring Commands

```bash
# Watch backend logs in real-time
tail -f logs/application.log | grep ERROR

# Monitor database connections
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db \
  -c "SELECT * FROM pg_stat_activity WHERE datname = 'salvation_army_db';"

# Check disk space for uploads
du -sh uploads/

# Count uploaded files
ls -1 uploads/ | wc -l
```

## 🎯 Quick Commands

```bash
# Full restart (everything)
docker-compose down && docker-compose up -d
mvn spring-boot:run &
cd frontend && npm run dev

# Reset database
docker-compose down -v
docker-compose up -d
# Wait 10 seconds
mvn spring-boot:run

# Run backend in background
nohup mvn spring-boot:run > backend.log 2>&1 &

# Stop background backend
pkill -f "spring-boot:run"
```

## 📖 Help Commands

```bash
# Maven help
mvn --help

# Spring Boot help
mvn spring-boot:help

# Docker Compose help
docker-compose --help

# npm help
npm help
```

---

For more detailed information, see [README.md](README.md) or [QUICKSTART.md](QUICKSTART.md)






