# GitHub & Vercel Deployment Guide

Complete guide for cloning, building, pushing updates, branching, deploying to Vercel, and rolling back the HTD RollBook frontend.

---

## Clone Project

```bash
git clone https://github.com/liyandah/HTD-RollBook.git
cd HTD-RollBook
```

For frontend-only work:

```bash
cd frontend
```

---

## Install Dependencies

```bash
cd frontend
npm install
```

Copy environment variables:

```bash
cp env.example .env
```

Edit `.env` with your local or production API URL.

---

## Build

Development:

```bash
npm start
```

Production build:

```bash
npm run build:prod
```

Verify output in `frontend/dist/`.

Preview production build locally:

```bash
npm run preview
```

---

## Push Updates

1. Check status:

```bash
git status
```

2. Stage changes:

```bash
git add .
```

3. Commit:

```bash
git commit -m "Describe your change"
```

4. Push to main:

```bash
git push origin main
```

Vercel will automatically redeploy if the project is connected to this repository.

---

## Create Branches

Create a feature branch:

```bash
git checkout -b feature/your-feature-name
```

Push the branch to GitHub:

```bash
git push -u origin feature/your-feature-name
```

Open a Pull Request on GitHub to merge into `main`.

---

## Merge Branches

### Via GitHub (recommended)

1. Open the Pull Request on GitHub.
2. Review changes and resolve conflicts if any.
3. Click **Merge pull request**.
4. Delete the feature branch after merge.

### Via command line

```bash
git checkout main
git pull origin main
git merge feature/your-feature-name
git push origin main
```

---

## Deploy to Vercel

### Initial setup

1. Go to [vercel.com](https://vercel.com) and sign in with GitHub.
2. Click **Add New Project** → import `liyandah/HTD-RollBook`.
3. Set **Root Directory** to `frontend`.
4. Configure build settings:
   - **Framework Preset:** Vite
   - **Build Command:** `npm run build:prod`
   - **Output Directory:** `dist`
   - **Install Command:** `npm install`
5. Add environment variable:
   - `VITE_API_BASE_URL` = your production backend URL
6. Click **Deploy**.

### Subsequent deploys

Every push to `main` triggers an automatic production deployment. Pull request branches can get preview deployments if enabled in Vercel settings.

### Custom domain (optional)

1. In Vercel project → **Settings** → **Domains**.
2. Add your domain and follow DNS instructions.

---

## Rollback Procedures

### Vercel rollback (fastest)

1. Open your project in the Vercel dashboard.
2. Go to **Deployments**.
3. Find the last known good deployment.
4. Click **⋯** → **Promote to Production**.

### Git rollback

Revert the last commit and push:

```bash
git revert HEAD
git push origin main
```

Or reset to a specific commit (use with caution on shared branches):

```bash
git log --oneline
git reset --hard <commit-hash>
git push --force origin main
```

> **Warning:** Force push rewrites history. Only use when coordinating with your team.

### Redeploy a previous Git tag

```bash
git checkout v1.0.0
cd frontend && npm install && npm run build:prod
```

Then promote a matching Vercel deployment or create a release branch from that tag.

---

## CI/CD Checklist

Before merging to `main`, confirm:

- [ ] `npm run build:prod` succeeds locally
- [ ] `.env` files are not committed (check `.gitignore`)
- [ ] `VITE_API_BASE_URL` is set in Vercel for production
- [ ] Backend CORS allows your Vercel domain
- [ ] Client-side routes work (handled by `vercel.json` rewrites)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| 404 on page refresh | Ensure `vercel.json` rewrites are present in `frontend/` |
| API calls fail in production | Set `VITE_API_BASE_URL` in Vercel environment variables |
| Build fails on Vercel | Run `npm run build:prod` locally to reproduce errors |
| Old assets cached | Hard refresh or clear CDN cache in Vercel deployment settings |
| Git push rejected | Pull latest: `git pull --rebase origin main` then push again |

---

## Authentication Methods for GitHub

If `git push` fails:

### HTTPS with Personal Access Token (PAT)

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens**.
2. Generate a token with `repo` scope.
3. Use the token as your password when prompted.

### GitHub CLI

```bash
gh auth login
git push -u origin main
```

### SSH Key

```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
# Add public key to GitHub → Settings → SSH and GPG keys
git remote set-url origin git@github.com:liyandah/HTD-RollBook.git
git push -u origin main
```

---

## Repository

**URL:** https://github.com/liyandah/HTD-RollBook

**Default branch:** `main`

**Frontend root for Vercel:** `frontend/`
