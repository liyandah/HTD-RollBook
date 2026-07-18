# GitHub & Vercel Deployment Guide — HTD RollBook

## Repository

- **Remote**: https://github.com/liyandah/HTD-RollBook.git
- **Default branch**: `main`
- **Frontend**: React + Vite in `frontend/`
- **Backend**: Spring Boot at repo root (`src/`, `pom.xml`)

## Clone

```bash
git clone https://github.com/liyandah/HTD-RollBook.git
cd HTD-RollBook
```

## Install & build (frontend)

```bash
cd frontend
npm install
npm run build
```

Preview locally:

```bash
npm run preview
```

## Push workflow

```bash
git checkout main
git pull origin main
# make changes...
git add .
git status   # confirm no .env or password scripts
git commit --trailer "Co-authored-by: Cursor <cursoragent@cursor.com>" -m "Describe your change"
git push -u origin main
```

### Windows PowerShell

Use `;` instead of `&&`:

```powershell
cd frontend; npm install; npm run build
```

## Branches & merge

```bash
git checkout -b feature/my-change
# ... commit ...
git push -u origin feature/my-change
# Open a PR on GitHub into main, review, merge
git checkout main
git pull origin main
```

## Vercel deploy (frontend)

1. Connect the GitHub repo in the Vercel dashboard.
2. Set **Root Directory** to `frontend`.
3. Build: `npm run build` → Output: `dist`.
4. Add env: `VITE_API_BASE_URL` (production API origin).
5. Ensure `frontend/vercel.json` SPA rewrite is present (already in repo).
6. Redeploy after each push to `main` (or your production branch).

### Optional: deploy from monorepo root

Root `vercel.json` builds with `cd frontend && npm run build` and outputs `frontend/dist`. Prefer Root Directory = `frontend` for simplicity.

## Rollback

### Git

```bash
git revert <bad-commit-sha>
git push origin main
```

Or redeploy a previous Vercel deployment from the Vercel **Deployments** UI (Promote to Production).

### Vercel

1. Open project → **Deployments**.
2. Select a known-good deployment → **Promote to Production**.

## Secrets checklist

Do **not** commit:

- `.env`, `frontend/.env`
- Deploy/probe scripts with SSH passwords (listed in `.gitignore`)
- Private keys, Dialogflow credential JSON with secrets

Use Vercel / host environment variables and `env.example` templates only.

## Auth troubleshooting (push)

If `git push` fails:

1. `gh auth status` — sign in with `gh auth login` if needed
2. Or use a GitHub PAT as the password over HTTPS
3. Or switch remote to SSH: `git remote set-url origin git@github.com:liyandah/HTD-RollBook.git`