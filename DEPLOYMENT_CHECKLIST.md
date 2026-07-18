# Deployment Checklist

Use this checklist before deploying to production.

## 🔐 Security

- [ ] Change default admin credentials in `.env`
  ```properties
  ADMIN_USERNAME=your_secure_username
  ADMIN_PASSWORD=your_secure_password_here
  ```

- [ ] Generate strong JWT secret (at least 256 bits)
  ```bash
  # Linux/Mac
  openssl rand -base64 64
  
  # Windows PowerShell
  -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 64 | % {[char]$_})
  ```

- [ ] Update `JWT_SECRET` in `.env` with generated value

- [ ] Verify all environment variables are set correctly

- [ ] Remove or secure Swagger UI in production
  ```properties
  # Add to application.properties
  springdoc.swagger-ui.enabled=false
  ```

- [ ] Enable HTTPS for both frontend and backend

- [ ] Configure proper CORS origins (remove wildcard)
  ```properties
  CORS_ALLOWED_ORIGINS=https://your-domain.com
  ```

## 🗄️ Database

- [ ] Set up production PostgreSQL database

- [ ] Update database credentials in `.env`
  ```properties
  DATABASE_URL=jdbc:postgresql://prod-host:5432/prod_db
  DATABASE_USERNAME=prod_user
  DATABASE_PASSWORD=strong_prod_password
  ```

- [ ] Run Flyway migrations on production database
  ```bash
  mvn flyway:migrate -Dflyway.url=jdbc:postgresql://prod-host:5432/prod_db
  ```

- [ ] Set up database backups
  ```bash
  # Example cron job for daily backup
  0 2 * * * pg_dump -U prod_user prod_db > /backups/backup-$(date +\%Y\%m\%d).sql
  ```

- [ ] Configure connection pooling for production load

- [ ] Set up monitoring for database performance

## 📁 File Storage

- [ ] Consider cloud storage for images (S3, Azure Blob, etc.)
  - More scalable than local disk
  - Better for multi-instance deployments
  - Automatic backups

- [ ] If using local storage:
  - [ ] Set up persistent volume for uploads
  - [ ] Configure backup strategy for uploads folder
  - [ ] Set appropriate file permissions
  - [ ] Monitor disk space

- [ ] Update `app.upload.dir` if needed

## 🚀 Backend Deployment

- [ ] Build production JAR
  ```bash
  mvn clean package -DskipTests
  ```

- [ ] Test JAR locally
  ```bash
  java -jar target/whatsapp-data-collection-1.0.0.jar
  ```

- [ ] Set up production server (VM, container, etc.)

- [ ] Install Java 17 on production server

- [ ] Copy JAR and `.env` to production server

- [ ] Set up systemd service or process manager
  ```ini
  # Example: /etc/systemd/system/salvation-army.service
  [Unit]
  Description=Salvation Army WhatsApp Service
  After=network.target

  [Service]
  Type=simple
  User=appuser
  WorkingDirectory=/opt/salvation-army
  ExecStart=/usr/bin/java -jar whatsapp-data-collection-1.0.0.jar
  Restart=on-failure

  [Install]
  WantedBy=multi-user.target
  ```

- [ ] Configure reverse proxy (nginx/Apache)
  ```nginx
  # Example nginx config
  server {
    listen 80;
    server_name api.yourdomain.com;
    
    location / {
      proxy_pass http://localhost:8080;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
    }
  }
  ```

