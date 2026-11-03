const jwt = require('jsonwebtoken');

const JWT_SECRET = 'super_secret_moodify_key';
const token = jwt.sign({ userId: 1 }, JWT_SECRET, { expiresIn: '7d' });

async function test() {
    try {
        console.log("Testing Recommendations...");
        const recRes = await fetch('http://localhost:3000/api/recommendations?mood=Happy', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const recText = await recRes.text();
        console.log(`Recommendations: ${recRes.status} -> ${recText}`);

        console.log("Testing Liked Songs...");
        const likeRes = await fetch('http://localhost:3000/api/liked-songs', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const likeText = await likeRes.text();
        console.log(`Liked Songs: ${likeRes.status} -> ${likeText}`);
    } catch (e) {
        console.error(e);
    }
}
test();
