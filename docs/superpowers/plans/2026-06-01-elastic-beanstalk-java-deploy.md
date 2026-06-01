# Elastic Beanstalk Java Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy a versioned Java Spring Boot web application to AWS Elastic Beanstalk with a GitHub Actions CI/CD pipeline that auto-deploys on every push, plus optional S3 integration for visit logging.

**Architecture:** Spring Boot 3.x JAR packaged via Maven, uploaded as a ZIP source bundle to S3, then deployed to an Elastic Beanstalk Java (Corretto 21) environment. GitHub Actions automates build → S3 upload → EB version creation → EB deployment on every push to `main`. The optional challenge wires a second S3 bucket as a visit log store, with connection details managed via EB environment variables.

**Tech Stack:** Java 21, Spring Boot 3.3, Maven, AWS Elastic Beanstalk (Corretto 21 platform), Amazon S3, GitHub Actions, AWS CLI v2.

> **No tests are written in this plan** — out of scope for this lab.

---

## File Structure

```
.
├── .github/
│   └── workflows/
│       └── deploy.yml                           # CI/CD pipeline
├── .ebextensions/
│   └── environment.config                       # EB JVM options
├── src/
│   └── main/
│       ├── java/com/example/beanstalkdemo/
│       │   ├── BeanstalkDemoApplication.java    # Spring Boot entry point
│       │   ├── controller/
│       │   │   └── AppController.java           # REST endpoints
│       │   └── service/
│       │       └── S3Service.java               # (Task 6) S3 visit logging
│       └── resources/
│           └── application.properties           # Port + env var references
├── Procfile                                     # EB start command
└── pom.xml                                      # Maven build
```

---

## Task 1: Create Spring Boot Application Skeleton

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/beanstalkdemo/BeanstalkDemoApplication.java`
- Create: `src/main/resources/application.properties`

- [ ] **Step 1: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>beanstalk-demo</artifactId>
    <version>1.0.0</version>
    <name>beanstalk-demo</name>
    <description>Java app deployed via Elastic Beanstalk</description>
    <packaging>jar</packaging>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- S3 SDK for optional challenge (Task 6) -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>2.25.60</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
        <finalName>beanstalk-demo</finalName>
    </build>
</project>
```

- [ ] **Step 2: Create main application class**

Create `src/main/java/com/example/beanstalkdemo/BeanstalkDemoApplication.java`:
```java
package com.example.beanstalkdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BeanstalkDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(BeanstalkDemoApplication.class, args);
    }
}
```

- [ ] **Step 3: Create `src/main/resources/application.properties`**

```properties
server.port=5000
app.version=1.0.0
```

> EB expects the app to listen on port **5000** by default for the Java (Corretto) platform.

- [ ] **Step 4: Verify the project compiles**

Run from the project root (requires Java 21 and Maven installed locally):
```bash
mvn clean compile -q
```
Expected: `BUILD SUCCESS` with no errors.

---

## Task 2: Implement REST Controller

**Files:**
- Create: `src/main/java/com/example/beanstalkdemo/controller/AppController.java`

- [ ] **Step 1: Create `AppController`**

Create `src/main/java/com/example/beanstalkdemo/controller/AppController.java`:
```java
package com.example.beanstalkdemo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AppController {

    @Value("${app.version:unknown}")
    private String version;

    @GetMapping("/")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "message", "Java app deployed via Elastic Beanstalk"
        );
    }

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of(
            "version", version,
            "platform", "AWS Elastic Beanstalk"
        );
    }
}
```

- [ ] **Step 2: Verify the app starts locally**

```bash
mvn clean package -q
java -jar target/beanstalk-demo.jar &
sleep 5
curl -s http://localhost:5000/
curl -s http://localhost:5000/version
kill %1
```
Expected: Both endpoints return JSON. Kill the background process after verifying.

---

## Task 3: Add Elastic Beanstalk Configuration Files

**Files:**
- Create: `Procfile`
- Create: `.ebextensions/environment.config`

- [ ] **Step 1: Create `Procfile`**

```
web: java -jar beanstalk-demo.jar
```

