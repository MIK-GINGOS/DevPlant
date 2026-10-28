import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet, TextInput, View } from 'react-native';

import { ask, syncSoon, useQuestions, useReport } from '@/bridge/reports';
import type { Question } from '@/contracts/reports';
import { useColors } from '@/mixes/theme';
import { Button, Card, dateTime, Page, Pill, Row, Txt } from '@/mixes/ui';

/**
 * Ask an extension officer (CropGuard): a question, maybe about one report, sent as soon as the
 * phone has signal, and the answers that came back. Opened as /help or /help?report=12.
 */
export default function Help() {
  const c = useColors();
  const { report: reportParam } = useLocalSearchParams<{ report?: string }>();
  const reportId = reportParam ? Number(reportParam) : null;
  const report = useReport(reportId);
  const questions = useQuestions();
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  // Fetch new answers; they show up here when they arrive.
  useEffect(() => syncSoon(), []);

  async function send() {
    if (text.trim().length < 3) return setError('Write your question first.');
    setError('');
    const words = text;
    setText('');
    await ask(words, report?.id ?? null);
  }

  return (
    <Page>
      <Txt type="title">Ask an extension officer</Txt>
      <Txt style={{ marginTop: 8 }}>
        Describe what you see: which plants, how many, since when. An officer answers here, usually within a few days.
      </Txt>

      {report && (
        <Card tint="leafSoft">
          <Txt type="label">About this report</Txt>
          <Txt type="heading" style={{ marginTop: 4 }}>
            {report.scanId == null ? 'Not recognised yet' : (report.resultName ?? 'Not diagnosed')}
          </Txt>
          <Txt type="caption">{[report.cropName, dateTime(report.createdAt)].filter(Boolean).join(' · ')}</Txt>
        </Card>
      )}

      <Card>
        <TextInput
          style={[styles.input, { color: c.ink, backgroundColor: c.surface, borderColor: c.line }]}
          placeholder="For example: Yellow streaks on young maize leaves in half the field since last week."
          placeholderTextColor={c.inkSoft}
          value={text}
          onChangeText={setText}
          multiline
          textAlignVertical="top"
          maxLength={1000}
        />
        {!!error && <Txt type="caption" style={{ marginTop: 4, color: c.severe }}>{error}</Txt>}
        <Button title="Send to an officer" style={{ marginTop: 10 }} onPress={send} />
        <Txt type="caption" style={{ marginTop: 8 }}>
          {report
            ? "Sent with this report's details: crop, result, how sure, date and area. No photos."
            : 'Sent with your country and district. No photos.'}
        </Txt>
      </Card>

      {questions.length > 0 && <Txt type="heading" style={{ marginTop: 24 }}>Your questions</Txt>}
      {questions.map(q => <QuestionCard key={q.id} q={q} />)}
    </Page>
  );
}

function QuestionCard({ q }: { q: Question }) {
  const c = useColors();
  return (
    <Card>
      <Txt>{q.text}</Txt>
      <Txt type="caption" style={{ marginTop: 4 }}>{[q.cropName, dateTime(q.askedAt)].filter(Boolean).join(' · ')}</Txt>
      {q.answer ? (
        <View style={[styles.answer, { backgroundColor: c.leafSoft }]}>
          <Txt>{q.answer}</Txt>
          <Txt type="caption" style={{ marginTop: 4 }}>
            {(q.answeredBy ?? 'Extension officer') + (q.answeredAt ? ' · ' + dateTime(q.answeredAt) : '')}
          </Txt>
        </View>
      ) : (
        <Row style={{ marginTop: 8 }}>
          <Pill
            text={q.sync === 'SENT' ? 'Sent. Waiting for an answer' : 'Waiting for signal to send'}
            color={q.sync === 'SENT' ? 'leaf' : 'unsure'}
          />
        </Row>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  input: { borderWidth: 1, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, fontSize: 16, minHeight: 96 },
  answer: { marginTop: 10, borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10 },
});
