# CloudTracker Server

A Node.js REST API server for the CloudTracker Android app.

## Setup

```bash
cd server
npm install
node server.js
```

The server will start on port 3000 by default.

## Environment Variables

| Variable     | Default              | Description              |
|--------------|----------------------|--------------------------|
| PORT         | 3000                 | Server port              |
| JWT_SECRET   | change-me-in-production | Secret for JWT signing |

Set them before running in production:
```bash
JWT_SECRET=your-very-long-random-secret PORT=8080 node server.js
```

## API Endpoints

| Method | Endpoint          | Auth | Description              |
|--------|-------------------|------|--------------------------|
| POST   | /auth/register    | No   | Register a new user      |
| POST   | /auth/login       | No   | Login, returns JWT token |
| GET    | /data/:key        | Yes  | Retrieve JSON by key     |
| POST   | /data/:key        | Yes  | Store JSON at key        |
| DELETE | /data/:key        | Yes  | Delete a key             |
| GET    | /data             | Yes  | List all keys            |

## Auth

Include the JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

Tokens expire after 24 hours.