> The JAR name matches `<finalName>` in `pom.xml`.

- [ ] **Step 2: Create `.ebextensions/environment.config`**

```yaml
option_settings:
  aws:elasticbeanstalk:application:environment:
    SERVER_PORT: "5000"
  aws:elasticbeanstalk:container:java:jvmoptions:
    Xms: 256m
    Xmx: 512m
```

---

## Task 4: Initial Manual AWS Setup (One-Time Console Steps)

> **These steps are performed in the AWS Console / AWS CLI — not automated. Complete them once before CI/CD is configured.**

**Prerequisites:** AWS account with EB + S3 + IAM permissions. AWS CLI v2 installed and configured (`aws configure`).

- [ ] **Step 1: Create the S3 bucket for deployment bundles**

```bash
aws s3 mb s3://YOUR_DEPLOY_BUCKET_NAME --region YOUR_AWS_REGION
```
Expected: `make_bucket: YOUR_DEPLOY_BUCKET_NAME`

- [ ] **Step 2: Build and package the initial source bundle**

```bash
mvn clean package -q
mkdir -p bundle
cp target/beanstalk-demo.jar bundle/
cp Procfile bundle/
cp -r .ebextensions bundle/
cd bundle && zip -r ../beanstalk-demo-v1.0.0.zip . && cd ..
```
Expected: `beanstalk-demo-v1.0.0.zip` created at project root.

- [ ] **Step 3: Upload initial bundle to S3**

```bash
aws s3 cp beanstalk-demo-v1.0.0.zip s3://YOUR_DEPLOY_BUCKET_NAME/beanstalk-demo-v1.0.0.zip
```

- [ ] **Step 4: Create the Elastic Beanstalk application**

```bash
aws elasticbeanstalk create-application \
  --application-name beanstalk-java-demo \
  --description "Java Spring Boot app on Elastic Beanstalk"
```

- [ ] **Step 5: Create the initial application version**

```bash
aws elasticbeanstalk create-application-version \
  --application-name beanstalk-java-demo \
  --version-label v1.0.0 \
  --source-bundle S3Bucket=YOUR_DEPLOY_BUCKET_NAME,S3Key=beanstalk-demo-v1.0.0.zip \
  --description "Initial deployment"
```

- [ ] **Step 6: Find the latest Corretto 21 solution stack name**

```bash
aws elasticbeanstalk list-available-solution-stacks \
  --query "SolutionStacks[?contains(@, 'Corretto 21')]" \
  --output text | head -1
```
Copy the full string (e.g. `64bit Amazon Linux 2023 v4.x.x running Corretto 21`).

- [ ] **Step 7: Create the Elastic Beanstalk environment**

```bash
aws elasticbeanstalk create-environment \
  --application-name beanstalk-java-demo \
  --environment-name beanstalk-java-demo-env \
  --solution-stack-name "64bit Amazon Linux 2023 v4.x.x running Corretto 21" \
  --version-label v1.0.0 \
  --option-settings \
    Namespace=aws:elasticbeanstalk:environment,OptionName=EnvironmentType,Value=SingleInstance \
    Namespace=aws:autoscaling:launchconfiguration,OptionName=IamInstanceProfile,Value=aws-elasticbeanstalk-ec2-role
```

> `SingleInstance` avoids load balancer costs for this lab.

- [ ] **Step 8: Wait for the environment and retrieve the URL**

```bash
aws elasticbeanstalk wait environment-updated \
  --application-name beanstalk-java-demo \
  --environment-names beanstalk-java-demo-env

aws elasticbeanstalk describe-environments \
  --application-name beanstalk-java-demo \
  --environment-names beanstalk-java-demo-env \
  --query "Environments[0].CNAME" \
  --output text
```
Expected: a URL like `beanstalk-java-demo-env.eba-xxxxxxxx.REGION.elasticbeanstalk.com`

- [ ] **Step 9: Verify the live endpoints**

```bash
curl -s http://YOUR_EB_URL/
curl -s http://YOUR_EB_URL/version
```
Expected: JSON from both endpoints.

---

## Task 5: GitHub Actions CI/CD Workflow

