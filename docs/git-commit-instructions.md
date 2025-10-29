# Conventions for Development

## Git strategy

#### 1\. Start a New Feature Branch from an Updated Main
Always start from the latest `dev` branch.
bash

```bash
git checkout dev
git pull origin dev                         # Ensure you have the latest changes
git checkout -b feature/amazing-feature
```

####   

#### 2\. Do Your Work and Commit Regularly
Work on your feature and make small, focused commits.
bash

```bash
git add .
git commit -m "Add login form component"

git add .
git commit -m "Implement form validation logic"

# ... and so on
```

####   

#### 3\. Periodically Rebase onto Main (The Key Step)
While you are working, the `dev` branch might have moved forward with other people's work. To integrate those changes **into your branch** without a merge commit, you **rebase**.
bash

```bash
# While on your feature branch (feature/amazing-feature)
git fetch origin dev          # Get the latest dev commits, but don't merge yet
git rebase origin/dev
```

**What this does:**
1. Git temporarily saves your commits (`A`, `B`).
2. It fast-forwards your branch to the tip of `origin/dev`.
3. It then replays your saved commits, one by one, on top of the new base.
If there are conflicts, Git will pause and ask you to resolve them for each commit that conflicts.
####   

#### 4\. Resolve Rebase Conflicts (If Any)
This is the most interactive part. When a conflict occurs:
1. **Resolve the conflict** in your files. The conflict markers are the same as for a merge.
2. **Stage the resolved files** with `git add .` (or the specific files).
3. **Continue the rebase** with `git rebase --continue`.
4. Repeat for each conflicting commit.
If it gets too messy, you can always abort with `git rebase --abort` and start over.
####   

#### 5\. Final Integration: Rebase and Fast-Forward
Once your feature is complete and tested, you're ready to integrate it into `dev`.
bash

```bash
# 1. First, do one final rebase onto the latest dev
git fetch origin dev
git rebase origin/dev

# 2. Switch to the dev branch and fast-forward it.
git checkout dev
git merge feature/amazing-feature

git checkout feature/amazing-feature

git push origin feature/amazing-feature
```

Because you just rebased, `feature/amazing-feature` is a direct descendant of `dev`. The `git merge` will be a **fast-forward**, meaning it just moves the `dev` pointer forward, resulting in a perfectly linear history.

**Note**: Steps 2 and 3 might happen separately - you push for a PR, then after review, you merge.

### Complete flow Summary:

```bash
# 1. Start fresh
git checkout dev
git pull origin dev
git checkout -b feature/amazing-feature

# 2. Do work and commit
# ... your commits

# 3. Periodic updates
git fetch origin dev
git rebase origin/dev

# 4. When feature is ready
git fetch origin dev
git rebase origin/dev

# 5. Push for code review
git push origin feature/amazing-feature --force-with-lease

# 6. Create Pull Request (on GitHub)

# 7. After PR approval and merge
git checkout dev
git pull origin dev
git branch -d feature/amazing-feature  # Clean up local branch
```
