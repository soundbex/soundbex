const express = require('express');
const cors = require('cors');
const ytsr = require('youtube-sr');
const youtubedl = require('yt-dlp-exec');

const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

app.get('/api/search', async (req, res) => {
    const query = req.query.q;
    if (!query) {
        return res.status(400).json({ success: false, error: 'Arama sorgusu (q) gereklidir.' });
    }

    console.log(`🔍 Aranıyor: "${query}"`);

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

        console.log(`✅ ${results.length} sonuç bulundu.`);

        return res.json({
            result: results,
            success: true,
            query: query,
            totalResults: results.length
        });

    } catch (error) {
        console.error(`❌ Arama hatası: ${error.message}`);
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

    console.log(`🎵 Gerçek Audio: ${videoId}`);

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
            console.log(`✅ Gerçek audio URL bulundu: ${result.ext} - ${result.abr}kbps`);

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
        console.error('❌ yt-dlp hatası:', error.message);
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
    console.log(`🎵 SoundBex Backend http://localhost:${port} adresinde çalışıyor`);
    console.log(`📱 Android için: http://10.0.2.2:${port}`);
    console.log(`✨ yt-dlp ile gerçek audio stream`);
});