**Files:**
- Create: `.github/workflows/deploy.yml`

**GitHub Actions secrets to add** (repository → Settings → Secrets → Actions):

| Secret | Value |
|--------|-------|
| `AWS_ACCESS_KEY_ID` | IAM user access key |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key |
| `AWS_REGION` | e.g. `us-east-1` |
| `S3_DEPLOY_BUCKET` | Bucket name from Task 4 Step 1 |
| `EB_APPLICATION_NAME` | `beanstalk-java-demo` |
| `EB_ENVIRONMENT_NAME` | `beanstalk-java-demo-env` |

- [ ] **Step 1: Create `.github/workflows/deploy.yml`**

```yaml
name: Deploy to Elastic Beanstalk

on:
  push:
    branches:
      - main

permissions:
  contents: read

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'corretto'
          cache: maven

      - name: Build with Maven
        run: mvn clean package -q --no-transfer-progress

      - name: Generate version label
        id: version
        run: |
          VERSION="v$(date -u +%Y%m%d%H%M%S)-${GITHUB_SHA::8}"
          echo "label=$VERSION" >> "$GITHUB_OUTPUT"
          echo "bundle=beanstalk-demo-${VERSION}.zip" >> "$GITHUB_OUTPUT"

      - name: Package source bundle
        run: |
          mkdir -p bundle
          cp target/beanstalk-demo.jar bundle/
          cp Procfile bundle/
          cp -r .ebextensions bundle/
          cd bundle
          zip -r "../${{ steps.version.outputs.bundle }}" .

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}

      - name: Upload bundle to S3
        run: |
          aws s3 cp "${{ steps.version.outputs.bundle }}" \
            "s3://${{ secrets.S3_DEPLOY_BUCKET }}/${{ steps.version.outputs.bundle }}"

      - name: Create Elastic Beanstalk application version
        run: |
          aws elasticbeanstalk create-application-version \
            --application-name "${{ secrets.EB_APPLICATION_NAME }}" \
            --version-label "${{ steps.version.outputs.label }}" \
            --source-bundle "S3Bucket=${{ secrets.S3_DEPLOY_BUCKET }},S3Key=${{ steps.version.outputs.bundle }}" \
            --description "Deployed from ${{ github.sha }} by ${{ github.actor }}"

      - name: Deploy to Elastic Beanstalk
        run: |
          aws elasticbeanstalk update-environment \
            --application-name "${{ secrets.EB_APPLICATION_NAME }}" \
            --environment-name "${{ secrets.EB_ENVIRONMENT_NAME }}" \
            --version-label "${{ steps.version.outputs.label }}"

      - name: Wait for deployment to complete
        run: |
          aws elasticbeanstalk wait environment-updated \
            --application-name "${{ secrets.EB_APPLICATION_NAME }}" \
            --environment-names "${{ secrets.EB_ENVIRONMENT_NAME }}"
          echo "Deployment complete: ${{ steps.version.outputs.label }}"
```

---

## Task 6: S3 Visit Logging — Optional Challenge

**Files:**
- Create: `src/main/java/com/example/beanstalkdemo/service/S3Service.java`
- Modify: `src/main/java/com/example/beanstalkdemo/controller/AppController.java`
- Modify: `src/main/resources/application.properties`

**AWS Setup (once):**

- [ ] **Step 1: Create a second S3 bucket for visit logs**

```bash
aws s3 mb s3://YOUR_VISITS_BUCKET_NAME --region YOUR_AWS_REGION
```

- [ ] **Step 2: Grant the EB EC2 role S3 access**

