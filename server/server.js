const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
const Database = require('better-sqlite3');

const app = express();
const db = new Database('cloud.db');
app.use(express.json());

const cors = require('cors');
app.use(cors()); // ← add this right after app.use(express.json())


const SECRET = process.env.JWT_SECRET || 'change-me-in-production';



// --- Database setup ---
db.exec(`
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS data_store (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        key TEXT NOT NULL,
        value TEXT NOT NULL,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id),
        UNIQUE(user_id, key)
    );
`);

// --- Auth Middleware ---
function authenticate(req, res, next) {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) return res.status(401).json({ error: 'No token provided' });
    try {
        req.user = jwt.verify(token, SECRET);
        next();
    } catch {
        res.status(401).json({ error: 'Invalid or expired token' });
    }
}

// --- Auth Routes ---

// Register
app.post('/auth/register', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password)
        return res.status(400).json({ error: 'Username and password required' });

    const passwordHash = await bcrypt.hash(password, 10);

    try {
        db.prepare('INSERT INTO users (username, password_hash) VALUES (?, ?)')
            .run(username, passwordHash);
        res.status(201).json({ success: true, message: 'User registered successfully' });
    } catch (err) {
        if (err.message.includes('UNIQUE constraint failed'))
            return res.status(409).json({ error: 'Username already taken' });
        res.status(500).json({ error: 'Registration failed' });
    }
});

// Login
app.post('/auth/login', async (req, res) => {
    const { username, password } = req.body;
    const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username);

    if (!user || !(await bcrypt.compare(password, user.password_hash)))
        return res.status(401).json({ error: 'Invalid credentials' });

    const token = jwt.sign({ userId: user.id }, SECRET, { expiresIn: '24h' });
    res.json({ token });
});

// --- Data Routes ---

// Store JSON at key
app.post('/data/:key', authenticate, (req, res) => {
    const value = JSON.stringify(req.body);
    db.prepare(`
        INSERT INTO data_store (user_id, key, value)
        VALUES (?, ?, ?)
        ON CONFLICT(user_id, key) DO UPDATE SET value = excluded.value, updated_at = CURRENT_TIMESTAMP
    `).run(req.user.userId, req.params.key, value);
    res.json({ success: true });
});

// Retrieve JSON by key
app.get('/data/:key', authenticate, (req, res) => {
    const row = db.prepare('SELECT value FROM data_store WHERE user_id = ? AND key = ?')
        .get(req.user.userId, req.params.key);
    if (!row) return res.status(404).json({ error: 'Key not found' });
    res.json(JSON.parse(row.value));
});

// List all keys for user
app.get('/data', authenticate, (req, res) => {
    const rows = db.prepare('SELECT key, updated_at FROM data_store WHERE user_id = ? ORDER BY updated_at DESC')
        .all(req.user.userId);
    res.json({ keys: rows });
});

// Delete a key
app.delete('/data/:key', authenticate, (req, res) => {
    const result = db.prepare('DELETE FROM data_store WHERE user_id = ? AND key = ?')
        .run(req.user.userId, req.params.key);
    if (result.changes === 0) return res.status(404).json({ error: 'Key not found' });
    res.json({ success: true });
});

// --- Start ---
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`CloudTracker server running on port ${PORT}`));
