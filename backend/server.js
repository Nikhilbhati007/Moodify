const express = require('express');
const cors = require('cors');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { PrismaClient } = require('@prisma/client');
require('dotenv').config();

const app = express();
const prisma = new PrismaClient({ log: ['info', 'warn', 'error'] });

app.use(cors());
app.use(express.json());

const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_moodify_key';

// Dummy implementation of Spotify API to just return Mock tracks for now
// We removed MOCK_TRACKS to stream real music via iTunes Search API

// ---- AUTHENTICATION ROUTES ----

app.post('/api/auth/register', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password required' });
  }

  try {
    const existingUser = await prisma.user.findUnique({ where: { username } });
    if (existingUser) {
      return res.status(400).json({ error: 'Username already exists' });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const newUser = await prisma.user.create({
      data: { username, passwordHash }
    });

    res.status(201).json({ message: 'User registered successfully', userId: newUser.id });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password required' });
  }

  try {
    const user = await prisma.user.findUnique({ where: { username } });
    if (!user) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const isValidPassword = await bcrypt.compare(password, user.passwordHash);
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const token = jwt.sign({ userId: user.id }, JWT_SECRET, { expiresIn: '7d' });
    res.status(200).json({ message: 'Login successful', token });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Middleware to protect routes
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) return res.sendStatus(401);

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.sendStatus(403);
    req.user = user;
    next();
  });
};

// ---- CORE MUSIC ROUTES (Phase 3 Prep) ----

// Cache to speed up search and recommendations
const searchCache = new Map();

async function searchMusic(query, lang = 'English', limit = 25, offset = 0) {
  const cacheKey = `${query}_${lang}_${limit}_${offset}`;
  if (searchCache.has(cacheKey)) {
    return searchCache.get(cacheKey);
  }

  const isIndicLang = ['hindi', 'punjabi', 'tamil', 'telugu'].includes(lang.toLowerCase());
  let apiUrl = `https://itunes.apple.com/search?term=${encodeURIComponent(query)}&entity=song&limit=${limit}&offset=${offset}`;

  if (isIndicLang) {
    apiUrl += '&country=IN';
  }

  const fetchTracks = async (url) => {
    const res = await fetch(url);
    if (!res.ok) {
      throw new Error('Music search failed: ' + await res.text());
    }
    const data = await res.json();
    return data.results.map(track => ({
      trackId: track.trackId.toString(),
      title: track.trackName || 'Unknown Title',
      artist: track.artistName || 'Unknown Artist',
      imageUrl: track.artworkUrl100?.replace('100x100', '300x300') || null,
      previewUrl: track.previewUrl
    })).filter(t => t.previewUrl);
  };

  let tracks = await fetchTracks(apiUrl);

  // Fallback retry: If search did not use country=IN and returned very few results (fewer than 5),
  // retry the search with &country=IN to capture Hindi/Punjabi regional tracks.
  if (!isIndicLang && tracks.length < 5) {
    try {
      const fallbackUrl = apiUrl + '&country=IN';
      const fallbackTracks = await fetchTracks(fallbackUrl);
      if (fallbackTracks.length > tracks.length) {
        tracks = fallbackTracks;
      }
    } catch (fallbackError) {
      console.warn('Fallback search failed, using original results:', fallbackError.message);
    }
  }

  searchCache.set(cacheKey, tracks);
  if (searchCache.size > 200) searchCache.clear(); // Keep small footprint
  return tracks;
}

app.get('/api/recommendations', authenticateToken, async (req, res) => {
  const mood = req.query.mood || 'Neutral';
  const lang = req.query.lang || 'English';
  const page = parseInt(req.query.page) || 1;
  const limit = 25;
  const offset = (page - 1) * limit;

  try {
    const mappedMood = mood.toLowerCase() === 'neutral' ? 'Chill' : mood;
    let searchTerm = `${lang} ${mappedMood} song`;
    
    if (lang.toLowerCase() === 'hindi') {
        searchTerm = `hindi ${mappedMood} bollywood`;
    } else if (lang.toLowerCase() === 'punjabi') {
        searchTerm = `punjabi ${mappedMood}`;
    }

    const tracks = await searchMusic(searchTerm, lang, limit, offset);

    res.status(200).json({
      mood_detected: mood,
      recommendations: tracks
    });
  } catch (err) {
    console.error('Music API Error:', err);
    res.status(500).json({ error: 'Failed to fetch recommendations' });
  }
});