```bash
aws iam attach-role-policy \
  --role-name aws-elasticbeanstalk-ec2-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

> In production, scope this to the specific bucket with a least-privilege policy.

- [ ] **Step 3: Add EB environment variable for the visits bucket**

AWS Console → Elastic Beanstalk → `beanstalk-java-demo-env` → **Configuration** → **Updates, monitoring, and logging** → **Environment properties** → Add:
- Key: `S3_VISITS_BUCKET`
- Value: `YOUR_VISITS_BUCKET_NAME`

Click **Apply** and wait for the environment to update.

**Code changes:**

- [ ] **Step 4: Update `application.properties`**

```properties
server.port=5000
app.version=1.0.0
s3.visits.bucket=${S3_VISITS_BUCKET:}
s3.region=${AWS_REGION:us-east-1}
```

- [ ] **Step 5: Create `S3Service`**

Create `src/main/java/com/example/beanstalkdemo/service/S3Service.java`:
```java
package com.example.beanstalkdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3;
    private final String bucket;

    public S3Service(
        @Value("${s3.region:us-east-1}") String region,
        @Value("${s3.visits.bucket:}") String bucket
    ) {
        this.s3 = S3Client.builder()
            .region(Region.of(region))
            .build();
        this.bucket = bucket;
    }

    public String recordVisit(String path) {
        String key = "visits/" + UUID.randomUUID() + ".json";
        String body = String.format(
            "{\"path\":\"%s\",\"timestamp\":\"%s\"}",
            path, Instant.now()
        );
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/json")
                .build(),
            RequestBody.fromString(body)
        );
        return key;
    }
}
```

- [ ] **Step 6: Add `/visit` endpoint to `AppController`**

Replace the entire contents of `src/main/java/com/example/beanstalkdemo/controller/AppController.java`:
```java
package com.example.beanstalkdemo.controller;

import com.example.beanstalkdemo.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AppController {

    @Value("${app.version:unknown}")
    private String version;

    private final S3Service s3Service;

    public AppController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "message", "Java app deployed via Elastic Beanstalk"
        );
    }

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of(
            "version", version,
            "platform", "AWS Elastic Beanstalk"
        );
    }

    @GetMapping("/visit")
    public Map<String, String> recordVisit() {
        String key = s3Service.recordVisit("/visit");
        return Map.of(
            "status", "recorded",
            "s3Key", key,
            "dataStore", "Amazon S3"
        );
    }
}
```

- [ ] **Step 7: Verify local build still compiles**

```bash
mvn clean package -q
```
Expected: `BUILD SUCCESS`.

---

## Task 7: Trigger Redeployment and Validate Version Bump

**Files:**
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Bump the version**

Change `app.version=1.0.0` to `app.version=1.1.0` in `src/main/resources/application.properties`.

- [ ] **Step 2: Push to trigger CI/CD**

Push all changes to `main`. Navigate to GitHub repo → **Actions** tab to watch the workflow (expected: 3–6 min).

- [ ] **Step 3: Validate the updated version**

```bash
curl -s http://YOUR_EB_URL/version
```
Expected: `{"version":"1.1.0","platform":"AWS Elastic Beanstalk"}`

- [ ] **Step 4: Validate S3 visit logging (if Task 6 completed)**

```bash
curl -s http://YOUR_EB_URL/visit
```
Expected: `{"status":"recorded","s3Key":"visits/UUID.json","dataStore":"Amazon S3"}`

Then confirm the object exists in S3:
```bash
aws s3 ls s3://YOUR_VISITS_BUCKET_NAME/visits/
```

- [ ] **Step 5: Confirm version history in EB console**

AWS Console → Elastic Beanstalk → `beanstalk-java-demo` → **Application versions** — you should see the initial `v1.0.0` plus at least one timestamped CI/CD version.

---

## Self-Review Against Spec

| Requirement | Covered by |
|-------------|-----------|
| Java application | Tasks 1–3 |
| Maven build files | Task 1 (`pom.xml`) |
| Returns response confirming deployment | Task 2 (`/`, `/version`) |
| EB application + environment | Task 4 |
| Initial deployment via S3 source bundle | Task 4 Steps 2–9 |
| Publicly accessible EB endpoint | Task 4 |
| GitHub Actions packages into ZIP | Task 5 |
| GitHub Actions uploads to S3 | Task 5 |
| GitHub Actions creates EB version | Task 5 |
| Auto-deploys on push, no manual intervention | Task 5 |
| Versioned releases | Task 5 (timestamp+SHA label), Task 7 |
| External service integration — S3 visit log | Task 6 |
| Env vars for connection details | Task 6 Step 3 (EB env property) |
| Redeployment demo | Task 7 |
