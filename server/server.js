const express = require('express');
const cors = require('cors');
const ytsr = require('youtube-sr');
const youtubedl = require('yt-dlp-exec');

const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

const playlists = new Map();

function generatePlaylistId() {
    return Math.random().toString(36).substring(2, 15) +
           Math.random().toString(36).substring(2, 15);
}

app.post('/api/playlist', async (req, res) => {
    try {
        const { songs } = req.body;

        if (!songs || !Array.isArray(songs)) {
            return res.status(400).json({
                success: false,
                error: 'Geçerli şarkı listesi gereklidir.'
            });
        }

        const playlistId = generatePlaylistId();
        const playlist = {
            id: playlistId,
            songs: songs,
            currentIndex: 0,
            createdAt: new Date().toISOString()
        };

        playlists.set(playlistId, playlist);

        setTimeout(() => {
            playlists.delete(playlistId);
        }, 60 * 60 * 1000);

        res.json({
            success: true,
            playlistId: playlistId,
            totalSongs: songs.length
        });

    } catch (error) {
        console.error('Playlist oluşturma hatası:', error);
        res.status(500).json({
            success: false,
            error: 'Playlist oluşturulamadı'
        });
    }
});

app.get('/api/playlist/:playlistId/next', async (req, res) => {
    try {
        const { playlistId } = req.params;
        const playlist = playlists.get(playlistId);

        if (!playlist) {
            return res.status(404).json({
                success: false,
                error: 'Playlist bulunamadı'
            });
        }

        const nextIndex = (playlist.currentIndex + 1) % playlist.songs.length;
        playlist.currentIndex = nextIndex;

        const nextSong = playlist.songs[nextIndex];

        res.json({
            success: true,
            song: nextSong,
            currentIndex: nextIndex,
            totalSongs: playlist.songs.length,
            hasNext: nextIndex < playlist.songs.length - 1,
            hasPrevious: nextIndex > 0
        });

    } catch (error) {
        console.error('Next song hatası:', error);
        res.status(500).json({
            success: false,
            error: 'Sonraki şarkı alınamadı'
        });
    }
});

app.get('/api/playlist/:playlistId/previous', async (req, res) => {
    try {
        const { playlistId } = req.params;
        const playlist = playlists.get(playlistId);

        if (!playlist) {
            return res.status(404).json({
                success: false,
                error: 'Playlist bulunamadı'
            });
        }

        const prevIndex = playlist.currentIndex - 1 < 0 ?
            playlist.songs.length - 1 : playlist.currentIndex - 1;
        playlist.currentIndex = prevIndex;

        const previousSong = playlist.songs[prevIndex];

        res.json({
            success: true,
            song: previousSong,
            currentIndex: prevIndex,
            totalSongs: playlist.songs.length,
            hasNext: prevIndex < playlist.songs.length - 1,
            hasPrevious: prevIndex > 0
        });

    } catch (error) {
        console.error('Previous song hatası:', error);
        res.status(500).json({
            success: false,
            error: 'Önceki şarkı alınamadı'
        });
    }
});

app.get('/api/playlist/:playlistId/current', async (req, res) => {
    try {
        const { playlistId } = req.params;
        const playlist = playlists.get(playlistId);

        if (!playlist) {
            return res.status(404).json({
                success: false,
                error: 'Playlist bulunamadı'
            });
        }

        const currentSong = playlist.songs[playlist.currentIndex];

        res.json({
            success: true,
            song: currentSong,
            currentIndex: playlist.currentIndex,
            totalSongs: playlist.songs.length,
            hasNext: playlist.currentIndex < playlist.songs.length - 1,
            hasPrevious: playlist.currentIndex > 0
        });

    } catch (error) {
        console.error('Current song hatası:', error);
        res.status(500).json({
            success: false,
            error: 'Mevcut şarkı bilgisi alınamadı'
        });
    }
});

