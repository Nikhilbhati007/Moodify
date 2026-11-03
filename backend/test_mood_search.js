async function testMood(mood, lang) {
  const mappedMood = mood.toLowerCase() === 'neutral' ? 'Chill' : mood;
  let searchTerm = `${lang} ${mappedMood} song`;
  if (lang.toLowerCase() === 'hindi') {
    searchTerm = `hindi ${mappedMood} bollywood`;
  } else if (lang.toLowerCase() === 'punjabi') {
    searchTerm = `punjabi ${mappedMood}`;
  }

  const url = `https://itunes.apple.com/search?term=${encodeURIComponent(searchTerm)}&entity=song&limit=5&country=IN`;
  console.log(`Mood Recommendation Search for mood="${mood}" (${lang}) -> Search Term: "${searchTerm}"`);
  try {
    const res = await fetch(url);
    const data = await res.json();
    const tracks = data.results.map(track => ({
      title: track.trackName,
      artist: track.artistName,
      hasPreview: !!track.previewUrl
    })).filter(t => t.hasPreview);
    
    console.log(`Found ${tracks.length} tracks with previews:`);
    tracks.forEach((track, i) => {
      console.log(`  ${i+1}. ${track.title} by ${track.artist}`);
    });
  } catch (err) {
    console.error(err);
  }
  console.log("------------------------------------------");
}

async function run() {
  await testMood("Happy", "Hindi");
  await testMood("Sad", "Hindi");
  await testMood("Neutral", "Hindi");
  await testMood("Energetic", "Hindi");
  await testMood("Calm", "Hindi");
  await testMood("Happy", "Punjabi");
  await testMood("Sad", "Punjabi");
}

run();
