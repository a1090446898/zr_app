package com.gsoft.inventory.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;
import android.util.Log;

import com.gsoft.inventory.R;

/**
 * 盘点扫描语音提示播放器：扫描成功(saomiaochenggong)/重复(chongfu)/已盘(yipan)
 * <p>
 * 三种盘点方式（RFID/红外/摄像头）共用：页面创建时预加载，页面销毁时释放。
 * SoundPool 支持短音频预加载和低延迟重复播放，适合连续扫码场景。
 * <p>
 * 大批量扫描防轰炸：每种语音各自带最小播报间隔，窗口内的重复调用直接丢弃，
 * 避免扫描数量大时语音叠放、排队或长时间连续播报。
 *
 * @author 10904
 */
public class ScanSoundPlayer {

    private static final String TAG = "ScanSoundPlayer";

    /** 扫描成功提示最小间隔：连续扫到多件资产时避免语音叠在一起（页面连扫时会先降级为短哔） */
    private static final long SCAN_SUCCESS_MIN_INTERVAL_MS = 1200;
    /** 已盘提示最小间隔：连续扫到多个新资产时避免语音叠在一起 */
    private static final long YIPAN_MIN_INTERVAL_MS = 800;
    /** 重复提示最小间隔：避免同一已盘码被连续识别时反复播报 */
    private static final long CHONGFU_MIN_INTERVAL_MS = 1500;

    private final SoundPool soundPool;
    private int scanSuccessSoundId;
    private int yipanSoundId;
    private int chongfuSoundId;
    private boolean scanSuccessLoaded;
    private boolean yipanLoaded;
    private boolean chongfuLoaded;
    private boolean enabled = true;
    private boolean released;
    private long lastScanSuccessTime;
    private long lastYipanTime;
    private long lastChongfuTime;

    public ScanSoundPlayer(Context context) {
        // 铃声音量，AudioAttributes.USAGE_NOTIFICATION
        // 铃声类型，AudioAttributes.CONTENT_TYPE_SONIFICATION
        // 媒体音量，AudioAttributes.USAGE_MEDIA
        // 媒体类型，AudioAttributes.CONTENT_TYPE_SPEECH
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status != 0) {
                Log.w(TAG, "提示音加载失败 sampleId=" + sampleId + " status=" + status);
                return;
            }
            if (sampleId == scanSuccessSoundId) {
                scanSuccessLoaded = true;
            } else if (sampleId == yipanSoundId) {
                yipanLoaded = true;
            } else if (sampleId == chongfuSoundId) {
                chongfuLoaded = true;
            }
        });
        Context appContext = context.getApplicationContext();
        scanSuccessSoundId = soundPool.load(appContext, R.raw.saomiaochenggong, 1);
        yipanSoundId = soundPool.load(appContext, R.raw.yipan, 1);
        chongfuSoundId = soundPool.load(appContext, R.raw.chongfu, 1);
    }

    /** 开关：关闭后所有语音都不播放，立即生效 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 扫描成功语音：本次扫码首次命中的资产（含本次刚标记盘实与历史已盘首次扫到）
     * <p>
     * 返回是否真正开始播放：已释放/开关关闭/音频未加载完成/处于最小间隔内时返回false，
     * 由调用方决定是否用短哔兜底（连续快扫场景由页面主动降级，不依赖本方法返回值）
     */
    public boolean playScanSuccess() {
        if (!canPlay(scanSuccessLoaded)) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastScanSuccessTime < SCAN_SUCCESS_MIN_INTERVAL_MS) {
            return false;
        }
        lastScanSuccessTime = now;
        play(scanSuccessSoundId);
        return true;
    }

    /** 已盘语音：本次扫码把未盘资产自动置为已盘（当前首次命中统一播扫描成功音，本方法保留备用） */
    public void playCounted() {
        if (!canPlay(yipanLoaded)) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastYipanTime < YIPAN_MIN_INTERVAL_MS) {
            return;
        }
        lastYipanTime = now;
        play(yipanSoundId);
    }

    /** 重复语音：本次扫码会话内再次扫到同一已处理过的资产 */
    public void playDuplicate() {
        if (!canPlay(chongfuLoaded)) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastChongfuTime < CHONGFU_MIN_INTERVAL_MS) {
            return;
        }
        lastChongfuTime = now;
        play(chongfuSoundId);
    }

    /** 播放前置检查：已释放/开关关闭/音频未加载完成时不播放 */
    private boolean canPlay(boolean loaded) {
        return !released && enabled && loaded;
    }

    /** 播放音频：SoundPool低延迟播放，失败仅记录日志，不影响扫描业务 */
    private void play(int soundId) {
        int streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        if (streamId == 0) {
            Log.w(TAG, "提示音播放失败 soundId=" + soundId);
        }
    }

    /** 页面销毁时释放 SoundPool */
    public void release() {
        released = true;
        soundPool.release();
    }
}