app.get('/api/song/:videoId', async (req, res) => {
    try {
        const { videoId } = req.params;

        const url = `https://www.youtube.com/watch?v=${videoId}`;
        const result = await youtubedl(url, {
            dumpJson: true,
            noCheckCertificates: true,
            noWarnings: true,
            skipDownload: true,
        });

        if (!result) {
            return res.status(404).json({
                success: false,
                error: 'Şarkı bilgisi bulunamadı'
            });
        }

        const duration = result.duration ? formatDuration(result.duration) : "N/A";

        res.json({
            success: true,
            song: {
                title: result.title || 'Bilinmeyen Şarkı',
                artist: result.uploader || 'Bilinmeyen Sanatçı',
                duration: duration,
                thumbnail: result.thumbnail || null,
                videoId: videoId
            }
        });

    } catch (error) {
        console.error('Şarkı bilgisi alma hatası:', error);
        res.status(500).json({
            success: false,
            error: 'Şarkı bilgisi alınamadı'
        });
    }
});

app.get('/api/search', async (req, res) => {
    const query = req.query.q;
    if (!query) {
        return res.status(400).json({ success: false, error: 'Arama sorgusu (q) gereklidir.' });
    }

    console.log(`Aranıyor: "${query}"`);

    try {
        let searchFunction = null;

        if (typeof ytsr === 'function') {
            searchFunction = ytsr;
        } else if (ytsr && typeof ytsr.search === 'function') {
            searchFunction = ytsr.search;
        } else if (ytsr && ytsr.default && typeof ytsr.default.search === 'function') {
            searchFunction = ytsr.default.search;
        } else if (ytsr && ytsr.default && typeof ytsr.default === 'function') {
            searchFunction = ytsr.default;
        }

        if (typeof searchFunction !== 'function') {
            throw new Error("youtube-sr arama fonksiyonu bulunamadı.");
        }

        let searchResults = await searchFunction(query, {
            limit: 25,
            type: 'video'
        });

        const results = searchResults
            .filter(video =>
                video &&
                video.id &&
                video.title
            )
            .slice(0, 20)
            .map(video => {
                const authorName = video.channel && video.channel.name
                    ? video.channel.name
                    : 'Bilinmeyen Sanatçı';

                return {
                    title: video.title || 'Başlık Yok',
                    author: authorName,
                    duration: formatDuration(video.duration),
                    thumbnail: video.thumbnail && video.thumbnail.url ? video.thumbnail.url : null,
                    videoId: video.id
                };
            });

        console.log(`${results.length} sonuç bulundu.`);

        return res.json({
            result: results,
            success: true,
            query: query,
            totalResults: results.length
        });

    } catch (error) {
        console.error(`Arama hatası: ${error.message}`);
        res.status(500).json({ success: false, error: `Arama işlemi başarısız oldu: ${error.message}` });
    }
});

app.get('/api/stream', async (req, res) => {
    const videoId = req.query.videoId;

    if (!videoId) {
        return res.status(400).json({
            success: false,
            error: 'videoId parametresi eksik.'
        });
    }

    console.log(`Audio stream: ${videoId}`);

    try {
        const url = `https://www.youtube.com/watch?v=${videoId}`;

        const result = await youtubedl(url, {
            dumpJson: true,
            noCheckCertificates: true,
            noWarnings: true,
            format: 'bestaudio/best',
            skipDownload: true,
        });

        if (result && result.url) {
            console.log(`Audio URL bulundu: ${result.ext} - ${result.abr}kbps`);

            return res.json({
                success: true,
                streamUrl: result.url,
                videoId: videoId,
                bitrate: result.abr,
                format: result.ext,
                type: "direct_audio"
            });
        }

        throw new Error("Audio URL bulunamadı");

    } catch (error) {
        console.error('yt-dlp hatası:', error.message);
        res.status(500).json({
            success: false,
            error: `Audio stream alınamadı: ${error.message}`
        });
    }
});

function formatDuration(duration) {
    if (!duration) return "N/A";
    if (typeof duration === 'string' && duration.includes(':')) return duration;
    if (typeof duration === 'number') {
        const minutes = Math.floor(duration / 60);
        const seconds = duration % 60;
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    }
    return "N/A";
}

app.listen(port, () => {
    console.log(`SoundBex Backend http://localhost:${port} adresinde çalışıyor`);
    console.log(`Android için: http://10.0.2.2:${port}`);
    console.log(`Yeni özellikler: Playlist yönetimi, previous/next, progress bar desteği`);
});