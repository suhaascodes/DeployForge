#!/bin/bash

echo "=== STARTING SUCCESSFUL REACT BUILD VERIFICATION ==="

# Wait for backend to be fully initialized
echo "Checking if DeployForge backend is running on 8080..."
attempt=1
max_attempts=15
until curl -s -o /dev/null http://localhost:8080/v3/api-docs; do
  echo "Waiting for backend... (attempt $attempt/$max_attempts)"
  sleep 2
  attempt=$((attempt+1))
  if [ $attempt -gt $max_attempts ]; then
    echo "ERROR: Backend failed to initialize on port 8080 in time."
    exit 1
  fi
done
echo "Backend is reachable!"

# Attempt login first
echo "1. Attempting login to retrieve existing session token..."
AUTH_RESP=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev-react@deployforge.com","password":"securepassword123"}')

TOKEN=$(echo "$AUTH_RESP" | grep -o '"token":"[^"]*' | grep -o '[^"]*$' || true)

if [ -z "$TOKEN" ]; then
  echo "Session not found. Registering dev-react@deployforge.com profile..."
  curl -s -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"name":"Test React Developer","email":"dev-react@deployforge.com","password":"securepassword123"}'
  
  echo "Logging in to retrieve token..."
  AUTH_RESP=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"dev-react@deployforge.com","password":"securepassword123"}')
  TOKEN=$(echo "$AUTH_RESP" | grep -o '"token":"[^"]*' | grep -o '[^"]*$' || true)
fi

if [ -z "$TOKEN" ]; then
  echo "ERROR: Failed to authenticate user dev-react@deployforge.com"
  exit 1
fi

echo "Auth Token obtained successfully."

# Create Project with valid React repository URL
echo "2. Creating project connected to a React repository..."
PROJECT_RESP=$(curl -s -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Vite-React-App","description":"Vite React Starter","repositoryUrl":"https://github.com/riipandi/vite-react-template"}')

PROJECT_ID=$(echo "$PROJECT_RESP" | grep -o '"id":"[^"]*' | grep -o '[^"]*$' | head -n 1 || true)

if [ -z "$PROJECT_ID" ]; then
  echo "Project creation failed or project already exists. Attempting to fetch projects list..."
  PROJECT_LIST=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/projects)
  PROJECT_ID=$(echo "$PROJECT_LIST" | grep -o '"id":"[^"]*' | grep -o '[^"]*$' | head -n 1 || true)
fi

if [ -z "$PROJECT_ID" ]; then
  echo "ERROR: Failed to obtain Project ID"
  exit 1
fi

echo "Project ID: $PROJECT_ID"

# Trigger deployment
echo "3. Enqueuing deployment task..."
DEPLOY_RESP=$(curl -s -X POST http://localhost:8080/api/deployments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"projectId\":\"$PROJECT_ID\"}")

DEPLOY_ID=$(echo "$DEPLOY_RESP" | grep -o '"id":"[^"]*' | grep -o '[^"]*$' | head -n 1 || true)

if [ -z "$DEPLOY_ID" ]; then
  echo "ERROR: Failed to trigger deployment"
  echo "Response: $DEPLOY_RESP"
  exit 1
fi

echo "Deployment triggered successfully. ID: $DEPLOY_ID"

# Poll execution
echo "4. Polling container orchestrator..."
MAX_ATTEMPTS=45
ATTEMPT=1
STATUS="QUEUED"

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
  STATUS_RESP=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/deployments/$DEPLOY_ID)
  STATUS=$(echo "$STATUS_RESP" | grep -o '"status":"[^"]*' | grep -o '[^"]*$' | head -n 1 || true)
  
  echo "[Attempt $ATTEMPT/$MAX_ATTEMPTS] Status: $STATUS"
  
  if [ "$STATUS" = "RUNNING" ]; then
    echo "=== SUCCESS: Container successfully compiled, started, and checked! ==="
    echo "Deployment URL: $(echo "$STATUS_RESP" | grep -o '"deploymentUrl":"[^"]*' | grep -o '[^"]*$' | head -n 1 || true)"
    echo "Container Name: $(echo "$STATUS_RESP" | grep -o '"containerName":"[^"]*' | grep -o '[^"]*$' | head -n 1 || true)"
    echo "Host Port: $(echo "$STATUS_RESP" | grep -o '"hostPort":[^,]*' | grep -o '[0-9]*' | head -n 1 || true)"
    break
  elif [ "$STATUS" = "BUILD_FAILED" ] || [ "$STATUS" = "RUNTIME_FAILED" ]; then
    echo "=== FAILURE: Deployment failed at state $STATUS ==="
    echo "=== MONGO LOG STREAM ==="
    curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/logs/$DEPLOY_ID | grep -o '"message":"[^"]*' | grep -o '[^"]*$' || true
    exit 1
  fi
  
  sleep 5
  ATTEMPT=$((ATTEMPT+1))
done

if [ "$STATUS" != "RUNNING" ]; then
  echo "Timeout waiting for deployment."
  exit 1
fi
