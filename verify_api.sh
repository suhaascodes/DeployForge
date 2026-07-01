#!/bin/bash

# Color definitions
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

API_URL="http://localhost:8080/api"

echo -e "${YELLOW}=== STARTING DEPLOYFORGE E2E API VERIFICATION ===${NC}"

# Helper to check if backend is up
echo "Checking if DeployForge backend is running on 8080..."
for i in {1..10}; do
  if curl -s "${API_URL}/auth/login" > /dev/null; then
    echo -e "${GREEN}Backend is reachable!${NC}"
    break
  fi
  if [ $i -eq 10 ]; then
    echo -e "${RED}Backend is unreachable on port 8080. Make sure it is running.${NC}"
    exit 1
  fi
  echo "Waiting for backend... (attempt $i/10)"
  sleep 2
done

# 1. Register a new user
echo -e "\n${YELLOW}1. Registering user 'Test Developer' (dev@deployforge.com)...${NC}"
REG_RES=$(curl -s -X POST "${API_URL}/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Developer", "email": "dev@deployforge.com", "password": "password123"}')

echo "Registration Response:"
echo "$REG_RES" | grep -q "success"
if [ $? -eq 0 ]; then
  echo -e "${GREEN}User registered successfully (or already exists).${NC}"
else
  # Check if it failed due to already in use
  echo "$REG_RES" | grep -q "already in use"
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}User already exists. Proceeding to login...${NC}"
  else
    echo -e "${RED}Registration failed: $REG_RES${NC}"
    exit 1
  fi
fi

# 2. Login and get JWT token
echo -e "\n${YELLOW}2. Logging in to obtain JWT...${NC}"
LOGIN_RES=$(curl -s -X POST "${API_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "dev@deployforge.com", "password": "password123"}')

TOKEN=$(echo "$LOGIN_RES" | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

if [ -z "$TOKEN" ]; then
  echo -e "${RED}Login failed. Response: $LOGIN_RES${NC}"
  exit 1
fi

echo -e "${GREEN}JWT Obtained!${NC}"
AUTH_HEADER="Authorization: Bearer $TOKEN"

# 3. Create project with invalid repo url format
echo -e "\n${YELLOW}3. Testing invalid Repository URL format...${NC}"
BAD_FORMAT_RES=$(curl -s -X POST "${API_URL}/projects" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d '{"name": "Bad Project", "description": "Invalid URL", "repositoryUrl": "not_a_git_url"}')

echo "Response (expected 400):"
echo "$BAD_FORMAT_RES"
echo "$BAD_FORMAT_RES" | grep -q "VALIDATION_FAILED"
if [ $? -eq 0 ] || echo "$BAD_FORMAT_RES" | grep -q "BAD_REQUEST"; then
  echo -e "${GREEN}Success: Backend correctly blocked invalid URL format.${NC}"
else
  echo -e "${RED}Failure: Backend did not restrict bad URL format.${NC}"
fi

# 4. Create project with unreachable repository
echo -e "\n${YELLOW}4. Testing unreachable (private/non-existent) Repository URL...${NC}"
UNREACHABLE_RES=$(curl -s -X POST "${API_URL}/projects" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d '{"name": "Private Project", "description": "Unreachable URL", "repositoryUrl": "https://github.com/this-does-not-exist/private-repo.git"}')

echo "Response (expected 400 / Unreachable):"
echo "$UNREACHABLE_RES"
echo "$UNREACHABLE_RES" | grep -q "unreachable"
if [ $? -eq 0 ] || echo "$UNREACHABLE_RES" | grep -q "BAD_REQUEST"; then
  echo -e "${GREEN}Success: Backend correctly blocked unreachable repository.${NC}"
else
  echo -e "${RED}Failure: Backend accepted unreachable repository URL.${NC}"
fi

# 5. Create project with valid repository
echo -e "\n${YELLOW}5. Creating project with valid repository (octocat/Spoon-Knife)...${NC}"
GOOD_PROJECT_RES=$(curl -s -X POST "${API_URL}/projects" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d '{"name": "Spoon-Knife-App", "description": "A test fork application", "repositoryUrl": "https://github.com/octocat/Spoon-Knife"}')

echo "Response:"
echo "$GOOD_PROJECT_RES"

PROJECT_ID=$(echo "$GOOD_PROJECT_RES" | grep -o '"id":"[^"]*' | head -n 1 | grep -o '[^"]*$')

if [ -z "$PROJECT_ID" ]; then
  echo -e "${RED}Failed to create valid project.${NC}"
  exit 1
fi

echo -e "${GREEN}Project created successfully with ID: $PROJECT_ID${NC}"

# 6. Trigger Deployment
echo -e "\n${YELLOW}6. Triggering deployment for Project: $PROJECT_ID...${NC}"
DEPLOY_RES=$(curl -s -X POST "${API_URL}/deployments" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d "{\"projectId\": \"$PROJECT_ID\"}")

echo "Response:"
echo "$DEPLOY_RES"

DEPLOY_ID=$(echo "$DEPLOY_RES" | grep -o '"id":"[^"]*' | head -n 1 | grep -o '[^"]*$')

if [ -z "$DEPLOY_ID" ]; then
  echo -e "${RED}Failed to trigger deployment.${NC}"
  exit 1
fi

echo -e "${GREEN}Deployment triggered successfully with ID: $DEPLOY_ID${NC}"

# 7. Poll Deployment status and logs
echo -e "\n${YELLOW}7. Polling Asynchronous Deployment execution...${NC}"
PREV_LOG_COUNT=0

for i in {1..30}; do
  STATUS_RES=$(curl -s -X GET "${API_URL}/deployments/${DEPLOY_ID}" -H "${AUTH_HEADER}")
  STATUS=$(echo "$STATUS_RES" | grep -o '"status":"[^"]*' | head -n 1 | grep -o '[^"]*$')
  
  echo -e "\n[Attempt $i/30] Status: ${YELLOW}$STATUS${NC}"
  
  # Fetch logs
  LOGS_RES=$(curl -s -X GET "${API_URL}/logs/${DEPLOY_ID}" -H "${AUTH_HEADER}")
  
  # Extract and print new log lines
  echo "$LOGS_RES" | grep -o '"message":"[^"]*' | sed 's/"message":"//' | tail -n +$((PREV_LOG_COUNT + 1)) | while read -r line; do
    echo -e "  > $line"
  done
  
  # Update logged count
  CURRENT_LOG_COUNT=$(echo "$LOGS_RES" | grep -o '"message":"[^"]*' | wc -l | tr -d ' ')
  PREV_LOG_COUNT=$CURRENT_LOG_COUNT

  if [ "$STATUS" == "RUNNING" ]; then
    LIVE_URL=$(echo "$STATUS_RES" | grep -o '"deploymentUrl":"[^"]*' | head -n 1 | grep -o '[^"]*$')
    echo -e "\n${GREEN}=== SUCCESS: Deployment completed! ===${NC}"
    echo -e "Live Application URL: ${GREEN}$LIVE_URL${NC}"
    break
  fi

  if [ "$STATUS" == "FAILED" ]; then
    echo -e "\n${RED}=== FAILURE: Deployment failed! ===${NC}"
    exit 1
  fi

  sleep 2
done

echo -e "\n${GREEN}=== DEPLOYFORGE E2E VERIFICATION COMPLETED ===${NC}"
