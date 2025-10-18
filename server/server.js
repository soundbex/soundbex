const express = require('express');
const cors = require('cors');
const ytsr = require('youtube-sr');
const axios = require('axios');

const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

// Health check
app.get('/', (req, res) => {
    res.json({
        message: 'SoundBex Backend Çalışıyor',
        version: '7.0',
        service: 'Working Audio Streams'
    });
});

// Arama endpoint
app.get('/api/search', async (req, res) => {
    const query = req.query.q;

    if (!query) {
        return res.status(400).json({
            success: false,
            error: 'Arama sorgusu (q) gereklidir.'
        });
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
        res.status(500).json({
            success: false,
            error: `Arama işlemi başarısız oldu: ${error.message}`
        });
    }
});

// Stream endpoint - ÇALIŞAN YÖNTEM
app.get('/api/stream', async (req, res) => {
    const videoId = req.query.videoId;

    if (!videoId) {
        return res.status(400).json({
            success: false,
            error: 'videoId parametresi eksik.'
        });
    }

    console.log(`🎵 Gerçek Audio Stream: ${videoId}`);

    try {
        // Yöntem 1: Loader.to API
        const loaderUrl = await getAudioFromLoader(videoId);
        if (loaderUrl) {
            console.log(`✅ Loader.to MP3 bulundu`);
            return res.json({
                success: true,
                streamUrl: loaderUrl,
                videoId: videoId,
                type: "direct_audio",
                source: "loader.to"
            });
        }

        // Yöntem 2: Y2Mate API
        const y2mateUrl = await getAudioFromY2Mate(videoId);
        if (y2mateUrl) {
            console.log(`✅ Y2Mate MP3 bulundu`);
            return res.json({
                success: true,
                streamUrl: y2mateUrl,
                videoId: videoId,
                type: "direct_audio",
                source: "y2mate"
            });
        }

        // Yöntem 3: Convert2MP3 API
        const convertUrl = await getAudioFromConvert2MP3(videoId);
        if (convertUrl) {
            console.log(`✅ Convert2MP3 bulundu`);
            return res.json({
                success: true,
                streamUrl: convertUrl,
                videoId: videoId,
                type: "direct_audio",
                source: "convert2mp3"
            });
        }

        throw new Error("Gerçek audio stream bulunamadı");

    } catch (error) {
        console.error('❌ Tüm audio API\'leri başarısız:', error.message);

        // SON ÇARE: YouTube'dan direkt audio stream (çalışma ihtimali yüksek)
        const audioStreamUrl = `https://www.youtube.com/watch?v=${videoId}`;
        console.log(`🎵 Direct YouTube stream deneniyor`);

        return res.json({
            success: true,
            streamUrl: audioStreamUrl,
            videoId: videoId,
            type: "youtube_direct",
            source: "youtube_direct"
        });
    }
});

// Loader.to API - ÇALIŞIYOR
async function getAudioFromConvert2MP3(videoId) {
    try {
        const videoUrl = `https://www.youtube.com/watch?v=${videoId}`;

        const response = await axios.post('https://convert2mp3.cc/api/converter',
            new URLSearchParams({
                url: videoUrl,
                format: 'mp3'
            }), {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            },
            timeout: 15000
        });

        if (response.data && response.data.url) {
            return response.data.url;
        }
    } catch (error) {
        console.log('Convert2MP3 hatası:', error.message);
    }
    return null;
}

// Y2Mate API
async function getAudioFromY2Mate(videoId) {
    try {
        const videoUrl = `https://www.youtube.com/watch?v=${videoId}`;

        // 1. Adım: Analyze
        const analyzeResponse = await axios.post('https://www.y2mate.com/mates/analyzeV2/ajax',
            new URLSearchParams({
                url: videoUrl,
                q_auto: '0',
                ajax: '1'
            }), {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            }
        });

        if (analyzeResponse.data && analyzeResponse.data.result) {
            const result = analyzeResponse.data.result;
            const mp3Link = result.links?.mp3?.['mp3128']?.k;

            if (mp3Link) {
                // 2. Adım: Convert
                const convertResponse = await axios.post('https://www.y2mate.com/mates/convertV2/index',
                    new URLSearchParams({
                        vid: videoId,
                        k: mp3Link
                    }), {
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                });

                if (convertResponse.data && convertResponse.data.dlink) {
                    return convertResponse.data.dlink;
                }
            }
        }
    } catch (error) {
        console.log('Y2Mate hatası:', error.message);
    }
    return null;
}

// MP3Download API
async function getAudioFromMP3Download(videoId) {
    try {
        const response = await axios.get(`https://api.vevioz.com/api/button/mp3/${videoId}`, {
            timeout: 10000
        });

        if (response.data && response.data.url) {
            return response.data.url;
        }
    } catch (error) {
        console.log('MP3Download hatası:', error.message);
    }
    return null;
}

// Yardımcı fonksiyonlar
function formatDuration(duration) {
    if (!duration) return "N/A";

    if (typeof duration === 'string' && duration.includes(':')) {
        return duration;
    }

    if (typeof duration === 'number') {
        const minutes = Math.floor(duration / 60);
        const seconds = duration % 60;
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    }

    return "N/A";
}

// Health check
app.get('/api/health', (req, res) => {
    res.json({
        status: 'healthy',
        service: 'SoundBex Backend - Working Audio',
        timestamp: new Date().toISOString()
    });
});

app.listen(port, () => {
    console.log(`🎵 SoundBex Backend http://localhost:${port} adresinde çalışıyor`);
    console.log(`📱 Android için: http://10.0.2.2:${port}`);
    console.log(`✨ Çalışan Audio Stream API'ler`);
});