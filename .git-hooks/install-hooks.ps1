# PowerShell script to install git hooks
Write-Host "Installing git hooks for HireSync project..." -ForegroundColor Cyan

# Set hooks directory
Write-Host "Setting git hooks path to .git-hooks directory" -ForegroundColor Yellow
git config core.hooksPath .git-hooks

# Check if the config was set correctly
$hooksPath = git config --get core.hooksPath
if ($hooksPath -eq ".git-hooks") {
    Write-Host "✅ Git hooks installed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "The following hooks are now active:" -ForegroundColor Cyan
    Write-Host "  - pre-commit: Runs quick code quality checks before committing" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To bypass hooks temporarily, use: set SKIP_HOOKS=1 && git commit" -ForegroundColor Yellow
} else {
    Write-Host "❌ Failed to set git hooks path. Please configure manually:" -ForegroundColor Red
    Write-Host "git config core.hooksPath .git-hooks" -ForegroundColor Red
} 