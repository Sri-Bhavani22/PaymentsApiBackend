#!/usr/bin/env pwsh
# ============================================================
# GitHub PR Automation Script
# Usage: .\scripts\pr-automation.ps1 -Action <action> [options]
# ============================================================

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("create", "merge", "status", "add-reviewer", "list", "close")]
    [string]$Action,

    [string]$Branch = (git rev-parse --abbrev-ref HEAD),
    [string]$Base = "main",
    [string]$Title,
    [string]$Body,
    [string]$Reviewers,   # comma-separated usernames
    [string]$Labels,      # comma-separated labels
    [int]$PRNumber,
    [ValidateSet("merge", "squash", "rebase")]
    [string]$MergeMethod = "squash",
    [switch]$AutoMerge,
    [switch]$Draft,
    [switch]$DeleteBranch
)

# ---- Helpers -------------------------------------------------------

function Check-GHInstalled {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        Write-Error "GitHub CLI (gh) is not installed. Run: winget install --id GitHub.cli"
        exit 1
    }
}

function Get-PRNumber {
    param([string]$BranchName)
    $num = gh pr view $BranchName --json number --jq ".number" 2>$null
    if (-not $num) {
        Write-Error "No PR found for branch: $BranchName"
        exit 1
    }
    return [int]$num
}

# ---- Actions -------------------------------------------------------

function Create-PR {
    Write-Host "`n[CREATE] Creating PR from '$Branch' -> '$Base'" -ForegroundColor Cyan

    # Push branch if not already pushed
    git push origin $Branch 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Pushing branch to origin..." -ForegroundColor Yellow
        git push --set-upstream origin $Branch
    }

    $args = @(
        "pr", "create",
        "--base", $Base,
        "--head", $Branch
    )

    if ($Title)     { $args += "--title", $Title }
    else            { $args += "--fill" }  # auto-fill from commits

    if ($Body)      { $args += "--body", $Body }
    if ($Reviewers) { $args += "--reviewer", $Reviewers }
    if ($Labels)    { $args += "--label", $Labels }
    if ($Draft)     { $args += "--draft" }

    $result = & gh @args
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[SUCCESS] PR created: $result" -ForegroundColor Green
    } else {
        Write-Error "[FAILED] Could not create PR."
    }
}

function Get-PRStatus {
    Write-Host "`n[STATUS] PR Status for branch: '$Branch'" -ForegroundColor Cyan

    # Overall status
    gh pr status

    Write-Host "`n[CHECKS] CI/CD Check Runs:" -ForegroundColor Cyan
    $prNum = if ($PRNumber) { $PRNumber } else { Get-PRNumber $Branch }
    gh pr checks $prNum

    Write-Host "`n[DETAILS] PR Details:" -ForegroundColor Cyan
    gh pr view $prNum --json number,title,state,author,reviewDecision,mergeable,statusCheckRollup `
        | ConvertFrom-Json `
        | Format-List
}

function Add-PRReviewer {
    $prNum = if ($PRNumber) { $PRNumber } else { Get-PRNumber $Branch }
    if (-not $Reviewers) {
        Write-Error "Provide -Reviewers 'user1,user2'"
        exit 1
    }
    Write-Host "`n[REVIEWER] Adding reviewers '$Reviewers' to PR #$prNum" -ForegroundColor Cyan
    gh pr edit $prNum --add-reviewer $Reviewers
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[SUCCESS] Reviewers added." -ForegroundColor Green
    }
}

function Merge-PR {
    $prNum = if ($PRNumber) { $PRNumber } else { Get-PRNumber $Branch }
    Write-Host "`n[MERGE] Merging PR #$prNum using '$MergeMethod'" -ForegroundColor Cyan

    $args = @("pr", "merge", $prNum, "--$MergeMethod")
    if ($AutoMerge)     { $args += "--auto" }
    if ($DeleteBranch)  { $args += "--delete-branch" }

    & gh @args
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[SUCCESS] PR #$prNum merged." -ForegroundColor Green
    } else {
        Write-Error "[FAILED] Merge failed. Check for unresolved conflicts or pending checks."
    }
}

function List-PRs {
    Write-Host "`n[LIST] Open Pull Requests:" -ForegroundColor Cyan
    gh pr list --state open --json number,title,author,headRefName,reviewDecision,createdAt `
        --template '{{range .}}PR #{{.number}} | {{.title}} | {{.author.login}} | {{.reviewDecision}}{{"\n"}}{{end}}'
}

function Close-PR {
    $prNum = if ($PRNumber) { $PRNumber } else { Get-PRNumber $Branch }
    Write-Host "`n[CLOSE] Closing PR #$prNum" -ForegroundColor Yellow
    gh pr close $prNum
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[SUCCESS] PR #$prNum closed." -ForegroundColor Green
    }
}

# ---- Conflict Resolution Helper ------------------------------------

function Resolve-Conflicts {
    param([string]$TargetBranch = "main")
    Write-Host "`n[REBASE] Rebasing '$Branch' on '$TargetBranch' to resolve conflicts..." -ForegroundColor Cyan

    git fetch origin
    git checkout $Branch
    git rebase "origin/$TargetBranch"

    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ACTION NEEDED] Conflicts detected. Fix files, then run:" -ForegroundColor Yellow
        Write-Host "  git add ."
        Write-Host "  git rebase --continue"
        Write-Host "  git push --force-with-lease origin $Branch"
    } else {
        git push --force-with-lease origin $Branch
        Write-Host "[SUCCESS] Branch rebased and pushed cleanly." -ForegroundColor Green
    }
}

# ---- Entry Point ---------------------------------------------------

Check-GHInstalled

switch ($Action) {
    "create"       { Create-PR }
    "status"       { Get-PRStatus }
    "add-reviewer" { Add-PRReviewer }
    "merge"        { Merge-PR }
    "list"         { List-PRs }
    "close"        { Close-PR }
}
