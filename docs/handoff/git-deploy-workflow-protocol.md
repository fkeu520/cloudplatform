# Git + Deploy Workflow Protocol

**Established**: 2026-06-22
**Source**: User explicit instruction
**Status**: MANDATORY for all future work

## 3-Step Flow (Strict)

### Step 1: Local Commit (no push)
- I run `git add` + `git commit -m "..."` on Windows
- I show the user the diff / commit hash / files changed
- **STOP. Wait for user confirmation.**
- Forbidden: pushing without explicit "push" / "推送" / "OK push" / "go"

### Step 2: Push + CI Check
- Triggered only by user saying "push" / "推送" / etc.
- I run `git push origin <branch>` (双 pushurl: Gitee + GitHub)
- **STOP. Wait for user confirmation CI passed.**
- If no CI on this commit → skip to Step 3 directly
- Forbidden: SSH to 217 before CI passes / user says "go"

### Step 3: SSH 217 Update
- Triggered only by user saying "CI 过了" / "go" / "更新" / "deploy"
- Login: `ssh hugh@192.168.0.217` (already authorized, no password prompt)
- On 217: `cd ~/work/AI/output/platform && git pull`
- `docker compose pull <service>` / `docker compose up -d <service>` as needed
- Report back: container status, port checks, log tail
- Forbidden: declaring "done" before runtime verification on 217

## Anti-Patterns to Avoid

- ❌ Auto-push after commit (old behavior — banned)
- ❌ Auto-deploy to 217 without user signal (banned)
- ❌ Declare "complete" without 217 runtime verification (banned)
- ❌ Assume "git push = deployed" (banned)

## SSH Connection Details

- Host: `192.168.0.217`
- User: `hugh`
- Auth: pre-authorized key (no password)
- Shell: PowerShell on Windows
- Repo path on 217: `~/work/AI/output/platform` (verify with `pwd` first time)

## What "Done" Means (Revised)

A task is done only when ALL of:
1. Code committed locally
2. Pushed to origin (only after user approval)
3. CI green (only after user confirms)
4. Pulled on 217 (only after user says "go")
5. Service running on 217 (docker compose ps verified)
6. End-to-end smoke test on 217 (curl / browser as applicable)

Anything less = "code shipped, runtime not verified" — must be stated as such.

## Current State Snapshot (2026-06-22)

- W3.6 frontend `f43afd4` is ALREADY pushed (violated new protocol — was old auto-push)
- User has been told to check CI on GitHub Actions
- After CI passes, user will say "go" → I SSH to 217 and run deploy
- For W3.7+ and beyond: STRICT 3-step flow

## Communication Templates

When I commit, end message with:
> "Commit `<hash>` ready. **Waiting for your go-ahead to push.**"

When I push, end with:
> "Pushed. **Waiting for your CI confirmation before 217 deploy.**"

When you give CI green light, I:
> "Starting 217 deploy. Will report: `git pull` output, `docker compose ps`, smoke test."

When deploy complete, I:
> "Deployed. Evidence: [paste docker ps + smoke test results]"
