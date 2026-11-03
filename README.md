# Moodify - Your Personalized Mood-Based Music Companion

Moodify is an innovative music application that bridges the gap between your emotions and your music library. Using advanced facial recognition and AI-driven recommendations, Moodify identifies your current mood and curates the perfect playlist to match your vibe.

---

## 🚀 Key Features

### 1. Mood Detection via Facial Recognition
- **Face Scan**: Uses Google's ML Kit and Android's CameraX to analyze facial expressions in real-time.
- **Smart Mapping**: Maps your expression to moods like Happy, Chill, Workout, or Focus.

### 2. Personalized Recommendations
- **Dynamic Playlists**: Fetches high-quality tracks based on your detected mood and preferred language (English, Hindi, Punjabi, etc.).
- **Global Search**: Search for millions of tracks powered by the iTunes Search API.

### 3. Advanced Music Player
- **Sleek UI**: Beautifully designed MiniPlayer and FullScreenPlayer.
- **Dominant Color Extraction**: The player UI background dynamically changes based on the album art of the currently playing track.
- **Lyrics Support**: View lyrics of your favorite songs directly within the app.

### 4. AI-Powered "Ask Grok"
- **Similar Song Suggestions**: Powered by Google Gemini AI, this feature recommends tracks similar to what you are currently listening to with just a tap.

### 5. Library Management
- **Liked Songs**: Save your favorite tracks for quick access.
- **History**: Keep track of your recently played vibes.

---

## 🛠 Tech Stack

### Frontend (Android App)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Asynchronous Programming**: Kotlin Coroutines & Flow
- **Image Loading**: Coil
- **Networking**: Retrofit & OkHttp
- **Database**: Room (for local caching)
- **ML/Camera**: ML Kit Face Detection & CameraX
- **Theme**: Material Design 3 (Dynamic Light/Dark mode)

### Backend (Node.js Server)
- **Runtime**: Node.js
- **Framework**: Express.js
- **ORM**: Prisma
- **Authentication**: JWT (JSON Web Tokens) & Bcrypt
- **APIs Integrated**:
  - iTunes Search API (Music streaming data)
  - Google Gemini API (AI recommendations)

---

## 📂 Project Structure

- `/Moodify`: Android client source code (Kotlin, Jetpack Compose).
- `/backend`: Node.js Express server source code.
- `/backend/prisma`: Database schema and migrations.

---

## ⚙️ Setup Instructions

### Backend Setup
1. Navigate to the `backend` folder:
   ```bash
   cd backend
   ```
2. Run `npm install` to install dependencies.
3. Create a `.env` file from `.env.example`:
   ```bash
   cp .env.example .env
   ```
4. Configure your `.env` variables:
   ```env
   PORT=3000
   DATABASE_URL="file:./prisma/dev.db"
   JWT_SECRET=your_jwt_secret
   GEMINI_API_KEY=your_gemini_api_key
   ```
5. Run `npx prisma db push` or `npx prisma migrate dev` to setup the database.
6. Start the server:
   ```bash
   npm start
   ```

### Android Setup
1. Open the `Moodify` folder in Android Studio.
2. Ensure you have the latest Android SDK and Gradle setup.
3. Update the `BASE_URL` in the Retrofit client (if necessary) to point to your backend server IP (or use local tunnel via `start-tunnel.js`).
4. Build and Run the app on an Android device or emulator.

---

## 📄 License
This project is licensed under the ISC License.

---
Created by [Nikhil Bhati](https://github.com/Nikhilbhati007)