app.get('/api/search', authenticateToken, async (req, res) => {
  const query = req.query.q || '';
  const page = parseInt(req.query.page) || 1;
  const limit = 25;
  const offset = (page - 1) * limit;

  if (!query) {
    return res.status(400).json({ error: 'Search query is required' });
  }

  try {
    // Read optional lang query parameter, default to English
    let lang = req.query.lang || 'English';
    
    // Auto-detect language preference from query keywords if it's default 'English'
    if (lang.toLowerCase() === 'english') {
      const qLower = query.toLowerCase();
      if (/hindi|bollywood|arijit|shreya|kishore|rafi|lata|sonu|udit|sukhwinder|atif|darshan|jubin|mithoon|nehru/i.test(qLower)) {
        lang = 'Hindi';
      } else if (/punjabi|punjab|diljit|sidhu|moosewala|aujla|dhillon|ap dhillon|bhangra|karan|shinda|amrinder|gippy|ikky|satisfya|imran/i.test(qLower)) {
        lang = 'Punjabi';
      } else if (/tamil|anirudh|rahman|ilayaraja|yuvan|spb/i.test(qLower)) {
        lang = 'Tamil';
      } else if (/telugu|keeravani|dsp|sid sriram|thaman/i.test(qLower)) {
        lang = 'Telugu';
      }
    }

    const tracks = await searchMusic(query, lang, limit, offset);
    res.status(200).json({
      query: query,
      recommendations: tracks
    });
  } catch (err) {
    console.error('Music Search API Error:', err);
    res.status(500).json({ error: 'Failed to search music' });
  }
});

app.get('/api/ai-recommend', authenticateToken, async (req, res) => {
  const title = req.query.title;
  const artist = req.query.artist;

  if (!title || !artist) {
    return res.status(400).json({ error: 'Title and artist are required' });
  }

  try {
    const grokKey = process.env.GROK_API_KEY;
    console.log('Fetching Grok AI recommendations for:', title, artist);

    if (!grokKey) {
       return res.status(500).json({ error: 'Grok API Key is missing on the server.' });
    }

    const payload = {
      messages: [
        {
          role: "system",
          content: "You are a music recommendation engine. Respond ONLY with a comma-separated list of exactly 5 similar tracks in the format: Title by Artist, Title by Artist. Do not include numbering, quotes, or conversational text. Just the tracks separated by commas."
        },
        {
          role: "user",
          content: `Recommend 5 similar songs to "${title}" by "${artist}".`
        }
      ],
      model: "grok-2-latest",
      stream: false,
      temperature: 0
    };

    const gRes = await fetch('https://api.x.ai/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${grokKey}`
      },
      body: JSON.stringify(payload)
    });

    if (!gRes.ok) {
       const errorText = await gRes.text();
       console.error('Grok API Error Response:', errorText);
       throw new Error(`Grok API Error: ${errorText}`);
    }

    const data = await gRes.json();
    console.log('Grok API Response:', JSON.stringify(data, null, 2));
    const resultText = data.choices[0].message.content.trim();
    
    const recommendations = resultText.split(',').map(s => s.trim()).filter(s => s);
    
    // Gather matching tracks using iTunes lookup
    const finalTracks = [];
    for (const rec of recommendations) {
       const parts = rec.split(' by ');
       const t = parts[0];
       const a = parts.length > 1 ? parts[1] : '';
       if (t) {
         try {
           const tracks = await searchMusic(rec, 'English', 1, 0); // use full "rec" as query
           if (tracks && tracks.length > 0) {
             finalTracks.push(tracks[0]);
           }
         } catch (e) {
             console.error("Lookup error for", rec, e);
         }
       }
    }

    res.status(200).json({
      recommendations: finalTracks
    });

  } catch (err) {
    console.error('AI Recommend API Error:', err);
    res.status(500).json({ error: 'Failed to fetch AI recommendations' });
  }
});

