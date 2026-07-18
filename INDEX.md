# 📚 Documentation Index

Quick navigation to all project documentation.

## 🚀 Getting Started

**New to the project? Start here:**

1. **[QUICKSTART.md](QUICKSTART.md)** - Get running in 5 minutes
2. **[README.md](README.md)** - Complete setup guide and documentation
3. **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - What's been built

## 📖 Documentation Files

### Essential Documentation

| Document | Description | When to Use |
|----------|-------------|-------------|
| **[README.md](README.md)** | Complete project documentation | First-time setup, full reference |
| **[QUICKSTART.md](QUICKSTART.md)** | 5-minute quick start guide | Fast setup, quick reference |
| **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** | Comprehensive project overview | Understanding architecture |
| **[COMMANDS.md](COMMANDS.md)** | All commands reference | Development and operations |
| **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** | Common issues and solutions | When things go wrong |
| **[DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)** | Production deployment steps | Before going live |
| **[INDEX.md](INDEX.md)** | This file - documentation index | Finding documentation |

### Scripts

| File | Description | Usage |
|------|-------------|-------|
| **start.sh** | One-command startup script | `./start.sh` |
| **stop.sh** | Clean shutdown script | `./stop.sh` |

### Configuration

| File | Description | Location |
|------|-------------|----------|
| **env.example** | Backend environment template | Root directory |
| **frontend/env.example** | Frontend environment template | frontend/ |
| **docker-compose.yml** | PostgreSQL setup | Root directory |
| **Dockerfile** | Backend container image | Root directory |
| **pom.xml** | Maven dependencies | Root directory |
| **package.json** | npm dependencies | frontend/ |

## 🎯 Quick Links by Task

### Setup & Installation
- New install: [QUICKSTART.md](QUICKSTART.md)
- Detailed setup: [README.md](README.md) → Setup Instructions
- Docker setup: [README.md](README.md) → Database Setup
- Environment config: [README.md](README.md) → Environment Variables

### Development
- All commands: [COMMANDS.md](COMMANDS.md)
- Backend development: [COMMANDS.md](COMMANDS.md) → Backend Commands
- Frontend development: [COMMANDS.md](COMMANDS.md) → Frontend Commands
- Database operations: [COMMANDS.md](COMMANDS.md) → Database Commands

### WhatsApp Integration
- Initial setup: [README.md](README.md) → WhatsApp Setup & Testing
- Testing flow: [QUICKSTART.md](QUICKSTART.md) → WhatsApp Testing Setup
- Troubleshooting: [TROUBLESHOOTING.md](TROUBLESHOOTING.md) → WhatsApp Issues

### Troubleshooting
- Common issues: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- Backend issues: [TROUBLESHOOTING.md](TROUBLESHOOTING.md) → Backend Issues
- Frontend issues: [TROUBLESHOOTING.md](TROUBLESHOOTING.md) → Frontend Issues
- Database issues: [TROUBLESHOOTING.md](TROUBLESHOOTING.md) → Database Issues

### Deployment
- Pre-deployment: [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)
- Production setup: [README.md](README.md) → Deployment
- Security checklist: [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) → Security

### Understanding the System
- Architecture: [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) → What's Been Built
- File structure: [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) → File Structure
- Features: [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) → Key Features
- Technologies: [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) → Technologies Used

## 🏗️ Project Structure

```
salvation-army-whatsapp/
│
├── 📚 DOCUMENTATION
│   ├── README.md                    # Main documentation
│   ├── QUICKSTART.md                # Quick start guide
│   ├── PROJECT_SUMMARY.md           # Project overview
│   ├── COMMANDS.md                  # Commands reference
│   ├── TROUBLESHOOTING.md           # Troubleshooting guide
│   ├── DEPLOYMENT_CHECKLIST.md      # Deployment checklist
│   └── INDEX.md                     # This file
│
├── ⚙️ CONFIGURATION
│   ├── env.example                  # Backend env template
│   ├── docker-compose.yml           # Database setup
│   ├── Dockerfile                   # Backend container
│   └── pom.xml                      # Maven config
│
├── 🔧 SCRIPTS
│   ├── start.sh                     # Startup script
│   └── stop.sh                      # Shutdown script
│
├── 🔙 BACKEND (src/main/java/org/salvationarmy/whatsapp/)
│   ├── controller/                  # REST Controllers
│   ├── service/                     # Business Logic
│   ├── repository/                  # Data Access
│   ├── entity/                      # JPA Entities
│   ├── dto/                         # Data Transfer Objects
│   ├── config/                      # Configuration
│   ├── security/                    # Security Components
│   ├── util/                        # Utilities
│   └── exception/                   # Exception Handlers
│
├── 🎨 FRONTEND (frontend/)
│   ├── src/
│   │   ├── api/                     # HTTP Client
│   │   ├── components/              # React Components
│   │   ├── pages/                   # Page Components
│   │   └── utils/                   # Utilities
│   ├── package.json                 # npm config
│   ├── vite.config.js               # Vite config
│   └── tailwind.config.js           # Tailwind config
│
└── 📁 DATA
    └── uploads/                     # Image storage (auto-created)
```

