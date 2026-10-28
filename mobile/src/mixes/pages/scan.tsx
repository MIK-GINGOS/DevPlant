import { CameraView, useCameraPermissions } from 'expo-camera';
import * as ImagePicker from 'expo-image-picker';
import { router } from 'expo-router';
import { useRef, useState } from 'react';
import { ActivityIndicator, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { scanLeaf } from '@/bridge/reports';
import { useFields } from '@/bridge/store';
import { useColors } from '@/mixes/theme';
import { Button, Card, Chip, ChipRow, ISLAND_CLEARANCE, Txt } from '@/mixes/ui';

/** Camera that frames one leaf. The photo stays on the phone. */
export default function ScanPage({ active }: { active: boolean }) {
  const c = useColors();
  const camera = useRef<CameraView>(null);
  const [permission, requestPermission] = useCameraPermissions();
  const [ready, setReady] = useState(false);
  const [busy, setBusy] = useState(false);
  const [banner, setBanner] = useState<string | null>(null);
  const [fieldId, setFieldId] = useState<number | null>(null);
  const fields = useFields();
  // A camera that is not on screen is not ready; it signals again when it comes back.
  if (!active && ready) setReady(false);
  const selected = fields.some(f => f.id === fieldId) ? fieldId : null;

  async function run(uri: string) {
    setBusy(true);
    setBanner(null);
    try {
      const outcome = await scanLeaf(uri, selected);
      if (outcome.retake) setBanner(outcome.reason);
      else router.push(`/result/${outcome.scan.id}`);
    } catch {
      setBanner('Could not read that photo. Try another one.');
    } finally {
      setBusy(false);
    }
  }

  async function takePhoto() {
    if (!camera.current || !ready) return;
    try {
      setBusy(true);
      const photo = await camera.current.takePictureAsync({ quality: 0.8 });
      await run(photo.uri);
    } catch {
      setBusy(false);
      setBanner('Could not take the photo. Try again.');
    }
  }

  async function pickPhoto() {
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: 'images', quality: 0.9 });
    if (!result.canceled && result.assets[0]) await run(result.assets[0].uri);
  }

  const granted = permission?.granted === true;

  return (
    <View style={{ flex: 1, backgroundColor: c.bg }}>
      <View style={styles.stage}>
        {/* The camera runs only while this page is on screen. */}
        {granted && active && (
          <CameraView
            ref={camera}
            style={StyleSheet.absoluteFill}
            facing="back"
            onCameraReady={() => setReady(true)}
            onMountError={() => setBanner('Camera is not available. You can still pick a photo.')}
          />
        )}
        {granted && <LeafFrame />}

        {permission && !granted && (
          <View style={styles.center}>
            <Card style={{ marginHorizontal: 24 }}>
              <Txt type="heading">Camera needed</Txt>
              <Txt style={{ marginTop: 6 }}>
                DevPlant photographs leaves with the camera. Photos stay on this phone. You can also pick an existing photo.
              </Txt>
              <Button title="Allow camera" style={{ marginTop: 12 }} onPress={requestPermission} />
            </Card>
          </View>
        )}

        {banner && (
          <Pressable onPress={() => setBanner(null)} style={[styles.banner, { backgroundColor: c.moderate }]}>
            <Text style={styles.bannerText}>{banner}</Text>
          </Pressable>
        )}

        <View style={styles.controls}>
          {fields.length > 0 && (
            <View style={{ paddingHorizontal: 16 }}>
              <ChipRow>
                <Chip text="No field" selected={selected == null} onPress={() => setFieldId(null)} />
                {fields.map(f => (
                  <Chip key={f.id} text={`${f.name} · ${f.cropName}`} selected={selected === f.id} onPress={() => setFieldId(f.id)} />
                ))}
              </ChipRow>
            </View>
          )}
          <View style={styles.buttons}>
            <Pressable accessibilityLabel="Pick a photo" onPress={pickPhoto} disabled={busy} style={[styles.round, styles.gallery]}>
              <Image source={require('@/assets/icons/gallery.png')} style={{ width: 24, height: 24, tintColor: '#FFFFFF' }} />
            </Pressable>
            <Pressable
              accessibilityLabel="Take photo"
              onPress={granted ? takePhoto : requestPermission}
              disabled={busy}
              style={({ pressed }) => [styles.round, styles.shutter, pressed && { opacity: 0.7 }]}>
              <Image source={require('@/assets/icons/scan.png')} style={{ width: 30, height: 30, tintColor: c.leaf }} />
            </Pressable>
            <View style={{ width: 52 }} />
          </View>
        </View>

        {busy && (
          <View style={[StyleSheet.absoluteFill, styles.center, { backgroundColor: c.scrim }]}>
            <ActivityIndicator color="#FFFFFF" size="large" />
            <Text style={[styles.bannerText, { marginTop: 12, fontSize: 17, fontWeight: '700' }]}>Checking the leaf…</Text>
          </View>
        )}
      </View>
    </View>
  );
}

/** A rounded square guide in the middle of the camera view. */
function LeafFrame() {
  return (
    <View pointerEvents="none" style={[StyleSheet.absoluteFill, styles.center]}>
      <View style={styles.frame} />
      <Text style={styles.frameHint}>Fill the frame with one leaf</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  stage: { flex: 1, backgroundColor: '#000000', overflow: 'hidden' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  frame: { width: '72%', aspectRatio: 1, borderRadius: 28, borderWidth: 3, borderColor: 'rgba(255,255,255,0.85)', borderCurve: 'continuous', marginTop: -120 },
  frameHint: { color: '#FFFFFF', marginTop: 12, fontSize: 14, fontWeight: '600', textShadowColor: 'rgba(0,0,0,0.6)', textShadowRadius: 4 },
  banner: { position: 'absolute', top: 12, left: 16, right: 16, borderRadius: 16, paddingHorizontal: 16, paddingVertical: 12 },
  bannerText: { color: '#FFFFFF', fontSize: 15, lineHeight: 21 },
  controls: { position: 'absolute', left: 0, right: 0, bottom: ISLAND_CLEARANCE },
  buttons: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 28, paddingTop: 12 },
  round: { alignItems: 'center', justifyContent: 'center' },
  gallery: { width: 52, height: 52, borderRadius: 26, backgroundColor: 'rgba(255,255,255,0.35)' },
  shutter: { width: 76, height: 76, borderRadius: 38, backgroundColor: '#FFFFFF' },
});