- [ ] Set up SSL certificate (Let's Encrypt)

- [ ] Configure logging
  ```properties
  # application.properties
  logging.file.name=/var/log/salvation-army/application.log
  logging.level.org.salvationarmy=INFO
  ```

- [ ] Set up log rotation

## 🎨 Frontend Deployment

- [ ] Update API URL for production
  ```bash
  # In frontend/.env
  VITE_API_URL=https://api.yourdomain.com
  ```

- [ ] Build production bundle
  ```bash
  cd frontend
  npm run build
  ```

- [ ] Test production build locally
  ```bash
  npm run preview
  ```

- [ ] Deploy `dist/` folder to web server
  - Options: Vercel, Netlify, AWS S3 + CloudFront, nginx
  
- [ ] Configure web server for SPA routing
  ```nginx
  # nginx example
  location / {
    try_files $uri $uri/ /index.html;
  }
  ```

- [ ] Set up SSL certificate

- [ ] Configure caching headers

- [ ] Enable gzip compression

## 📱 WhatsApp Configuration

- [ ] Set up production WhatsApp Business account

- [ ] Configure webhook with production URL
  ```
  https://api.yourdomain.com/webhooks/whatsapp
  ```

- [ ] Verify webhook with production token

- [ ] Subscribe to `messages` event

- [ ] Add production phone numbers to allowed list

- [ ] Test message flow end-to-end

- [ ] Set up webhook monitoring/alerting

## 🔍 Monitoring & Logging

- [ ] Set up application monitoring
  - Consider: New Relic, Datadog, Prometheus

- [ ] Set up error tracking
  - Consider: Sentry, Rollbar

- [ ] Configure alerts for:
  - [ ] Server down
  - [ ] Database connection failures
  - [ ] High error rate
  - [ ] Disk space low
  - [ ] High memory usage

- [ ] Set up uptime monitoring
  - Consider: UptimeRobot, Pingdom

- [ ] Configure log aggregation
  - Consider: ELK Stack, Splunk, CloudWatch

## 🧪 Testing

- [ ] Run backend tests
  ```bash
  mvn test
  ```

- [ ] Test all API endpoints
  - [ ] Login
  - [ ] Dashboard statistics
  - [ ] List records
  - [ ] Get record details
  - [ ] Update status
  - [ ] Export CSV
  - [ ] Image serving

- [ ] Test WhatsApp flow
  - [ ] Send text message
  - [ ] Send image
  - [ ] Test restart command
  - [ ] Test help command
  - [ ] Complete full enrollment

- [ ] Test frontend
  - [ ] Login
  - [ ] Dashboard loading
  - [ ] Records table
  - [ ] Filters and search
  - [ ] Pagination
  - [ ] Record details
  - [ ] Image preview
  - [ ] CSV export
  - [ ] Status updates

- [ ] Load testing
  - Test concurrent WhatsApp messages
  - Test dashboard with large dataset

- [ ] Security testing
  - [ ] Test authentication
  - [ ] Test authorization
  - [ ] Test CORS
  - [ ] Test SQL injection prevention
  - [ ] Test XSS prevention

## 📊 Performance

- [ ] Database indexing verified

- [ ] Query performance tested

- [ ] Image optimization (if needed)

- [ ] Frontend bundle size checked
  ```bash
  npm run build
  # Check dist/ folder size
  ```

- [ ] API response times measured

- [ ] CDN configured for static assets (optional)

## 💾 Backup Strategy

- [ ] Database backup schedule
  - Frequency: Daily recommended
  - Retention: 30 days recommended
  - Test restore process

- [ ] Image backup schedule
  - Frequency: Daily or weekly
  - Retention: Based on requirements
  - Test restore process

- [ ] Configuration backup
  - `.env` files
  - Application configurations
  - Store securely (encrypted)

- [ ] Document backup/restore procedures

## 📞 Support & Documentation

- [ ] Document production URLs

- [ ] Document admin credentials (securely)

- [ ] Create runbook for common issues

- [ ] Document deployment process

- [ ] Train support team on:
  - Dashboard usage
  - Troubleshooting
  - WhatsApp configuration
  - Data export

- [ ] Set up support contact/escalation

## 🚨 Disaster Recovery

- [ ] Document recovery procedures

- [ ] Test database restore

- [ ] Test application redeployment

- [ ] Document rollback procedures

- [ ] Maintain previous version for quick rollback

- [ ] Test fail-over scenarios (if multi-instance)

## ✅ Pre-Launch Verification

- [ ] All environment variables configured

- [ ] Database migrations run successfully

- [ ] Application starts without errors

- [ ] All endpoints respond correctly

- [ ] WhatsApp webhook verified

- [ ] Frontend loads correctly

- [ ] Authentication works

- [ ] Images upload and display

- [ ] CSV export works

- [ ] Logs are being written

- [ ] Monitoring alerts are configured

- [ ] Backups are running

- [ ] SSL certificates valid

- [ ] DNS configured correctly

## 📝 Post-Deployment

- [ ] Monitor error logs for 24 hours

- [ ] Check application performance

- [ ] Verify WhatsApp messages being received

- [ ] Test full enrollment flow in production

- [ ] Monitor database performance

- [ ] Check disk space usage

- [ ] Verify backups are working

- [ ] Test dashboard with real data

- [ ] Collect user feedback

- [ ] Document any issues and resolutions

## 🔄 Regular Maintenance

### Daily
- [ ] Check error logs
- [ ] Monitor uptime
- [ ] Verify backups completed

### Weekly
- [ ] Review system performance
- [ ] Check disk space
- [ ] Review error trends
- [ ] Test restore procedure (monthly)

### Monthly
- [ ] Review and rotate logs
- [ ] Update dependencies (security patches)
- [ ] Review access logs
- [ ] Clean up old test data

### Quarterly
- [ ] Security audit
- [ ] Performance review
- [ ] Capacity planning
- [ ] Documentation update

---

## 🆘 Emergency Contacts

Document your emergency contacts:

```
Technical Lead: _______________
Database Admin: _______________
DevOps: _______________
WhatsApp Support: _______________
Hosting Provider: _______________
```

## 📋 Deployment Sign-Off

```
Deployed by: _______________
Date: _______________
Version: 1.0.0
Environment: Production

Verified by: _______________
Date: _______________

Approved by: _______________
Date: _______________
```

---

**Remember**: Always test in staging before production!