## 🎓 Learning Path

### For New Users
1. Read [QUICKSTART.md](QUICKSTART.md) to understand basic setup
2. Follow setup steps to get system running
3. Test WhatsApp flow
4. Explore dashboard
5. Read [README.md](README.md) for details

### For Developers
1. Read [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) to understand architecture
2. Review file structure and technologies used
3. Explore source code (backend and frontend)
4. Reference [COMMANDS.md](COMMANDS.md) for development commands
5. Use [TROUBLESHOOTING.md](TROUBLESHOOTING.md) when stuck

### For DevOps/Deployment
1. Ensure development environment works
2. Read [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)
3. Set up production environment
4. Configure monitoring and backups
5. Test disaster recovery procedures

## 🆘 Need Help?

### Quick References

| Problem | See |
|---------|-----|
| System won't start | [TROUBLESHOOTING.md](TROUBLESHOOTING.md) |
| Don't know a command | [COMMANDS.md](COMMANDS.md) |
| WhatsApp not working | [TROUBLESHOOTING.md](TROUBLESHOOTING.md) → WhatsApp Issues |
| Database issues | [TROUBLESHOOTING.md](TROUBLESHOOTING.md) → Database Issues |
| Preparing for production | [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) |
| Understanding the code | [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) |

### Debug Steps

1. **Check the logs**
   ```bash
   # Backend
   tail -f backend.log
   
   # Frontend
   # Browser console (F12)
   
   # Database
   docker-compose logs -f postgres
   ```

2. **Verify services are running**
   ```bash
   # Backend
   curl http://localhost:8080/swagger-ui.html
   
   # Frontend
   curl http://localhost:5173
   
   # Database
   docker ps | grep postgres
   ```

3. **Check documentation**
   - Look in [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
   - Search [COMMANDS.md](COMMANDS.md)
   - Review [README.md](README.md)

## 📊 Key Information

### URLs (Default)
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html
- Database: localhost:5432

### Default Credentials
- Username: `admin`
- Password: `admin123`
- ⚠️ Change in production!

### Environment Variables
- Backend: `.env` in root directory
- Frontend: `.env` in frontend/ directory
- Templates: `env.example` files

### Important Commands
```bash
# Start everything
./start.sh

# Stop everything
./stop.sh

# Manual start
docker-compose up -d
mvn spring-boot:run
cd frontend && npm run dev

# Manual stop
pkill -f "spring-boot:run"
pkill -f "vite"
docker-compose down
```

## 📞 Support

### Before Asking for Help

1. Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. Review relevant logs
3. Verify environment configuration
4. Try restarting services

### When Reporting Issues

Include:
- Exact error message
- Steps to reproduce
- Relevant log output
- Environment (OS, Java version, Node version)
- Configuration (without sensitive data)

## 🎉 Quick Wins

### Test if Everything Works

```bash
# 1. Test backend
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. Test database
docker exec -it salvation-army-db psql -U postgres -d salvation_army_db -c "SELECT 1;"

# 3. Test frontend
# Open http://localhost:5173 in browser

# 4. Test webhook
curl "http://localhost:8080/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=test&hub.challenge=123"
```

## 📈 Next Steps

After getting the system running:

1. **Configure WhatsApp** - [README.md](README.md) → WhatsApp Setup
2. **Test the flow** - Send messages to WhatsApp
3. **Explore dashboard** - View records, try filters
4. **Export data** - Test CSV export
5. **Plan deployment** - Review [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)

## 📚 Additional Resources

### External Documentation
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React Docs](https://react.dev/)
- [Tailwind CSS Docs](https://tailwindcss.com/docs)
- [WhatsApp Business API](https://developers.facebook.com/docs/whatsapp)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)

### Helpful Tools
- [Postman](https://www.postman.com/) - API testing
- [pgAdmin](https://www.pgadmin.org/) - PostgreSQL GUI
- [ngrok](https://ngrok.com/) - Webhook testing
- [Swagger UI](http://localhost:8080/swagger-ui.html) - API documentation

---

## 🔍 Document Search

Can't find what you need? Try searching these keywords in the documentation:

- **Setup**: QUICKSTART.md, README.md
- **Commands**: COMMANDS.md
- **Errors**: TROUBLESHOOTING.md
- **Deploy**: DEPLOYMENT_CHECKLIST.md
- **Architecture**: PROJECT_SUMMARY.md
- **WhatsApp**: README.md, TROUBLESHOOTING.md
- **Database**: COMMANDS.md, TROUBLESHOOTING.md
- **Security**: DEPLOYMENT_CHECKLIST.md
- **API**: README.md, Swagger UI

---

**Version**: 1.0.0  
**Last Updated**: January 2026  
**Status**: ✅ Complete

For questions or issues, refer to the appropriate documentation file above.

**Happy Coding! 🚀**






