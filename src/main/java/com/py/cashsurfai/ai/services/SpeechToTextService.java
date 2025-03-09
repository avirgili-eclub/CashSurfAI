package com.py.cashsurfai.ai.services;

//import com.google.cloud.speech.v1.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SpeechToTextService {

    private static final String PYTHON_PATH = "C:\\whisper_env\\Scripts\\python.exe"; // O especifica la ruta completa si está en el entorno virtual
    private static final String WHISPER_SCRIPT = "import whisper; model = whisper.load_model('small'); result = model.transcribe('%s'); print(result['text'])";
    private static final Pattern AUDIO_TEXT_PATTERN = Pattern.compile("(?s)(?:FutureWarning.*?(?:\\r?\\n)+)?(.+)$");

    public String convertAudioToText(byte[] audioBytes) throws IOException {
        // Usar una ruta con separadores Windows y escapada correctamente
        String tempAudioPath = "C:\\temp\\temp_" + System.currentTimeMillis() + ".wav";
        Files.write(Paths.get(tempAudioPath), audioBytes);

        // Depurar la ruta generada
        System.out.println("Temp audio path: " + tempAudioPath);
        try {
            // Escapar la ruta para Python usando raw string (r'...')
            String escapedPath = tempAudioPath.replace("\\", "\\\\");
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_PATH, "-c",
                    String.format(WHISPER_SCRIPT, escapedPath)
            );
            pb.redirectErrorStream(true); // Combinar stdout y stderr como antes
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Whisper process failed with exit code: " + exitCode + ". Output: " + output);
            }

            // Filtrar el texto del audio con regex
            String fullOutput = output.toString().trim();
            System.out.println("Full Whisper output: " + fullOutput); // Depuración

            return extractAudioText(fullOutput);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running Whisper", e);
        } finally {
            Files.deleteIfExists(Paths.get(tempAudioPath));
        }
    }

    private String extractAudioText(String rawTranscript) {
        String marker = "checkpoint = torch.load(fp, map_location=device)";
        int index = rawTranscript.indexOf(marker);
        if (index == -1) {
            return rawTranscript.trim(); // Si no encuentra el marcador, devuelve todo limpio
        }
        String codificadoUTF8 = new String(rawTranscript.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return codificadoUTF8.substring(index + marker.length()).trim(); // Toma desde después del marcador
    }

    public static void testPythonPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, "--version");
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            System.out.println(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Google Service
//    public String convertAudioToText(byte[] audioBytes) throws IOException {
//        try (SpeechClient speechClient = SpeechClient.create()) {
//            RecognitionConfig config = RecognitionConfig.newBuilder()
//                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16) // Ajusta según formato de audio
//                    .setSampleRateHertz(16000) // Ajusta según tu audio
//                    .setLanguageCode("es-ES") // Español, ajusta según necesidad
//                    .build();
//
//            RecognitionAudio audio = RecognitionAudio.newBuilder()
//                    .setContent(com.google.protobuf.ByteString.copyFrom(audioBytes))
//                    .build();
//
//            RecognizeResponse response = speechClient.recognize(config, audio);
//            if (response.getResultsList().isEmpty()) {
//                return null;
//            }
//            return response.getResultsList().get(0).getAlternativesList().get(0).getTranscript();
//        }
//    }
}