app.get('/api/liked-songs', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const liked = await prisma.likedSong.findMany({ 
      where: { userId },
      orderBy: { createdAt: 'desc' }
    });
    const tracks = liked.map(l => ({
        trackId: l.trackId,
        title: l.title,
        artist: l.artist,
        imageUrl: l.imageUrl,
        previewUrl: l.previewUrl
    }));
    res.json(tracks);
  } catch (error) {
    console.error("GET Liked Songs Error:", error);
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

app.post('/api/liked-songs', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { trackId, title, artist, imageUrl, previewUrl } = req.body;
    
    const existing = await prisma.likedSong.findUnique({
      where: { userId_trackId: { userId, trackId } }
    });

    if (existing) {
      await prisma.likedSong.delete({ where: { id: existing.id } });
      res.status(200).json({ message: 'Song unliked', status: 'removed' });
    } else {
      await prisma.likedSong.create({
        data: { trackId, title, artist, imageUrl, previewUrl, userId }
      });
      res.status(201).json({ message: 'Song liked', status: 'added' });
    }
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ---- PLAYLIST ROUTES ----

app.get('/api/playlists', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const playlists = await prisma.playlist.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' }
    });
    res.json(playlists);
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/playlists', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { name, mood_tag } = req.body;
    
    if (!name) return res.status(400).json({ error: 'Playlist name required' });

    const newPlaylist = await prisma.playlist.create({
      data: { name, mood_tag: mood_tag || 'Custom', userId }
    });
    res.status(201).json(newPlaylist);
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.get('/api/playlists/:id/tracks', authenticateToken, async (req, res) => {
  try {
    const playlistId = parseInt(req.params.id);
    const userId = req.user.userId;
    
    const playlist = await prisma.playlist.findUnique({ where: { id: playlistId } });
    if (!playlist || playlist.userId !== userId) return res.status(403).json({ error: 'Forbidden' });

    const tracks = await prisma.playlistTrack.findMany({
      where: { playlistId },
      orderBy: { addedAt: 'desc' }
    });
    
    const dtos = tracks.map(t => ({
      trackId: t.trackId,
      title: t.title,
      artist: t.artist,
      imageUrl: t.imageUrl,
      previewUrl: t.previewUrl
    }));
    res.json(dtos);
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/playlists/:id/tracks', authenticateToken, async (req, res) => {
  try {
    const playlistId = parseInt(req.params.id);
    const userId = req.user.userId;
    const { trackId, title, artist, imageUrl, previewUrl } = req.body;
    
    const playlist = await prisma.playlist.findUnique({ where: { id: playlistId } });
    if (!playlist || playlist.userId !== userId) return res.status(403).json({ error: 'Forbidden' });

    const existing = await prisma.playlistTrack.findUnique({
      where: { playlistId_trackId: { playlistId, trackId } }
    });

    if (existing) {
      return res.status(400).json({ error: 'Track already in playlist' });
    }

    await prisma.playlistTrack.create({
      data: { playlistId, trackId, title, artist, imageUrl, previewUrl }
    });
    res.status(201).json({ message: 'Track added successfully' });
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.delete('/api/playlists/:id/tracks/:trackId', authenticateToken, async (req, res) => {
  try {
    const playlistId = parseInt(req.params.id);
    const trackId = req.params.trackId;

    // Check ownership
    const playlist = await prisma.playlist.findFirst({
      where: { id: playlistId, userId: req.user.userId }
    });

    if (!playlist) {
      return res.status(404).json({ error: 'Playlist not found' });
    }

    await prisma.playlistTrack.delete({
      where: { playlistId_trackId: { playlistId, trackId } }
    });

    res.status(200).json({ message: 'Track removed successfully' });
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Moodify Backend is running on port ${PORT}`);
});
