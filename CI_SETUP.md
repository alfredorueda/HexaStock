# CI/CD Setup Instructions

This document provides step-by-step instructions for completing the SonarCloud setup after the CI/CD workflow has been deployed.

## Overview

The CI/CD pipeline has been configured with:
- ✅ GitHub Actions workflow that triggers on push to `main` and PRs targeting `main`
- ✅ Java 21 with Maven build and test execution
- ✅ JaCoCo coverage report generation
- ✅ SonarCloud integration (requires manual configuration)
- ✅ Quality Gate enforcement
- ✅ Test result artifacts upload

## Required Manual Steps

### 1. SonarCloud Organization and Project Setup

1. **Visit SonarCloud**: Go to https://sonarcloud.io
2. **Sign in**: Use your GitHub account to sign in
3. **Create Organization** (if you don't have one):
   - Click the "+" icon → "Create new organization"
   - Choose "Free plan" for public repositories
   - Link it to your GitHub account
   - Note the organization key (you'll need this)

4. **Create Project**:
   - Click "Analyze new project"
   - Select "alfredorueda/HexaStock" from your repositories
   - Choose "With GitHub Actions" as the analysis method
   - Note the project key (usually in format `organization-key_repository-name`)

### 2. Generate SonarCloud Token

1. **In SonarCloud**: Click your avatar → "My Account"
2. **Security tab**: Click "Security"
3. **Generate token**: 
   - Name: "HexaStock-CI" 
   - Type: "Project Analysis Token"
   - Select your project
   - Click "Generate"
   - **Copy the token immediately** (you won't see it again)

### 3. Configure GitHub Repository Secrets and Variables

**Go to**: GitHub → alfredorueda/HexaStock → Settings → Secrets and variables → Actions

#### Add Repository Secret:
- **Name**: `SONAR_TOKEN`
- **Value**: The token you generated in step 2

#### Add Repository Variables:
- **Name**: `SONAR_ORGANIZATION`
  - **Value**: Your SonarCloud organization key (e.g., `alfredorueda`)
  
- **Name**: `SONAR_PROJECT_KEY`  
  - **Value**: Your SonarCloud project key (e.g., `alfredorueda_HexaStock`)
  
- **Name**: `SONAR_HOST_URL`
  - **Value**: `https://sonarcloud.io`

### 4. Update README Badge URLs

Once you have your project key, update the badge URLs in README.md:

Replace `${SONAR_PROJECT_KEY}` with your actual project key in these lines:
```markdown
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=YOUR_PROJECT_KEY&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=YOUR_PROJECT_KEY)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=YOUR_PROJECT_KEY&metric=coverage)](https://sonarcloud.io/summary/new_code?id=YOUR_PROJECT_KEY)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=YOUR_PROJECT_KEY&metric=bugs)](https://sonarcloud.io/summary/new_code?id=YOUR_PROJECT_KEY)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=YOUR_PROJECT_KEY&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=YOUR_PROJECT_KEY)
```

### 5. Configure Branch Protection — done, via a ruleset

This step is complete. `main` is protected by a repository **ruleset**
(`Settings → Rules → Rulesets`, not the classic `Settings → Branches` page
this section originally pointed to — GitHub's newer mechanism for the same
purpose). It requires the `build-and-test` check to pass, requires every
change to arrive through a pull request, and applies with no exception for
administrators.

This was verified end to end — a pull request with a deliberately failing
test was confirmed blocked from merging, and a passing pull request was
confirmed mergeable. See
[CI_DISCIPLINE_WALKTHROUGH.md](CI_DISCIPLINE_WALKTHROUGH.md) for the full
explanation and the evidence, or
[CI_DEMO_SCRIPT.md](CI_DEMO_SCRIPT.md) for a condensed run sheet to
reproduce it.

## Testing the Setup

1. **Push a change** to the `main` branch or create a PR
2. **Check Actions tab**: Verify the workflow runs successfully
3. **Check SonarCloud**: Visit your project dashboard to see analysis results
4. **Verify badges**: Confirm badges in README.md display correctly

## Troubleshooting

### Common Issues:

1. **SonarCloud scan fails**: 
   - Verify `SONAR_TOKEN` secret is correct
   - Check that organization and project keys match exactly

2. **Quality Gate not enforcing**:
   - Ensure the quality gate action runs after the scan
   - Check SonarCloud project settings for quality gate configuration

3. **Coverage not showing**:
   - Verify JaCoCo reports are generated in `target/site/jacoco-merged/`
   - Check `sonar-project.properties` paths

4. **Badges not displaying**:
   - Confirm project key replacement in README.md
   - Wait for first successful SonarCloud analysis

## Example Project Keys

If your SonarCloud organization is `alfredorueda`, your project key would typically be:
- `alfredorueda_HexaStock`

## Support

For issues with:
- **GitHub Actions**: Check the Actions tab for detailed logs
- **SonarCloud**: Visit SonarCloud documentation or community forum
- **This setup**: Create an issue in the repository

---

**Note**: This setup provides enterprise-grade CI/CD practices suitable for professional Java development with comprehensive quality checks and automated testing.